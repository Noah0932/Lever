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
                String raw = Files.readString(configFile, StandardCharsets.UTF_8);
                if (raw.isBlank()) {
                    SecureLog.warn("AI agent config file is empty, keeping defaults without overwriting disk");
                    config = new AgentConfig();
                    config.activeProfile();
                    return;
                }
                try {
                    config = gson.fromJson(raw, AgentConfig.class);
                } catch (Exception parseException) {
                    SecureLog.error("Failed to parse AI agent config, restoring from backup", parseException);
                    Path backupFile = configDir.resolve("config.json.bak");
                    if (Files.exists(backupFile)) {
                        try {
                            config = gson.fromJson(Files.readString(backupFile, StandardCharsets.UTF_8), AgentConfig.class);
                            if (config != null) {
                                SecureLog.info("Restored config from backup");
                            }
                        } catch (Exception backupException) {
                            SecureLog.error("Backup also corrupt", backupException);
                        }
                    }
                }
                if (config == null) {
                    config = new AgentConfig();
                }
            }
            config.activeProfile();
        } catch (IOException exception) {
            SecureLog.error("Failed to load AI agent config", exception);
        }
    }

    public synchronized void save() {
        try {
            AgentProfile active = config.activeProfile();
            if (active != null && active.apiKey.isBlank()) {
                Path bak = configDir.resolve("config.json.bak");
                if (Files.exists(bak)) {
                    String bakRaw = Files.readString(bak, StandardCharsets.UTF_8);
                    AgentConfig bakConfig = gson.fromJson(bakRaw, AgentConfig.class);
                    if (bakConfig != null) {
                        AgentProfile bakActive = bakConfig.activeProfile();
                        if (bakActive != null && !bakActive.apiKey.isBlank()) {
                            SecureLog.warn("Refusing to save config with blank API key while backup has a valid key");
                            return;
                        }
                    }
                }
                if (Files.exists(configFile)) {
                    String existingRaw = Files.readString(configFile, StandardCharsets.UTF_8);
                    AgentConfig existingConfig = gson.fromJson(existingRaw, AgentConfig.class);
                    if (existingConfig != null) {
                        AgentProfile existingActive = existingConfig.activeProfile();
                        if (existingActive != null && !existingActive.apiKey.isBlank()) {
                            SecureLog.warn("Refusing to overwrite existing valid API key with blank value");
                            return;
                        }
                    }
                }
            }
            Files.createDirectories(configDir);
            String json = gson.toJson(config);
            if (Files.exists(configFile)) {
                Files.copy(configFile, configDir.resolve("config.json.bak"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(configFile, json, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            SecureLog.error("Failed to save AI agent config", exception);
        }
    }

    public Path configDir() {
        return configDir;
    }
}
