package com.noah.minecraftagent.common.bot.payload;

import com.noah.minecraftagent.MinecraftAgentMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record BotListPayload(List<String> botUuids, List<String> botNames) implements CustomPayload {
    public static final CustomPayload.Id<BotListPayload> ID = new CustomPayload.Id<>(Identifier.of(MinecraftAgentMod.MOD_ID, "bot_list"));
    public static final PacketCodec<RegistryByteBuf, BotListPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeInt(payload.botUuids.size());
                for (int i = 0; i < payload.botUuids.size(); i++) {
                    buf.writeString(payload.botUuids.get(i));
                    buf.writeString(payload.botNames.get(i));
                }
            },
            buf -> {
                int size = buf.readInt();
                List<String> uuids = new ArrayList<>();
                List<String> names = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    uuids.add(buf.readString(128));
                    names.add(buf.readString(128));
                }
                return new BotListPayload(uuids, names);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
