package com.noah.minecraftagent.common.network;

import com.noah.minecraftagent.MinecraftAgentMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AgentPromptRequestPayload(String requestId, String requesterName, String requesterUuid, String prompt, boolean operatorApproved, boolean selfRequest) implements CustomPayload {
    public static final CustomPayload.Id<AgentPromptRequestPayload> ID = new CustomPayload.Id<>(Identifier.of(MinecraftAgentMod.MOD_ID, "prompt_request"));
    public static final PacketCodec<RegistryByteBuf, AgentPromptRequestPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeString(payload.requestId);
                buf.writeString(payload.requesterName);
                buf.writeString(payload.requesterUuid);
                buf.writeString(payload.prompt);
                buf.writeBoolean(payload.operatorApproved);
                buf.writeBoolean(payload.selfRequest);
            },
            buf -> new AgentPromptRequestPayload(buf.readString(128), buf.readString(128), buf.readString(128), buf.readString(32767), buf.readBoolean(), buf.readBoolean())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
