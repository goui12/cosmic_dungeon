package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractTextAreaWidget extends AbstractScrollArea {
    private static final WidgetSprites BACKGROUND_SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("widget/text_field"), ResourceLocation.withDefaultNamespace("widget/text_field_highlighted")
    );
    private static final int INNER_PADDING = 4;
    public static final int DEFAULT_TOTAL_PADDING = 8;
    private boolean showBackground = true;
    private boolean showDecorations = true;

    public AbstractTextAreaWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public AbstractTextAreaWidget(int x, int y, int width, int height, Component message, boolean showBackground, boolean showDecorations) {
        this(x, y, width, height, message);
        this.showBackground = showBackground;
        this.showDecorations = showDecorations;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        boolean flag = this.updateScrolling(event);
        return super.mouseClicked(event, isDoubleClick) || flag;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean flag = event.isUp();
        boolean flag1 = event.isDown();
        if (flag || flag1) {
            double d0 = this.scrollAmount();
            this.setScrollAmount(this.scrollAmount() + (flag ? -1 : 1) * this.scrollRate());
            if (d0 != this.scrollAmount()) {
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.visible) {
            if (this.showBackground) {
                this.renderBackground(guiGraphics);
            }

            guiGraphics.enableScissor(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1);
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(0.0F, (float)(-this.scrollAmount()));
            this.renderContents(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.pose().popMatrix();
            guiGraphics.disableScissor();
            this.renderScrollbar(guiGraphics, mouseX, mouseY);
            if (this.showDecorations) {
                this.renderDecorations(guiGraphics);
            }
        }
    }

    protected void renderDecorations(GuiGraphics guiGraphics) {
    }

    protected int innerPadding() {
        return 4;
    }

    protected int totalInnerPadding() {
        return this.innerPadding() * 2;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.active
            && this.visible
            && mouseX >= this.getX()
            && mouseY >= this.getY()
            && mouseX < this.getRight() + 6
            && mouseY < this.getBottom();
    }

    @Override
    protected int scrollBarX() {
        return this.getRight();
    }

    @Override
    protected int contentHeight() {
        return this.getInnerHeight() + this.totalInnerPadding();
    }

    protected void renderBackground(GuiGraphics guiGraphics) {
        this.renderBorder(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    protected void renderBorder(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        ResourceLocation resourcelocation = BACKGROUND_SPRITES.get(this.isActive(), this.isFocused());
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation, x, y, width, height);
    }

    protected boolean withinContentAreaTopBottom(int top, int bottom) {
        return bottom - this.scrollAmount() >= this.getY() && top - this.scrollAmount() <= this.getY() + this.height;
    }

    protected abstract int getInnerHeight();

    protected abstract void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    protected int getInnerLeft() {
        return this.getX() + this.innerPadding();
    }

    protected int getInnerTop() {
        return this.getY() + this.innerPadding();
    }

    @Override
    public void playDownSound(SoundManager handler) {
    }
}
