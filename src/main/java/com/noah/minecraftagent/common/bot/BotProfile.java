package com.noah.minecraftagent.common.bot;

import com.noah.minecraftagent.common.config.AgentProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BotProfile {
    public String id = UUID.randomUUID().toString();
    public String name = "Bot";
    public int botNumber;
    public String ownerUuid = "";
    public String ownerName = "";
    public String skinPlayerName = "";
    public AgentProfile aiConfig = new AgentProfile();
    public List<String> whitelist = new ArrayList<>();
    public boolean aiConfigInherited = false;
    public long createdAt = System.currentTimeMillis();
    public String customName = "";

    public String displayName() {
        String base = name + " " + botNumber;
        return customName.isBlank() ? base : customName;
    }

    public boolean isOwner(String uuid) {
        return ownerUuid.equals(uuid);
    }

    public boolean isAuthorized(String uuid) {
        return isOwner(uuid) || whitelist.contains(uuid);
    }
}
