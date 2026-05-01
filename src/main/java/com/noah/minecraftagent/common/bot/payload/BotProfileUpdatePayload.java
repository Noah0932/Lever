package com.noah.minecraftagent.common.bot.payload;

import com.noah.minecraftagent.MinecraftAgentMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BotProfileUpdatePayload(String botUuid, String field, String value) implements CustomPayload {
    public static final CustomPayload.Id<BotProfileUpdatePayload> ID = new CustomPayload.Id<>(Identifier.of(MinecraftAgentMod.MOD_ID, "bot_profile_update"));
    public static final PacketCodec<RegistryByteBuf, BotProfileUpdatePayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeString(payload.botUuid);
                buf.writeString(payload.field);
                buf.writeString(payload.value);
            },
            buf -> new BotProfileUpdatePayload(buf.readString(128), buf.readString(64), buf.readString(32767))
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
