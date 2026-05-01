package com.noah.minecraftagent.server;

import com.noah.minecraftagent.common.network.CommandResultPayload;
import com.noah.minecraftagent.common.tool.CommandSafety;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class ServerCommandExecutor {
    private ServerCommandExecutor() {
    }

    public static void execute(ServerPlayerEntity player, String rawCommand) {
        CommandSafety.SafetyResult safety = CommandSafety.validate(rawCommand);
        if (!safety.allowed()) {
            send(player, false, safety.reason());
            return;
        }
        if (!player.hasPermissionLevel(2) && !player.getServer().isSingleplayer()) {
            send(player, false, "AI command execution requires OP permission on this server");
            return;
        }
        try {
            ServerCommandSource source = player.getCommandSource().withSilent().withLevel(player.hasPermissionLevel(2) ? 2 : 4);
            player.getServer().getCommandManager().executeWithPrefix(source, safety.normalizedCommand());
            send(player, true, "Executed /" + safety.normalizedCommand());
        } catch (Exception exception) {
            send(player, false, "Command failed: " + exception.getMessage());
        }
    }

    private static void send(ServerPlayerEntity player, boolean success, String message) {
        player.sendMessage(Text.literal("[AI Agent] " + message), false);
        ServerPlayNetworking.send(player, new CommandResultPayload(success, message));
    }
}

