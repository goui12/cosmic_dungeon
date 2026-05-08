package net.minecraft.client.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractScrollArea extends AbstractWidget {
    public static final int SCROLLBAR_WIDTH = 6;
    private double scrollAmount;
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller");
    private static final ResourceLocation SCROLLER_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller_background");
    private boolean scrolling;

    public AbstractScrollArea(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.visible) {
            return false;
        } else {
            this.setScrollAmount(this.scrollAmount() - scrollY * this.scrollRate());
            return true;
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (this.scrolling) {
            if (event.y() < this.getY()) {
                this.setScrollAmount(0.0);
            } else if (event.y() > this.getBottom()) {
                this.setScrollAmount(this.maxScrollAmount());
            } else {
                double d0 = Math.max(1, this.maxScrollAmount());
                int i = this.scrollerHeight();
                double d1 = Math.max(1.0, d0 / (this.height - i));
                this.setScrollAmount(this.scrollAmount() + mouseY * d1);
            }

            return true;
        } else {
            return super.mouseDragged(event, mouseX, mouseY);
        }
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        this.scrolling = false;
    }

    public double scrollAmount() {
        return this.scrollAmount;
    }

    public void setScrollAmount(double scrollAmount) {
        this.scrollAmount = Mth.clamp(scrollAmount, 0.0, (double)this.maxScrollAmount());
    }

    public boolean updateScrolling(MouseButtonEvent event) {
        this.scrolling = this.scrollbarVisible() && this.isValidClickButton(event.buttonInfo()) && this.isOverScrollbar(event.x(), event.y());
        return this.scrolling;
    }

    protected boolean isOverScrollbar(double mouseX, double mouseY) {
        return mouseX >= this.scrollBarX() && mouseX <= this.scrollBarX() + 6 && mouseY >= this.getY() && mouseY < this.getBottom();
    }

    public void refreshScrollAmount() {
        this.setScrollAmount(this.scrollAmount);
    }

    public int maxScrollAmount() {
        return Math.max(0, this.contentHeight() - this.height);
    }

    protected boolean scrollbarVisible() {
        return this.maxScrollAmount() > 0;
    }

    protected int scrollerHeight() {
        return Mth.clamp((int)((float)(this.height * this.height) / this.contentHeight()), 32, this.height - 8);
    }

    protected int scrollBarX() {
        return this.getRight() - 6;
    }

    protected int scrollBarY() {
        return Math.max(this.getY(), (int)this.scrollAmount * (this.height - this.scrollerHeight()) / this.maxScrollAmount() + this.getY());
    }

    protected void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.scrollbarVisible()) {
            int i = this.scrollBarX();
            int j = this.scrollerHeight();
            int k = this.scrollBarY();
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_BACKGROUND_SPRITE, i, this.getY(), 6, this.getHeight());
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, i, k, 6, j);
            if (this.isOverScrollbar(mouseX, mouseY)) {
                guiGraphics.requestCursor(this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
            }
        }
    }

    protected abstract int contentHeight();

    protected abstract double scrollRate();
}
