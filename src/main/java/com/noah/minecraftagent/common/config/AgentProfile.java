package com.noah.minecraftagent.common.config;

import java.util.UUID;

public final class AgentProfile {
    public String id = UUID.randomUUID().toString();
    public String name = "OpenAI Compatible";
    public String baseUrl = "https://api.openai.com/v1";
    public String apiKey = "";
    public String model = "gpt-4o-mini";
    public String systemPrompt = "You are a safe Minecraft assistant. Use execute_command only when needed and obey server safety limits.";
    public String httpProxy = "";
    public double temperature = 0.0D;
    public int maxTokens = 1200;
    public double inputUsdPerMillion = 0.15D;
    public double outputUsdPerMillion = 0.60D;
    public double usdToCny = 7.2D;
    public double dailyLimitCny = 5.0D;
    public boolean streamingEnabled = true;
    public boolean visionEnabled = true;
    public boolean toolCallsEnabled = true;
    public boolean cacheEnabled = true;
    public boolean allowResponseCacheWhenNonDeterministic = false;
    public boolean locked = false;
    public int maxAgentSteps = 8;
    public ProviderCapabilities capabilities = ProviderCapabilities.openAiLike();

    public boolean isComplete() {
        return !baseUrl.isBlank() && !model.isBlank() && !apiKey.isBlank();
    }

    public double estimateCny(int inputTokens, int outputTokens) {
        double usd = inputTokens / 1_000_000D * inputUsdPerMillion + outputTokens / 1_000_000D * outputUsdPerMillion;
        return usd * usdToCny;
    }

    public void copyFrom(AgentProfile source) {
        this.name = source.name;
        this.baseUrl = source.baseUrl;
        this.apiKey = source.apiKey;
        this.model = source.model;
        this.systemPrompt = source.systemPrompt;
        this.httpProxy = source.httpProxy;
        this.temperature = source.temperature;
        this.maxTokens = source.maxTokens;
        this.inputUsdPerMillion = source.inputUsdPerMillion;
        this.outputUsdPerMillion = source.outputUsdPerMillion;
        this.usdToCny = source.usdToCny;
        this.dailyLimitCny = source.dailyLimitCny;
        this.streamingEnabled = source.streamingEnabled;
        this.visionEnabled = source.visionEnabled;
        this.toolCallsEnabled = source.toolCallsEnabled;
        this.cacheEnabled = source.cacheEnabled;
        this.locked = source.locked;
        this.maxAgentSteps = source.maxAgentSteps;
    }
}
