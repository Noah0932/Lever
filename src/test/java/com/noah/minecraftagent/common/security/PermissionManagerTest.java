package com.noah.minecraftagent.common.security;

import com.noah.minecraftagent.common.bot.BotProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD: PermissionManager — owner/whitelist/OP access control.
 */
class PermissionManagerTest {

    private BotProfile profile;
    private static final String OWNER_UUID = "00000000-0000-0000-0000-00000000AAAA";
    private static final String WHITELIST_UUID = "00000000-0000-0000-0000-00000000BBBB";
    private static final String STRANGER_UUID = "00000000-0000-0000-0000-00000000CCCC";
    private static final String OP_UUID = "00000000-0000-0000-0000-00000000DDDD";
    private static final String OTHER_UUID = "00000000-0000-0000-0000-00000000EEEE";

    @BeforeEach
    void setup() {
        profile = new BotProfile();
        profile.ownerUuid = OWNER_UUID;
        profile.ownerName = "Owner";
        profile.whitelist = new ArrayList<>();
        profile.whitelist.add(WHITELIST_UUID);
    }

    @Test
    void shouldAllowOwnerAccess() {
        assertTrue(PermissionManager.canAccess(profile, OWNER_UUID, false),
                "Owner must have access to their own bot");
    }

    @Test
    void shouldAllowWhitelistAccess() {
        assertTrue(PermissionManager.canAccess(profile, WHITELIST_UUID, false),
                "Whitelisted player must have access");
    }

    @Test
    void shouldAllowOpAccess() {
        assertTrue(PermissionManager.canAccess(profile, OP_UUID, true),
                "OP (level 2+) must have access to any bot");
    }

    @Test
    void shouldDenyStrangerAccess() {
        assertFalse(PermissionManager.canAccess(profile, STRANGER_UUID, false),
                "Unauthorized stranger must be denied access");
    }

    @Test
    void shouldDenyAccessWhenWhitelistIsNull() {
        profile.whitelist = null;
        assertFalse(PermissionManager.canAccess(profile, STRANGER_UUID, false),
                "Null whitelist must be handled as empty");
    }

    @Test
    void shouldAllowOwnerModify() {
        assertTrue(PermissionManager.canModify(profile, OWNER_UUID, false),
                "Owner must be able to modify bot settings");
    }

    @Test
    void shouldAllowOpModify() {
        assertTrue(PermissionManager.canModify(profile, OP_UUID, true),
                "OP must be able to modify bot settings");
    }

    @Test
    void shouldDenyWhitelistModify() {
        assertFalse(PermissionManager.canModify(profile, WHITELIST_UUID, false),
                "Whitelisted player must NOT be able to modify bot settings");
    }

    @Test
    void shouldDenyStrangerModify() {
        assertFalse(PermissionManager.canModify(profile, STRANGER_UUID, false),
                "Stranger must NOT be able to modify bot settings");
    }

    @Test
    void shouldAllowOwnerTransfer() {
        assertTrue(PermissionManager.canTransfer(profile, OWNER_UUID, false),
                "Only owner must be able to transfer ownership");
    }

    @Test
    void shouldDenyOpTransfer() {
        assertFalse(PermissionManager.canTransfer(profile, OP_UUID, true),
                "OP must NOT be able to steal bot ownership");
    }

    @Test
    void shouldDenyWhitelistTransfer() {
        assertFalse(PermissionManager.canTransfer(profile, WHITELIST_UUID, false),
                "Whitelisted player must NOT be able to transfer ownership");
    }

    @Test
    void shouldDenyStrangerTransfer() {
        assertFalse(PermissionManager.canTransfer(profile, STRANGER_UUID, false),
                "Stranger must NOT be able to transfer ownership");
    }

    @Test
    void shouldHandleNullProfileGracefully() {
        assertFalse(PermissionManager.canAccess(null, STRANGER_UUID, false),
                "Null profile must be handled safely");
    }

    @Test
    void shouldHandleNullOwnerUuidGracefully() {
        profile.ownerUuid = null;
        assertFalse(PermissionManager.canAccess(profile, OWNER_UUID, false),
                "Null ownerUuid must be handled safely");
    }
}
