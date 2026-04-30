package com.noah.minecraftagent.common.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.noah.minecraftagent.common.config.AgentConfigStore;
import com.noah.minecraftagent.common.provider.ChatResponse;
import com.noah.minecraftagent.common.util.SecureLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.stream.Stream;

public final class CacheManager {
    private static final Duration TTL = Duration.ofDays(7);
    private static final int MAX_CACHE_FILES = 200;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path cacheDir = AgentConfigStore.getInstance().configDir().resolve("cache");

    public Optional<ChatResponse> readResponse(String key) {
        Path file = cacheDir.resolve(key + ".json");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            CachedResponse cached = gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), CachedResponse.class);
            if (cached == null || cached.response == null) {
                Files.deleteIfExists(file);
                return Optional.empty();
            }
            if (Instant.now().isAfter(Instant.parse(cached.createdAt).plus(TTL))) {
                Files.deleteIfExists(file);
                return Optional.empty();
            }
            cached.response.cached = true;
            return Optional.of(cached.response);
        } catch (IOException exception) {
            SecureLog.error("Failed to read response cache", exception);
            return Optional.empty();
        }
    }

    public void writeResponse(String key, ChatResponse response) {
        try {
            Files.createDirectories(cacheDir);
            evictIfNeeded();
            CachedResponse cached = new CachedResponse();
            cached.createdAt = Instant.now().toString();
            cached.response = response;
            Files.writeString(cacheDir.resolve(key + ".json"), gson.toJson(cached), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            SecureLog.error("Failed to write response cache", exception);
        }
    }

    private void evictIfNeeded() {
        try (Stream<Path> files = Files.list(cacheDir)) {
            long count = files.count();
            if (count <= MAX_CACHE_FILES) {
                return;
            }
        } catch (IOException ignored) {
            return;
        }
        try (Stream<Path> files = Files.list(cacheDir)) {
            files.filter(path -> path.toString().endsWith(".json"))
                    .sorted((a, b) -> Long.compare(b.toFile().lastModified(), a.toFile().lastModified()))
                    .skip(MAX_CACHE_FILES)
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    public static String key(String raw) {
        return sha256(raw);
    }

    public static String sha256(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class CachedResponse {
        String createdAt;
        ChatResponse response;
    }
}
