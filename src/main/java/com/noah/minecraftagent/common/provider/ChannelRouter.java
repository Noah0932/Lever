package com.noah.minecraftagent.common.provider;

import com.noah.minecraftagent.common.config.AgentConfig;
import com.noah.minecraftagent.common.config.AgentProfile;
import com.noah.minecraftagent.common.config.RequestRequirements;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class ChannelRouter {
    private final Map<String, ProfileHealth> health = new HashMap<>();

    public AgentProfile select(AgentConfig config, RequestRequirements requirements) {
        AgentProfile active = config.activeProfile();
        if (active.locked) {
            return active;
        }
        return config.profiles.stream()
                .filter(AgentProfile::isComplete)
                .filter(profile -> health(profile).available())
                .filter(profile -> !requirements.needsStreaming || profile.capabilities.supportsStreaming)
                .filter(profile -> !requirements.needsVision || profile.capabilities.supportsVision)
                .filter(profile -> !requirements.needsToolCalls || profile.capabilities.supportsToolCalls)
                .max(Comparator.comparingInt(profile -> score(profile, active, requirements)))
                .orElse(active);
    }

    public void recordSuccess(AgentProfile profile, long latencyMs) {
        health(profile).recordSuccess(latencyMs);
    }

    public void recordFailure(AgentProfile profile, int cooldownSeconds) {
        health(profile).recordFailure(cooldownSeconds);
    }

    public ProfileHealth health(AgentProfile profile) {
        return health.computeIfAbsent(profile.id, ignored -> new ProfileHealth());
    }

    private int score(AgentProfile profile, AgentProfile active, RequestRequirements requirements) {
        int score = profile.capabilities.matchScore(requirements) + health(profile).score();
        if (profile.id.equals(active.id)) {
            score += 15;
        }
        double cost = profile.inputUsdPerMillion + profile.outputUsdPerMillion;
        score -= (int) Math.min(20, cost);
        return score;
    }
}
