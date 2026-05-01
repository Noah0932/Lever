package com.noah.minecraftagent.common.provider;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.noah.minecraftagent.common.config.AgentConfigStore;
import com.noah.minecraftagent.common.config.AgentProfile;
import com.noah.minecraftagent.common.util.SecureLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OpenAiCompatibleProvider implements ChatProvider {
    private final Gson gson = new Gson();

    @Override
    public CompletableFuture<ChatResponse> complete(ChatRequest request, StreamListener listener, AtomicBoolean cancelled, Executor executor) {
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
        }, executor);
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
        HttpResponse<InputStream> response = client(request.profile).send(buildRequest(request, true), HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new IOException("HTTP " + response.statusCode() + ": " + SecureLog.mask(body));
        }

        StringBuilder text = new StringBuilder();
        List<AgentToolCall> toolCalls = new ArrayList<>();
        Usage usage = new Usage(request.estimatedInputTokens, 0, true);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (cancelled.get()) {
                    throw new IOException("Request cancelled");
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.equals("[DONE]")) {
                    break;
                }
                JsonObject chunk = gson.fromJson(data, JsonObject.class);
                JsonArray choices = chunk.has("choices") ? chunk.getAsJsonArray("choices") : new JsonArray();
                if (!choices.isEmpty()) {
                    JsonObject delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");
                    if (delta != null && delta.has("content") && !delta.get("content").isJsonNull()) {
                        String token = delta.get("content").getAsString();
                        text.append(token);
                        listener.onToken(token);
                    }
                    if (delta != null && delta.has("tool_calls")) {
                        toolCalls.addAll(parseToolCalls(delta.getAsJsonArray("tool_calls")));
                    }
                }
                if (chunk.has("usage") && !chunk.get("usage").isJsonNull()) {
                    usage = parseUsage(chunk.getAsJsonObject("usage"), false, request);
                }
            }
        }
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.text = text.toString();
        chatResponse.toolCalls = toolCalls;
        chatResponse.usage = usage;
        chatResponse.providerName = request.profile.name;
        return chatResponse;
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
        root.addProperty("max_tokens", request.profile.maxTokens);
        root.addProperty("stream", stream);
        JsonArray messages = new JsonArray();
        for (int index = 0; index < request.messages.size(); index++) {
            ChatMessage message = request.messages.get(index);
            JsonObject item = new JsonObject();
            item.addProperty("role", message.role());
            if (index == request.messages.size() - 1 && request.hasImage()) {
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

    private JsonArray tools() {
        JsonArray tools = new JsonArray();
        JsonObject tool = new JsonObject();
        tool.addProperty("type", "function");
        JsonObject function = new JsonObject();
        function.addProperty("name", "execute_command");
        function.addProperty("description", "Execute a Minecraft command after server-side permission and safety checks.");
        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject command = new JsonObject();
        command.addProperty("type", "string");
        command.addProperty("description", "Minecraft command without leading slash preferred.");
        properties.add("command", command);
        parameters.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("command");
        parameters.add("required", required);
        function.add("parameters", parameters);
        tool.add("function", function);
        tools.add(tool);
        return tools;
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


