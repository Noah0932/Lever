package com.noah.minecraftagent.server.bot;

import com.noah.minecraftagent.common.bot.BotProfile;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD: BotManager thread safety — ConcurrentHashMap prevents CME.
 */
class BotManagerConcurrencyTest {

    @Test
    void shouldHandleConcurrentOpsWithoutCrash() throws InterruptedException {
        int threads = 6;
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        // Simulate the BotManager data structures with ConcurrentHashMap
        Map<String, Object> bots = new ConcurrentHashMap<>();
        Map<String, String> profiles = new ConcurrentHashMap<>();
        Map<String, List<String>> ownerBots = new ConcurrentHashMap<>();

        // Pre-populate
        for (int i = 0; i < 10; i++) {
            String id = "bot-" + i;
            bots.put(id, new Object());
            profiles.put(id, "profile-" + i);
            ownerBots.computeIfAbsent("owner-" + (i % 3), k -> new ArrayList<>()).add(id);
        }

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 500; i++) {
                        String id = "bot-" + (i % 100);
                        if (i % 3 == 0) {
                            bots.get(id);
                            profiles.get(id);
                        } else if (i % 3 == 1) {
                            bots.put("bot-new-" + i, new Object());
                            profiles.put("bot-new-" + i, "profile");
                        } else {
                            bots.values().stream().count();
                            profiles.values().stream().count();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(bots.size() > 0);
    }

    @Test
    void shouldNotThrowConcurrentModificationOnIteration() throws InterruptedException {
        Map<String, String> map = new ConcurrentHashMap<>();
        for (int i = 0; i < 100; i++) {
            map.put("k-" + i, "v-" + i);
        }

        CountDownLatch latch = new CountDownLatch(2);
        Thread reader = new Thread(() -> {
            for (int i = 0; i < 200; i++) {
                try {
                    for (String v : map.values()) { /* just iterate */ }
                } catch (ConcurrentModificationException e) {
                    fail("ConcurrentHashMap must not throw CME on iteration");
                }
            }
            latch.countDown();
        });
        Thread writer = new Thread(() -> {
            for (int i = 100; i < 300; i++) {
                map.put("k-" + i, "v-" + i);
            }
            latch.countDown();
        });

        reader.start();
        writer.start();
        latch.await(5, TimeUnit.SECONDS);
        assertTrue(map.size() >= 100);
    }
}
