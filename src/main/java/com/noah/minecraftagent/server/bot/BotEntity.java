package com.noah.minecraftagent.server.bot;

import com.mojang.authlib.GameProfile;
import com.noah.minecraftagent.common.bot.BotProfile;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class BotEntity extends ServerPlayerEntity {
    private final BotProfile botProfile;
    private ServerPlayerEntity owner;

    public BotEntity(MinecraftServer server, ServerWorld world, BotProfile profile) {
        super(server, world, createGameProfile(profile), null);
        this.botProfile = profile;
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
