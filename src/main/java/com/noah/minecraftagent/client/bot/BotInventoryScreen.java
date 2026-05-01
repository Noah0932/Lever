package com.noah.minecraftagent.client.bot;

import com.noah.minecraftagent.common.bot.BotProfile;
import com.noah.minecraftagent.common.bot.payload.BotProfileUpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public final class BotInventoryScreen extends Screen {
    private static final String MOD_VERSION = "V1.1-Beta";
    private final BotProfile profile;
    private final Screen parent;
    private TextFieldWidget nameField;
    private TextFieldWidget whitelistField;

    public BotInventoryScreen(BotProfile profile, Screen parent) {
        super(Text.literal(profile.displayName() + " - 背包"));
        this.profile = profile;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelWidth = 300;
        int x = (width - panelWidth) / 2;
        int y = 30;

        nameField = new TextFieldWidget(textRenderer, x + 10, y + 20, panelWidth - 20, 18, Text.literal("Bot 名称"));
        nameField.setText(profile.customName.isBlank() ? profile.name + " " + profile.botNumber : profile.customName);
        nameField.setMaxLength(32);
        addDrawableChild(nameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("重命名"), btn -> {
            ClientPlayNetworking.send(new BotProfileUpdatePayload(profile.id, "customName", nameField.getText()));
        }).dimensions(x + 10, y + 44, panelWidth - 20, 20).build());

        whitelistField = new TextFieldWidget(textRenderer, x + 10, y + 84, panelWidth - 110, 18, Text.literal("玩家名/UUID"));
        whitelistField.setMaxLength(64);
        addDrawableChild(whitelistField);

        addDrawableChild(ButtonWidget.builder(Text.literal("加白名单"), btn -> {
            String val = whitelistField.getText().trim();
            if (!val.isEmpty()) {
                ClientPlayNetworking.send(new BotProfileUpdatePayload(profile.id, "whitelist_add", val));
            }
        }).dimensions(x + panelWidth - 96, y + 82, 86, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("移白名单"), btn -> {
            String val = whitelistField.getText().trim();
            if (!val.isEmpty()) {
                ClientPlayNetworking.send(new BotProfileUpdatePayload(profile.id, "whitelist_remove", val));
            }
        }).dimensions(x + panelWidth - 96, y + 106, 86, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("转移所有权 (新主人UUID)"), btn -> {
            String val = whitelistField.getText().trim();
            if (!val.isEmpty()) {
                ClientPlayNetworking.send(new BotProfileUpdatePayload(profile.id, "transfer_owner", val));
            }
        }).dimensions(x + 10, y + 110, panelWidth - 110, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("关闭"), btn -> close()).dimensions(x + panelWidth / 2 - 40, y + 184, 80, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int panelWidth = 300;
        int x = (width - panelWidth) / 2;
        context.fill(x, 20, x + panelWidth, 220, 0xDD000000);
        context.drawBorder(x, 20, panelWidth, 200, 0xFF909090);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 22, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("所有者: " + profile.ownerName), x + 10, 70, 0xFFAAAAAA);
        context.drawTextWithShadow(textRenderer, Text.literal("白名单: " + String.join(", ", profile.whitelist.stream().limit(3).toList()) + (profile.whitelist.size() > 3 ? "..." : "")), x + 10, 140, 0xFFAAAAAA);
        context.drawTextWithShadow(textRenderer, Text.literal("AI配置: " + (profile.aiConfigInherited ? "继承自主人" : "独立配置")), x + 10, 156, 0xFFAAAAAA);
        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(textRenderer, Text.literal("v" + MOD_VERSION), x + panelWidth - textRenderer.getWidth("v" + MOD_VERSION) - 4, 210, 0xFF505050);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
