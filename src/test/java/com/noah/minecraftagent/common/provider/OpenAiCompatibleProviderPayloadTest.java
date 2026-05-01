package com.noah.minecraftagent.common.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.noah.minecraftagent.common.config.AgentProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for OpenAiCompatibleProvider payload construction.
 * These tests verify the JSON payload format is valid for OpenAI-compatible APIs.
 */
class OpenAiCompatibleProviderPayloadTest {

    private final Gson gson = new Gson();

    @Test
    void shouldNotIncludeStreamOptionsWhenStreamingDisabled() {
        ChatRequest request = createBasicRequest(false);
        JsonObject payload = buildPayload(request, false);

        // stream_options should NOT be present when not streaming
        assertFalse(payload.has("stream_options"), "stream_options must not be present when stream=false");
    }

    @Test
    void shouldNotIncludeStreamOptionsForNonOpenAiProviders() {
        ChatRequest request = createBasicRequest(true);
        // When capabilities do not explicitly support stream_options, omit them
        JsonObject payload = buildPayload(request, true);

        // stream_options should be omitted or conditional
        // Many compatible APIs (DeepSeek, Ollama) reject this field
        assertFalse(payload.has("stream_options"),
                "stream_options should be conditional to avoid HTTP 400 on non-OpenAI APIs");
    }

    @Test
    void shouldIncludeToolsOnlyWhenEnabled() {
        ChatRequest request = createBasicRequest(false);
        request.toolsEnabled = false;
        JsonObject payload = buildPayload(request, false);
        assertFalse(payload.has("tools"));
        assertFalse(payload.has("tool_choice"));
    }

    @Test
    void shouldIncludeToolsWhenEnabled() {
        ChatRequest request = createBasicRequest(false);
        request.toolsEnabled = true;
        request.profile.toolCallsEnabled = true;
        request.profile.capabilities.supportsToolCalls = true;
        JsonObject payload = buildPayload(request, false);
        assertTrue(payload.has("tools"));
        assertEquals("auto", payload.get("tool_choice").getAsString());
    }

    @Test
    void shouldBuildValidMessagesArray() {
        ChatRequest request = createBasicRequest(false);
        request.messages = List.of(
                new ChatMessage("system", "You are a helper"),
                new ChatMessage("user", "Hello")
        );
        JsonObject payload = buildPayload(request, false);
        JsonArray messages = payload.getAsJsonArray("messages");
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).getAsJsonObject().get("role").getAsString());
        assertEquals("user", messages.get(1).getAsJsonObject().get("role").getAsString());
    }

    @Test
    void shouldBuildMultimodalContentWhenImagePresent() {
        ChatRequest request = createBasicRequest(false);
        request.screenshotBase64Jpeg = "abc123";
        request.messages = List.of(new ChatMessage("user", "What do you see?"));
        JsonObject payload = buildPayload(request, false);
        JsonObject lastMessage = payload.getAsJsonArray("messages").get(0).getAsJsonObject();
        assertTrue(lastMessage.has("content"));
        assertTrue(lastMessage.get("content").isJsonArray());
    }

    @Test
    void shouldIncludeRequiredRootFields() {
        ChatRequest request = createBasicRequest(false);
        JsonObject payload = buildPayload(request, false);
        assertTrue(payload.has("model"));
        assertTrue(payload.has("messages"));
        assertTrue(payload.has("temperature"));
        assertTrue(payload.has("max_tokens"));
        assertTrue(payload.has("stream"));
    }

    @Test
    void shouldSerializeToValidJson() {
        ChatRequest request = createBasicRequest(false);
        JsonObject payload = buildPayload(request, false);
        String json = gson.toJson(payload);
        assertNotNull(json);
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
        assertTrue(json.contains("\"model\""));
        assertTrue(json.contains("\"messages\""));
    }

    @Test
    void shouldNotContainNullValues() {
        ChatRequest request = createBasicRequest(false);
        JsonObject payload = buildPayload(request, false);
        String json = gson.toJson(payload);
        assertFalse(json.contains("null"), "JSON must not contain null values");
    }

    private ChatRequest createBasicRequest(boolean stream) {
        ChatRequest request = new ChatRequest();
        request.profile = new AgentProfile();
        request.profile.model = "test-model";
        request.profile.temperature = 0.0;
        request.profile.maxTokens = 500;
        request.profile.streamingEnabled = stream;
        request.profile.capabilities.supportsStreaming = stream;
        request.profile.toolCallsEnabled = false;
        request.messages = List.of(new ChatMessage("user", "test"));
        request.stream = stream;
        request.toolsEnabled = false;
        request.estimatedInputTokens = 10;
        return request;
    }

    private JsonObject buildPayload(ChatRequest request, boolean stream) {
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider();
        return provider.toPayload(request, stream);
    }
}
