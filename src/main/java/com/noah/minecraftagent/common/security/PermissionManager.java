package com.noah.minecraftagent.common.security;

import com.noah.minecraftagent.common.bot.BotProfile;

public final class PermissionManager {
    private PermissionManager() {
    }

    public static boolean canAccess(BotProfile profile, String playerUuid, boolean isOp) {
        if (profile == null || playerUuid == null) {
            return false;
        }
        if (isOp) {
            return true;
        }
        if (playerUuid.equals(profile.ownerUuid)) {
            return true;
        }
        if (profile.whitelist != null && profile.whitelist.contains(playerUuid)) {
            return true;
        }
        return false;
    }

    public static boolean canModify(BotProfile profile, String playerUuid, boolean isOp) {
        if (profile == null || playerUuid == null) {
            return false;
        }
        if (playerUuid.equals(profile.ownerUuid)) {
            return true;
        }
        return isOp;
    }

    public static boolean canTransfer(BotProfile profile, String playerUuid, boolean isOp) {
        if (profile == null || playerUuid == null) {
            return false;
        }
        return playerUuid.equals(profile.ownerUuid);
    }
}
