package com.noah.minecraftagent.common.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.noah.minecraftagent.common.util.SecureLog;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatHistoryManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ENTRY_LIST_TYPE = new TypeToken<List<ChatEntry>>() {}.getType();

    private final Map<String, List<ChatEntry>> conversations = new ConcurrentHashMap<>();
    private final Path historyDir;
    private int maxEntries = 200;

    public ChatHistoryManager(Path historyDir) {
        this.historyDir = historyDir;
    }

    public void setMaxEntries(int max) {
        this.maxEntries = max;
    }

    public void addEntry(String threadId, String role, String content) {
        ChatEntry entry = new ChatEntry(threadId, role, content, System.currentTimeMillis());
        List<ChatEntry> entries = conversations.computeIfAbsent(threadId,
                k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (entries) {
            entries.add(entry);
            while (entries.size() > maxEntries) {
                entries.remove(0);
            }
        }
    }

    public List<ChatEntry> getEntries(String threadId, int offset, int limit) {
        List<ChatEntry> entries = conversations.getOrDefault(threadId, Collections.emptyList());
        synchronized (entries) {
            if (offset >= entries.size()) {
                return Collections.emptyList();
            }
            int to = Math.min(offset + limit, entries.size());
            return new ArrayList<>(entries.subList(offset, to));
        }
    }

    public int countEntries(String threadId) {
        List<ChatEntry> entries = conversations.get(threadId);
        if (entries == null) return 0;
        synchronized (entries) {
            return entries.size();
        }
    }

    public void clearThread(String threadId) {
        conversations.remove(threadId);
    }

    public void persist() {
        try {
            Files.createDirectories(historyDir);
            for (Map.Entry<String, List<ChatEntry>> entry : conversations.entrySet()) {
                String safeName = entry.getKey().replaceAll("[^a-zA-Z0-9._-]", "_");
                Path file = historyDir.resolve(safeName + ".json");
                List<ChatEntry> list = entry.getValue();
                List<ChatEntry> snapshot;
                synchronized (list) {
                    snapshot = new ArrayList<>(list);
                }
                String json = GSON.toJson(snapshot);
                Files.writeString(file, json, StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            SecureLog.error("Failed to persist chat history", exception);
        }
    }

    public void load() {
        try {
            if (!Files.exists(historyDir)) {
                return;
            }
            Files.list(historyDir).filter(p -> p.toString().endsWith(".json")).forEach(file -> {
                try {
                    String json = Files.readString(file, StandardCharsets.UTF_8);
                    List<ChatEntry> entries = GSON.fromJson(json, ENTRY_LIST_TYPE);
                    if (entries != null && !entries.isEmpty()) {
                        String threadId = entries.get(0).threadId();
                        List<ChatEntry> list = conversations.computeIfAbsent(threadId,
                                k -> Collections.synchronizedList(new ArrayList<>()));
                        synchronized (list) {
                            for (ChatEntry e : entries) {
                                list.add(e);
                                while (list.size() > maxEntries) {
                                    list.remove(0);
                                }
                            }
                        }
                    }
                } catch (IOException exception) {
                    SecureLog.error("Failed to load chat history file", exception);
                }
            });
        } catch (IOException exception) {
            SecureLog.error("Failed to list chat history directory", exception);
        }
    }
}
