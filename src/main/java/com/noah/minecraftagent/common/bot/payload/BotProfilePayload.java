package com.noah.minecraftagent.common.bot.payload;

import com.noah.minecraftagent.MinecraftAgentMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BotProfilePayload(String botUuid, String name, String customName, String skinPlayerName, boolean aiConfigInherited) implements CustomPayload {
    public static final CustomPayload.Id<BotProfilePayload> ID = new CustomPayload.Id<>(Identifier.of(MinecraftAgentMod.MOD_ID, "bot_profile"));
    public static final PacketCodec<RegistryByteBuf, BotProfilePayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeString(payload.botUuid);
                buf.writeString(payload.name);
                buf.writeString(payload.customName);
                buf.writeString(payload.skinPlayerName);
                buf.writeBoolean(payload.aiConfigInherited);
            },
            buf -> new BotProfilePayload(buf.readString(128), buf.readString(128), buf.readString(128), buf.readString(64), buf.readBoolean())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
