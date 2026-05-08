package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSelectionList<E extends AbstractSelectionList.Entry<E>> extends AbstractContainerWidget {
    private static final ResourceLocation MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png");
    private static final ResourceLocation INWORLD_MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");
    private static final int SEPARATOR_HEIGHT = 2;
    protected final Minecraft minecraft;
    protected final int defaultEntryHeight;
    private final List<E> children = new AbstractSelectionList.TrackedList();
    protected boolean centerListVertically = true;
    @Nullable
    private E selected;
    @Nullable
    private E hovered;

    public AbstractSelectionList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(0, y, width, height, CommonComponents.EMPTY);
        this.minecraft = minecraft;
        this.defaultEntryHeight = itemHeight;
    }

    @Nullable
    public E getSelected() {
        return this.selected;
    }

    public void setSelected(@Nullable E selected) {
        this.selected = selected;
        if (selected != null) {
            boolean flag = selected.getContentY() < this.getY();
            boolean flag1 = selected.getContentBottom() > this.getBottom();
            if (this.minecraft.getLastInputType().isKeyboard() || flag || flag1) {
                this.scrollToEntry(selected);
            }
        }
    }

    @Nullable
    public E getFocused() {
        return (E)super.getFocused();
    }

    @Override
    public final List<E> children() {
        return Collections.unmodifiableList(this.children);
    }

    protected void sort(Comparator<E> comparator) {
        this.children.sort(comparator);
        this.repositionEntries();
    }

    protected void swap(int index1, int index2) {
        Collections.swap(this.children, index1, index2);
        this.repositionEntries();
        this.scrollToEntry(this.children.get(index2));
    }

    public void clearEntries() {
        this.children.clear();
        this.selected = null;
    }

    protected void clearEntriesExcept(E entry) {
        this.children.removeIf(p_438723_ -> p_438723_ != entry);
        if (this.selected != entry) {
            this.setSelected(null);
        }
    }

    public void replaceEntries(Collection<E> entries) {
        this.clearEntries();

        for (E e : entries) {
            this.addEntry(e);
        }
    }

    private int getFirstEntryY() {
        return this.getY() + 2;
    }

    public int getNextY() {
        int i = this.getFirstEntryY() - (int)this.scrollAmount();

        for (E e : this.children) {
            i += e.getHeight();
        }

        return i;
    }

    protected int addEntry(E entry) {
        return this.addEntry(entry, this.defaultEntryHeight);
    }

    protected int addEntry(E entry, int index) {
        entry.setX(this.getRowLeft());
        entry.setWidth(this.getRowWidth());
        entry.setY(this.getNextY());
        entry.setHeight(index);
        this.children.add(entry);
        return this.children.size() - 1;
    }

    protected void addEntryToTop(E entry) {
        this.addEntryToTop(entry, this.defaultEntryHeight);
    }

    protected void addEntryToTop(E entry, int height) {
        double d0 = this.maxScrollAmount() - this.scrollAmount();
        entry.setHeight(height);
        this.children.addFirst(entry);
        this.repositionEntries();
        this.setScrollAmount(this.maxScrollAmount() - d0);
    }

    private void repositionEntries() {
        int i = this.getFirstEntryY() - (int)this.scrollAmount();

        for (E e : this.children) {
            e.setY(i);
            i += e.getHeight();
            e.setX(this.getRowLeft());
            e.setWidth(this.getRowWidth());
        }
    }

    protected void removeEntryFromTop(E entry) {
        double d0 = this.maxScrollAmount() - this.scrollAmount();
        this.removeEntry(entry);
        this.setScrollAmount(this.maxScrollAmount() - d0);
    }

    protected int getItemCount() {
        return this.children().size();
    }

    protected boolean entriesCanBeSelected() {
        return true;
    }

    @Nullable
    protected final E getEntryAtPosition(double mouseX, double mouseY) {
        for (E e : this.children) {
            if (e.isMouseOver(mouseX, mouseY)) {
                return e;
            }
        }

        return null;
    }

    public void updateSize(int width, HeaderAndFooterLayout layout) {
        this.updateSizeAndPosition(width, layout.getContentHeight(), layout.getHeaderHeight());
    }

    public void updateSizeAndPosition(int width, int height, int y) {
        this.updateSizeAndPosition(width, height, 0, y);
    }

    public void updateSizeAndPosition(int width, int height, int x, int y) {
        this.setSize(width, height);
        this.setPosition(x, y);
        this.repositionEntries();
        if (this.getSelected() != null) {
            this.scrollToEntry(this.getSelected());
        }

        this.refreshScrollAmount();
    }

    @Override
    protected int contentHeight() {
        int i = 0;

        for (E e : this.children) {
            i += e.getHeight();
        }

        return i + 4;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.hovered = this.isMouseOver(mouseX, mouseY) ? this.getEntryAtPosition(mouseX, mouseY) : null;
        this.renderListBackground(guiGraphics);
        this.enableScissor(guiGraphics);
        this.renderListItems(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.disableScissor();
        this.renderListSeparators(guiGraphics);
        this.renderScrollbar(guiGraphics, mouseX, mouseY);
    }

    protected void renderListSeparators(GuiGraphics guiGraphics) {
        ResourceLocation resourcelocation = this.minecraft.level == null ? Screen.HEADER_SEPARATOR : Screen.INWORLD_HEADER_SEPARATOR;
        ResourceLocation resourcelocation1 = this.minecraft.level == null ? Screen.FOOTER_SEPARATOR : Screen.INWORLD_FOOTER_SEPARATOR;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, resourcelocation, this.getX(), this.getY() - 2, 0.0F, 0.0F, this.getWidth(), 2, 32, 2);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, resourcelocation1, this.getX(), this.getBottom(), 0.0F, 0.0F, this.getWidth(), 2, 32, 2);
    }

    protected void renderListBackground(GuiGraphics guiGraphics) {
        ResourceLocation resourcelocation = this.minecraft.level == null ? MENU_LIST_BACKGROUND : INWORLD_MENU_LIST_BACKGROUND;
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            resourcelocation,
            this.getX(),
            this.getY(),
            this.getRight(),
            this.getBottom() + (int)this.scrollAmount(),
            this.getWidth(),
            this.getHeight(),
            32,
            32
        );
    }

    protected void enableScissor(GuiGraphics guiGraphics) {
        guiGraphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
    }

    protected void scrollToEntry(E entry) {
        int i = entry.getY() - this.getY() - 2;
        if (i < 0) {
            this.scroll(i);
        }

        int j = this.getBottom() - entry.getY() - entry.getHeight() - 2;
        if (j < 0) {
            this.scroll(-j);
        }
    }

    protected void centerScrollOn(E entry) {
        int i = 0;

        for (E e : this.children) {
            if (e == entry) {
                i += e.getHeight() / 2;
                break;
            }

            i += e.getHeight();
        }

        this.setScrollAmount(i - this.height / 2.0);
    }

    private void scroll(int scroll) {
        this.setScrollAmount(this.scrollAmount() + scroll);
    }

    @Override
    public void setScrollAmount(double scrollAmount) {
        super.setScrollAmount(scrollAmount);
        this.repositionEntries();
    }

    @Override
    protected double scrollRate() {
        return this.defaultEntryHeight / 2.0;
    }

    @Override
    protected int scrollBarX() {
        return this.getRowRight() + 6 + 2;
    }

    /**
     * Returns the first event listener that intersects with the mouse coordinates.
     */
    @Override
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        return Optional.ofNullable(this.getEntryAtPosition(mouseX, mouseY));
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.setFocused(null);
        }
    }

    /**
     * Sets the focus state of the GUI element.
     *
     * @param focused the focused GUI element.
     */
    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        E e = this.getFocused();
        if (e != focused && e instanceof ContainerEventHandler containereventhandler) {
            containereventhandler.setFocused(null);
        }

        super.setFocused(focused);
        int i = this.children.indexOf(focused);
        if (i >= 0) {
            E e1 = this.children.get(i);
            this.setSelected(e1);
        }
    }

    @Nullable
    protected E nextEntry(ScreenDirection direction) {
        return this.nextEntry(direction, p_93510_ -> true);
    }

    @Nullable
    protected E nextEntry(ScreenDirection direction, Predicate<E> predicate) {
        return this.nextEntry(direction, predicate, this.getSelected());
    }

    @Nullable
    protected E nextEntry(ScreenDirection direction, Predicate<E> predicate, @Nullable E selected) {
        int i = switch (direction) {
            case RIGHT, LEFT -> 0;
            case UP -> -1;
            case DOWN -> 1;
        };
        if (!this.children().isEmpty() && i != 0) {
            int j;
            if (selected == null) {
                j = i > 0 ? 0 : this.children().size() - 1;
            } else {
                j = this.children().indexOf(selected) + i;
            }

            for (int k = j; k >= 0 && k < this.children.size(); k += i) {
                E e = this.children().get(k);
                if (predicate.test(e)) {
                    return e;
                }
            }
        }

        return null;
    }

    protected void renderListItems(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (E e : this.children) {
            if (e.getY() + e.getHeight() >= this.getY() && e.getY() <= this.getBottom()) {
                this.renderItem(guiGraphics, mouseX, mouseY, partialTick, e);
            }
        }
    }

    protected void renderItem(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, E item) {
        if (this.entriesCanBeSelected() && this.getSelected() == item) {
            int i = this.isFocused() ? -1 : -8355712;
            this.renderSelection(guiGraphics, item, i);
        }

        item.renderContent(guiGraphics, mouseX, mouseY, Objects.equals(this.hovered, item), partialTick);
    }

    protected void renderSelection(GuiGraphics guiGraphics, E entry, int backgroundColor) {
        int i = entry.getX();
        int j = entry.getY();
        int k = i + entry.getWidth();
        int l = j + entry.getHeight();
        guiGraphics.fill(i, j, k, l, backgroundColor);
        guiGraphics.fill(i + 1, j + 1, k - 1, l - 1, -16777216);
    }

    public int getRowLeft() {
        return this.getX() + this.width / 2 - this.getRowWidth() / 2;
    }

    public int getRowRight() {
        return this.getRowLeft() + this.getRowWidth();
    }

    public int getRowTop(int index) {
        return this.children.get(index).getY();
    }

    public int getRowBottom(int index) {
        E e = this.children.get(index);
        return e.getY() + e.getHeight();
    }

    public int getRowWidth() {
        return 220;
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        if (this.isFocused()) {
            return NarratableEntry.NarrationPriority.FOCUSED;
        } else {
            return this.hovered != null ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
        }
    }

    protected void removeEntries(List<E> entries) {
        entries.forEach(this::removeEntry);
    }

    protected void removeEntry(E entry) {
        boolean flag = this.children.remove(entry);
        if (flag) {
            this.repositionEntries();
            if (entry == this.getSelected()) {
                this.setSelected(null);
            }
        }
    }

    @Nullable
    protected E getHovered() {
        return this.hovered;
    }

    void bindEntryToSelf(AbstractSelectionList.Entry<E> entry) {
        entry.list = this;
    }

    protected void narrateListElementPosition(NarrationElementOutput narrationElementOutput, E entry) {
        List<E> list = this.children();
        if (list.size() > 1) {
            int i = list.indexOf(entry);
            if (i != -1) {
                narrationElementOutput.add(NarratedElementType.POSITION, Component.translatable("narrator.position.list", i + 1, list.size()));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected abstract static class Entry<E extends AbstractSelectionList.Entry<E>> implements GuiEventListener, LayoutElement {
        public static final int CONTENT_PADDING = 2;
        private int x = 0;
        private int y = 0;
        private int width = 0;
        private int height;
        @Deprecated
        protected AbstractSelectionList<E> list;

        /**
         * Sets the focus state of the GUI element.
         *
         * @param focused {@code true} to apply focus, {@code false} to remove focus
         */
        @Override
        public void setFocused(boolean focused) {
        }

        @Override
        public boolean isFocused() {
            return this.list.getFocused() == this;
        }

        public abstract void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick);

        /**
         * Checks if the given mouse coordinates are over the GUI element.
         * <p>
         * @return {@code true} if the mouse is over the GUI element, {@code false} otherwise.
         *
         * @param mouseX the X coordinate of the mouse.
         * @param mouseY the Y coordinate of the mouse.
         */
        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.getRectangle().containsPoint((int)mouseX, (int)mouseY);
        }

        @Override
        public void setX(int x) {
            this.x = x;
        }

        @Override
        public void setY(int y) {
            this.y = y;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getContentX() {
            return this.getX() + 2;
        }

        public int getContentY() {
            return this.getY() + 2;
        }

        public int getContentHeight() {
            return this.getHeight() - 4;
        }

        public int getContentYMiddle() {
            return this.getContentY() + this.getContentHeight() / 2;
        }

        public int getContentBottom() {
            return this.getContentY() + this.getContentHeight();
        }

        public int getContentWidth() {
            return this.getWidth() - 4;
        }

        public int getContentXMiddle() {
            return this.getContentX() + this.getContentWidth() / 2;
        }

        public int getContentRight() {
            return this.getContentX() + this.getContentWidth();
        }

        @Override
        public int getX() {
            return this.x;
        }

        @Override
        public int getY() {
            return this.y;
        }

        @Override
        public int getWidth() {
            return this.width;
        }

        @Override
        public int getHeight() {
            return this.height;
        }

        @Override
        public void visitWidgets(Consumer<AbstractWidget> consumer) {
        }

        @Override
        public ScreenRectangle getRectangle() {
            return LayoutElement.super.getRectangle();
        }
    }

    @OnlyIn(Dist.CLIENT)
    class TrackedList extends AbstractList<E> {
        private final List<E> delegate = Lists.newArrayList();

        public E get(int index) {
            return this.delegate.get(index);
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        public E set(int index, E entry) {
            E e = this.delegate.set(index, entry);
            AbstractSelectionList.this.bindEntryToSelf(entry);
            return e;
        }

        public void add(int index, E entry) {
            this.delegate.add(index, entry);
            AbstractSelectionList.this.bindEntryToSelf(entry);
        }

        public E remove(int index) {
            return this.delegate.remove(index);
        }
    }
}
