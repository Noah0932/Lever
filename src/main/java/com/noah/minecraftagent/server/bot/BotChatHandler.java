package com.noah.minecraftagent.server.bot;

import com.noah.minecraftagent.common.bot.BotProfile;
import com.noah.minecraftagent.common.bot.payload.BotMessagePayload;
import com.noah.minecraftagent.common.security.PermissionManager;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BotChatHandler {
    private static final Pattern BOT_COMMAND = Pattern.compile("^@(\\S+)\\s+(.+)$");

    private BotChatHandler() {
    }

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String text = message.getSignedContent();
            Matcher matcher = BOT_COMMAND.matcher(text);
            if (!matcher.matches()) return;

            String botName = matcher.group(1);
            String command = matcher.group(2);

            var bot = BotManager.getInstance().getBotByName(botName);
            if (bot.isEmpty()) {
                sender.sendMessage(Text.literal("[Lever] Bot '" + botName + "' 未找到").formatted(Formatting.AQUA), false);
                return;
            }

            BotEntity botEntity = bot.get();
            BotProfile profile = BotManager.getInstance().getProfile(botEntity.getUuidAsString());
            if (profile == null) return;

            if (!PermissionManager.canAccess(profile, sender.getUuidAsString(), sender.hasPermissionLevel(2))) {
                sender.sendMessage(Text.literal("[Lever] 你没有权限向此 Bot 发送指令").formatted(Formatting.AQUA), false);
                return;
            }

            botEntity.sendMessage(Text.literal("[Bot Cmd] " + sender.getName().getString() + ": " + command));
            broadcastMessage(botEntity, "@" + sender.getName().getString() + "> " + command, sender);
        });
    }

    public static void broadcastMessage(BotEntity bot, String message, ServerPlayerEntity sender) {
        BotProfile profile = bot.getBotProfile();
        for (var player : bot.getServer().getPlayerManager().getPlayerList()) {
            if (PermissionManager.canAccess(profile, player.getUuidAsString(), player.hasPermissionLevel(2))) {
                ServerPlayNetworking.send(player, new BotMessagePayload(
                        bot.getUuidAsString(), profile.displayName(), message, false));
            }
        }
    }
}
