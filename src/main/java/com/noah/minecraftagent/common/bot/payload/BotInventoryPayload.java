package com.noah.minecraftagent.common.bot.payload;

import com.noah.minecraftagent.MinecraftAgentMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BotInventoryPayload(String botUuid, String botName, String ownerUuid) implements CustomPayload {
    public static final CustomPayload.Id<BotInventoryPayload> ID = new CustomPayload.Id<>(Identifier.of(MinecraftAgentMod.MOD_ID, "bot_inventory"));
    public static final PacketCodec<RegistryByteBuf, BotInventoryPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeString(payload.botUuid);
                buf.writeString(payload.botName);
                buf.writeString(payload.ownerUuid);
            },
            buf -> new BotInventoryPayload(buf.readString(128), buf.readString(128), buf.readString(128))
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
