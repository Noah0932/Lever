package com.noah.minecraftagent.client.ui;

import com.noah.minecraftagent.MinecraftAgentMod;
import com.noah.minecraftagent.common.config.AgentConfig;
import com.noah.minecraftagent.common.config.AgentConfigStore;
import com.noah.minecraftagent.common.config.AgentProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class AgentConfigScreen extends Screen {
    private final Screen parent;
    private final AgentConfig config;
    private AgentProfile profile;
    private ConfigListWidget listWidget;
    private final List<TextFieldPool> pool = new ArrayList<>();

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
        pool.clear();
        int listTop = 30;
        int listBottom = height - 6;
        int listWidth = Math.min(480, width - 12);

        listWidget = new ConfigListWidget(client, listWidth, listTop, listBottom);
        listWidget.setX((width - listWidth) / 2);
        addDrawableChild(listWidget);

        addSection("section.minecraftagent.profile");
        addF("field.minecraftagent.name", profile.name, v -> profile.name = v);
        addF("field.minecraftagent.base_url", profile.baseUrl, v -> profile.baseUrl = v);
        addF("field.minecraftagent.api_key", profile.apiKey, v -> profile.apiKey = v);
        addF("field.minecraftagent.model", profile.model, v -> profile.model = v);
        addF("field.minecraftagent.system_prompt", profile.systemPrompt, v -> profile.systemPrompt = v);
        addF("field.minecraftagent.http_proxy", profile.httpProxy, v -> profile.httpProxy = v);

        addSection("section.minecraftagent.runtime");
        addF("field.minecraftagent.temperature", d2s(profile.temperature), v -> profile.temperature = pDouble(v, profile.temperature));
        addF("field.minecraftagent.max_tokens", i2s(profile.maxTokens), v -> profile.maxTokens = pInt(v, profile.maxTokens));
        addF("field.minecraftagent.max_steps", i2s(profile.maxAgentSteps), v -> profile.maxAgentSteps = pInt(v, profile.maxAgentSteps));
        addF("field.minecraftagent.chat_prefix", config.chatPrefix, v -> config.chatPrefix = v);

        addSection("section.minecraftagent.billing");
        addF("field.minecraftagent.input_price", d2s(profile.inputUsdPerMillion), v -> profile.inputUsdPerMillion = pDouble(v, profile.inputUsdPerMillion));
        addF("field.minecraftagent.output_price", d2s(profile.outputUsdPerMillion), v -> profile.outputUsdPerMillion = pDouble(v, profile.outputUsdPerMillion));
        addF("field.minecraftagent.exchange", d2s(profile.usdToCny), v -> profile.usdToCny = pDouble(v, profile.usdToCny));
        addF("field.minecraftagent.daily_limit", d2s(profile.dailyLimitCny), v -> profile.dailyLimitCny = pDouble(v, profile.dailyLimitCny));

        listWidget.addEntry(listWidget.new ToggleRowEntry());
        listWidget.addEntry(listWidget.new ButtonRowEntry());
    }

    private void addSection(String key) { listWidget.addEntry(listWidget.new SectionEntry(key)); }

    private void addF(String labelKey, String value, Consumer<String> setter) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, 0, 0, listWidget.getRowWidth() - 12, 18, Text.translatable(labelKey));
        field.setText(value == null ? "" : value);
        field.setMaxLength(4096);
        listWidget.addEntry(listWidget.new FieldEntry(labelKey, field, setter));
        pool.add(new TextFieldPool(field, labelKey, setter));
    }

    private void doSave() {
        for (TextFieldPool tf : pool) tf.setter.accept(tf.widget.getText());
        config.activeProfileId = profile.id;
        AgentConfigStore.getInstance().save();
    }

    private static String d2s(double v) { return Double.toString(v); }
    private static String i2s(int v) { return Integer.toString(v); }
    private int pInt(String s, int fb) { try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return fb; } }
    private double pDouble(String s, double fb) { try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return fb; } }

    @Override public void close() { client.setScreen(parent); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int panelWidth = Math.min(480, width - 12);
        int x = (width - panelWidth) / 2;
        context.fill(x, 10, x + panelWidth, 28, 0xCC1E1E30);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Lever " + MinecraftAgentMod.MOD_VERSION).append(Text.literal("  ")).append(Text.translatable("screen.minecraftagent.config.title")),
                width / 2, 14, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }

    private record TextFieldPool(TextFieldWidget widget, String label, Consumer<String> setter) {}

    final class ConfigListWidget extends EntryListWidget<ConfigListWidget.ConfigEntry> {
        ConfigListWidget(MinecraftClient client, int width, int top, int bottom) {
            super(client, width, top, bottom, 26);
        }

        @Override public int getRowWidth() { return Math.min(420, width - 16); }

        @Override
        public void appendClickableNarrations(NarrationMessageBuilder builder) {}

        public int addEntry(ConfigEntry entry) { return super.addEntry(entry); }

        abstract class ConfigEntry extends EntryListWidget.Entry<ConfigEntry> {}

        class SectionEntry extends ConfigEntry {
            final String key;
            SectionEntry(String k) { key = k; }
            @Override
            public void render(DrawContext c, int i, int y, int x, int ew, int eh, int mx, int my, boolean hov, float d) {
                c.drawTextWithShadow(textRenderer, Text.translatable(key), x + 8, y + 2, 0xFFFFE082);
            }
        }

        class FieldEntry extends ConfigEntry {
            final String labelKey;
            final TextFieldWidget widget;
            final Consumer<String> setter;

            FieldEntry(String lk, TextFieldWidget w, Consumer<String> s) { labelKey = lk; widget = w; setter = s; }
            @Override
            public void render(DrawContext c, int i, int y, int x, int ew, int eh, int mx, int my, boolean hov, float d) {
                c.drawTextWithShadow(textRenderer, Text.translatable(labelKey), x + 8, y + 6, 0xFFFFE040);
                widget.setX(x + 120);
                widget.setY(y + 2);
                widget.setWidth(Math.min(260, ew - 130));
                widget.render(c, mx, my, d);
            }
            @Override public boolean mouseClicked(double mx, double my, int btn) { return widget.mouseClicked(mx, my, btn); }
            @Override public boolean keyPressed(int kc, int sc, int mod) { return widget.keyPressed(kc, sc, mod); }
            @Override public boolean charTyped(char ch, int mod) { return widget.charTyped(ch, mod); }
        }

        class ToggleRowEntry extends ConfigEntry {
            @Override
            public void render(DrawContext c, int i, int y, int x, int ew, int eh, int mx, int my, boolean hov, float d) {
                String[] ks = {"streaming", "vision", "tools", "cache", "lock", "confirm"};
                boolean[] vs = {profile.streamingEnabled, profile.visionEnabled, profile.toolCallsEnabled, profile.cacheEnabled, profile.locked, config.delegationConfirmRequired};
                int bw = Math.min(100, (ew - 20) / 6);
                int bx = x + 4;
                for (int j = 0; j < 6; j++) {
                    Text label = Text.translatable("toggle.minecraftagent." + ks[j])
                            .append(Text.literal(":"))
                            .append(Text.translatable(vs[j] ? "value.minecraftagent.on" : "value.minecraftagent.off"));
                    c.drawTextWithShadow(textRenderer, label, bx, y + 2, 0xFFCCCCCC);
                    bx += Math.max(70, bw) + 4;
                }
            }
            @Override
            public boolean mouseClicked(double mx, double my, int btn) {
                boolean[] vs = {profile.streamingEnabled, profile.visionEnabled, profile.toolCallsEnabled, profile.cacheEnabled, profile.locked, config.delegationConfirmRequired};
                Consumer<Boolean> ss0 = v -> profile.streamingEnabled = v;
                Consumer<Boolean> ss1 = v -> profile.visionEnabled = v;
                Consumer<Boolean> ss2 = v -> profile.toolCallsEnabled = v;
                Consumer<Boolean> ss3 = v -> profile.cacheEnabled = v;
                Consumer<Boolean> ss4 = v -> profile.locked = v;
                Consumer<Boolean> ss5 = v -> config.delegationConfirmRequired = v;
                Consumer<Boolean>[] ss = new Consumer[]{ss0, ss1, ss2, ss3, ss4, ss5};
                int bw = Math.min(100, (getRowWidth() - 20) / 6);
                int bx = getX() + 4;
                for (int j = 0; j < 6; j++) {
                    int w = Math.max(70, bw);
                    if (mx >= bx && mx <= bx + w + 4) {
                        vs[j] = !vs[j];
                        ss[j].accept(vs[j]);
                        return true;
                    }
                    bx += w + 4;
                }
                return false;
            }
        }

        class ButtonRowEntry extends ConfigEntry {
            @Override
            public void render(DrawContext c, int i, int y, int x, int ew, int eh, int mx, int my, boolean hov, float d) {
                int bw = Math.min(84, (ew - 30) / 4);
                int bx = x + 4;
                drawBtn(c, Text.translatable("button.minecraftagent.new_profile"), bx, y, bw, eh); bx += bw + 6;
                drawBtn(c, Text.translatable("button.minecraftagent.next_profile"), bx, y, bw, eh); bx += bw + 6;
                drawBtn(c, Text.translatable("button.minecraftagent.save"), bx, y, bw, eh); bx += bw + 6;
                drawBtn(c, Text.translatable("button.minecraftagent.cancel"), bx, y, bw, eh);
            }
            private void drawBtn(DrawContext c, Text t, int bx, int by, int bw, int bh) {
                c.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, 0xBB404040);
                c.drawCenteredTextWithShadow(textRenderer, t, bx + bw / 2, by + (bh - 8) / 2, 0xFFFFFFFF);
            }
            @Override
            public boolean mouseClicked(double mx, double my, int btn) {
                int bw = Math.min(84, (getRowWidth() - 30) / 4);
                int bx = getX() + 4;
                if (mx >= bx && mx <= bx + bw) { doSave(); AgentProfile n = new AgentProfile(); n.id = UUID.randomUUID().toString(); config.profiles.add(n); config.activeProfileId = n.id; profile = n; clearAndInit(); return true; }
                bx += bw + 6;
                if (mx >= bx && mx <= bx + bw) { doSave(); int idx = Math.max(0, config.profiles.indexOf(profile)); profile = config.profiles.get((idx + 1) % config.profiles.size()); config.activeProfileId = profile.id; clearAndInit(); return true; }
                bx += bw + 6;
                if (mx >= bx && mx <= bx + bw) { doSave(); close(); return true; }
                bx += bw + 6;
                if (mx >= bx && mx <= bx + bw) { close(); return true; }
                return false;
            }
        }
    }
}
