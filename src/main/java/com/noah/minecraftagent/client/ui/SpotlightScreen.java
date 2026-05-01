package com.noah.minecraftagent.client.ui;

import com.noah.minecraftagent.client.AgentRuntime;
import com.noah.minecraftagent.client.AgentStatus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class SpotlightScreen extends Screen {
    private static final String MOD_VERSION = "V1.1-Beta";
    private static final List<String> HISTORY = new ArrayList<>();
    private final AgentRuntime runtime;
    private TextFieldWidget input;
    private AgentStatus status = AgentStatus.READY;
    private String message = "";
    private String output = "";
    private String channel = "";
    private boolean cached;
    private double costCny;
    private int historyIndex = -1;

    public SpotlightScreen(AgentRuntime runtime) {
        super(Text.translatable("screen.minecraftagent.spotlight.title"));
        this.runtime = runtime;
        this.runtime.setListener(update -> MinecraftClient.getInstance().execute(() -> {
            status = update.status();
            message = update.message();
            output = update.text();
            channel = update.channel();
            cached = update.cached();
            costCny = update.costCny();
        }));
    }

    @Override
    protected void init() {
        int boxWidth = Math.min(660, width - 60);
        int x = (width - boxWidth) / 2;
        int y = height / 3;
        input = new TextFieldWidget(textRenderer, x + 12, y + 12, boxWidth - 96, 22, Text.translatable("placeholder.minecraftagent.ask"));
        input.setMaxLength(2000);
        input.setFocused(true);
        input.setChangedListener(text -> costCny = runtime.estimateCurrentCost(text));
        addDrawableChild(input);
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.minecraftagent.stop"), button -> runtime.cancel()).dimensions(x + boxWidth - 76, y + 12, 62, 22).build());
        setInitialFocus(input);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            history(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            history(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void submit() {
        String text = input.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        HISTORY.remove(text);
        HISTORY.add(0, text);
        while (HISTORY.size() > 20) {
            HISTORY.remove(HISTORY.size() - 1);
        }
        historyIndex = -1;
        output = "";
        status = AgentStatus.ROUTING;
        message = Text.translatable("status.minecraftagent.routing").getString();
        runtime.submit(text);
    }

    private void history(int delta) {
        if (HISTORY.isEmpty()) {
            return;
        }
        historyIndex = Math.max(0, Math.min(HISTORY.size() - 1, historyIndex + delta));
        input.setText(HISTORY.get(historyIndex));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int boxWidth = Math.min(660, width - 60);
        int x = (width - boxWidth) / 2;
        int y = height / 3;
        context.fill(x, y, x + boxWidth, y + 124, 0xDD000000);
        context.drawBorder(x, y, boxWidth, 124, colorForStatus());
        super.render(context, mouseX, mouseY, delta);
        Text line = Text.translatable("label.minecraftagent.status", Text.translatable("status.minecraftagent." + status.name().toLowerCase()), cached ? Text.translatable("label.minecraftagent.cached") : Text.literal(""), channel);
        context.drawTextWithShadow(textRenderer, line, x + 12, y + 42, colorForStatus());
        context.drawTextWithShadow(textRenderer, Text.translatable("label.minecraftagent.cost", String.format("%.4f", costCny)), x + 12, y + 58, 0xFFBDBDBD);
        if (!message.equals(output) && !message.isBlank()) {
            context.drawTextWithShadow(textRenderer, trim(message, 96), x + 12, y + 76, 0xFFE0E0E0);
        }
        if (!output.isBlank()) {
            context.drawTextWithShadow(textRenderer, trim(output.replace('\n', ' '), 110), x + 12, y + 76 + (!message.equals(output) ? 18 : 0), 0xFFFFFFFF);
        }
        int footerX = x + boxWidth;
        context.drawTextWithShadow(textRenderer, Text.literal("v" + MOD_VERSION), footerX - textRenderer.getWidth("v" + MOD_VERSION), y + 124 + 6, 0xFF909090);
        context.drawTextWithShadow(textRenderer, Text.literal("github.com/noah0932"), footerX - textRenderer.getWidth("github.com/noah0932"), y + 124 + 18, 0xFF909090);
        context.drawTextWithShadow(textRenderer, Text.literal("MCAI.noah0932.top"), footerX - textRenderer.getWidth("MCAI.noah0932.top"), y + 124 + 30, 0xFF909090);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int colorForStatus() {
        return switch (status) {
            case READY -> 0xFFFFFFFF;
            case ROUTING, CACHE_LOOKUP, CAPTURE, THINKING, STREAMING, ACTING, OBSERVING -> 0xFFFFFF00;
            case CACHED, DONE -> 0xFF55FF55;
            case ERROR, BLOCKED -> 0xFFFF5555;
        };
    }

    private String trim(String value, int length) {
        return value.length() <= length ? value : value.substring(0, length - 3) + "...";
    }
}
