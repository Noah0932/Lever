package com.noah.minecraftagent.common.config;

public final class ProviderCapabilities {
    public boolean supportsStreaming = true;
    public boolean supportsVision = true;
    public boolean supportsToolCalls = true;
    public boolean supportsUsage = true;
    public boolean supportsJsonMode = false;
    public boolean supportsStreamOptions = false;
    public boolean supportsMaxTokens = true;
    public int maxOutputTokens = 4096;
    public int maxContextTokens = 128000;

    public static ProviderCapabilities openAiLike() {
        return new ProviderCapabilities();
    }

    public int matchScore(RequestRequirements requirements) {
        int score = 0;
        if (!requirements.needsStreaming || supportsStreaming) score += 20;
        if (!requirements.needsVision || supportsVision) score += 20;
        if (!requirements.needsToolCalls || supportsToolCalls) score += 20;
        if (!requirements.needsJsonMode || supportsJsonMode) score += 10;
        if (maxContextTokens >= requirements.estimatedContextTokens) score += 10;
        return score;
    }
}
