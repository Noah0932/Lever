package com.noah.minecraftagent;

import com.noah.minecraftagent.common.config.AgentConfigStore;
import com.noah.minecraftagent.common.network.AgentNetworking;
import com.noah.minecraftagent.common.util.SecureLog;
import net.fabricmc.api.ModInitializer;

public final class MinecraftAgentMod implements ModInitializer {
    public static final String MOD_ID = "minecraftagent";

    @Override
    public void onInitialize() {
        SecureLog.info("Initializing Minecraft AI Agent");
        AgentConfigStore.getInstance().load();
        AgentNetworking.registerPayloads();
        AgentNetworking.registerServerHandlers();
    }
}
