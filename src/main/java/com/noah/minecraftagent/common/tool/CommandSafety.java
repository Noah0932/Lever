package com.noah.minecraftagent.common.tool;

import java.util.Set;

public final class CommandSafety {
    private static final Set<String> BLOCKED = Set.of(
            "stop", "op", "deop", "ban", "ban-ip", "pardon", "pardon-ip", "kick", "whitelist",
            "save-off", "reload", "function", "datapack"
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
        return new SafetyResult(true, "OK", normalized);
    }

    public record SafetyResult(boolean allowed, String reason, String normalizedCommand) {
    }
}
