package com.noah.minecraftagent.client.bot;

import com.noah.minecraftagent.common.bot.BotProfile;
import com.noah.minecraftagent.common.bot.payload.BotInventoryPayload;
import com.noah.minecraftagent.common.bot.payload.BotMessagePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class BotChatScreen extends Screen {
    private static final String MOD_VERSION = "V1.1-Beta";
    private static final List<String> CHAT_HISTORY = new ArrayList<>();
    private static final int MAX_HISTORY = 100;
    private final List<BotProfile> bots;
    private int selectedBotIndex;
    private TextFieldWidget input;
    private final List<String> messages = new ArrayList<>();

    public BotChatScreen(List<BotProfile> bots) {
        super(Text.literal("Bot 聊天"));
        this.bots = bots;
    }

    @Override
    protected void init() {
        int chatWidth = Math.min(700, width - 20);
        int x = (width - chatWidth) / 2;
        int y = 10;

        if (!bots.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), btn -> {
                selectedBotIndex = (selectedBotIndex - 1 + bots.size()) % bots.size();
                messages.clear();
            }).dimensions(x + 4, y + 4, 16, 16).build());
            BotProfile current = bots.get(selectedBotIndex);
            addDrawableChild(ButtonWidget.builder(Text.literal(current.displayName()), btn -> {
                selectedBotIndex = (selectedBotIndex + 1) % bots.size();
                messages.clear();
            }).dimensions(x + 24, y + 4, 150, 16).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), btn -> {
                selectedBotIndex = (selectedBotIndex + 1) % bots.size();
                messages.clear();
            }).dimensions(x + 178, y + 4, 16, 16).build());
        }

        input = new TextFieldWidget(textRenderer, x + 4, height - 30, chatWidth - 140, 20, Text.literal("输入消息..."));
        input.setMaxLength(512);
        addDrawableChild(input);

        addDrawableChild(ButtonWidget.builder(Text.literal("发送"), btn -> submit()).dimensions(x + chatWidth - 130, height - 30, 60, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("关闭"), btn -> close()).dimensions(x + chatWidth - 64, height - 30, 54, 20).build());
        setInitialFocus(input);
    }

    private void submit() {
        String text = input.getText().trim();
        if (text.isEmpty() || bots.isEmpty() || selectedBotIndex >= bots.size()) return;
        BotProfile bot = bots.get(selectedBotIndex);
        addMessage("你: " + text);
        ClientPlayNetworking.send(new BotMessagePayload(bot.id, bot.displayName(), text, false));
        CHAT_HISTORY.add(text);
        while (CHAT_HISTORY.size() > MAX_HISTORY) {
            CHAT_HISTORY.remove(0);
        }
        input.setText("");
    }

    public void addMessage(String msg) {
        messages.add(msg);
        while (messages.size() > 50) {
            messages.remove(0);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int chatWidth = Math.min(700, width - 20);
        int x = (width - chatWidth) / 2;
        int top = 26;
        int bottom = height - 38;
        context.fill(x, top, x + chatWidth, bottom, 0xBB000000);
        context.drawBorder(x, top, chatWidth, bottom - top, 0xFF606060);
        context.drawTextWithShadow(textRenderer, Text.literal("Bot 聊天 - 智械 Lever v" + MOD_VERSION).formatted(Formatting.AQUA), x + 4, 12, 0xFFFFFF00);

        int msgY = bottom - 12;
        int maxVisible = (bottom - top) / 12;
        int startIdx = Math.max(0, messages.size() - maxVisible);
        for (int i = messages.size() - 1; i >= startIdx && msgY > top; i--) {
            context.drawTextWithShadow(textRenderer, Text.literal(messages.get(i)), x + 6, msgY, 0xFFE0E0E0);
            msgY -= 12;
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        client.setScreen(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
