package com.noah.minecraftagent.server.bot;

import com.mojang.authlib.GameProfile;
import com.noah.minecraftagent.common.bot.BotProfile;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Formatting;

import java.util.UUID;

public final class BotEntity extends ServerPlayerEntity {
    private final BotProfile botProfile;
    private final BotActionController controller;
    private ServerPlayerEntity owner;

    public BotEntity(MinecraftServer server, ServerWorld world, BotProfile profile) {
        super(server, world, createGameProfile(profile),
                new SyncedClientOptions("en_us", 8, net.minecraft.network.message.ChatVisibility.FULL, true, 0x7F, Arm.RIGHT, false, false));
        this.botProfile = profile;

        DummyClientConnection dummyConnection = new DummyClientConnection();
        networkHandler = new ServerPlayNetworkHandler(server, dummyConnection, this);

        this.controller = new BotActionController(this);
    }

    private static GameProfile createGameProfile(BotProfile profile) {
        UUID uuid = UUID.nameUUIDFromBytes(("lever-bot-" + profile.id).getBytes());
        return new GameProfile(uuid, profile.displayName());
    }

    public BotProfile getBotProfile() {
        return botProfile;
    }

    public ServerPlayerEntity getOwner() {
        return owner;
    }

    public void setOwner(ServerPlayerEntity owner) {
        this.owner = owner;
    }

    @Override
    public void tick() {
        super.tick();
        controller.tick();
    }

    public BotActionController getController() {
        return controller;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        BotDeathHandler.handle(this);
    }

    public void updateDisplayName() {
        Text name = Text.literal("[Bot] ").formatted(Formatting.AQUA)
                .append(Text.literal(botProfile.displayName()).formatted(Formatting.WHITE));
        setCustomName(name);
        setCustomNameVisible(true);
    }
}
