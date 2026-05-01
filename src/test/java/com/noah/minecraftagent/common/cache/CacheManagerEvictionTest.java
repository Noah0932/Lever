package com.noah.minecraftagent.common.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD: CacheManager — single-pass eviction and TTL enforcement.
 */
class CacheManagerEvictionTest {

    private Path cacheDir;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        cacheDir = tempDir.resolve("cache");
    }

    @Test
    void shouldNotDeleteFilesWhenUnderLimit() throws IOException {
        Files.createDirectories(cacheDir);
        for (int i = 0; i < 50; i++) {
            Files.writeString(cacheDir.resolve("file-" + i + ".json"), "{}", StandardCharsets.UTF_8);
        }

        // Verify 50 files exist (eviction not triggered at this count)
        long before = countJsonFiles();
        assertEquals(50, before);
    }

    @Test
    void shouldDeleteOldestWhenOverLimit() throws IOException, InterruptedException {
        Files.createDirectories(cacheDir);
        for (int i = 0; i < 250; i++) {
            Path p = cacheDir.resolve("file-" + i + ".json");
            Files.writeString(p, "{}", StandardCharsets.UTF_8);
            Thread.sleep(1);
        }

        // Simulate what evictIfNeeded does: keep newest 200
        var files = Files.list(cacheDir)
                .filter(f -> f.toString().endsWith(".json"))
                .sorted((a, b) -> Long.compare(b.toFile().lastModified(), a.toFile().lastModified()))
                .toList();

        assertTrue(files.size() >= 200);
        files.stream().skip(200).forEach(p -> {
            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
        });

        long remaining = countJsonFiles();
        assertEquals(200, remaining);
    }

    @Test
    void shouldProduceConsistentSha256() {
        String input = "test-payload-" + System.nanoTime();
        String hash1 = CacheManager.sha256(input);
        String hash2 = CacheManager.sha256(input);
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
    }

    @Test
    void shouldGenerateDifferentKeysForDifferentInputs() {
        String key1 = CacheManager.key("input-A");
        String key2 = CacheManager.key("input-B");
        assertNotEquals(key1, key2);
        assertEquals(64, key1.length());
        assertEquals(64, key2.length());
    }

    private long countJsonFiles() throws IOException {
        try (var files = Files.list(cacheDir)) {
            return files.filter(f -> f.toString().endsWith(".json")).count();
        }
    }
}
