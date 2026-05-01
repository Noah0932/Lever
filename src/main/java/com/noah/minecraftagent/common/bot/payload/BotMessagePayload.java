package com.noah.minecraftagent.common.bot.payload;

import com.noah.minecraftagent.MinecraftAgentMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BotMessagePayload(String botUuid, String botName, String message, boolean fromBot) implements CustomPayload {
    public static final CustomPayload.Id<BotMessagePayload> ID = new CustomPayload.Id<>(Identifier.of(MinecraftAgentMod.MOD_ID, "bot_message"));
    public static final PacketCodec<RegistryByteBuf, BotMessagePayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeString(payload.botUuid);
                buf.writeString(payload.botName);
                buf.writeString(payload.message);
                buf.writeBoolean(payload.fromBot);
            },
            buf -> new BotMessagePayload(buf.readString(128), buf.readString(128), buf.readString(32767), buf.readBoolean())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
