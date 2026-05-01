package com.noah.minecraftagent.common.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD: ChatHistoryManager — subList concurrency safety and persistence round-trip.
 */
class ChatHistorySubListConcurrencyTest {

    private ChatHistoryManager manager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        manager = new ChatHistoryManager(tempDir);
        manager.setMaxEntries(500);
    }

    @Test
    void shouldNotThrowConcurrentModificationOnSubListAccess() throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            manager.addEntry("bot-X", "user", "prefill-" + i);
        }

        AtomicInteger errors = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int t = 0; t < 5; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 100; i++) {
                        manager.addEntry("bot-X", "user", "msg-" + Thread.currentThread().getId() + "-" + i);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        for (int t = 0; t < 5; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 200; i++) {
                        try {
                            manager.getEntries("bot-X", 0, 50);
                        } catch (Exception e) {
                            errors.incrementAndGet();
                            fail("getEntries must not throw: " + e.getClass().getSimpleName());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertEquals(0, errors.get(), "Concurrent getEntries must never throw");
    }

    @Test
    void shouldPersistAndReloadWithSynchronizedSafety() throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            manager.addEntry("bot-PS", "user", "msg-" + i);
        }

        manager.persist();

        ChatHistoryManager reloaded = new ChatHistoryManager(tempDir);
        reloaded.setMaxEntries(200);
        reloaded.load();

        List<ChatEntry> entries = reloaded.getEntries("bot-PS", 0, 100);
        assertEquals(50, entries.size());
        assertEquals("msg-0", entries.get(0).content());
        assertEquals("msg-49", entries.get(49).content());
    }

    @Test
    void shouldReturnEmptyForNonExistentThread() {
        List<ChatEntry> entries = manager.getEntries("nonexistent", 0, 10);
        assertTrue(entries.isEmpty());
    }

    @Test
    void shouldReturnCorrectSubListRange() {
        for (int i = 0; i < 10; i++) {
            manager.addEntry("bot-R", "user", "r-" + i);
        }
        List<ChatEntry> page = manager.getEntries("bot-R", 3, 4);
        assertEquals(4, page.size());
        assertEquals("r-3", page.get(0).content());
        assertEquals("r-6", page.get(3).content());
    }
}
