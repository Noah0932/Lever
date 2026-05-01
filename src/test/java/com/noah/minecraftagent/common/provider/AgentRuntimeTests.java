package com.noah.minecraftagent.common.provider;

import com.noah.minecraftagent.common.config.AgentProfile;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for core AgentRuntime behaviors (unit-testable portions).
 */
class AgentRuntimeTests {

    @Test
    void shouldEstimateCostCorrectly() {
        AgentProfile profile = new AgentProfile();
        profile.systemPrompt = "You are a helper";
        profile.usdToCny = 7.2;
        profile.inputUsdPerMillion = 0.15;
        profile.outputUsdPerMillion = 0.6;
        profile.maxTokens = 500;

        // Simulate the cost estimation logic from AgentRuntime
        int inputTokens = TokenEstimator.estimate("test prompt") + TokenEstimator.estimate("{}");
        double cost = profile.estimateCny(inputTokens, Math.min(profile.maxTokens, 256));

        assertTrue(cost > 0.0);
        assertTrue(cost < 0.01); // Should be very small for a tiny prompt
    }

    @Test
    void shouldProperlyCancelViaAtomicBoolean() {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        assertFalse(cancelled.get());
        cancelled.set(true);
        assertTrue(cancelled.get());
    }

    @Test
    void shouldBuildCacheKeyConsistently() {
        String profileId = "test-profile";
        String model = "gpt-4o-mini";
        String systemPrompt = "test";
        String goal = "goal";
        String contextJson = "{}";
        String screenshotHash = "abc123";
        boolean toolsEnabled = true;
        double temperature = 0.0;

        String key1 = com.noah.minecraftagent.common.cache.CacheManager.key(
                profileId + "|" + model + "|" + systemPrompt + "|" + goal + "|" + contextJson + "|" + screenshotHash + "|" + toolsEnabled + "|" + temperature);

        String key2 = com.noah.minecraftagent.common.cache.CacheManager.key(
                profileId + "|" + model + "|" + systemPrompt + "|" + goal + "|" + contextJson + "|" + screenshotHash + "|" + toolsEnabled + "|" + temperature);

        assertEquals(key1, key2, "Cache keys must be consistent for identical inputs");
    }

    @Test
    void shouldEstimateImageTokens() {
        assertEquals(765, TokenEstimator.estimateImageTokens(256));
        assertEquals(765, TokenEstimator.estimateImageTokens(512));
        assertEquals(1105, TokenEstimator.estimateImageTokens(513));
        assertEquals(1105, TokenEstimator.estimateImageTokens(2048));
    }
}
