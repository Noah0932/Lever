package com.noah.minecraftagent.common.network;

import com.noah.minecraftagent.MinecraftAgentMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ExecuteCommandPayload(String command) implements CustomPayload {
    public static final CustomPayload.Id<ExecuteCommandPayload> ID = new CustomPayload.Id<>(Identifier.of(MinecraftAgentMod.MOD_ID, "execute_command"));
    public static final PacketCodec<RegistryByteBuf, ExecuteCommandPayload> CODEC = PacketCodec.of(
            (payload, buf) -> buf.writeString(payload.command),
            buf -> new ExecuteCommandPayload(buf.readString(32767))
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
