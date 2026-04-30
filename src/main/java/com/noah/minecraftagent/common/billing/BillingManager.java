package com.noah.minecraftagent.common.billing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.noah.minecraftagent.common.config.AgentConfigStore;
import com.noah.minecraftagent.common.config.AgentProfile;
import com.noah.minecraftagent.common.provider.Usage;
import com.noah.minecraftagent.common.util.SecureLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public final class BillingManager {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path statsFile = AgentConfigStore.getInstance().configDir().resolve("stats.json");
    private Stats stats = new Stats();

    public BillingManager() {
        load();
    }

    public synchronized boolean isBlocked(AgentProfile profile) {
        return today().costCny >= profile.dailyLimitCny;
    }

    public synchronized double todayCostCny() {
        return today().costCny;
    }

    public synchronized double estimate(AgentProfile profile, int inputTokens, int outputTokens) {
        return profile.estimateCny(inputTokens, outputTokens);
    }

    public synchronized void record(AgentProfile profile, Usage usage) {
        DayStats day = today();
        day.inputTokens += usage.inputTokens();
        day.outputTokens += usage.outputTokens();
        if (usage.estimated()) {
            day.estimatedRequests++;
        }
        day.requests++;
        day.costCny += profile.estimateCny(usage.inputTokens(), usage.outputTokens());
        save();
    }

    private DayStats today() {
        return stats.days.computeIfAbsent(LocalDate.now().toString(), ignored -> new DayStats());
    }

    private void load() {
        try {
            if (Files.exists(statsFile)) {
                stats = gson.fromJson(Files.readString(statsFile, StandardCharsets.UTF_8), Stats.class);
                if (stats == null) {
                    stats = new Stats();
                }
            }
        } catch (IOException exception) {
            SecureLog.error("Failed to load billing stats", exception);
        }
    }

    private void save() {
        try {
            Files.createDirectories(statsFile.getParent());
            Files.writeString(statsFile, gson.toJson(stats), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            SecureLog.error("Failed to save billing stats", exception);
        }
    }

    private static final class Stats {
        Map<String, DayStats> days = new HashMap<>();
    }

    private static final class DayStats {
        int requests;
        int estimatedRequests;
        int inputTokens;
        int outputTokens;
        double costCny;
    }
}
