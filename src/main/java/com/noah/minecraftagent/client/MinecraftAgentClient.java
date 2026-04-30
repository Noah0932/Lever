package com.noah.minecraftagent.client;

import com.noah.minecraftagent.client.ui.AgentConfigScreen;
import com.noah.minecraftagent.client.ui.DelegationConfirmScreen;
import com.noah.minecraftagent.client.ui.SpotlightScreen;
import com.noah.minecraftagent.common.config.AgentConfig;
import com.noah.minecraftagent.common.config.AgentConfigStore;
import com.noah.minecraftagent.common.network.AgentNetworking;
import com.noah.minecraftagent.common.network.AgentPromptRequestPayload;
import com.noah.minecraftagent.common.network.AgentPromptResponsePayload;
import com.noah.minecraftagent.common.network.CommandResultPayload;
import com.noah.minecraftagent.common.network.DelegateConfigPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class MinecraftAgentClient implements ClientModInitializer {
    private final AgentRuntime runtime = new AgentRuntime();
    private KeyBinding configKey;
    private KeyBinding spotlightKey;

    @Override
    public void onInitializeClient() {
        AgentNetworking.registerPayloads();
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.minecraftagent.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.minecraftagent"));
        spotlightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.minecraftagent.spotlight", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.minecraftagent"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configKey.wasPressed()) {
                client.setScreen(AgentConfigScreen.create(client.currentScreen));
            }
            while (spotlightKey.wasPressed()) {
                client.setScreen(new SpotlightScreen(runtime));
            }
        });
        ClientSendMessageEvents.ALLOW_CHAT.register(this::handleLocalAiChat);
        ClientPlayNetworking.registerGlobalReceiver(CommandResultPayload.ID, (payload, context) -> context.client().execute(() -> {
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(Text.translatable("message.minecraftagent.prefix", payload.message()), false);
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(AgentPromptRequestPayload.ID, (payload, context) -> context.client().execute(() -> handlePromptRequest(payload)));
        ClientPlayNetworking.registerGlobalReceiver(DelegateConfigPayload.ID, (payload, context) -> context.client().execute(() -> handleDelegateConfig(payload)));
    }

    private boolean handleLocalAiChat(String message) {
        AgentConfig config = AgentConfigStore.getInstance().config();
        String prefix = config.chatPrefix == null || config.chatPrefix.isBlank() ? ".ai" : config.chatPrefix.trim();
        if (!message.equals(prefix) && !message.startsWith(prefix + " ")) {
            return true;
        }
        String prompt = message.length() == prefix.length() ? "" : message.substring(prefix.length()).trim();
        MinecraftClient client = MinecraftClient.getInstance();
        if (prompt.isBlank()) {
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("message.minecraftagent.chat.empty", prefix), false);
            }
            return false;
        }
        runtime.submit(prompt);
        if (client.player != null) {
            client.player.sendMessage(Text.translatable("message.minecraftagent.chat.started"), false);
        }
        return false;
    }

    private void handlePromptRequest(AgentPromptRequestPayload payload) {
        AgentConfig config = AgentConfigStore.getInstance().config();
        boolean allowedDelegate = isAllowedDelegate(config, payload.requesterName()) || isAllowedDelegate(config, payload.requesterUuid());
        boolean autoAccept = payload.selfRequest() || allowedDelegate || (payload.operatorApproved() && config.allowOperatorDelegation) || !config.delegationConfirmRequired;
        if (autoAccept) {
            acceptPrompt(payload);
            return;
        }
        MinecraftClient.getInstance().setScreen(new DelegationConfirmScreen(payload, this::acceptPrompt, this::rejectPrompt, runtime.estimateCurrentCost(payload.prompt())));
    }

    private boolean isAllowedDelegate(AgentConfig config, String value) {
        return config.allowedDelegates != null && config.allowedDelegates.stream().anyMatch(entry -> entry.equalsIgnoreCase(value));
    }

    private void acceptPrompt(AgentPromptRequestPayload payload) {
        runtime.submit(payload.prompt());
        ClientPlayNetworking.send(new AgentPromptResponsePayload(payload.requestId(), "accepted", Text.translatable("message.minecraftagent.delegate.accepted").getString()));
    }

    private void rejectPrompt(AgentPromptRequestPayload payload) {
        ClientPlayNetworking.send(new AgentPromptResponsePayload(payload.requestId(), "rejected", Text.translatable("message.minecraftagent.delegate.rejected").getString()));
    }

    private void handleDelegateConfig(DelegateConfigPayload payload) {
        AgentConfig config = AgentConfigStore.getInstance().config();
        if (config.allowedDelegates == null) {
            config.allowedDelegates = new java.util.ArrayList<>();
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (payload.action().equals("allow")) {
            if (config.allowedDelegates.stream().noneMatch(entry -> entry.equalsIgnoreCase(payload.entry()))) {
                config.allowedDelegates.add(payload.entry());
                AgentConfigStore.getInstance().save();
            }
            if (client.player != null) client.player.sendMessage(Text.translatable("message.minecraftagent.delegate.allowed", payload.entry()), false);
        } else if (payload.action().equals("deny")) {
            config.allowedDelegates.removeIf(entry -> entry.equalsIgnoreCase(payload.entry()));
            AgentConfigStore.getInstance().save();
            if (client.player != null) client.player.sendMessage(Text.translatable("message.minecraftagent.delegate.denied", payload.entry()), false);
        } else if (payload.action().equals("list")) {
            String list = config.allowedDelegates.isEmpty() ? "-" : String.join(", ", config.allowedDelegates);
            if (client.player != null) client.player.sendMessage(Text.translatable("message.minecraftagent.delegate.list", list), false);
        }
    }
}
