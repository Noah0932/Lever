package com.noah.minecraftagent.common.provider;

import com.google.gson.JsonObject;
import com.noah.minecraftagent.common.config.AgentProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD: Audit compliance tests for API parameter validation and multimodel compatibility.
 */
class ProviderComplianceTest {

    private final OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider();

    @Test
    void shouldNotSendMaxTokensWhenUnsupported() {
        AgentProfile profile = new AgentProfile();
        profile.capabilities.supportsMaxTokens = false;
        profile.maxTokens = 1200;
        ChatRequest req = buildRequest(profile, false);
        JsonObject payload = provider.toPayload(req, false);
        assertFalse(payload.has("max_tokens"),
                "max_tokens MUST NOT be present when provider doesn't support it");
    }

    @Test
    void shouldSendMaxTokensWhenSupported() {
        AgentProfile profile = new AgentProfile();
        profile.capabilities.supportsMaxTokens = true;
        profile.maxTokens = 1200;
        ChatRequest req = buildRequest(profile, false);
        JsonObject payload = provider.toPayload(req, false);
        assertTrue(payload.has("max_tokens"));
        assertEquals(1200, payload.get("max_tokens").getAsInt());
    }

    @Test
    void shouldTrimMaxTokensToProviderLimit() {
        AgentProfile profile = new AgentProfile();
        profile.maxTokens = 16000;
        profile.capabilities.supportsMaxTokens = true;
        profile.capabilities.maxOutputTokens = 4096;
        ChatRequest req = buildRequest(profile, false);
        JsonObject payload = provider.toPayload(req, false);
        assertEquals(4096, payload.get("max_tokens").getAsInt(),
                "max_tokens must be trimmed to provider's maxOutputTokens limit");
    }

    @Test
    void shouldNotIncludeImageWhenVisionUnsupported() {
        AgentProfile profile = new AgentProfile();
        profile.capabilities.supportsVision = false;
        ChatRequest req = buildRequest(profile, false);
        req.screenshotBase64Jpeg = "abc123base64fake";
        req.profile = profile;
        JsonObject payload = provider.toPayload(req, false);
        JsonObject lastMsg = payload.getAsJsonArray("messages")
                .get(0).getAsJsonObject();
        assertTrue(lastMsg.has("content"));
        assertFalse(lastMsg.get("content").isJsonArray(),
                "Image content must NOT be sent to vision-incapable models");
        assertTrue(lastMsg.get("content").isJsonPrimitive());
    }

    @Test
    void shouldIncludeImageWhenVisionSupported() {
        AgentProfile profile = new AgentProfile();
        profile.capabilities.supportsVision = true;
        ChatRequest req = buildRequest(profile, false);
        req.screenshotBase64Jpeg = "abc123base64fake";
        req.profile = profile;
        JsonObject payload = provider.toPayload(req, false);
        JsonObject lastMsg = payload.getAsJsonArray("messages")
                .get(0).getAsJsonObject();
        assertTrue(lastMsg.get("content").isJsonArray(),
                "Image content SHOULD be sent to vision-capable models");
    }

    @Test
    void shouldNotSendStreamOptionsWhenUnsupported() {
        AgentProfile profile = new AgentProfile();
        profile.capabilities.supportsStreamOptions = false;
        profile.streamingEnabled = true;
        profile.capabilities.supportsStreaming = true;
        ChatRequest req = buildRequest(profile, true);
        JsonObject payload = provider.toPayload(req, true);
        assertFalse(payload.has("stream_options"));
    }

    @Test
    void shouldNotSendToolsWhenUnsupported() {
        AgentProfile profile = new AgentProfile();
        profile.capabilities.supportsToolCalls = false;
        ChatRequest req = buildRequest(profile, false);
        req.toolsEnabled = true;
        req.profile.toolCallsEnabled = true;
        JsonObject payload = provider.toPayload(req, false);
        assertFalse(payload.has("tools"));
        assertFalse(payload.has("tool_choice"));
    }

    private ChatRequest buildRequest(AgentProfile profile, boolean stream) {
        ChatRequest req = new ChatRequest();
        req.profile = profile;
        req.messages = List.of(new ChatMessage("user", "test message"));
        req.stream = stream;
        req.toolsEnabled = true;
        req.estimatedInputTokens = 10;
        return req;
    }
}
