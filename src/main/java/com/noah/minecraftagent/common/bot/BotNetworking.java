package com.noah.minecraftagent.common.bot;

import com.noah.minecraftagent.common.bot.payload.*;
import com.noah.minecraftagent.server.bot.BotManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class BotNetworking {
    private static boolean registered;

    private BotNetworking() {
    }

    public static void registerPayloads() {
        if (registered) return;
        PayloadTypeRegistry.playS2C().register(BotInventoryPayload.ID, BotInventoryPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BotListPayload.ID, BotListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BotProfilePayload.ID, BotProfilePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BotMessagePayload.ID, BotMessagePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BotProfileUpdatePayload.ID, BotProfileUpdatePayload.CODEC);
        registered = true;
    }

    public static void registerServerHandlers() {
        ServerPlayNetworking.registerGlobalReceiver(BotProfileUpdatePayload.ID, (payload, context) ->
                context.server().execute(() -> handleProfileUpdate(context.player(), payload)));
    }

    private static void handleProfileUpdate(ServerPlayerEntity player, BotProfileUpdatePayload payload) {
        BotManager manager = BotManager.getInstance();
        var bot = manager.getBot(payload.botUuid());
        if (bot.isEmpty()) return;

        var profile = manager.getProfile(payload.botUuid());
        if (profile == null) return;
        if (!profile.isOwner(player.getUuidAsString()) && !player.hasPermissionLevel(2)) return;

        switch (payload.field()) {
            case "customName" -> {
                profile.customName = payload.value();
                bot.get().updateDisplayName();
            }
            case "skin" -> profile.skinPlayerName = payload.value();
            case "whitelist_add" -> {
                if (!profile.whitelist.contains(payload.value())) {
                    profile.whitelist.add(payload.value());
                }
            }
            case "whitelist_remove" -> profile.whitelist.remove(payload.value());
            case "transfer_owner" -> {
                profile.ownerUuid = payload.value();
                profile.whitelist.clear();
            }
        }
        manager.updateProfile(payload.botUuid(), profile);
    }
}
