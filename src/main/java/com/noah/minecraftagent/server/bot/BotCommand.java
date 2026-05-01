package com.noah.minecraftagent.server.bot;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.noah.minecraftagent.common.bot.BotProfile;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public final class BotCommand {
    private BotCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("bot")
                .then(CommandManager.literal("list").executes(BotCommand::listBots))
                .then(CommandManager.literal("remove").then(CommandManager.argument("botName", StringArgumentType.word()).executes(BotCommand::removeBot)))
                .then(CommandManager.literal("tp").then(CommandManager.argument("botName", StringArgumentType.word()).executes(BotCommand::tpToBot)))
                .then(CommandManager.literal("rename").then(CommandManager.argument("botName", StringArgumentType.word()).then(CommandManager.argument("newName", StringArgumentType.greedyString()).executes(BotCommand::renameBot))))
                .then(CommandManager.literal("skin").then(CommandManager.argument("botName", StringArgumentType.word()).then(CommandManager.argument("playerName", StringArgumentType.word()).executes(BotCommand::setSkin)))));
    }

    private static int listBots(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        List<BotEntity> bots = BotManager.getInstance().getBotsByOwner(player.getUuidAsString());
        if (bots.isEmpty()) {
            player.sendMessage(Text.literal("[Lever] 你没有活跃的 Bot").formatted(Formatting.AQUA), false);
            return 1;
        }
        player.sendMessage(Text.literal("[Lever] 你的 Bots:").formatted(Formatting.AQUA), false);
        for (BotEntity bot : bots) {
            player.sendMessage(Text.literal("  - " + bot.getBotProfile().displayName()).formatted(Formatting.WHITE), false);
        }
        return bots.size();
    }

    private static int removeBot(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(context, "botName");
        var bot = BotManager.getInstance().getBotByName(name);
        if (bot.isEmpty()) {
            player.sendMessage(Text.literal("[Lever] Bot 未找到").formatted(Formatting.AQUA), false);
            return 0;
        }
        BotEntity entity = bot.get();
        if (!entity.getBotProfile().isOwner(player.getUuidAsString()) && !player.hasPermissionLevel(2)) {
            player.sendMessage(Text.literal("[Lever] 你不是该 Bot 的所有者").formatted(Formatting.AQUA), false);
            return 0;
        }
        BotManager.getInstance().remove(entity);
        player.sendMessage(Text.literal("[Lever] Bot " + name + " 已移除").formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int tpToBot(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(context, "botName");
        var bot = BotManager.getInstance().getBotByName(name);
        if (bot.isEmpty()) {
            player.sendMessage(Text.literal("[Lever] Bot 未找到").formatted(Formatting.AQUA), false);
            return 0;
        }
        player.teleport(player.getServerWorld(), bot.get().getX(), bot.get().getY(), bot.get().getZ(), player.getYaw(), player.getPitch());
        player.sendMessage(Text.literal("[Lever] 已传送至 " + name).formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int renameBot(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(context, "botName");
        String newName = StringArgumentType.getString(context, "newName");
        var bot = BotManager.getInstance().getBotByName(name);
        if (bot.isEmpty()) {
            player.sendMessage(Text.literal("[Lever] Bot 未找到").formatted(Formatting.AQUA), false);
            return 0;
        }
        BotProfile profile = BotManager.getInstance().getProfile(bot.get().getUuidAsString());
        if (profile != null && (profile.isOwner(player.getUuidAsString()) || player.hasPermissionLevel(2))) {
            profile.customName = newName;
            bot.get().updateDisplayName();
            player.sendMessage(Text.literal("[Lever] Bot 已重命名为 " + newName).formatted(Formatting.AQUA), false);
        }
        return 1;
    }

    private static int setSkin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(context, "botName");
        String skinName = StringArgumentType.getString(context, "playerName");
        var bot = BotManager.getInstance().getBotByName(name);
        if (bot.isEmpty()) {
            player.sendMessage(Text.literal("[Lever] Bot 未找到").formatted(Formatting.AQUA), false);
            return 0;
        }
        BotProfile profile = BotManager.getInstance().getProfile(bot.get().getUuidAsString());
        if (profile != null && (profile.isOwner(player.getUuidAsString()) || player.hasPermissionLevel(2))) {
            profile.skinPlayerName = skinName;
            BotManager.getInstance().updateProfile(bot.get().getUuidAsString(), profile);
            player.sendMessage(Text.literal("[Lever] Bot 皮肤已设为 " + skinName).formatted(Formatting.AQUA), false);
        }
        return 1;
    }
}
