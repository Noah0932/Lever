package com.noah.minecraftagent.common.network;

import com.noah.minecraftagent.MinecraftAgentMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DelegateConfigPayload(String action, String entry) implements CustomPayload {
    public static final CustomPayload.Id<DelegateConfigPayload> ID = new CustomPayload.Id<>(Identifier.of(MinecraftAgentMod.MOD_ID, "delegate_config"));
    public static final PacketCodec<RegistryByteBuf, DelegateConfigPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeString(payload.action);
                buf.writeString(payload.entry);
            },
            buf -> new DelegateConfigPayload(buf.readString(32), buf.readString(32767))
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
