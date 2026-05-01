package com.noah.minecraftagent.common.provider;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.noah.minecraftagent.common.config.AgentConfigStore;
import com.noah.minecraftagent.common.config.AgentProfile;
import com.noah.minecraftagent.common.util.SecureLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public final class OpenAiCompatibleProvider implements ChatProvider {
    private final Gson gson = new Gson();

    @Override
    public CompletableFuture<ChatResponse> complete(ChatRequest request, StreamListener listener, AtomicBoolean cancelled, Executor executor) {
        int timeoutSeconds = AgentConfigStore.getInstance().config().requestTimeoutSeconds;
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (request.stream && request.profile.streamingEnabled && request.profile.capabilities.supportsStreaming) {
                    try {
                        return stream(request, listener, cancelled);
                    } catch (Exception exception) {
                        if (!AgentConfigStore.getInstance().config().fallbackStreamingToNonStreaming || cancelled.get()) {
                            throw exception;
                        }
                        listener.onStatus("Streaming failed, retrying non-streaming");
                        ChatRequest fallback = copyForNonStreaming(request);
                        ChatResponse response = nonStreaming(fallback, cancelled);
                        response.streamingFallback = true;
                        return response;
                    }
                }
                return nonStreaming(request, cancelled);
            } catch (Exception exception) {
                throw new RuntimeException(SecureLog.mask(exception.getMessage()), exception);
            }
        }, executor).orTimeout(timeoutSeconds + 10, java.util.concurrent.TimeUnit.SECONDS);
    }

    private ChatRequest copyForNonStreaming(ChatRequest request) {
        ChatRequest copy = new ChatRequest();
        copy.profile = request.profile;
        copy.messages = request.messages;
        copy.screenshotBase64Jpeg = request.screenshotBase64Jpeg;
        copy.screenshotHash = request.screenshotHash;
        copy.stream = false;
        copy.toolsEnabled = request.toolsEnabled;
        copy.estimatedInputTokens = request.estimatedInputTokens;
        return copy;
    }

    private ChatResponse nonStreaming(ChatRequest request, AtomicBoolean cancelled) throws IOException, InterruptedException {
        HttpResponse<String> response = client(request.profile).send(buildRequest(request, false), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (cancelled.get()) {
            throw new IOException("Request cancelled");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + ": " + SecureLog.mask(response.body()));
        }
        return parseFullResponse(response.body(), request, false);
    }

    private ChatResponse stream(ChatRequest request, StreamListener listener, AtomicBoolean cancelled) throws IOException, InterruptedException {
        HttpResponse<Stream<String>> response = client(request.profile).send(buildRequest(request, true), HttpResponse.BodyHandlers.ofLines());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String body = response.body().reduce("", (a, b) -> a + b);
            throw new IOException("HTTP " + response.statusCode() + ": " + SecureLog.mask(body));
        }

        StringBuilder text = new StringBuilder();
        Map<Integer, AgentToolCall> toolCallMap = new LinkedHashMap<>();
        java.util.concurrent.atomic.AtomicReference<Usage> usageRef =
                new java.util.concurrent.atomic.AtomicReference<>(new Usage(request.estimatedInputTokens, 0, true));

        response.body()
                .takeWhile(line -> !cancelled.get())
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring(5).trim())
                .takeWhile(data -> !data.equals("[DONE]"))
                .forEach(data -> {
                    JsonObject chunk = gson.fromJson(data, JsonObject.class);
                    JsonArray choices = chunk.has("choices") ? chunk.getAsJsonArray("choices") : new JsonArray();
                    if (!choices.isEmpty()) {
                        JsonObject delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");
                        if (delta != null) {
                            if (delta.has("content") && !delta.get("content").isJsonNull()) {
                                String token = delta.get("content").getAsString();
                                text.append(token);
                                listener.onToken(token);
                            }
                            if (delta.has("tool_calls")) {
                                mergeToolCallChunks(toolCallMap, delta.getAsJsonArray("tool_calls"));
                            }
                        }
                    }
                    if (chunk.has("usage") && !chunk.get("usage").isJsonNull()) {
                        usageRef.set(parseUsage(chunk.getAsJsonObject("usage"), false, request));
                    }
                });

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.text = text.toString();
        chatResponse.toolCalls = new ArrayList<>(toolCallMap.values());
        chatResponse.usage = usageRef.get();
        chatResponse.providerName = request.profile.name;
        return chatResponse;
    }

    private void mergeToolCallChunks(Map<Integer, AgentToolCall> map, JsonArray array) {
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            int index = object.has("index") ? object.get("index").getAsInt() : 0;
            AgentToolCall existing = map.get(index);
            String id = object.has("id") ? object.get("id").getAsString() : (existing != null ? existing.id() : "");
            JsonObject function = object.has("function") ? object.getAsJsonObject("function") : null;
            String name = "";
            String args = "{}";
            if (function != null) {
                name = function.has("name") && !function.get("name").isJsonNull()
                        ? function.get("name").getAsString() : (existing != null ? existing.name() : "");
                args = function.has("arguments") && !function.get("arguments").isJsonNull()
                        ? function.get("arguments").getAsString() : "{}";
            }
            if (existing != null) {
                name = name.isEmpty() ? existing.name() : name;
                args = args.equals("{}") ? existing.argumentsJson() : existing.argumentsJson() + args;
                map.put(index, new AgentToolCall(existing.id().isEmpty() ? id : existing.id(), name, args));
            } else {
                map.put(index, new AgentToolCall(id, name, args));
            }
        }
    }

    private HttpClient client(AgentProfile profile) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(AgentConfigStore.getInstance().config().requestTimeoutSeconds));
        if (profile.httpProxy != null && !profile.httpProxy.isBlank()) {
            URI proxyUri = URI.create(profile.httpProxy);
            int port = proxyUri.getPort() > 0 ? proxyUri.getPort() : ("https".equalsIgnoreCase(proxyUri.getScheme()) ? 443 : 80);
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxyUri.getHost(), port)));
        }
        return builder.build();
    }

    private HttpRequest buildRequest(ChatRequest request, boolean stream) {
        String endpoint = request.profile.baseUrl.replaceAll("/+$", "") + "/chat/completions";
        return HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(AgentConfigStore.getInstance().config().requestTimeoutSeconds))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + request.profile.apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(toPayload(request, stream)), StandardCharsets.UTF_8))
                .build();
    }

    JsonObject toPayload(ChatRequest request, boolean stream) {
        JsonObject root = new JsonObject();
        root.addProperty("model", request.profile.model);
        root.addProperty("temperature", request.profile.temperature);
        addMaxTokens(root, request);
        root.addProperty("stream", stream);
        JsonArray messages = new JsonArray();
        for (int index = 0; index < request.messages.size(); index++) {
            ChatMessage message = request.messages.get(index);
            JsonObject item = new JsonObject();
            item.addProperty("role", message.role());
            boolean isLast = index == request.messages.size() - 1;
            boolean hasVision = request.profile.capabilities.supportsVision;
            if (isLast && request.hasImage() && hasVision) {
                JsonArray content = new JsonArray();
                JsonObject text = new JsonObject();
                text.addProperty("type", "text");
                text.addProperty("text", message.content());
                content.add(text);
                JsonObject image = new JsonObject();
                image.addProperty("type", "image_url");
                JsonObject imageUrl = new JsonObject();
                imageUrl.addProperty("url", "data:image/jpeg;base64," + request.screenshotBase64Jpeg);
                image.add("image_url", imageUrl);
                content.add(image);
                item.add("content", content);
            } else {
                item.addProperty("content", message.content());
            }
            messages.add(item);
        }
        root.add("messages", messages);
        if (request.toolsEnabled && request.profile.toolCallsEnabled && request.profile.capabilities.supportsToolCalls) {
            root.add("tools", tools());
            root.addProperty("tool_choice", "auto");
        }
        if (stream && request.profile.capabilities.supportsStreamOptions) {
            JsonObject streamOptions = new JsonObject();
            streamOptions.addProperty("include_usage", true);
            root.add("stream_options", streamOptions);
        }
        return root;
    }

    private void addMaxTokens(JsonObject root, ChatRequest request) {
        if (!request.profile.capabilities.supportsMaxTokens) {
            return;
        }
        int effectiveMax = request.profile.maxTokens;
        int providerLimit = request.profile.capabilities.maxOutputTokens;
        if (providerLimit > 0 && effectiveMax > providerLimit) {
            effectiveMax = providerLimit;
        }
        root.addProperty("max_tokens", effectiveMax);
    }

    private JsonArray tools() {
        JsonArray tools = new JsonArray();
        tools.add(buildTool("look_at", "Rotate camera to look at target coordinates.",
                prop("x", "number", "Target X coordinate"),
                prop("y", "number", "Target Y coordinate (eye level)"),
                prop("z", "number", "Target Z coordinate")));
        tools.add(buildTool("walk_to", "Walk the player toward target coordinates by holding forward key.",
                prop("x", "number", "Target X"),
                prop("y", "number", "Target Y"),
                prop("z", "number", "Target Z")));
        tools.add(buildTool("mine_block", "Look at and continuously mine a block at the given position until destroyed.",
                prop("x", "number", "Block X"),
                prop("y", "number", "Block Y"),
                prop("z", "number", "Block Z")));
        tools.add(buildTool("place_block", "Face a block face and right-click to place the held item.",
                prop("x", "number", "Block X"),
                prop("y", "number", "Block Y"),
                prop("z", "number", "Block Z"),
                prop("face", "string", "Block face: up/down/north/south/east/west")));
        tools.add(buildTool("use_item", "Right-click to use the currently held item.", new String[][]{}));
        tools.add(buildTool("jump", "Make the player jump once.", new String[][]{}));
        tools.add(buildTool("sneak", "Toggle sneak/crouch state.",
                prop("on", "boolean", "true to sneak, false to stand")));
        tools.add(buildTool("get_position", "Report current coordinates, dimension, and facing direction.", new String[][]{}));
        return tools;
    }

    private JsonObject buildTool(String name, String description, String[]... props) {
        JsonObject tool = new JsonObject();
        tool.addProperty("type", "function");
        JsonObject fn = new JsonObject();
        fn.addProperty("name", name);
        fn.addProperty("description", description);
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonArray required = new JsonArray();
        for (String[] p : props) {
            JsonObject prop = new JsonObject();
            prop.addProperty("type", p[1]);
            prop.addProperty("description", p[2]);
            properties.add(p[0], prop);
            required.add(p[0]);
        }
        params.add("properties", properties);
        params.add("required", required);
        fn.add("parameters", params);
        tool.add("function", fn);
        return tool;
    }

    private static String[] prop(String name, String type, String desc) {
        return new String[]{name, type, desc};
    }

    private ChatResponse parseFullResponse(String body, ChatRequest request, boolean cached) {
        JsonObject root = gson.fromJson(body, JsonObject.class);
        ChatResponse response = new ChatResponse();
        response.cached = cached;
        response.providerName = request.profile.name;
        JsonArray choices = root.has("choices") ? root.getAsJsonArray("choices") : new JsonArray();
        if (!choices.isEmpty()) {
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (message != null) {
                if (message.has("content") && !message.get("content").isJsonNull()) {
                    response.text = message.get("content").getAsString();
                }
                if (message.has("tool_calls") && message.get("tool_calls").isJsonArray()) {
                    response.toolCalls = parseToolCalls(message.getAsJsonArray("tool_calls"));
                }
            }
        }
        response.usage = root.has("usage") && root.get("usage").isJsonObject()
                ? parseUsage(root.getAsJsonObject("usage"), false, request)
                : new Usage(request.estimatedInputTokens, TokenEstimator.estimate(response.text), true);
        return response;
    }

    private Usage parseUsage(JsonObject usage, boolean estimated, ChatRequest request) {
        int input = getInt(usage, "prompt_tokens", request.estimatedInputTokens);
        int output = getInt(usage, "completion_tokens", 0);
        return new Usage(input, output, estimated);
    }

    private int getInt(JsonObject object, String key, int fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsInt() : fallback;
    }

    private List<AgentToolCall> parseToolCalls(JsonArray array) {
        List<AgentToolCall> calls = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            JsonObject function = object.has("function") ? object.getAsJsonObject("function") : null;
            if (function == null) {
                continue;
            }
            calls.add(new AgentToolCall(
                    object.has("id") ? object.get("id").getAsString() : "",
                    function.has("name") ? function.get("name").getAsString() : "",
                    function.has("arguments") ? function.get("arguments").getAsString() : "{}"
            ));
        }
        return calls;
    }
}
