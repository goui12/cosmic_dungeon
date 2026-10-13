package net.goui.cosmicdungeon.client.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/** Presentation only: the existing server-confirmed Ready action gets a restrained green prompt. */
public final class ReadyActionButton extends Button {
    private boolean readyAction;
    public ReadyActionButton(int x, int y, int width, Component message, boolean readyAction, OnPress action) {
        super(x, y, width, DEFAULT_HEIGHT, message, action, DEFAULT_NARRATION);
        this.readyAction = readyAction;
    }
    public void setReadyAction(boolean value) { readyAction = value; }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!active || !readyAction) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            return;
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES.get(active, isHoveredOrFocused()),
                getX(), getY(), getWidth(), getHeight(),
                ARGB.color(alpha, ReadyActionStyle.tint(Util.getMillis(), isHoveredOrFocused())));
        renderString(graphics, Minecraft.getInstance().font, ARGB.color(alpha, getFGColor()));
        if (isHovered()) graphics.requestCursor(isActive() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
    }
}
