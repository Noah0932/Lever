package com.noah.minecraftagent.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class ServerAiCommands {
    private ServerAiCommands() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("ai")
                .then(CommandManager.argument("prompt", StringArgumentType.greedyString())
                        .executes(ServerAiCommands::selfPrompt))
                .then(CommandManager.literal("ask")
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .then(CommandManager.argument("prompt", StringArgumentType.greedyString())
                                        .executes(ServerAiCommands::askTarget))))
                .then(CommandManager.literal("allow")
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(context -> delegateConfig(context, "allow"))))
                .then(CommandManager.literal("deny")
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(context -> delegateConfig(context, "deny"))))
                .then(CommandManager.literal("list")
                        .executes(context -> delegateConfig(context, "list"))));
    }

    private static int selfPrompt(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String prompt = StringArgumentType.getString(context, "prompt");
        ServerPromptRouter.request(player, player, prompt, true, true);
        return 1;
    }

    private static int askTarget(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity requester = context.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        String prompt = StringArgumentType.getString(context, "prompt");
        boolean operatorApproved = requester.hasPermissionLevel(2);
        ServerPromptRouter.request(requester, target, prompt, operatorApproved, requester.getUuid().equals(target.getUuid()));
        return 1;
    }

    private static int delegateConfig(CommandContext<ServerCommandSource> context, String action) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String entry = action.equals("list") ? "" : StringArgumentType.getString(context, "player");
        if (!net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, com.noah.minecraftagent.common.network.DelegateConfigPayload.ID)) {
            player.sendMessage(Text.translatable("message.minecraftagent.delegate.unavailable", player.getName().getString()), false);
            return 0;
        }
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.noah.minecraftagent.common.network.DelegateConfigPayload(action, entry));
        return 1;
    }
}
