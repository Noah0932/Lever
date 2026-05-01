package com.noah.minecraftagent.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD: API Key persistence round-trip and config corruption recovery tests.
 */
class AgentConfigStorePersistenceTest {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @TempDir
    Path tempDir;

    @Test
    void shouldPersistApiKeyRoundTrip() throws IOException {
        Path configFile = tempDir.resolve("config.json");
        AgentConfig config = new AgentConfig();
        AgentProfile profile = new AgentProfile();
        profile.id = "p1";
        profile.apiKey = "sk-test-key-123456";
        profile.baseUrl = "https://api.openai.com/v1";
        profile.model = "gpt-4o-mini";
        config.profiles.add(profile);
        config.activeProfileId = "p1";
        Files.writeString(configFile, gson.toJson(config), StandardCharsets.UTF_8);

        String json = Files.readString(configFile, StandardCharsets.UTF_8);
        AgentConfig loaded = gson.fromJson(json, AgentConfig.class);

        assertNotNull(loaded);
        assertNotNull(loaded.activeProfile());
        assertEquals("sk-test-key-123456", loaded.activeProfile().apiKey,
                "API Key must survive serialize-deserialize round trip");
    }

    @Test
    void shouldRetainApiKeyAfterNullFieldsDeserialization() {
        String json = """
                {
                  "activeProfileId": "p1",
                  "profiles": [{
                    "id": "p1",
                    "name": "Test",
                    "baseUrl": "https://api.example.com",
                    "apiKey": "sk-important-key-999",
                    "model": "test-model"
                  }]
                }""";
        AgentConfig config = gson.fromJson(json, AgentConfig.class);
        assertNotNull(config);
        AgentProfile profile = config.activeProfile();
        assertEquals("sk-important-key-999", profile.apiKey,
                "apiKey must not be lost when other fields are null/default");
    }

    @Test
    void shouldNotLoseApiKeyWhenConfigReloaded() throws IOException {
        Path configFile = tempDir.resolve("config.json");
        AgentConfig config = new AgentConfig();
        AgentProfile profile = new AgentProfile();
        profile.id = "p1";
        profile.apiKey = "sk-original-key";
        profile.baseUrl = "https://api.example.com";
        profile.model = "test-model";
        config.profiles.add(profile);
        config.activeProfileId = "p1";
        Files.writeString(configFile, gson.toJson(config), StandardCharsets.UTF_8);

        String firstRead = Files.readString(configFile, StandardCharsets.UTF_8);
        AgentConfig first = gson.fromJson(firstRead, AgentConfig.class);

        Files.writeString(configFile, gson.toJson(first), StandardCharsets.UTF_8);
        String secondRead = Files.readString(configFile, StandardCharsets.UTF_8);
        AgentConfig second = gson.fromJson(secondRead, AgentConfig.class);

        assertEquals("sk-original-key", second.activeProfile().apiKey,
                "API Key must not be lost across multiple save-reload cycles");
    }

    @Test
    void shouldHandleEmptyConfigGracefully() {
        AgentConfig config = new AgentConfig();
        assertNotNull(config);
        assertNotNull(config.profiles);
        assertTrue(config.profiles.isEmpty(),
                "Empty config should start with empty profiles before activeProfile() is called");
        AgentProfile profile = config.activeProfile();
        assertNotNull(profile);
        assertFalse(profile.id.isEmpty());
        assertFalse(config.profiles.isEmpty(),
                "Calling activeProfile() must auto-populate profiles");
    }

    @Test
    void shouldProvideDefaultProfileWhenNoneMatch() {
        AgentConfig config = new AgentConfig();
        config.activeProfileId = "non-existent-id";
        AgentProfile profile = config.activeProfile();
        assertNotNull(profile);
        assertFalse(profile.id.isEmpty(), "Must return a valid profile even when activeProfileId is stale");
    }
}
