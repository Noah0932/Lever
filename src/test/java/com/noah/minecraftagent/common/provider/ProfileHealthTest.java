package com.noah.minecraftagent.common.provider;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for ProfileHealth cooldown and scoring logic.
 */
class ProfileHealthTest {

    @Test
    void shouldBeAvailableByDefault() {
        ProfileHealth health = new ProfileHealth();
        assertTrue(health.available());
    }

    @Test
    void shouldBecomeUnavailableAfterConsecutiveFailures() {
        ProfileHealth health = new ProfileHealth();
        health.recordFailure(60);
        assertTrue(health.available()); // 1 failure shouldn't trigger cooldown
        health.recordFailure(60);
        assertFalse(health.available()); // 2 consecutive failures triggers cooldown
    }

    @Test
    void shouldResetConsecutiveFailuresOnSuccess() {
        ProfileHealth health = new ProfileHealth();
        health.recordFailure(60);
        health.recordSuccess(100);
        assertEquals(0, health.consecutiveFailures);
    }

    @Test
    void shouldCalculateScoreCorrectly() {
        ProfileHealth health = new ProfileHealth();
        health.successes = 90;
        health.failures = 10;
        health.averageLatencyMs = 0;
        assertEquals(90, health.score()); // 90% success rate - 0 penalty
    }

    @Test
    void shouldPenalizeHighLatency() {
        ProfileHealth health = new ProfileHealth();
        health.successes = 10;
        health.failures = 0;
        health.averageLatencyMs = 5000;
        assertEquals(95, health.score()); // 100% - min(30, 5) latency penalty
    }
}
