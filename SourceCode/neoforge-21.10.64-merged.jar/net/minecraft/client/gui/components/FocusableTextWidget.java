package net.minecraft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FocusableTextWidget extends MultiLineTextWidget {
    public static final int DEFAULT_PADDING = 4;
    private final boolean alwaysShowBorder;
    private final FocusableTextWidget.BackgroundFill backgroundFill;
    private final int padding;

    public FocusableTextWidget(int maxWidth, Component message, Font font) {
        this(maxWidth, message, font, 4);
    }

    public FocusableTextWidget(int maxWidth, Component message, Font font, int padding) {
        this(maxWidth, message, font, true, FocusableTextWidget.BackgroundFill.ALWAYS, padding);
    }

    public FocusableTextWidget(
        int maxWidth, Component message, Font font, boolean alwaysShowBorder, FocusableTextWidget.BackgroundFill backgroundFill, int padding
    ) {
        super(message, font);
        this.setMaxWidth(maxWidth);
        this.setCentered(true);
        this.active = true;
        this.alwaysShowBorder = alwaysShowBorder;
        this.backgroundFill = backgroundFill;
        this.padding = padding;
    }

    public void containWithin(int width) {
        this.setMaxWidth(width - this.padding * 4);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.getMessage());
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int i = this.getX() - this.padding;
        int j = this.getY() - this.padding;
        int k = this.getWidth() + this.padding * 2;
        int l = this.getHeight() + this.padding * 2;
        int i1 = ARGB.color(this.alpha, this.alwaysShowBorder ? (this.isFocused() ? -1 : -6250336) : -1);
        switch (this.backgroundFill) {
            case ALWAYS:
                guiGraphics.fill(i + 1, j, i + k, j + l, ARGB.color(this.alpha, -16777216));
                break;
            case ON_FOCUS:
                if (this.isFocused()) {
                    guiGraphics.fill(i + 1, j, i + k, j + l, ARGB.color(this.alpha, -16777216));
                }
            case NEVER:
        }

        if (this.isFocused() || this.alwaysShowBorder) {
            guiGraphics.submitOutline(i, j, k, l, i1);
        }

        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void playDownSound(SoundManager handler) {
    }

    @OnlyIn(Dist.CLIENT)
    public static enum BackgroundFill {
        ALWAYS,
        ON_FOCUS,
        NEVER;
    }
}
