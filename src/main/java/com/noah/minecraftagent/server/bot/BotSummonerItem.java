package com.noah.minecraftagent.server.bot;

import com.noah.minecraftagent.common.bot.BotProfile;
import com.noah.minecraftagent.common.config.AgentConfigStore;
import com.noah.minecraftagent.common.config.AgentProfile;
import com.noah.minecraftagent.common.util.SecureLog;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class BotSummonerItem extends Item {
    public BotSummonerItem() {
        super(new Settings().maxCount(1).rarity(Rarity.EPIC));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.fail(stack);
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            SecureLog.warn("BotSummoner: player.getServer() returned null for " + user.getName().getString());
            player.sendMessage(Text.literal("[Lever] 无法获取服务器实例").formatted(Formatting.RED), false);
            return TypedActionResult.fail(stack);
        }

        ServerWorld serverWorld = player.getServerWorld();
        if (serverWorld == null) {
            SecureLog.warn("BotSummoner: player.getServerWorld() returned null");
            player.sendMessage(Text.literal("[Lever] 无法获取世界实例").formatted(Formatting.RED), false);
            return TypedActionResult.fail(stack);
        }

        BotManager manager = BotManager.getInstance();
        BotProfile profile = new BotProfile();
        profile.name = "Bot";
        profile.botNumber = manager.nextBotId();
        profile.ownerUuid = user.getUuidAsString();
        profile.ownerName = user.getName().getString();

        AgentProfile activeAi = AgentConfigStore.getInstance().config().activeProfile();
        if (activeAi == null || !activeAi.isComplete()) {
            player.sendMessage(Text.literal("[Lever] AI 配置不完整，请先按 N 配置").formatted(Formatting.RED), false);
            return TypedActionResult.fail(stack);
        }

        profile.aiConfig = copyAgentProfile(activeAi);
        profile.aiConfigInherited = true;

        BotEntity bot = new BotEntity(server, serverWorld, profile);
        bot.setOwner(player);
        bot.updateDisplayName();
        serverWorld.spawnEntity(bot);
        bot.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());

        if (!player.isCreative()) {
            stack.decrement(1);
        }
        player.sendMessage(Text.literal("[Lever] Bot " + profile.displayName() + " 已召唤").formatted(Formatting.AQUA), false);
        return TypedActionResult.consume(stack);
    }

    private static AgentProfile copyAgentProfile(AgentProfile source) {
        AgentProfile copy = new AgentProfile();
        copy.copyFrom(source);
        return copy;
    }
}
