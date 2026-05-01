package com.noah.minecraftagent.common.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentProfileTest {

    @Test
    void shouldConsiderProfileCompleteWhenFieldsFilled() {
        AgentProfile profile = new AgentProfile();
        profile.baseUrl = "https://api.openai.com/v1";
        profile.model = "gpt-4o-mini";
        profile.apiKey = "sk-test";
        assertTrue(profile.isComplete());
    }

    @Test
    void shouldConsiderProfileIncompleteWhenApiKeyMissing() {
        AgentProfile profile = new AgentProfile();
        profile.baseUrl = "https://api.openai.com/v1";
        profile.model = "gpt-4o-mini";
        profile.apiKey = "";
        assertFalse(profile.isComplete());
    }

    @Test
    void shouldConsiderProfileIncompleteWhenBaseUrlMissing() {
        AgentProfile profile = new AgentProfile();
        profile.baseUrl = "";
        profile.model = "gpt-4o-mini";
        profile.apiKey = "sk-test";
        assertFalse(profile.isComplete());
    }

    @Test
    void shouldConsiderProfileIncompleteWhenModelMissing() {
        AgentProfile profile = new AgentProfile();
        profile.baseUrl = "https://api.openai.com/v1";
        profile.model = "";
        profile.apiKey = "sk-test";
        assertFalse(profile.isComplete());
    }

    @Test
    void shouldEstimateCnyCostCorrectly() {
        AgentProfile profile = new AgentProfile();
        profile.inputUsdPerMillion = 0.15D;
        profile.outputUsdPerMillion = 0.60D;
        profile.usdToCny = 7.2D;
        double cost = profile.estimateCny(1000, 500);
        double expected = (1000.0 / 1_000_000 * 0.15 + 500.0 / 1_000_000 * 0.60) * 7.2;
        assertEquals(expected, cost, 0.0001);
    }

    @Test
    void shouldHaveDefaultValues() {
        AgentProfile profile = new AgentProfile();
        assertNotNull(profile.id);
        assertEquals("OpenAI Compatible", profile.name);
        assertEquals("gpt-4o-mini", profile.model);
        assertEquals(0.0D, profile.temperature);
        assertEquals(1200, profile.maxTokens);
        assertEquals(5.0D, profile.dailyLimitCny);
    }
}
