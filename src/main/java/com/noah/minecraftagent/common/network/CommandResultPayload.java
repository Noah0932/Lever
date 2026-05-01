package com.noah.minecraftagent.common.network;

import com.noah.minecraftagent.MinecraftAgentMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CommandResultPayload(boolean success, String message) implements CustomPayload {
    public static final CustomPayload.Id<CommandResultPayload> ID = new CustomPayload.Id<>(Identifier.of(MinecraftAgentMod.MOD_ID, "command_result"));
    public static final PacketCodec<RegistryByteBuf, CommandResultPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeBoolean(payload.success);
                buf.writeString(payload.message);
            },
            buf -> new CommandResultPayload(buf.readBoolean(), buf.readString(32767))
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
