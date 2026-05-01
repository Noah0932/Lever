package com.noah.minecraftagent.common.tool;

import java.util.Set;

public final class CommandSafety {
    private static final Set<String> BLOCKED = Set.of(
            "stop", "op", "deop", "ban", "ban-ip", "pardon", "pardon-ip", "kick", "whitelist",
            "save-off", "reload", "function", "datapack",
            "fill", "setblock", "clone", "execute", "summon", "give", "enchant",
            "xp", "experience", "gamemode", "defaultgamemode", "difficulty",
            "seed", "bossbar", "clear",
            "data", "debug", "forceload", "locate", "loot", "me",
            "msg", "tell", "w", "particle", "place", "playsound",
            "publish", "recipe", "return", "ride", "schedule",
            "scoreboard", "spectate", "spreadplayers", "stopsound",
            "team", "teammsg", "teleport", "tellraw", "title",
            "trigger", "worldborder"
    );

    private static final Set<String> ALLOWED = Set.of(
            "say", "tp", "time", "weather", "gamerule"
    );

    private CommandSafety() {
    }

    public static SafetyResult validate(String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) {
            return new SafetyResult(false, "Empty command", "");
        }
        String normalized = rawCommand.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        String root = normalized.split("\\s+", 2)[0].toLowerCase();
        if (BLOCKED.contains(root)) {
            return new SafetyResult(false, "Blocked dangerous command: /" + root, normalized);
        }
        if (!ALLOWED.contains(root)) {
            return new SafetyResult(false, "Command not in allowlist: /" + root, normalized);
        }
        return new SafetyResult(true, "OK", normalized);
    }

    public record SafetyResult(boolean allowed, String reason, String normalizedCommand) {
    }
}
