package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScrollableLayout implements Layout {
    private static final int SCROLLBAR_SPACING = 4;
    private static final int SCROLLBAR_RESERVE = 10;
    final Layout content;
    private final ScrollableLayout.Container container;
    private int minWidth;
    private int maxHeight;

    public ScrollableLayout(Minecraft minecraft, Layout content, int height) {
        this.content = content;
        this.container = new ScrollableLayout.Container(minecraft, 0, height);
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
        this.container.setWidth(Math.max(this.content.getWidth(), minWidth));
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        this.container.setHeight(Math.min(this.content.getHeight(), maxHeight));
        this.container.refreshScrollAmount();
    }

    @Override
    public void arrangeElements() {
        this.content.arrangeElements();
        int i = this.content.getWidth();
        this.container.setWidth(Math.max(i + 20, this.minWidth));
        this.container.setHeight(Math.min(this.content.getHeight(), this.maxHeight));
        this.container.refreshScrollAmount();
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        visitor.accept(this.container);
    }

    @Override
    public void setX(int x) {
        this.container.setX(x);
    }

    @Override
    public void setY(int y) {
        this.container.setY(y);
    }

    @Override
    public int getX() {
        return this.container.getX();
    }

    @Override
    public int getY() {
        return this.container.getY();
    }

    @Override
    public int getWidth() {
        return this.container.getWidth();
    }

    @Override
    public int getHeight() {
        return this.container.getHeight();
    }

    @OnlyIn(Dist.CLIENT)
    class Container extends AbstractContainerWidget {
        private final Minecraft minecraft;
        private final List<AbstractWidget> children = new ArrayList<>();

        public Container(Minecraft minecraft, int width, int height) {
            super(0, 0, width, height, CommonComponents.EMPTY);
            this.minecraft = minecraft;
            ScrollableLayout.this.content.visitWidgets(this.children::add);
        }

        @Override
        protected int contentHeight() {
            return ScrollableLayout.this.content.getHeight();
        }

        @Override
        protected double scrollRate() {
            return 10.0;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);

            for (AbstractWidget abstractwidget : this.children) {
                abstractwidget.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            guiGraphics.disableScissor();
            this.renderScrollbar(guiGraphics, mouseX, mouseY);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }

        @Override
        public ScreenRectangle getBorderForArrowNavigation(ScreenDirection direction) {
            return new ScreenRectangle(this.getX(), this.getY(), this.width, this.contentHeight());
        }

        /**
         * Sets the focus state of the GUI element.
         *
         * @param focused the focused GUI element.
         */
        @Override
        public void setFocused(@Nullable GuiEventListener focused) {
            super.setFocused(focused);
            if (focused != null && this.minecraft.getLastInputType().isKeyboard()) {
                ScreenRectangle screenrectangle = this.getRectangle();
                ScreenRectangle screenrectangle1 = focused.getRectangle();
                int i = screenrectangle1.top() - screenrectangle.top();
                int j = screenrectangle1.bottom() - screenrectangle.bottom();
                if (i < 0) {
                    this.setScrollAmount(this.scrollAmount() + i - 14.0);
                } else if (j > 0) {
                    this.setScrollAmount(this.scrollAmount() + j + 14.0);
                }
            }
        }

        @Override
        public void setX(int x) {
            super.setX(x);
            ScrollableLayout.this.content.setX(x + 10);
        }

        @Override
        public void setY(int y) {
            super.setY(y);
            ScrollableLayout.this.content.setY(y - (int)this.scrollAmount());
        }

        @Override
        public void setScrollAmount(double scrollAmount) {
            super.setScrollAmount(scrollAmount);
            ScrollableLayout.this.content.setY(this.getRectangle().top() - (int)this.scrollAmount());
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public Collection<? extends NarratableEntry> getNarratables() {
            return this.children;
        }
    }
}
