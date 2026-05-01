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
    private static final String MOD_VERSION = "V1.1-Beta";
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
        int fieldX = x + 16;
        int y = 46;

        y = addSection("section.minecraftagent.profile", x + 14, y);
        addField("field.minecraftagent.name", profile.name, fieldX, y, value -> profile.name = value); y += 22;
        addField("field.minecraftagent.base_url", profile.baseUrl, fieldX, y, value -> profile.baseUrl = value); y += 22;
        addField("field.minecraftagent.api_key", profile.apiKey, fieldX, y, value -> profile.apiKey = value); y += 22;
        addField("field.minecraftagent.model", profile.model, fieldX, y, value -> profile.model = value); y += 22;
        addField("field.minecraftagent.system_prompt", profile.systemPrompt, fieldX, y, value -> profile.systemPrompt = value); y += 22;
        addField("field.minecraftagent.http_proxy", profile.httpProxy, fieldX, y, value -> profile.httpProxy = value); y += 26;

        y = addSection("section.minecraftagent.runtime", x + 14, y);
        addField("field.minecraftagent.temperature", Double.toString(profile.temperature), fieldX, y, value -> profile.temperature = parseDouble(value, profile.temperature)); y += 22;
        addField("field.minecraftagent.max_tokens", Integer.toString(profile.maxTokens), fieldX, y, value -> profile.maxTokens = parseInt(value, profile.maxTokens)); y += 22;
        addField("field.minecraftagent.max_steps", Integer.toString(profile.maxAgentSteps), fieldX, y, value -> profile.maxAgentSteps = parseInt(value, profile.maxAgentSteps)); y += 22;
        addField("field.minecraftagent.chat_prefix", config.chatPrefix, fieldX, y, value -> config.chatPrefix = value); y += 26;

        y = addSection("section.minecraftagent.billing", x + 14, y);
        addField("field.minecraftagent.input_price", Double.toString(profile.inputUsdPerMillion), fieldX, y, value -> profile.inputUsdPerMillion = parseDouble(value, profile.inputUsdPerMillion)); y += 22;
        addField("field.minecraftagent.output_price", Double.toString(profile.outputUsdPerMillion), fieldX, y, value -> profile.outputUsdPerMillion = parseDouble(value, profile.outputUsdPerMillion)); y += 22;
        addField("field.minecraftagent.exchange", Double.toString(profile.usdToCny), fieldX, y, value -> profile.usdToCny = parseDouble(value, profile.usdToCny)); y += 22;
        addField("field.minecraftagent.daily_limit", Double.toString(profile.dailyLimitCny), fieldX, y, value -> profile.dailyLimitCny = parseDouble(value, profile.dailyLimitCny)); y += 26;

        int buttonX = x + 14;
        addDrawableChild(toggle("toggle.minecraftagent.streaming", profile.streamingEnabled, buttonX, y, value -> profile.streamingEnabled = value)); buttonX += 110;
        addDrawableChild(toggle("toggle.minecraftagent.vision", profile.visionEnabled, buttonX, y, value -> profile.visionEnabled = value)); buttonX += 110;
        addDrawableChild(toggle("toggle.minecraftagent.tools", profile.toolCallsEnabled, buttonX, y, value -> profile.toolCallsEnabled = value)); buttonX += 110;
        addDrawableChild(toggle("toggle.minecraftagent.cache", profile.cacheEnabled, buttonX, y, value -> profile.cacheEnabled = value)); buttonX += 110;
        addDrawableChild(toggle("toggle.minecraftagent.lock", profile.locked, buttonX, y, value -> profile.locked = value)); buttonX += 110;
        addDrawableChild(toggle("toggle.minecraftagent.confirm", config.delegationConfirmRequired, buttonX, y, value -> config.delegationConfirmRequired = value));
        y += 26;

        addDrawableChild(ButtonWidget.builder(Text.translatable("button.minecraftagent.new_profile"), button -> {
            saveFields();
            AgentProfile next = new AgentProfile();
            next.id = UUID.randomUUID().toString();
            config.profiles.add(next);
            config.activeProfileId = next.id;
            profile = next;
            clearAndInit();
        }).dimensions(x + 14, y, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.minecraftagent.next_profile"), button -> {
            saveFields();
            int index = Math.max(0, config.profiles.indexOf(profile));
            profile = config.profiles.get((index + 1) % config.profiles.size());
            config.activeProfileId = profile.id;
            clearAndInit();
        }).dimensions(x + 130, y, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.minecraftagent.delete_profile"), button -> {
            if (config.profiles.size() > 1) {
                config.profiles.remove(profile);
                profile = config.profiles.get(0);
                config.activeProfileId = profile.id;
                clearAndInit();
            }
        }).dimensions(x + 246, y, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.minecraftagent.save"), button -> {
            saveFields();
            AgentConfigStore.getInstance().save();
            close();
        }).dimensions(x + panelWidth - 210, y, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.minecraftagent.cancel"), button -> close()).dimensions(x + panelWidth - 106, y, 96, 20).build());
    }

    private int addSection(String key, int labelX, int y) {
        sections.add(new SectionAnchor(key, labelX, y));
        return y + 16;
    }

    private void addField(String labelKey, String value, int x, int y, Consumer<String> setter) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x + 8, y, Math.min(300, width - x - 36), 18, Text.translatable(labelKey));
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
        }).dimensions(x, y, 104, 20).build();
    }

    private Text toggleText(String labelKey, boolean value) {
        return Text.translatable(labelKey).append(Text.literal(": ")).append(Text.translatable(value ? "value.minecraftagent.on" : "value.minecraftagent.off"));
    }

    private void saveFields() {
        fields.forEach(field -> field.setter.accept(field.widget.getText()));
        config.activeProfileId = profile.id;
    }

    private int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException ignored) { return fallback; }
    }

    private double parseDouble(String value, double fallback) {
        try { return Double.parseDouble(value.trim()); } catch (NumberFormatException ignored) { return fallback; }
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
        context.fill(x, top, x + panelWidth, bottom, 0xDD000000);
        context.drawBorder(x, top, panelWidth, bottom - top, 0xFF909090);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.minecraftagent.config.title"), width / 2, top + 8, 0xFFFFFFFF);

        for (SectionAnchor section : sections) {
            context.drawTextWithShadow(textRenderer, Text.translatable(section.key), section.x, section.y, 0xFFFFE082);
        }
        for (FieldBinding field : fields) {
            context.drawTextWithShadow(textRenderer, Text.translatable(field.labelKey), field.widget.getX() - textRenderer.getWidth(Text.translatable(field.labelKey)) - 12, field.widget.getY() + 6, 0xFFE0E0E0);
        }
        super.render(context, mouseX, mouseY, delta);

        int footY = bottom + 4;
        context.drawTextWithShadow(textRenderer, Text.literal("v" + MOD_VERSION + "  MCAI.noah0932.top  github.com/noah0932"), x + 4, footY, 0xFF606060);
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
