package com.noah.minecraftagent.common.config;

import com.noah.minecraftagent.common.config.RequestRequirements;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for ProviderCapabilities scoring and ProfileHealth availability.
 */
class ProviderCapabilitiesTest {

    @Test
    void shouldGiveFullScoreForMatchingCapabilities() {
        ProviderCapabilities caps = ProviderCapabilities.openAiLike();
        RequestRequirements reqs = new RequestRequirements();
        reqs.needsStreaming = true;
        reqs.needsVision = true;
        reqs.needsToolCalls = true;
        reqs.needsJsonMode = false;
        reqs.estimatedContextTokens = 1000;

        int score = caps.matchScore(reqs);
        assertEquals(80, score); // 20+20+20+10+10
    }

    @Test
    void shouldPenalizeMissingVisionSupport() {
        ProviderCapabilities caps = ProviderCapabilities.openAiLike();
        caps.supportsVision = false;
        RequestRequirements reqs = new RequestRequirements();
        reqs.needsVision = true;
        reqs.needsStreaming = false;
        reqs.needsToolCalls = false;
        reqs.needsJsonMode = false;
        reqs.estimatedContextTokens = 0;

        int score = caps.matchScore(reqs);
        assertEquals(60, score); // 20+0+20+10+10 (vision fails, gets at most 20+20+10+10=60 if others pass)
    }

    @Test
    void shouldHaveDefaultOpenAiCapabilities() {
        ProviderCapabilities caps = ProviderCapabilities.openAiLike();
        assertTrue(caps.supportsStreaming);
        assertTrue(caps.supportsVision);
        assertTrue(caps.supportsToolCalls);
        assertTrue(caps.supportsUsage);
        assertFalse(caps.supportsJsonMode);
        assertFalse(caps.supportsStreamOptions);
        assertEquals(128000, caps.maxContextTokens);
    }
}
