package com.noah.minecraftagent.common.provider;

import java.util.ArrayList;
import java.util.List;

public final class ChatResponse {
    public String text = "";
    public Usage usage = new Usage(0, 0, true);
    public List<AgentToolCall> toolCalls = new ArrayList<>();
    public boolean cached;
    public boolean streamingFallback;
    public boolean truncated;
    public String providerName = "";
}
