package com.noah.minecraftagent.common.provider;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for ChatMessage, ChatRequest, ChatResponse data records.
 */
class ChatDataTest {

    @Test
    void shouldCreateChatMessage() {
        ChatMessage msg = new ChatMessage("user", "Hello");
        assertEquals("user", msg.role());
        assertEquals("Hello", msg.content());
    }

    @Test
    void shouldDetectImagePresence() {
        ChatRequest req = new ChatRequest();
        assertFalse(req.hasImage());
        req.screenshotBase64Jpeg = "abc123";
        assertTrue(req.hasImage());
        req.screenshotBase64Jpeg = "";
        assertFalse(req.hasImage());
    }

    @Test
    void shouldTrackUsageTotalTokens() {
        Usage usage = new Usage(100, 50, false);
        assertEquals(150, usage.totalTokens());
        assertFalse(usage.estimated());
    }

    @Test
    void shouldTrackEstimatedUsage() {
        Usage usage = new Usage(200, 0, true);
        assertTrue(usage.estimated());
    }

    @Test
    void shouldCreateDefaultChatResponse() {
        ChatResponse resp = new ChatResponse();
        assertEquals("", resp.text);
        assertFalse(resp.cached);
        assertTrue(resp.toolCalls.isEmpty());
    }
}
