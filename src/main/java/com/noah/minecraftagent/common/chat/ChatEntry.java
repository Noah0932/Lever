package com.noah.minecraftagent.common.chat;

public record ChatEntry(String threadId, String role, String content, long timestamp) {
    public String formatDisplay() {
        return "[" + formatTime(timestamp) + "] " + content;
    }

    private static String formatTime(long ms) {
        int totalSec = (int) (ms / 1000) % 86400;
        int h = totalSec / 3600;
        int m = (totalSec % 3600) / 60;
        return String.format("%02d:%02d", h, m);
    }
}
