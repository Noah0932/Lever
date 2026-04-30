package com.noah.minecraftagent.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.noah.minecraftagent.common.util.SecureLog;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AgentConfigStore {
    private static final AgentConfigStore INSTANCE = new AgentConfigStore();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDir = FabricLoader.getInstance().getConfigDir().resolve("ai_agent");
    private final Path configFile = configDir.resolve("config.json");
    private AgentConfig config = new AgentConfig();

    private AgentConfigStore() {
    }

    public static AgentConfigStore getInstance() {
        return INSTANCE;
    }

    public synchronized AgentConfig config() {
        config.activeProfile();
        return config;
    }

    public synchronized void load() {
        try {
            Files.createDirectories(configDir);
            if (Files.exists(configFile)) {
                config = gson.fromJson(Files.readString(configFile, StandardCharsets.UTF_8), AgentConfig.class);
                if (config == null) {
                    config = new AgentConfig();
                }
            }
            config.activeProfile();
            save();
        } catch (IOException exception) {
            SecureLog.error("Failed to load AI agent config", exception);
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(configDir);
            Files.writeString(configFile, gson.toJson(config), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            SecureLog.error("Failed to save AI agent config", exception);
        }
    }

    public Path configDir() {
        return configDir;
    }
}
