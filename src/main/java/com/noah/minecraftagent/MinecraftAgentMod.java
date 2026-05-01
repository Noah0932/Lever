package com.noah.minecraftagent;

import com.noah.minecraftagent.common.bot.BotNetworking;
import com.noah.minecraftagent.common.config.AgentConfigStore;
import com.noah.minecraftagent.common.network.AgentNetworking;
import com.noah.minecraftagent.common.util.SecureLog;
import com.noah.minecraftagent.server.bot.BotChatHandler;
import com.noah.minecraftagent.server.bot.BotCommand;
import com.noah.minecraftagent.server.bot.BotManager;
import com.noah.minecraftagent.server.bot.BotSummonerItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

public final class MinecraftAgentMod implements ModInitializer {
    public static final String MOD_ID = "minecraftagent";
    public static final String MOD_VERSION = "V1.1-Beta";
    public static final BotSummonerItem BOT_SUMMONER = new BotSummonerItem();

    @Override
    public void onInitialize() {
        SecureLog.info("Initializing Lever - MineAgent");
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "bot_summoner"), BOT_SUMMONER);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.addAfter(Items.END_CRYSTAL, BOT_SUMMONER));

        AgentConfigStore.getInstance().load();
        AgentNetworking.registerPayloads();
        AgentNetworking.registerServerHandlers();
        BotNetworking.registerPayloads();
        BotNetworking.registerServerHandlers();

        CommandRegistrationCallback.EVENT.register(BotCommand::register);
        BotChatHandler.register();

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof com.noah.minecraftagent.server.bot.BotEntity bot) {
                if (world.isClient) return ActionResult.PASS;
                var profile = BotManager.getInstance().getProfile(bot.getUuidAsString());
                if (profile != null && (profile.isOwner(player.getUuidAsString())
                        || profile.whitelist.contains(player.getUuidAsString())
                        || player.hasPermissionLevel(2))) {
                    if (player instanceof net.minecraft.server.network.ServerPlayerEntity sPlayer) {
                        sPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                                (syncId, inv, p) -> new PlayerScreenHandler(inv, false, p),
                                net.minecraft.text.Text.literal(bot.getBotProfile().displayName())
                        ));
                        syncInventory(sPlayer, bot);
                    }
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
    }

    private static void syncInventory(net.minecraft.server.network.ServerPlayerEntity player, com.noah.minecraftagent.server.bot.BotEntity bot) {
        for (int i = 0; i < player.currentScreenHandler.slots.size() && i < bot.getInventory().size(); i++) {
            player.currentScreenHandler.getSlot(i).setStackNoCallbacks(bot.getInventory().getStack(i));
        }
        player.currentScreenHandler.onContentChanged(bot.getInventory());
    }
}
