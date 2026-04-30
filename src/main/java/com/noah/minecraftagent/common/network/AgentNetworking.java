package com.noah.minecraftagent.common.network;

import com.noah.minecraftagent.server.ServerCommandExecutor;
import com.noah.minecraftagent.server.ServerAiCommands;
import com.noah.minecraftagent.server.ServerPromptRouter;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class AgentNetworking {
    private static boolean payloadsRegistered;
    private static boolean serverHandlersRegistered;

    private AgentNetworking() {
    }

    public static void registerPayloads() {
        if (payloadsRegistered) {
            return;
        }
        PayloadTypeRegistry.playC2S().register(ExecuteCommandPayload.ID, ExecuteCommandPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AgentPromptResponsePayload.ID, AgentPromptResponsePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CommandResultPayload.ID, CommandResultPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AgentPromptRequestPayload.ID, AgentPromptRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DelegateConfigPayload.ID, DelegateConfigPayload.CODEC);
        payloadsRegistered = true;
    }

    public static void registerServerHandlers() {
        if (serverHandlersRegistered) {
            return;
        }
        ServerPlayNetworking.registerGlobalReceiver(ExecuteCommandPayload.ID, (payload, context) ->
                context.server().execute(() -> ServerCommandExecutor.execute(context.player(), payload.command())));
        ServerPlayNetworking.registerGlobalReceiver(AgentPromptResponsePayload.ID, (payload, context) ->
                context.server().execute(() -> ServerPromptRouter.handleResponse(context.player(), payload)));
        CommandRegistrationCallback.EVENT.register(ServerAiCommands::register);
        ServerPromptRouter.register();
        serverHandlersRegistered = true;
    }
}
