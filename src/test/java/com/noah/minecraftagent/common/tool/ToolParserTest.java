package com.noah.minecraftagent.common.tool;

import com.noah.minecraftagent.common.provider.AgentToolCall;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for ToolParser - validates tool call argument extraction.
 */
class ToolParserTest {

    @Test
    void shouldParseExecuteCommandToolCall() {
        AgentToolCall call = new AgentToolCall("call_1", "execute_command", "{\"command\":\"time set day\"}");
        var result = ToolParser.executeCommand(call);
        assertTrue(result.isPresent());
        assertEquals("time set day", result.get());
    }

    @Test
    void shouldReturnEmptyForNonExecuteCommand() {
        AgentToolCall call = new AgentToolCall("call_1", "other_tool", "{\"command\":\"stop\"}");
        var result = ToolParser.executeCommand(call);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyForMissingCommandField() {
        AgentToolCall call = new AgentToolCall("call_1", "execute_command", "{\"other\":\"value\"}");
        var result = ToolParser.executeCommand(call);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyForInvalidJson() {
        AgentToolCall call = new AgentToolCall("call_1", "execute_command", "not-json");
        var result = ToolParser.executeCommand(call);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNullJson() {
        AgentToolCall call = new AgentToolCall("call_1", "execute_command", "null");
        var result = ToolParser.executeCommand(call);
        assertTrue(result.isEmpty());
    }
}
