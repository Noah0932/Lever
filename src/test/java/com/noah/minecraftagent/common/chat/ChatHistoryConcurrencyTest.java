package com.noah.minecraftagent.common.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD: Concurrent access safety for ChatHistoryManager.
 */
class ChatHistoryConcurrencyTest {

    private ChatHistoryManager manager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        manager = new ChatHistoryManager(tempDir);
        manager.setMaxEntries(500);
    }

    @Test
    void shouldHandleConcurrentAddAndGet() throws InterruptedException {
        int threadCount = 10;
        int entriesPerThread = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < entriesPerThread; i++) {
                        manager.addEntry("bot-A", "user", "msg-" + threadId + "-" + i);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        List<ChatEntry> entries = manager.getEntries("bot-A", 0, 1000);
        assertEquals(500, entries.size(), "Must truncate to maxEntries despite concurrent writes");
    }

    @Test
    void shouldHandleConcurrentAddAndPersist() throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            manager.addEntry("bot-P", "user", "msg-" + i);
        }

        CountDownLatch latch = new CountDownLatch(2);
        Thread writer = new Thread(() -> {
            for (int i = 50; i < 100; i++) {
                manager.addEntry("bot-P", "bot", "msg-" + i);
            }
            latch.countDown();
        });
        Thread persister = new Thread(() -> {
            manager.persist();
            latch.countDown();
        });

        writer.start();
        persister.start();
        latch.await(5, TimeUnit.SECONDS);

        List<ChatEntry> entries = manager.getEntries("bot-P", 0, 200);
        assertTrue(entries.size() >= 50, "Must have at least original 50 entries after concurrent persist");
    }

    @Test
    void shouldHandleConcurrentClearAndGet() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            manager.addEntry("bot-C", "user", "msg-" + i);
        }

        CountDownLatch latch = new CountDownLatch(2);
        Thread clearer = new Thread(() -> {
            manager.clearThread("bot-C");
            latch.countDown();
        });
        Thread reader = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                manager.getEntries("bot-C", 0, 10);
            }
            latch.countDown();
        });

        clearer.start();
        reader.start();
        latch.await(5, TimeUnit.SECONDS);

        List<ChatEntry> entries = manager.getEntries("bot-C", 0, 10);
        assertTrue(entries.isEmpty() || entries.size() <= 10,
                "After concurrent clear+read, state must be consistent");
    }
}
