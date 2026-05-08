package net.minecraft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StringWidget extends AbstractStringWidget {
    private int maxWidth = 0;
    private int cachedWidth = 0;
    private boolean cachedWidthDirty = true;
    private StringWidget.TextOverflow textOverflow = StringWidget.TextOverflow.CLAMPED;

    public StringWidget(Component message, Font font) {
        this(0, 0, font.width(message.getVisualOrderText()), 9, message, font);
    }

    public StringWidget(int width, int height, Component message, Font font) {
        this(0, 0, width, height, message, font);
    }

    public StringWidget(int x, int y, int width, int height, Component message, Font font) {
        super(x, y, width, height, message, font);
        this.active = false;
    }

    public StringWidget setColor(int color) {
        super.setColor(color);
        return this;
    }

    @Override
    public void setMessage(Component message) {
        super.setMessage(message);
        this.cachedWidthDirty = true;
    }

    public StringWidget setMaxWidth(int maxWidth) {
        return this.setMaxWidth(maxWidth, StringWidget.TextOverflow.CLAMPED);
    }

    public StringWidget setMaxWidth(int maxWidth, StringWidget.TextOverflow textOverflow) {
        this.maxWidth = maxWidth;
        this.textOverflow = textOverflow;
        return this;
    }

    @Override
    public int getWidth() {
        if (this.maxWidth > 0) {
            if (this.cachedWidthDirty) {
                this.cachedWidth = Math.min(this.maxWidth, this.getFont().width(this.getMessage().getVisualOrderText()));
                this.cachedWidthDirty = false;
            }

            return this.cachedWidth;
        } else {
            return super.getWidth();
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Component component = this.getMessage();
        Font font = this.getFont();
        int i = this.maxWidth > 0 ? this.maxWidth : this.getWidth();
        int j = font.width(component);
        int k = this.getX();
        int l = this.getY() + (this.getHeight() - 9) / 2;
        boolean flag = j > i;
        if (flag) {
            switch (this.textOverflow) {
                case CLAMPED:
                    guiGraphics.drawString(font, this.clipText(component, i), k, l, this.getColor());
                    break;
                case SCROLLING:
                    this.renderScrollingString(guiGraphics, font, 2, this.getColor());
            }
        } else {
            guiGraphics.drawString(font, component.getVisualOrderText(), k, l, this.getColor());
        }
    }

    private FormattedCharSequence clipText(Component message, int width) {
        Font font = this.getFont();
        FormattedText formattedtext = font.substrByWidth(message, width - font.width(CommonComponents.ELLIPSIS));
        return Language.getInstance().getVisualOrder(FormattedText.composite(formattedtext, CommonComponents.ELLIPSIS));
    }

    @OnlyIn(Dist.CLIENT)
    public static enum TextOverflow {
        CLAMPED,
        SCROLLING;
    }
}
