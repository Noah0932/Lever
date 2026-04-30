package com.noah.minecraftagent.common.network;

import com.noah.minecraftagent.MinecraftAgentMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AgentPromptResponsePayload(String requestId, String status, String message) implements CustomPayload {
    public static final CustomPayload.Id<AgentPromptResponsePayload> ID = new CustomPayload.Id<>(Identifier.of(MinecraftAgentMod.MOD_ID, "prompt_response"));
    public static final PacketCodec<RegistryByteBuf, AgentPromptResponsePayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeString(payload.requestId);
                buf.writeString(payload.status);
                buf.writeString(payload.message);
            },
            buf -> new AgentPromptResponsePayload(buf.readString(128), buf.readString(64), buf.readString(32767))
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
