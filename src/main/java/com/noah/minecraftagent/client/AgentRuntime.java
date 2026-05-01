package com.noah.minecraftagent.client;

import com.noah.minecraftagent.client.physical.PhysicalAction;
import com.noah.minecraftagent.client.physical.PhysicalActionExecutor;
import com.noah.minecraftagent.common.billing.BillingManager;
import com.noah.minecraftagent.common.cache.CacheManager;
import com.noah.minecraftagent.common.config.AgentConfig;
import com.noah.minecraftagent.common.config.AgentConfigStore;
import com.noah.minecraftagent.common.config.AgentProfile;
import com.noah.minecraftagent.common.config.RequestRequirements;
import com.noah.minecraftagent.common.network.ExecuteCommandPayload;
import com.noah.minecraftagent.common.provider.*;
import com.noah.minecraftagent.common.tool.CommandSafety;
import com.noah.minecraftagent.common.tool.ToolParser;
import com.noah.minecraftagent.common.util.SecureLog;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class AgentRuntime {
    private final ExecutorService executor = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "MinecraftAgent-Worker");
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((t, e) ->
                SecureLog.error("Uncaught exception in agent worker thread " + t.getName(), e));
        return thread;
    });
    private final ChannelRouter router = new ChannelRouter();
    private final CacheManager cache = new CacheManager();
    private final BillingManager billing = new BillingManager();
    private final ChatProvider provider = new OpenAiCompatibleProvider();
    private final ClientContextCollector contextCollector = new ClientContextCollector();
    private final ScreenshotCapture screenshotCapture = new ScreenshotCapture();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private Consumer<RuntimeUpdate> listener = update -> {};
    private volatile String lastObservation = "";

    public void setListener(Consumer<RuntimeUpdate> listener) {
        this.listener = listener == null ? update -> {} : listener;
    }

    public double estimateCurrentCost(String prompt) {
        AgentProfile profile = AgentConfigStore.getInstance().config().activeProfile();
        int input = TokenEstimator.estimate(profile.systemPrompt + prompt) + TokenEstimator.estimate(contextCollector.collectJson(lastObservation));
        if (profile.visionEnabled && profile.capabilities.supportsVision) {
            input += TokenEstimator.estimateImageTokens(AgentConfigStore.getInstance().config().screenshotMaxWidth);
        }
        return billing.estimate(profile, input, Math.min(profile.maxTokens, 256));
    }

    public void cancel() {
        cancelled.set(true);
        publish(AgentStatus.ERROR, "Stopped", "", "", false, estimateCurrentCost(""));
    }

    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public void submit(String goal) {
        cancelled.set(false);
        CompletableFuture.runAsync(() -> runLoop(goal), executor);
    }

    private void runLoop(String goal) {
        AgentConfig config = AgentConfigStore.getInstance().config();
        String observation = lastObservation;
        for (int step = 1; step <= Math.max(1, config.activeProfile().maxAgentSteps); step++) {
            if (cancelled.get()) {
                return;
            }
            try {
                publish(AgentStatus.ROUTING, "Routing step " + step, "", "", false, 0);
                AgentProfile profile = selectProfile(config, goal);
                if (!profile.isComplete()) {
                    publish(AgentStatus.ERROR, "Active profile is incomplete", "", profile.name, false, 0);
                    return;
                }
                if (billing.isBlocked(profile)) {
                    publish(AgentStatus.BLOCKED, "Daily limit reached", "", profile.name, false, billing.todayCostCny());
                    return;
                }

                String currentObservation = observation;
                ScreenshotCapture.CapturedScreenshot screenshot = captureIfEnabled(profile).join();
                String contextJson = runOnClient(() -> contextCollector.collectJson(currentObservation)).join();
                ChatRequest request = buildRequest(profile, goal, contextJson, screenshot, observation, step);
                String cacheKey = cacheKey(request, goal, contextJson);
                if (profile.cacheEnabled && canReadResponseCache(profile)) {
                    publish(AgentStatus.CACHE_LOOKUP, "Checking cache", "", profile.name, false, billing.estimate(profile, request.estimatedInputTokens, 0));
                    Optional<ChatResponse> cached = cache.readResponse(cacheKey);
                    if (cached.isPresent()) {
                        ChatResponse response = cached.get();
                        publish(AgentStatus.CACHED, response.text, response.text, profile.name, true, billing.todayCostCny());
                        if (!handleToolCalls(response, profile)) {
                            lastObservation = response.text;
                            return;
                        }
                        observation = "Cached response executed";
                        continue;
                    }
                }

                long started = System.currentTimeMillis();
                publish(request.stream ? AgentStatus.STREAMING : AgentStatus.THINKING, "Requesting " + profile.name, "", profile.name, false,
                        billing.estimate(profile, request.estimatedInputTokens, profile.maxTokens));
                StringBuilder streamed = new StringBuilder();
                ChatResponse response = provider.complete(request, new StreamListener() {
                    @Override
                    public void onToken(String token) {
                        streamed.append(token);
                        if (streamed.length() < 200) {
                            runOnClient(() -> {
                                publish(AgentStatus.STREAMING, streamed.toString(), streamed.toString(), profile.name, false,
                                        billing.estimate(profile, request.estimatedInputTokens, TokenEstimator.estimate(streamed.toString())));
                                return null;
                            });
                        }
                    }

                    @Override
                    public void onStatus(String status) {
                        runOnClient(() -> {
                            publish(AgentStatus.THINKING, status, "", profile.name, false, 0);
                            return null;
                        });
                    }
                }, cancelled, executor).join();
                router.recordSuccess(profile, System.currentTimeMillis() - started);
                billing.record(profile, response.usage);
                if (profile.cacheEnabled && canWriteResponseCache(profile, response)) {
                    cache.writeResponse(cacheKey, response);
                }
                publish(AgentStatus.DONE, response.text, response.text, profile.name, response.cached, billing.todayCostCny());
                if (!handleToolCalls(response, profile)) {
                    lastObservation = response.text;
                    return;
                }
                observation = lastObservation;
            } catch (Exception exception) {
                AgentProfile active = config.activeProfile();
                router.recordFailure(active, config.providerHealthCooldownSeconds);
                SecureLog.error("Agent step failed", exception);
                publish(AgentStatus.ERROR, SecureLog.mask(exception.getMessage()), "", active.name, false, billing.todayCostCny());
                return;
            }
        }
        publish(AgentStatus.DONE, "Agent reached max steps", "", config.activeProfile().name, false, billing.todayCostCny());
    }

    private AgentProfile selectProfile(AgentConfig config, String goal) {
        RequestRequirements requirements = new RequestRequirements();
        requirements.needsStreaming = true;
        requirements.needsVision = config.activeProfile().visionEnabled;
        requirements.needsToolCalls = true;
        requirements.estimatedContextTokens = TokenEstimator.estimate(goal) + 2000;
        AgentProfile selected = router.select(config, requirements);
        if (!selected.capabilities.supportsVision) {
            selected.visionEnabled = false;
        }
        return selected;
    }

    private CompletableFuture<ScreenshotCapture.CapturedScreenshot> captureIfEnabled(AgentProfile profile) {
        if (!profile.visionEnabled || !profile.capabilities.supportsVision) {
            return CompletableFuture.completedFuture(null);
        }
        publish(AgentStatus.CAPTURE, "Capturing screenshot", "", profile.name, false, 0);
        return runOnClient(() -> screenshotCapture.takeScreenshotOnMain())
                .thenApplyAsync(ni -> screenshotCapture.processAsync(ni), executor);
    }

    private ChatRequest buildRequest(AgentProfile profile, String goal, String contextJson, ScreenshotCapture.CapturedScreenshot screenshot, String observation, int step) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", profile.systemPrompt + "\nYou control the player entity directly. Available physical actions: look_at(x,y,z) to face direction, walk_to(x,y,z) to move, mine_block(x,y,z) to break blocks, place_block(x,y,z,face) to place blocks, use_item() to use held item, jump() to jump, sneak(on) to crouch, get_position() to check location. Stop when the goal is complete. Return tool_calls to execute actions."));
        messages.add(new ChatMessage("user", "Goal: " + goal + "\nStep: " + step + "\nContext JSON: " + contextJson + "\nLast observation: " + observation));
        ChatRequest request = new ChatRequest();
        request.profile = profile;
        request.messages = messages;
        request.screenshotBase64Jpeg = screenshot == null ? null : screenshot.base64Jpeg();
        request.screenshotHash = screenshot == null ? "no-image" : screenshot.hash();
        request.stream = profile.streamingEnabled && profile.capabilities.supportsStreaming;
        request.toolsEnabled = profile.toolCallsEnabled && profile.capabilities.supportsToolCalls;
        request.estimatedInputTokens = messages.stream().mapToInt(message -> TokenEstimator.estimate(message.content())).sum();
        if (screenshot != null) {
            request.estimatedInputTokens += TokenEstimator.estimateImageTokens(screenshot.width());
        }
        return request;
    }

    private boolean handleToolCalls(ChatResponse response, AgentProfile profile) {
        if (response.toolCalls.isEmpty()) {
            return false;
        }
        publish(AgentStatus.ACTING, "Executing " + response.toolCalls.size() + " tool call(s)", response.text, profile.name, response.cached, billing.todayCostCny());
        boolean anyExecuted = false;
        for (AgentToolCall call : response.toolCalls) {
            PhysicalAction action = parsePhysicalAction(call);
            if (action == null) {
                continue;
            }
            anyExecuted = true;
            MinecraftClient.getInstance().execute(() ->
                    PhysicalActionExecutor.execute(action,
                            update -> publish(update.status(), update.message(), update.text(), update.channel(), update.cached(), update.costCny())));
        }
        if (!anyExecuted) {
            lastObservation = response.text.isBlank() ? "No executable action found in response" : response.text;
            publish(AgentStatus.OBSERVING, lastObservation, response.text, profile.name, response.cached, billing.todayCostCny());
            return false;
        }
        publish(AgentStatus.OBSERVING, lastObservation, response.text, profile.name, response.cached, billing.todayCostCny());
        return true;
    }

    private PhysicalAction parsePhysicalAction(AgentToolCall call) {
        return switch (call.name()) {
            case "look_at" -> {
                var a = parseCoords(call.argumentsJson());
                yield a == null ? null : new PhysicalAction.LookAt(a[0], a[1], a[2]);
            }
            case "walk_to" -> {
                var a = parseCoords(call.argumentsJson());
                yield a == null ? null : new PhysicalAction.WalkTo(a[0], a[1], a[2]);
            }
            case "mine_block" -> {
                var a = parseCoords(call.argumentsJson());
                yield a == null ? null : new PhysicalAction.MineBlock(BlockPos.ofFloored(a[0], a[1], a[2]));
            }
            case "place_block" -> {
                var obj = new com.google.gson.Gson().fromJson(call.argumentsJson(), com.google.gson.JsonObject.class);
                if (obj == null) yield null;
                int x = obj.has("x") ? obj.get("x").getAsInt() : 0;
                int y = obj.has("y") ? obj.get("y").getAsInt() : 0;
                int z = obj.has("z") ? obj.get("z").getAsInt() : 0;
                String f = obj.has("face") ? obj.get("face").getAsString() : "up";
                yield new PhysicalAction.PlaceBlock(BlockPos.ofFloored(x, y, z), Direction.byName(f));
            }
            case "use_item" -> new PhysicalAction.UseItem();
            case "jump" -> new PhysicalAction.Jump();
            case "sneak" -> {
                var obj = new com.google.gson.Gson().fromJson(call.argumentsJson(), com.google.gson.JsonObject.class);
                boolean on = obj != null && obj.has("on") && obj.get("on").getAsBoolean();
                yield new PhysicalAction.Sneak(on);
            }
            case "get_position" -> new PhysicalAction.GetPosition();
            default -> null;
        };
    }

    private static double[] parseCoords(String json) {
        var obj = new com.google.gson.Gson().fromJson(json, com.google.gson.JsonObject.class);
        if (obj == null) return null;
        return new double[]{
                obj.has("x") ? obj.get("x").getAsDouble() : 0,
                obj.has("y") ? obj.get("y").getAsDouble() : 0,
                obj.has("z") ? obj.get("z").getAsDouble() : 0
        };
    }

    private boolean canReadResponseCache(AgentProfile profile) {
        return profile.temperature == 0.0D || profile.allowResponseCacheWhenNonDeterministic;
    }

    private boolean canWriteResponseCache(AgentProfile profile, ChatResponse response) {
        return canReadResponseCache(profile) && !response.usage.estimated();
    }

    private String cacheKey(ChatRequest request, String goal, String contextJson) {
        return CacheManager.key(request.profile.id + "|" + request.profile.model + "|" + request.profile.systemPrompt + "|" + goal + "|" + contextJson + "|" + request.screenshotHash + "|" + request.toolsEnabled + "|" + request.profile.temperature);
    }

    private <T> CompletableFuture<T> runOnClient(CheckedSupplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        MinecraftClient.getInstance().execute(() -> {
            try {
                future.complete(supplier.get());
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    private void publish(AgentStatus status, String message, String text, String channel, boolean cached, double costCny) {
        RuntimeUpdate update = new RuntimeUpdate(status, message == null ? "" : message, text == null ? "" : text, channel == null ? "" : channel, cached, costCny);
        listener.accept(update);
    }

    public record RuntimeUpdate(AgentStatus status, String message, String text, String channel, boolean cached, double costCny) {
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
