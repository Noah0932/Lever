package com.noah.minecraftagent.common.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

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
        List<ChatEntry> entries = conversations.computeIfAbsent(threadId, k -> new ArrayList<>());
        entries.add(entry);
        while (entries.size() > maxEntries) {
            entries.remove(0);
        }
    }

    public List<ChatEntry> getEntries(String threadId, int offset, int limit) {
        List<ChatEntry> entries = conversations.getOrDefault(threadId, Collections.emptyList());
        if (offset >= entries.size()) {
            return Collections.emptyList();
        }
        int to = Math.min(offset + limit, entries.size());
        return new ArrayList<>(entries.subList(offset, to));
    }

    public int countEntries(String threadId) {
        List<ChatEntry> entries = conversations.get(threadId);
        return entries == null ? 0 : entries.size();
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
                String json = GSON.toJson(entry.getValue());
                Files.writeString(file, json, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
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
                        List<ChatEntry> list = conversations.computeIfAbsent(threadId, k -> new ArrayList<>());
                        for (ChatEntry e : entries) {
                            list.add(e);
                            while (list.size() > maxEntries) {
                                list.remove(0);
                            }
                        }
                    }
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
