package net.minecraft.client.gui.components;

import java.util.OptionalInt;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.SingleKeyCache;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiLineTextWidget extends AbstractStringWidget {
    private OptionalInt maxWidth = OptionalInt.empty();
    private OptionalInt maxRows = OptionalInt.empty();
    private final SingleKeyCache<MultiLineTextWidget.CacheKey, MultiLineLabel> cache;
    private boolean centered = false;
    private boolean allowHoverComponents = false;
    @Nullable
    private Consumer<Style> componentClickHandler = null;

    public MultiLineTextWidget(Component message, Font font) {
        this(0, 0, message, font);
    }

    public MultiLineTextWidget(int x, int y, Component message, Font font) {
        super(x, y, 0, 0, message, font);
        this.cache = Util.singleKeyCache(
            p_352660_ -> p_352660_.maxRows.isPresent()
                ? MultiLineLabel.create(font, p_352660_.maxWidth, p_352660_.maxRows.getAsInt(), p_352660_.message)
                : MultiLineLabel.create(font, p_352660_.message, p_352660_.maxWidth)
        );
        this.active = false;
    }

    public MultiLineTextWidget setColor(int color) {
        super.setColor(color);
        return this;
    }

    public MultiLineTextWidget setMaxWidth(int maxWidth) {
        this.maxWidth = OptionalInt.of(maxWidth);
        return this;
    }

    public MultiLineTextWidget setMaxRows(int maxRows) {
        this.maxRows = OptionalInt.of(maxRows);
        return this;
    }

    public MultiLineTextWidget setCentered(boolean centered) {
        this.centered = centered;
        return this;
    }

    public MultiLineTextWidget configureStyleHandling(boolean allowHoverComponents, @Nullable Consumer<Style> componentClickHandler) {
        this.allowHoverComponents = allowHoverComponents;
        this.componentClickHandler = componentClickHandler;
        return this;
    }

    @Override
    public int getWidth() {
        return this.cache.getValue(this.getFreshCacheKey()).getWidth();
    }

    @Override
    public int getHeight() {
        return this.cache.getValue(this.getFreshCacheKey()).getLineCount() * 9;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        MultiLineLabel multilinelabel = this.cache.getValue(this.getFreshCacheKey());
        int i = this.getX();
        int j = this.getY();
        int k = 9;
        int l = this.getColor();
        if (this.centered) {
            int i1 = i + this.getWidth() / 2;
            multilinelabel.render(guiGraphics, MultiLineLabel.Align.CENTER, i1, j, k, true, l);
        } else {
            multilinelabel.render(guiGraphics, MultiLineLabel.Align.LEFT, i, j, k, true, l);
        }

        if (this.isHovered() && this.allowHoverComponents) {
            Style style = this.getComponentStyleAt(mouseX, mouseY);
            guiGraphics.renderComponentHoverEffect(this.getFont(), style, mouseX, mouseY);
        }
    }

    @Nullable
    private Style getComponentStyleAt(double mouseX, double mouseY) {
        MultiLineLabel multilinelabel = this.cache.getValue(this.getFreshCacheKey());
        int i = this.getX();
        int j = this.getY();
        int k = 9;
        if (this.centered) {
            int l = i + this.getWidth() / 2;
            return multilinelabel.getStyle(MultiLineLabel.Align.CENTER, l, j, k, mouseX, mouseY);
        } else {
            return multilinelabel.getStyle(MultiLineLabel.Align.LEFT, i, j, k, mouseX, mouseY);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
        if (this.componentClickHandler != null) {
            Style style = this.getComponentStyleAt(event.x(), event.y());
            if (style != null) {
                this.componentClickHandler.accept(style);
                return;
            }
        }

        super.onClick(event, isDoubleClick);
    }

    private MultiLineTextWidget.CacheKey getFreshCacheKey() {
        return new MultiLineTextWidget.CacheKey(this.getMessage(), this.maxWidth.orElse(Integer.MAX_VALUE), this.maxRows);
    }

    @OnlyIn(Dist.CLIENT)
    record CacheKey(Component message, int maxWidth, OptionalInt maxRows) {
    }
}
