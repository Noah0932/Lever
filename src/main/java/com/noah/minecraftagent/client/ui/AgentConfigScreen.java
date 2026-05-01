package com.noah.minecraftagent.client.ui;

import com.noah.minecraftagent.common.config.AgentConfig;
import com.noah.minecraftagent.common.config.AgentConfigStore;
import com.noah.minecraftagent.common.config.AgentProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class AgentConfigScreen extends Screen {
    private static final String MOD_VERSION = "dev-1.0.2";
    private final Screen parent;
    private final AgentConfig config;
    private final List<FieldBinding> fields = new ArrayList<>();
    private final List<SectionAnchor> sections = new ArrayList<>();
    private AgentProfile profile;

    public AgentConfigScreen(Screen parent) {
        super(Text.translatable("screen.minecraftagent.config.title"));
        this.parent = parent;
        this.config = AgentConfigStore.getInstance().config();
        this.profile = config.activeProfile();
    }

    public static Screen create(Screen parent) {
        return new AgentConfigScreen(parent);
    }

    @Override
    protected void init() {
        fields.clear();
        sections.clear();
        int panelWidth = Math.min(760, width - 40);
        int x = (width - panelWidth) / 2;
        int fieldX = x + 180;
        int fieldWidth = panelWidth - 200;
        int y = 42;

        y = addSection("section.minecraftagent.profile", x, fieldX, fieldWidth, y);
        addField("field.minecraftagent.name", profile.name, fieldX, y, value -> profile.name = value); y += 24;
        addField("field.minecraftagent.base_url", profile.baseUrl, fieldX, y, value -> profile.baseUrl = value); y += 24;
        addField("field.minecraftagent.api_key", profile.apiKey, fieldX, y, value -> profile.apiKey = value); y += 24;
        addField("field.minecraftagent.model", profile.model, fieldX, y, value -> profile.model = value); y += 24;
        addField("field.minecraftagent.system_prompt", profile.systemPrompt, fieldX, y, value -> profile.systemPrompt = value); y += 24;
        addField("field.minecraftagent.http_proxy", profile.httpProxy, fieldX, y, value -> profile.httpProxy = value); y += 30;

        y = addSection("section.minecraftagent.runtime", x, fieldX, fieldWidth, y);
        addField("field.minecraftagent.temperature", Double.toString(profile.temperature), fieldX, y, value -> profile.temperature = parseDouble(value, profile.temperature)); y += 24;
        addField("field.minecraftagent.max_tokens", Integer.toString(profile.maxTokens), fieldX, y, value -> profile.maxTokens = parseInt(value, profile.maxTokens)); y += 24;
        addField("field.minecraftagent.max_steps", Integer.toString(profile.maxAgentSteps), fieldX, y, value -> profile.maxAgentSteps = parseInt(value, profile.maxAgentSteps)); y += 24;
        addField("field.minecraftagent.chat_prefix", config.chatPrefix, fieldX, y, value -> config.chatPrefix = value); y += 30;

        y = addSection("section.minecraftagent.billing", x, fieldX, fieldWidth, y);
        addField("field.minecraftagent.input_price", Double.toString(profile.inputUsdPerMillion), fieldX, y, value -> profile.inputUsdPerMillion = parseDouble(value, profile.inputUsdPerMillion)); y += 24;
        addField("field.minecraftagent.output_price", Double.toString(profile.outputUsdPerMillion), fieldX, y, value -> profile.outputUsdPerMillion = parseDouble(value, profile.outputUsdPerMillion)); y += 24;
        addField("field.minecraftagent.exchange", Double.toString(profile.usdToCny), fieldX, y, value -> profile.usdToCny = parseDouble(value, profile.usdToCny)); y += 24;
        addField("field.minecraftagent.daily_limit", Double.toString(profile.dailyLimitCny), fieldX, y, value -> profile.dailyLimitCny = parseDouble(value, profile.dailyLimitCny)); y += 30;

        int buttonX = x + 14;
        addDrawableChild(toggle("toggle.minecraftagent.streaming", profile.streamingEnabled, buttonX, y, value -> profile.streamingEnabled = value)); buttonX += 116;
        addDrawableChild(toggle("toggle.minecraftagent.vision", profile.visionEnabled, buttonX, y, value -> profile.visionEnabled = value)); buttonX += 116;
        addDrawableChild(toggle("toggle.minecraftagent.tools", profile.toolCallsEnabled, buttonX, y, value -> profile.toolCallsEnabled = value)); buttonX += 116;
        addDrawableChild(toggle("toggle.minecraftagent.cache", profile.cacheEnabled, buttonX, y, value -> profile.cacheEnabled = value)); buttonX += 116;
        addDrawableChild(toggle("toggle.minecraftagent.lock", profile.locked, buttonX, y, value -> profile.locked = value)); buttonX += 116;
        addDrawableChild(toggle("toggle.minecraftagent.confirm", config.delegationConfirmRequired, buttonX, y, value -> config.delegationConfirmRequired = value));
        y += 28;

        addDrawableChild(ButtonWidget.builder(Text.translatable("button.minecraftagent.new_profile"), button -> {
            saveFields();
            AgentProfile next = new AgentProfile();
            next.id = UUID.randomUUID().toString();
            config.profiles.add(next);
            config.activeProfileId = next.id;
            profile = next;
            clearAndInit();
        }).dimensions(x + 14, y, 112, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.minecraftagent.next_profile"), button -> {
            saveFields();
            int index = Math.max(0, config.profiles.indexOf(profile));
            profile = config.profiles.get((index + 1) % config.profiles.size());
            config.activeProfileId = profile.id;
            clearAndInit();
        }).dimensions(x + 134, y, 112, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.minecraftagent.delete_profile"), button -> {
            if (config.profiles.size() > 1) {
                config.profiles.remove(profile);
                profile = config.profiles.get(0);
                config.activeProfileId = profile.id;
                clearAndInit();
            }
        }).dimensions(x + 254, y, 112, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.minecraftagent.save"), button -> {
            saveFields();
            AgentConfigStore.getInstance().save();
            close();
        }).dimensions(x + panelWidth - 216, y, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.minecraftagent.cancel"), button -> close()).dimensions(x + panelWidth - 110, y, 96, 20).build());
    }

    private int addSection(String key, int panelX, int fieldX, int fieldWidth, int y) {
        sections.add(new SectionAnchor(key, panelX, y));
        return y + 16;
    }

    private void addField(String labelKey, String value, int x, int y, Consumer<String> setter) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, Math.min(560, width - x - 24), 18, Text.translatable(labelKey));
        field.setText(value == null ? "" : value);
        field.setMaxLength(4096);
        fields.add(new FieldBinding(labelKey, field, setter));
        addDrawableChild(field);
    }

    private ButtonWidget toggle(String labelKey, boolean initial, int x, int y, Consumer<Boolean> setter) {
        final boolean[] value = {initial};
        return ButtonWidget.builder(toggleText(labelKey, value[0]), button -> {
            value[0] = !value[0];
            setter.accept(value[0]);
            button.setMessage(toggleText(labelKey, value[0]));
        }).dimensions(x, y, 108, 20).build();
    }

    private Text toggleText(String labelKey, boolean value) {
        return Text.translatable(labelKey).append(Text.literal(": ")).append(Text.translatable(value ? "value.minecraftagent.on" : "value.minecraftagent.off"));
    }

    private void saveFields() {
        fields.forEach(field -> field.setter.accept(field.widget.getText()));
        config.activeProfileId = profile.id;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int panelWidth = Math.min(760, width - 40);
        int x = (width - panelWidth) / 2;
        int top = 24;
        int bottom = Math.min(height - 18, 456);
        super.render(context, mouseX, mouseY, delta);
        context.fill(x, top, x + panelWidth, bottom, 0xDD000000);
        context.drawBorder(x, top, panelWidth, bottom - top, 0xFF909090);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.minecraftagent.config.title"), width / 2, top + 8, 0xFFFFFFFF);
        for (SectionAnchor section : sections) {
            context.drawTextWithShadow(textRenderer, Text.translatable(section.key), section.x, section.y + 4, 0xFFFFE082);
        }
        for (FieldBinding field : fields) {
            int labelX = Math.max(x + 10, field.widget.getX() - 156);
            context.drawTextWithShadow(textRenderer, Text.translatable(field.labelKey), labelX, field.widget.getY() + 5, 0xFFFFE040);
        }
        int footerX = x + panelWidth;
        context.drawTextWithShadow(textRenderer, Text.literal("v" + MOD_VERSION), footerX - textRenderer.getWidth("v" + MOD_VERSION) - 4, bottom - 8, 0xFF707070);
        context.drawTextWithShadow(textRenderer, Text.literal("github.com/noah0932"), footerX - textRenderer.getWidth("github.com/noah0932") - 4, bottom + 6, 0xFF707070);
        context.drawTextWithShadow(textRenderer, Text.literal("MCAI.noah0932.top"), footerX - textRenderer.getWidth("MCAI.noah0932.top") - 4, bottom + 20, 0xFF707070);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record FieldBinding(String labelKey, TextFieldWidget widget, Consumer<String> setter) {
    }

    private record SectionAnchor(String key, int x, int y) {
    }
}
