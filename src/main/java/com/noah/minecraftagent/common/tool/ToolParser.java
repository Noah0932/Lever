package com.noah.minecraftagent.common.tool;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.noah.minecraftagent.common.provider.AgentToolCall;

import java.util.Optional;

public final class ToolParser {
    private static final Gson GSON = new Gson();

    private ToolParser() {
    }

    public static Optional<String> executeCommand(AgentToolCall call) {
        if (!"execute_command".equals(call.name())) {
            return Optional.empty();
        }
        try {
            JsonObject object = GSON.fromJson(call.argumentsJson(), JsonObject.class);
            if (object == null || !object.has("command")) {
                return Optional.empty();
            }
            return Optional.of(object.get("command").getAsString());
        } catch (JsonSyntaxException exception) {
            return Optional.empty();
        }
    }
}
