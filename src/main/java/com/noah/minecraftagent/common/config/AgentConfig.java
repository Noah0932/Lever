package com.noah.minecraftagent.common.config;

import java.util.ArrayList;
import java.util.List;

public final class AgentConfig {
    public String activeProfileId = "";
    public List<AgentProfile> profiles = new ArrayList<>();
    public String chatPrefix = ".ai";
    public boolean delegationConfirmRequired = true;
    public boolean allowOperatorDelegation = true;
    public List<String> allowedDelegates = new ArrayList<>();
    public boolean abilityFirstRouting = true;
    public boolean fallbackStreamingToNonStreaming = true;
    public int screenshotMaxWidth = 768;
    public int screenshotJpegQuality = 75;
    public int requestTimeoutSeconds = 60;
    public int providerHealthCooldownSeconds = 120;

    public AgentProfile activeProfile() {
        if (allowedDelegates == null) {
            allowedDelegates = new ArrayList<>();
        }
        if (chatPrefix == null || chatPrefix.isBlank()) {
            chatPrefix = ".ai";
        }
        if (profiles.isEmpty()) {
            AgentProfile profile = new AgentProfile();
            profiles.add(profile);
            activeProfileId = profile.id;
            return profile;
        }
        AgentProfile result = profiles.stream()
                .filter(profile -> profile.id.equals(activeProfileId))
                .findFirst()
                .orElseGet(() -> {
                    activeProfileId = profiles.get(0).id;
                    return profiles.get(0);
                });
        if (!result.id.equals(activeProfileId)) {
            activeProfileId = result.id;
        }
        return result;
    }
}

