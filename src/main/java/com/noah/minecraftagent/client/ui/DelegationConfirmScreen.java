package com.noah.minecraftagent.client.ui;

import com.noah.minecraftagent.MinecraftAgentMod;
import com.noah.minecraftagent.common.network.AgentPromptRequestPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public final class DelegationConfirmScreen extends Screen {
    private final AgentPromptRequestPayload payload;
    private final Consumer<AgentPromptRequestPayload> accept;
    private final Consumer<AgentPromptRequestPayload> reject;
    private final double estimatedCost;

    public DelegationConfirmScreen(AgentPromptRequestPayload payload, Consumer<AgentPromptRequestPayload> accept, Consumer<AgentPromptRequestPayload> reject, double estimatedCost) {
        super(Text.translatable("screen.minecraftagent.delegate.title"));
        this.payload = payload;
        this.accept = accept;
        this.reject = reject;
        this.estimatedCost = estimatedCost;
    }

    @Override
    protected void init() {
        int y = height / 2 + 44;
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.minecraftagent.accept"), button -> {
            accept.accept(payload);
            close();
        }).dimensions(width / 2 - 104, y, 96, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.minecraftagent.reject"), button -> {
            reject.accept(payload);
            close();
        }).dimensions(width / 2 + 8, y, 96, 20).build());
    }

    @Override
    public void close() {
        client.setScreen(null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int panelWidth = Math.min(520, width - 40);
        int x = (width - panelWidth) / 2;
        int y = height / 2 - 76;
        context.fill(x, y, x + panelWidth, y + 140, 0xDD000000);
        context.drawBorder(x, y, panelWidth, 140, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, y + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("label.minecraftagent.delegate.requester", payload.requesterName()), x + 18, y + 38, 0xFFE0E0E0);
        context.drawTextWithShadow(textRenderer, Text.translatable("label.minecraftagent.delegate.cost", String.format("%.4f", estimatedCost)), x + 18, y + 52, 0xFFE0E0E0);
        context.drawTextWithShadow(textRenderer, trim(payload.prompt(), 80), x + 18, y + 72, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, delta);
        int footerX = x + panelWidth;
        context.drawTextWithShadow(textRenderer, Text.literal("v" + MinecraftAgentMod.MOD_VERSION), footerX - textRenderer.getWidth("v" + MinecraftAgentMod.MOD_VERSION), y + 140 + 6, 0xFF909090);
        context.drawTextWithShadow(textRenderer, Text.literal("github.com/noah0932"), footerX - textRenderer.getWidth("github.com/noah0932"), y + 140 + 18, 0xFF909090);
        context.drawTextWithShadow(textRenderer, Text.literal("MCAI.noah0932.top"), footerX - textRenderer.getWidth("MCAI.noah0932.top"), y + 140 + 30, 0xFF909090);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private String trim(String value, int length) {
        return value.length() <= length ? value : value.substring(0, length - 3) + "...";
    }
}
