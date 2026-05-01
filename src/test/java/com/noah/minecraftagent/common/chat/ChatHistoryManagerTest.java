package com.noah.minecraftagent.common.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD: ChatHistoryManager — conversation storage, persistence, and word wrapping.
 */
class ChatHistoryManagerTest {

    private ChatHistoryManager manager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        manager = new ChatHistoryManager(tempDir);
        manager.setMaxEntries(10);
    }

    @Test
    void shouldAddEntryAndRespectMaxLimit() {
        for (int i = 0; i < 25; i++) {
            manager.addEntry("bot-1", "user", "message-" + i);
        }
        List<ChatEntry> entries = manager.getEntries("bot-1", 0, 50);
        assertEquals(10, entries.size(), "Max entries (10) must be enforced even after 25 additions");
        assertEquals("message-24", entries.get(entries.size() - 1).content());
        assertEquals("message-15", entries.get(0).content());
    }

    @Test
    void shouldPersistAndReloadRoundTrip() {
        manager.addEntry("bot-1", "user", "Hello Bot");
        manager.addEntry("bot-1", "bot", "Hello User");
        manager.persist();

        ChatHistoryManager reloaded = new ChatHistoryManager(tempDir);
        reloaded.load();
        List<ChatEntry> entries = reloaded.getEntries("bot-1", 0, 50);
        assertEquals(2, entries.size());
        assertEquals("Hello Bot", entries.get(0).content());
        assertEquals("Hello User", entries.get(1).content());
    }

    @Test
    void shouldGroupEntriesByThreadId() {
        manager.addEntry("bot-A", "user", "msg-A1");
        manager.addEntry("bot-B", "user", "msg-B1");
        manager.addEntry("bot-A", "bot", "msg-A2");

        assertEquals(2, manager.getEntries("bot-A", 0, 50).size());
        assertEquals(1, manager.getEntries("bot-B", 0, 50).size());
        assertEquals(0, manager.getEntries("bot-C", 0, 50).size());
    }

    @Test
    void shouldSupportPagination() {
        for (int i = 0; i < 8; i++) {
            manager.addEntry("bot-1", "user", "m-" + i);
        }
        List<ChatEntry> page1 = manager.getEntries("bot-1", 0, 3);
        List<ChatEntry> page2 = manager.getEntries("bot-1", 3, 3);
        List<ChatEntry> page3 = manager.getEntries("bot-1", 6, 3);
        assertEquals(3, page1.size());
        assertEquals(3, page2.size());
        assertEquals(2, page3.size());
        assertEquals("m-0", page1.get(0).content());
        assertEquals("m-5", page2.get(2).content());
    }

    @Test
    void shouldClearEntriesForThreadId() {
        manager.addEntry("bot-1", "user", "test");
        manager.addEntry("bot-2", "user", "keep");
        manager.clearThread("bot-1");
        assertEquals(0, manager.getEntries("bot-1", 0, 50).size());
        assertEquals(1, manager.getEntries("bot-2", 0, 50).size());
    }

    @Test
    void shouldReturnEmptyForUnknownThread() {
        List<ChatEntry> entries = manager.getEntries("nonexistent", 0, 10);
        assertTrue(entries.isEmpty());
    }

    @Test
    void shouldIncludeTimestampInEntry() {
        long before = System.currentTimeMillis();
        manager.addEntry("t1", "user", "test");
        long after = System.currentTimeMillis();
        ChatEntry entry = manager.getEntries("t1", 0, 1).get(0);
        assertTrue(entry.timestamp() >= before);
        assertTrue(entry.timestamp() <= after);
    }
}
