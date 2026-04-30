package com.noah.minecraftagent.common.tool;

import com.noah.minecraftagent.common.tool.CommandSafety.SafetyResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CommandSafetyTest {

    @Test
    void shouldStripLeadingSlash() {
        SafetyResult result = CommandSafety.validate("/time set day");
        assertTrue(result.allowed());
        assertEquals("time set day", result.normalizedCommand());
    }

    @Test
    void shouldStripMultipleLeadingSlashes() {
        SafetyResult result = CommandSafety.validate("///tp 0 64 0");
        assertTrue(result.allowed());
        assertEquals("tp 0 64 0", result.normalizedCommand());
    }

    @Test
    void shouldBlockBanCommand() {
        SafetyResult result = CommandSafety.validate("ban Noah");
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("ban"));
    }

    @Test
    void shouldBlockOpCommand() {
        SafetyResult result = CommandSafety.validate("/op Noah");
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("op"));
    }

    @Test
    void shouldBlockStopCommand() {
        SafetyResult result = CommandSafety.validate("stop");
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("stop"));
    }

    @Test
    void shouldBlockDeopCommand() {
        SafetyResult result = CommandSafety.validate("/deop player");
        assertFalse(result.allowed());
    }

    @Test
    void shouldBlockKickCommand() {
        SafetyResult result = CommandSafety.validate("/kick player");
        assertFalse(result.allowed());
    }

    @Test
    void shouldBlockWhitelistCommand() {
        SafetyResult result = CommandSafety.validate("/whitelist add player");
        assertFalse(result.allowed());
    }

    @Test
    void shouldBlockReloadCommand() {
        SafetyResult result = CommandSafety.validate("/reload");
        assertFalse(result.allowed());
    }

    @Test
    void shouldBlockFunctionCommand() {
        SafetyResult result = CommandSafety.validate("/function namespace:path");
        assertFalse(result.allowed());
    }

    @Test
    void shouldBlockDatapackCommand() {
        SafetyResult result = CommandSafety.validate("/datapack disable");
        assertFalse(result.allowed());
    }

    @Test
    void shouldBlockSaveOffCommand() {
        SafetyResult result = CommandSafety.validate("/save-off");
        assertFalse(result.allowed());
    }

    @Test
    void shouldBlockBanIpCommand() {
        SafetyResult result = CommandSafety.validate("/ban-ip 127.0.0.1");
        assertFalse(result.allowed());
    }

    @Test
    void shouldBlockPardonCommand() {
        SafetyResult result = CommandSafety.validate("/pardon player");
        assertFalse(result.allowed());
    }

    @Test
    void shouldBlockPardonIpCommand() {
        SafetyResult result = CommandSafety.validate("/pardon-ip 127.0.0.1");
        assertFalse(result.allowed());
    }

    @Test
    void shouldNormalizeTrimmedCommand() {
        SafetyResult result = CommandSafety.validate("  gamerule doDaylightCycle true  ");
        assertTrue(result.allowed());
        assertEquals("gamerule doDaylightCycle true", result.normalizedCommand());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void shouldRejectNullOrEmptyCommand(String input) {
        SafetyResult result = CommandSafety.validate(input);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Empty"));
    }

    @Test
    void shouldAllowNormalCommands() {
        SafetyResult result = CommandSafety.validate("time set day");
        assertTrue(result.allowed());
    }

    @Test
    void shouldBeCaseInsensitiveForRootCommand() {
        SafetyResult result = CommandSafety.validate("/STOP");
        assertFalse(result.allowed());
    }
}
