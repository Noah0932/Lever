package com.noah.minecraftagent.common.provider;

import com.google.gson.JsonObject;
import com.noah.minecraftagent.common.config.AgentProfile;
import com.noah.minecraftagent.common.config.ProviderCapabilities;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiCompatibleProviderTest {

    @Test
    void payloadShouldNotIncludeStreamOptionsWhenCapabilityDisabled() {
        ChatRequest request = new ChatRequest();
        AgentProfile profile = new AgentProfile();
        profile.model = "deepseek-chat";
        profile.capabilities = new ProviderCapabilities();
        profile.capabilities.supportsStreamOptions = false;
        request.profile = profile;
        request.stream = true;
        request.messages = Collections.singletonList(new ChatMessage("user", "hello"));

        JsonObject payload = new TestableProvider().exposePayload(request, true);
        assertFalse(payload.has("stream_options"), "stream_options MUST NOT be present for providers without support");
    }

    @Test
    void payloadShouldNotIncludeToolChoiceWhenCapabilityDisabled() {
        ChatRequest request = new ChatRequest();
        AgentProfile profile = new AgentProfile();
        profile.model = "deepseek-chat";
        profile.capabilities = new ProviderCapabilities();
        profile.capabilities.supportsToolChoiceAuto = false;
        profile.toolCallsEnabled = true;
        request.profile = profile;
        request.toolsEnabled = true;
        request.messages = Collections.singletonList(new ChatMessage("user", "hello"));

        JsonObject payload = new TestableProvider().exposePayload(request, false);
        assertFalse(payload.has("tool_choice"), "tool_choice MUST NOT be present for providers without support");
    }

    @Test
    void payloadShouldIncludeStreamOptionsForOpenAI() {
        ChatRequest request = new ChatRequest();
        AgentProfile profile = new AgentProfile();
        profile.model = "gpt-4o-mini";
        profile.capabilities = ProviderCapabilities.openAiLike();
        request.profile = profile;
        request.stream = true;
        request.messages = Collections.singletonList(new ChatMessage("user", "hello"));

        JsonObject payload = new TestableProvider().exposePayload(request, true);
        assertTrue(payload.has("stream_options"), "stream_options must be present for OpenAI");
    }

    @Test
    void emptyScreenshotBase64ShouldNotTriggerVision() {
        ChatRequest request = new ChatRequest();
        request.screenshotBase64Jpeg = "";
        assertFalse(request.hasImage());
    }

    @Test
    void nullScreenshotBase64ShouldNotTriggerVision() {
        ChatRequest request = new ChatRequest();
        request.screenshotBase64Jpeg = null;
        assertFalse(request.hasImage());
    }

    @Test
    void validScreenshotShouldTriggerVision() {
        ChatRequest request = new ChatRequest();
        request.screenshotBase64Jpeg = "abc123";
        assertTrue(request.hasImage());
    }

    static final class TestableProvider extends OpenAiCompatibleProvider {
        JsonObject exposePayload(ChatRequest request, boolean stream) {
            return this.toExposedPayload(request, stream);
        }
    }
}
