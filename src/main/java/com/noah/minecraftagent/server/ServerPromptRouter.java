package com.noah.minecraftagent.server;

import com.noah.minecraftagent.common.network.AgentPromptRequestPayload;
import com.noah.minecraftagent.common.network.AgentPromptResponsePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ServerPromptRouter {
    private static final Map<String, PendingRequest> PENDING = new HashMap<>();

    private ServerPromptRouter() {
    }

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            String playerUuid = handler.getPlayer().getUuidAsString();
            synchronized (PENDING) {
                Iterator<Map.Entry<String, PendingRequest>> iterator = PENDING.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, PendingRequest> entry = iterator.next();
                    PendingRequest pending = entry.getValue();
                    if (playerUuid.equals(pending.requesterUuid) || playerUuid.equals(pending.targetUuid)) {
                        iterator.remove();
                    }
                }
            }
        });
    }

    public static void request(ServerPlayerEntity requester, ServerPlayerEntity target, String prompt, boolean operatorApproved, boolean selfRequest) {
        if (!ServerPlayNetworking.canSend(target, AgentPromptRequestPayload.ID)) {
            requester.sendMessage(Text.translatable("message.minecraftagent.delegate.unavailable", target.getName().getString()), false);
            return;
        }
        String requestId = UUID.randomUUID().toString();
        synchronized (PENDING) {
            PENDING.put(requestId, new PendingRequest(requester.getUuidAsString(), requester.getName().getString(), target.getUuidAsString(), target.getName().getString()));
        }
        ServerPlayNetworking.send(target, new AgentPromptRequestPayload(requestId, requester.getName().getString(), requester.getUuidAsString(), prompt, operatorApproved, selfRequest));
        requester.sendMessage(Text.translatable("message.minecraftagent.delegate.sent", target.getName().getString()), false);
    }

    public static void handleResponse(ServerPlayerEntity target, AgentPromptResponsePayload payload) {
        PendingRequest pending;
        synchronized (PENDING) {
            pending = PENDING.remove(payload.requestId());
        }
        if (pending == null) {
            return;
        }
        ServerPlayerEntity requester = target.getServer().getPlayerManager().getPlayer(UUID.fromString(pending.requesterUuid));
        if (requester != null) {
            requester.sendMessage(Text.translatable("message.minecraftagent.delegate.response", target.getName().getString(), payload.status(), payload.message()), false);
        }
    }

    private record PendingRequest(String requesterUuid, String requesterName, String targetUuid, String targetName) {
    }
}
