package com.noah.minecraftagent.common.provider;

public final class ProfileHealth {
    public int successes;
    public int failures;
    public int consecutiveFailures;
    public long averageLatencyMs;
    public long cooldownUntilEpochMs;

    public boolean available() {
        return System.currentTimeMillis() >= cooldownUntilEpochMs;
    }

    public int score() {
        int total = successes + failures;
        int successRate = total == 0 ? 80 : (successes * 100 / total);
        int latencyPenalty = (int) Math.min(30, averageLatencyMs / 1000);
        int failurePenalty = consecutiveFailures * 10;
        return Math.max(0, successRate - latencyPenalty - failurePenalty);
    }

    public void recordSuccess(long latencyMs) {
        successes++;
        consecutiveFailures = 0;
        averageLatencyMs = averageLatencyMs == 0 ? latencyMs : (averageLatencyMs * 4 + latencyMs) / 5;
    }

    public void recordFailure(int cooldownSeconds) {
        failures++;
        consecutiveFailures++;
        if (consecutiveFailures >= 2) {
            cooldownUntilEpochMs = System.currentTimeMillis() + cooldownSeconds * 1000L;
        }
    }
}
