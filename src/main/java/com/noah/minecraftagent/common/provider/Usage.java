package com.noah.minecraftagent.common.provider;

public record Usage(int inputTokens, int outputTokens, boolean estimated) {
    public int totalTokens() {
        return inputTokens + outputTokens;
    }
}
