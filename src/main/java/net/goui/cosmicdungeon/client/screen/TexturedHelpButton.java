package net.goui.cosmicdungeon.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class TexturedHelpButton {
    private static final int TEXT_COLOR = 0xFFEFE6D0;
    private static final int SELECTED_TEXT_COLOR = 0xFFFFD98A;
    private static final int DISABLED_TEXT_COLOR = 0xFF8A8479;

    private TexturedHelpButton() {}

    static void renderText(GuiGraphics g, Font font, Component label, int x, int y, boolean hovered, boolean selected, boolean enabled) {
        ResourceLocation texture = !enabled ? HelpMenuAssets.TEXT_BUTTON_DISABLED : selected ? HelpMenuAssets.TEXT_BUTTON_SELECTED : hovered ? HelpMenuAssets.TEXT_BUTTON_HOVER : HelpMenuAssets.TEXT_BUTTON;
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, HelpMenuAssets.TEXT_BUTTON_W, HelpMenuAssets.TEXT_BUTTON_H, HelpMenuAssets.TEXT_BUTTON_W, HelpMenuAssets.TEXT_BUTTON_H);
        int color = !enabled ? DISABLED_TEXT_COLOR : selected ? SELECTED_TEXT_COLOR : TEXT_COLOR;
        String text = font.plainSubstrByWidth(label.getString(), HelpMenuAssets.TEXT_BUTTON_W - 12);
        g.drawCenteredString(font, text, x + HelpMenuAssets.TEXT_BUTTON_W / 2, y + (HelpMenuAssets.TEXT_BUTTON_H - font.lineHeight) / 2, color);
    }

    static void renderArrow(GuiGraphics g, int x, int y, boolean up, boolean hovered) {
        ResourceLocation texture = up ? (hovered ? HelpMenuAssets.UP_BUTTON_HOVER : HelpMenuAssets.UP_BUTTON) : (hovered ? HelpMenuAssets.DOWN_BUTTON_HOVER : HelpMenuAssets.DOWN_BUTTON);
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, HelpMenuAssets.ARROW_W, HelpMenuAssets.ARROW_H, HelpMenuAssets.ARROW_W, HelpMenuAssets.ARROW_H);
    }
}
