package com.noah.minecraftagent.client.bot;

import com.noah.minecraftagent.MinecraftAgentMod;
import com.noah.minecraftagent.common.bot.BotProfile;
import com.noah.minecraftagent.common.bot.payload.BotMessagePayload;
import com.noah.minecraftagent.common.chat.ChatEntry;
import com.noah.minecraftagent.common.chat.ChatHistoryManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.List;

public final class BotChatScreen extends Screen {
    private static final ChatHistoryManager HISTORY = createHistoryManager();
    private static final int CHAT_AREA_TOP = 30;
    private static final int INPUT_AREA_BOTTOM_OFFSET = 44;

    private final List<BotProfile> bots;
    private int selectedBotIndex;
    private TextFieldWidget input;
    private int scrollOffset;

    public BotChatScreen(List<BotProfile> bots) {
        super(Text.literal("Bot Chat - Lever"));
        this.bots = bots;
    }

    private static ChatHistoryManager createHistoryManager() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("ai_agent").resolve("chat_history");
        ChatHistoryManager mgr = new ChatHistoryManager(dir);
        mgr.setMaxEntries(500);
        mgr.load();
        return mgr;
    }

    @Override
    protected void init() {
        int chatWidth = Math.min(800, width - 10);
        int x = (width - chatWidth) / 2;

        if (!bots.isEmpty()) {
            BotProfile current = bots.get(selectedBotIndex);
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), btn -> switchBot(-1)).dimensions(x + 4, 4, 16, 16).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(current.displayName()), btn -> switchBot(1)).dimensions(x + 24, 4, chatWidth - 240, 16).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), btn -> switchBot(1)).dimensions(x + chatWidth - 210, 4, 16, 16).build());
        }

        input = new TextFieldWidget(textRenderer, x + 4, height - 34, chatWidth - 160, 22, Text.literal("..."));
        input.setMaxLength(512);
        addDrawableChild(input);

        addDrawableChild(ButtonWidget.builder(Text.literal("Send"), btn -> submit()).dimensions(x + chatWidth - 152, height - 34, 52, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("✕"), btn -> close()).dimensions(x + chatWidth - 94, height - 34, 40, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("⌃"), btn -> clearChat()).dimensions(x + chatWidth - 48, height - 34, 40, 22).build());
        setInitialFocus(input);
    }

    private void switchBot(int delta) {
        selectedBotIndex = (selectedBotIndex + delta + bots.size()) % bots.size();
        scrollOffset = 0;
        clearAndInit();
    }

    private void clearChat() {
        if (!bots.isEmpty()) {
            HISTORY.clearThread(bots.get(selectedBotIndex).id);
            scrollOffset = 0;
        }
    }

    private void submit() {
        String text = input.getText().trim();
        if (text.isEmpty() || bots.isEmpty() || selectedBotIndex >= bots.size()) return;
        BotProfile bot = bots.get(selectedBotIndex);
        HISTORY.addEntry(bot.id, "user", text);
        ClientPlayNetworking.send(new BotMessagePayload(bot.id, bot.displayName(), text, false));
        input.setText("");
        scrollOffset = 0;
    }

    public void addMessage(String botId, String sender, String content) {
        HISTORY.addEntry(botId, sender, content);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int chatWidth = Math.min(800, width - 10);
        int x = (width - chatWidth) / 2;
        int chatTop = CHAT_AREA_TOP;
        int chatBottom = height - INPUT_AREA_BOTTOM_OFFSET;

        context.fill(x, chatTop, x + chatWidth, chatBottom, 0xBB12121A);
        context.drawBorder(x, chatTop, chatWidth, chatBottom - chatTop, 0xFF4A4A6A);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Lever " + MinecraftAgentMod.MOD_VERSION + " · Bot Chat"), width / 2, 14, 0xFFAAAAFF);

        if (bots.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("No bots connected"), width / 2, chatTop + 40, 0xFF888888);
        } else {
            BotProfile bot = bots.get(selectedBotIndex);
            List<ChatEntry> entries = HISTORY.getEntries(bot.id, 0, 500);
            renderMessages(context, entries, x + 6, chatTop + 2, chatBottom - 22, chatWidth - 12);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderMessages(DrawContext context, List<ChatEntry> entries, int startX, int topY, int bottomY, int width) {
        int msgWidth = width - 16;
        int lineHeight = 11;
        int totalHeight = 0;
        for (int i = entries.size() - 1; i >= 0; i--) {
            totalHeight += measureHeight(entries.get(i), textRenderer, msgWidth, lineHeight) + 4;
        }

        int availableHeight = bottomY - topY;
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, totalHeight - availableHeight)));

        int drawY = bottomY - scrollOffset;
        for (int i = entries.size() - 1; i >= 0 && drawY > topY; i--) {
            ChatEntry entry = entries.get(i);
            int lines = measureHeight(entry, textRenderer, msgWidth, lineHeight);

            int color = entry.role().equals("bot") ? 0xFF7CE88C : 0xFFFFE070;
            String prefix = entry.role().equals("bot") ? "[Bot] " : "[You] ";
            String time = " " + entry.formatDisplay() + " ";

            drawY -= lines * lineHeight;

            String header = prefix + time;
            context.drawTextWithShadow(textRenderer, Text.literal(header).formatted(Formatting.GRAY), startX, drawY, 0xFF888888);

            String body = entry.content();
            List<OrderedText> wrapped = textRenderer.wrapLines(StringVisitable.plain(body), msgWidth);
            int wrapY = drawY + lineHeight;
            for (OrderedText line : wrapped) {
                if (wrapY > bottomY) break;
                context.drawTextWithShadow(textRenderer, line, startX + 4, wrapY, color);
                wrapY += lineHeight;
            }
            drawY -= 8;
        }
    }

    private static int measureHeight(ChatEntry entry, TextRenderer renderer, int width, int lineHeight) {
        List<OrderedText> wrapped = renderer.wrapLines(StringVisitable.plain(entry.content()), width);
        return lineHeight + wrapped.size() * lineHeight;
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(null);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scrollOffset -= (int) (vertical * 20);
        scrollOffset = Math.max(0, scrollOffset);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (modifiers == GLFW.GLFW_MOD_SHIFT) {
                input.setText(input.getText() + "\n");
                return true;
            }
            submit();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
