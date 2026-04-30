package com.noah.minecraftagent.common.provider;

import com.noah.minecraftagent.common.config.AgentProfile;

import java.util.List;

public final class ChatRequest {
    public AgentProfile profile;
    public List<ChatMessage> messages;
    public String screenshotBase64Jpeg;
    public String screenshotHash;
    public boolean stream;
    public boolean toolsEnabled;
    public int estimatedInputTokens;

    public boolean hasImage() {
        return screenshotBase64Jpeg != null && !screenshotBase64Jpeg.isBlank();
    }
}
