package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2i;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractContainerScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
    /**
     * The location of the inventory background texture
     */
    public static final ResourceLocation INVENTORY_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final ResourceLocation SLOT_HIGHLIGHT_BACK_SPRITE = ResourceLocation.withDefaultNamespace("container/slot_highlight_back");
    private static final ResourceLocation SLOT_HIGHLIGHT_FRONT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot_highlight_front");
    protected static final int BACKGROUND_TEXTURE_WIDTH = 256;
    protected static final int BACKGROUND_TEXTURE_HEIGHT = 256;
    private static final float SNAPBACK_SPEED = 100.0F;
    private static final int QUICKDROP_DELAY = 500;
    /**
     * The X size of the inventory window in pixels.
     */
    protected int imageWidth = 176;
    /**
     * The Y size of the inventory window in pixels.
     */
    protected int imageHeight = 166;
    protected int titleLabelX;
    protected int titleLabelY;
    protected int inventoryLabelX;
    protected int inventoryLabelY;
    private final List<ItemSlotMouseAction> itemSlotMouseActions;
    /**
     * A list of the players inventory slots
     */
    protected final T menu;
    protected final Component playerInventoryTitle;
    /**
     * Holds the slot currently hovered
     */
    @Nullable
    protected Slot hoveredSlot;
    /**
     * Used when touchscreen is enabled
     */
    @Nullable
    private Slot clickedSlot;
    @Nullable
    private Slot quickdropSlot;
    @Nullable
    private Slot lastClickSlot;
    @Nullable
    private AbstractContainerScreen.SnapbackData snapbackData;
    /**
     * Starting X position for the Gui. Inconsistent use for Gui backgrounds.
     */
    protected int leftPos;
    /**
     * Starting Y position for the Gui. Inconsistent use for Gui backgrounds.
     */
    protected int topPos;
    /**
     * Used when touchscreen is enabled.
     */
    private boolean isSplittingStack;
    /**
     * Used when touchscreen is enabled
     */
    private ItemStack draggingItem = ItemStack.EMPTY;
    private long quickdropTime;
    protected final Set<Slot> quickCraftSlots = Sets.newHashSet();
    protected boolean isQuickCrafting;
    private int quickCraftingType;
    private int quickCraftingButton;
    private boolean skipNextRelease;
    private int quickCraftingRemainder;
    private boolean doubleclick;
    private ItemStack lastQuickMoved = ItemStack.EMPTY;

    public AbstractContainerScreen(T menu, Inventory playerInventory, Component title) {
        super(title);
        this.menu = menu;
        this.playerInventoryTitle = playerInventory.getDisplayName();
        this.skipNextRelease = true;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
        this.itemSlotMouseActions = new ArrayList<>();
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.itemSlotMouseActions.clear();
        this.addItemSlotMouseAction(new BundleMouseActions(this.minecraft));
    }

    protected void addItemSlotMouseAction(ItemSlotMouseAction itemSlotMouseAction) {
        this.itemSlotMouseActions.add(itemSlotMouseAction);
    }

    /**
     * Renders the graphical user interface (GUI) element.
     *
     * @param guiGraphics the GuiGraphics object used for rendering.
     * @param mouseX      the x-coordinate of the mouse cursor.
     * @param mouseY      the y-coordinate of the mouse cursor.
     * @param partialTick the partial tick time.
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderContents(guiGraphics, mouseX, mouseY, partialTick);
        this.renderCarriedItem(guiGraphics, mouseX, mouseY);
        this.renderSnapbackItem(guiGraphics);
    }

    public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int i = this.leftPos;
        int j = this.topPos;
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(i, j);
        this.renderLabels(guiGraphics, mouseX, mouseY);
        Slot slot = this.hoveredSlot;
        this.hoveredSlot = this.getHoveredSlot(mouseX, mouseY);
        this.renderSlotHighlightBack(guiGraphics);
        this.renderSlots(guiGraphics);
        this.renderSlotHighlightFront(guiGraphics);
        if (slot != null && slot != this.hoveredSlot) {
            this.onStopHovering(slot);
        }

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ContainerScreenEvent.Render.Foreground(this, guiGraphics, mouseX, mouseY));
        guiGraphics.pose().popMatrix();
    }

    public void renderCarriedItem(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack itemstack = this.draggingItem.isEmpty() ? this.menu.getCarried() : this.draggingItem;
        if (!itemstack.isEmpty()) {
            int i = 8;
            int j = this.draggingItem.isEmpty() ? 8 : 16;
            String s = null;
            if (!this.draggingItem.isEmpty() && this.isSplittingStack) {
                itemstack = itemstack.copyWithCount(Mth.ceil(itemstack.getCount() / 2.0F));
            } else if (this.isQuickCrafting && this.quickCraftSlots.size() > 1) {
                itemstack = itemstack.copyWithCount(this.quickCraftingRemainder);
                if (itemstack.isEmpty()) {
                    s = ChatFormatting.YELLOW + "0";
                }
            }

            guiGraphics.nextStratum();
            this.renderFloatingItem(guiGraphics, itemstack, mouseX - 8, mouseY - j, s);
        }
    }

    public void renderSnapbackItem(GuiGraphics guiGraphics) {
        if (this.snapbackData != null) {
            float f = Mth.clamp((float)(Util.getMillis() - this.snapbackData.time) / 100.0F, 0.0F, 1.0F);
            int i = this.snapbackData.end.x - this.snapbackData.start.x;
            int j = this.snapbackData.end.y - this.snapbackData.start.y;
            int k = this.snapbackData.start.x + (int)(i * f);
            int l = this.snapbackData.start.y + (int)(j * f);
            guiGraphics.nextStratum();
            this.renderFloatingItem(guiGraphics, this.snapbackData.item, k, l, null);
            if (f >= 1.0F) {
                this.snapbackData = null;
            }
        }
    }

    protected void renderSlots(GuiGraphics guiGraphics) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive()) {
                this.renderSlot(guiGraphics, slot);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            for (ItemSlotMouseAction itemslotmouseaction : this.itemSlotMouseActions) {
                if (itemslotmouseaction.matches(this.hoveredSlot)
                    && itemslotmouseaction.onMouseScrolled(scrollX, scrollY, this.hoveredSlot.index, this.hoveredSlot.getItem())) {
                    return true;
                }
            }
        }

        return false;
    }

    private void renderSlotHighlightBack(GuiGraphics guiGraphics) {
        if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
        }
    }

    private void renderSlotHighlightFront(GuiGraphics guiGraphics) {
        if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
        }
    }

    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack itemstack = this.hoveredSlot.getItem();
            if (this.menu.getCarried().isEmpty() || this.showTooltipWithItemInHand(itemstack)) {
                guiGraphics.setTooltipForNextFrame(
                    this.font,
                    this.getTooltipFromContainerItem(itemstack),
                    itemstack.getTooltipImage(),
                    itemstack,
                    x,
                    y,
                    itemstack.get(DataComponents.TOOLTIP_STYLE)
                );
            }
        }
    }

    private boolean showTooltipWithItemInHand(ItemStack stack) {
        return stack.getTooltipImage().map(ClientTooltipComponent::create).map(ClientTooltipComponent::showTooltipWithItemInHand).orElse(false);
    }

    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        return getTooltipFromItem(this.minecraft, stack);
    }

    private void renderFloatingItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y, @Nullable String text) {
        guiGraphics.renderItem(stack, x, y);
        var font = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(stack).getFont(stack, net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.FontContext.ITEM_COUNT);
        guiGraphics.renderItemDecorations(font != null ? font : this.font, stack, x, y - (this.draggingItem.isEmpty() ? 0 : 8), text);
    }

    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
    }

    protected abstract void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY);

    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        int i = slot.x;
        int j = slot.y;
        ItemStack itemstack = slot.getItem();
        boolean flag = false;
        boolean flag1 = slot == this.clickedSlot && !this.draggingItem.isEmpty() && !this.isSplittingStack;
        ItemStack itemstack1 = this.menu.getCarried();
        String s = null;
        if (slot == this.clickedSlot && !this.draggingItem.isEmpty() && this.isSplittingStack && !itemstack.isEmpty()) {
            itemstack = itemstack.copyWithCount(itemstack.getCount() / 2);
        } else if (this.isQuickCrafting && this.quickCraftSlots.contains(slot) && !itemstack1.isEmpty()) {
            if (this.quickCraftSlots.size() == 1) {
                return;
            }

            if (AbstractContainerMenu.canItemQuickReplace(slot, itemstack1, true) && this.menu.canDragTo(slot)) {
                flag = true;
                int k = Math.min(itemstack1.getMaxStackSize(), slot.getMaxStackSize(itemstack1));
                int l = slot.getItem().isEmpty() ? 0 : slot.getItem().getCount();
                int i1 = AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots, this.quickCraftingType, itemstack1) + l;
                if (i1 > k) {
                    i1 = k;
                    s = ChatFormatting.YELLOW.toString() + k;
                }

                itemstack = itemstack1.copyWithCount(i1);
            } else {
                this.quickCraftSlots.remove(slot);
                this.recalculateQuickCraftRemaining();
            }
        }

        if (itemstack.isEmpty() && slot.isActive()) {
            ResourceLocation resourcelocation = slot.getNoItemIcon();
            if (resourcelocation != null) {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation, i, j, 16, 16);
                flag1 = true;
            }
        }

        if (!flag1) {
            if (flag) {
                guiGraphics.fill(i, j, i + 16, j + 16, -2130706433);
            }

            renderSlotContents(guiGraphics, itemstack, slot, s);
        }
    }

    protected void renderSlotContents(GuiGraphics guiGraphics, ItemStack itemstack, Slot slot, @Nullable String countString) {
        {
            GuiGraphics p_281607_ = guiGraphics; Slot p_282613_ = slot; String s = countString; int i = slot.x; int j = slot.y;
            int j1 = p_282613_.x + p_282613_.y * this.imageWidth;
            if (p_282613_.isFake()) {
                p_281607_.renderFakeItem(itemstack, i, j, j1);
            } else {
                p_281607_.renderItem(itemstack, i, j, j1);
            }

            var font = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(itemstack).getFont(itemstack, net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.FontContext.ITEM_COUNT);
            p_281607_.renderItemDecorations(font != null ? font : this.font, itemstack, i, j, s);
        }
    }

    private void recalculateQuickCraftRemaining() {
        ItemStack itemstack = this.menu.getCarried();
        if (!itemstack.isEmpty() && this.isQuickCrafting) {
            if (this.quickCraftingType == 2) {
                this.quickCraftingRemainder = itemstack.getMaxStackSize();
            } else {
                this.quickCraftingRemainder = itemstack.getCount();

                for (Slot slot : this.quickCraftSlots) {
                    ItemStack itemstack1 = slot.getItem();
                    int i = itemstack1.isEmpty() ? 0 : itemstack1.getCount();
                    int j = Math.min(itemstack.getMaxStackSize(), slot.getMaxStackSize(itemstack));
                    int k = Math.min(AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots, this.quickCraftingType, itemstack) + i, j);
                    this.quickCraftingRemainder -= k - i;
                }
            }
        }
    }

    @Nullable
    private Slot getHoveredSlot(double mouseX, double mouseY) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive() && this.isHovering(slot, mouseX, mouseY)) {
                return slot;
            }
        }

        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (super.mouseClicked(event, isDoubleClick)) {
            return true;
        } else {
            var mouseKey = com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(event.button());
            boolean flag = this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey);
            Slot slot = this.getHoveredSlot(event.x(), event.y());
            this.doubleclick = this.lastClickSlot == slot && isDoubleClick;
            this.skipNextRelease = false;
            if (event.button() != 0 && event.button() != 1 && !flag) {
                this.checkHotbarMouseClicked(event);
            } else {
                int i = this.leftPos;
                int j = this.topPos;
                boolean flag1 = this.hasClickedOutside(event.x(), event.y(), i, j);
                if (slot != null) flag1 = false; // Forge, prevent dropping of items through slots outside of GUI boundaries
                int k = -1;
                if (slot != null) {
                    k = slot.index;
                }

                if (flag1) {
                    k = -999;
                }

                if (this.minecraft.options.touchscreen().get() && flag1 && this.menu.getCarried().isEmpty()) {
                    this.onClose();
                    return true;
                }

                if (k != -1) {
                    if (this.minecraft.options.touchscreen().get()) {
                        if (slot != null && slot.hasItem()) {
                            this.clickedSlot = slot;
                            this.draggingItem = ItemStack.EMPTY;
                            this.isSplittingStack = event.button() == 1;
                        } else {
                            this.clickedSlot = null;
                        }
                    } else if (!this.isQuickCrafting) {
                        if (this.menu.getCarried().isEmpty()) {
                            if (this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey)) {
                                this.slotClicked(slot, k, event.button(), ClickType.CLONE);
                            } else {
                                boolean flag2 = k != -999 && event.hasShiftDown();
                                ClickType clicktype = ClickType.PICKUP;
                                if (flag2) {
                                    this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                                    clicktype = ClickType.QUICK_MOVE;
                                } else if (k == -999) {
                                    clicktype = ClickType.THROW;
                                }

                                this.slotClicked(slot, k, event.button(), clicktype);
                            }

                            this.skipNextRelease = true;
                        } else {
                            this.isQuickCrafting = true;
                            this.quickCraftingButton = event.button();
                            this.quickCraftSlots.clear();
                            if (event.button() == 0) {
                                this.quickCraftingType = 0;
                            } else if (event.button() == 1) {
                                this.quickCraftingType = 1;
                            } else if (this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey)) {
                                this.quickCraftingType = 2;
                            }
                        }
                    }
                }
            }

            this.lastClickSlot = slot;
            return true;
        }
    }

    private void checkHotbarMouseClicked(MouseButtonEvent event) {
        if (this.hoveredSlot != null && this.menu.getCarried().isEmpty()) {
            if (this.minecraft.options.keySwapOffhand.matchesMouse(event)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ClickType.SWAP);
                return;
            }

            for (int i = 0; i < 9; i++) {
                if (this.minecraft.options.keyHotbarSlots[i].matchesMouse(event)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ClickType.SWAP);
                }
            }
        }
    }

    protected boolean hasClickedOutside(double x, double y, int left, int top) {
        return x < left || y < top || x >= left + this.imageWidth || y >= top + this.imageHeight;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        Slot slot = this.getHoveredSlot(event.x(), event.y());
        ItemStack itemstack = this.menu.getCarried();
        if (this.clickedSlot != null && this.minecraft.options.touchscreen().get()) {
            if (event.button() == 0 || event.button() == 1) {
                if (this.draggingItem.isEmpty()) {
                    if (slot != this.clickedSlot && !this.clickedSlot.getItem().isEmpty()) {
                        this.draggingItem = this.clickedSlot.getItem().copy();
                    }
                } else if (this.draggingItem.getCount() > 1 && slot != null && AbstractContainerMenu.canItemQuickReplace(slot, this.draggingItem, false)) {
                    long i = Util.getMillis();
                    if (this.quickdropSlot == slot) {
                        if (i - this.quickdropTime > 500L) {
                            this.slotClicked(this.clickedSlot, this.clickedSlot.index, 0, ClickType.PICKUP);
                            this.slotClicked(slot, slot.index, 1, ClickType.PICKUP);
                            this.slotClicked(this.clickedSlot, this.clickedSlot.index, 0, ClickType.PICKUP);
                            this.quickdropTime = i + 750L;
                            this.draggingItem.shrink(1);
                        }
                    } else {
                        this.quickdropSlot = slot;
                        this.quickdropTime = i;
                    }
                }
            }

            return true;
        } else if (this.isQuickCrafting
            && slot != null
            && !itemstack.isEmpty()
            && (itemstack.getCount() > this.quickCraftSlots.size() || this.quickCraftingType == 2)
            && AbstractContainerMenu.canItemQuickReplace(slot, itemstack, true)
            && slot.mayPlace(itemstack)
            && this.menu.canDragTo(slot)) {
            this.quickCraftSlots.add(slot);
            this.recalculateQuickCraftRemaining();
            return true;
        } else {
            return slot == null && this.menu.getCarried().isEmpty() ? super.mouseDragged(event, mouseX, mouseY) : true;
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        super.mouseReleased(event); //Forge: call parent to release buttons
        Slot slot = this.getHoveredSlot(event.x(), event.y());
        int i = this.leftPos;
        int j = this.topPos;
        boolean flag = this.hasClickedOutside(event.x(), event.y(), i, j);
        if (slot != null) flag = false; // Forge, prevent dropping of items through slots outside of GUI boundaries
        var mouseKey = com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(event.button());
        int k = -1;
        if (slot != null) {
            k = slot.index;
        }

        if (flag) {
            k = -999;
        }

        if (this.doubleclick && slot != null && event.button() == 0 && this.menu.canTakeItemForPickAll(ItemStack.EMPTY, slot)) {
            if (event.hasShiftDown()) {
                if (!this.lastQuickMoved.isEmpty()) {
                    for (Slot slot2 : this.menu.slots) {
                        if (slot2 != null
                            && slot2.mayPickup(this.minecraft.player)
                            && slot2.hasItem()
                            && slot2.isSameInventory(slot)
                            && AbstractContainerMenu.canItemQuickReplace(slot2, this.lastQuickMoved, true)) {
                            this.slotClicked(slot2, slot2.index, event.button(), ClickType.QUICK_MOVE);
                        }
                    }
                }
            } else {
                this.slotClicked(slot, k, event.button(), ClickType.PICKUP_ALL);
            }

            this.doubleclick = false;
        } else {
            if (this.isQuickCrafting && this.quickCraftingButton != event.button()) {
                this.isQuickCrafting = false;
                this.quickCraftSlots.clear();
                this.skipNextRelease = true;
                return true;
            }

            if (this.skipNextRelease) {
                this.skipNextRelease = false;
                return true;
            }

            if (this.clickedSlot != null && this.minecraft.options.touchscreen().get()) {
                if (event.button() == 0 || event.button() == 1) {
                    if (this.draggingItem.isEmpty() && slot != this.clickedSlot) {
                        this.draggingItem = this.clickedSlot.getItem();
                    }

                    boolean flag2 = AbstractContainerMenu.canItemQuickReplace(slot, this.draggingItem, false);
                    if (k != -1 && !this.draggingItem.isEmpty() && flag2) {
                        this.slotClicked(this.clickedSlot, this.clickedSlot.index, event.button(), ClickType.PICKUP);
                        this.slotClicked(slot, k, 0, ClickType.PICKUP);
                        if (this.menu.getCarried().isEmpty()) {
                            this.snapbackData = null;
                        } else {
                            this.slotClicked(this.clickedSlot, this.clickedSlot.index, event.button(), ClickType.PICKUP);
                            this.snapbackData = new AbstractContainerScreen.SnapbackData(
                                this.draggingItem,
                                new Vector2i((int)event.x(), (int)event.y()),
                                new Vector2i(this.clickedSlot.x + i, this.clickedSlot.y + j),
                                Util.getMillis()
                            );
                        }
                    } else if (!this.draggingItem.isEmpty()) {
                        this.snapbackData = new AbstractContainerScreen.SnapbackData(
                            this.draggingItem,
                            new Vector2i((int)event.x(), (int)event.y()),
                            new Vector2i(this.clickedSlot.x + i, this.clickedSlot.y + j),
                            Util.getMillis()
                        );
                    }

                    this.clearDraggingState();
                }
            } else if (this.isQuickCrafting && !this.quickCraftSlots.isEmpty()) {
                this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(0, this.quickCraftingType), ClickType.QUICK_CRAFT);

                for (Slot slot1 : this.quickCraftSlots) {
                    this.slotClicked(slot1, slot1.index, AbstractContainerMenu.getQuickcraftMask(1, this.quickCraftingType), ClickType.QUICK_CRAFT);
                }

                this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(2, this.quickCraftingType), ClickType.QUICK_CRAFT);
            } else if (!this.menu.getCarried().isEmpty()) {
                if (this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey)) {
                    this.slotClicked(slot, k, event.button(), ClickType.CLONE);
                } else {
                    boolean flag1 = k != -999 && event.hasShiftDown();
                    if (flag1) {
                        this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                    }

                    this.slotClicked(slot, k, event.button(), flag1 ? ClickType.QUICK_MOVE : ClickType.PICKUP);
                }
            }
        }

        this.isQuickCrafting = false;
        return true;
    }

    public void clearDraggingState() {
        this.draggingItem = ItemStack.EMPTY;
        this.clickedSlot = null;
    }

    private boolean isHovering(Slot slot, double mouseX, double mouseY) {
        return this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
    }

    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        mouseX -= i;
        mouseY -= j;
        return mouseX >= x - 1 && mouseX < x + width + 1 && mouseY >= y - 1 && mouseY < y + height + 1;
    }

    private void onStopHovering(Slot slot) {
        if (slot.hasItem()) {
            for (ItemSlotMouseAction itemslotmouseaction : this.itemSlotMouseActions) {
                if (itemslotmouseaction.matches(slot)) {
                    itemslotmouseaction.onStopHovering(slot);
                }
            }
        }
    }

    /**
     * Called when the mouse is clicked over a slot or outside the gui.
     */
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot != null) {
            slotId = slot.index;
        }

        this.onMouseClickAction(slot, type);
        this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, slotId, mouseButton, type, this.minecraft.player);
    }

    void onMouseClickAction(@Nullable Slot slot, ClickType type) {
        if (slot != null && slot.hasItem()) {
            for (ItemSlotMouseAction itemslotmouseaction : this.itemSlotMouseActions) {
                if (itemslotmouseaction.matches(slot)) {
                    itemslotmouseaction.onSlotClicked(slot, type);
                }
            }
        }
    }

    protected void handleSlotStateChanged(int slotId, int containerId, boolean newState) {
        this.minecraft.gameMode.handleSlotStateChanged(slotId, containerId, newState);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        var key = com.mojang.blaze3d.platform.InputConstants.getKey(event);
        if (super.keyPressed(event)) {
            return true;
        } else if (this.minecraft.options.keyInventory.isActiveAndMatches(key)) {
            this.onClose();
            return true;
        } else {
            boolean handled = this.checkHotbarKeyPressed(event); // Forge MC-146650: Needs to return true when the key is handled
            if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
                if (this.minecraft.options.keyPickItem.isActiveAndMatches(key)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ClickType.CLONE);
                    handled = true;
                } else if (this.minecraft.options.keyDrop.isActiveAndMatches(key)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, event.hasControlDown() ? 1 : 0, ClickType.THROW);
                    handled = true;
                }
            } else if (this.minecraft.options.keyDrop.isActiveAndMatches(key)) {
                handled = true; // Forge MC-146650: Emulate MC bug, so we don't drop from hotbar when pressing drop without hovering over a item.
            }

            return handled;
        }
    }

    protected boolean checkHotbarKeyPressed(KeyEvent event) {
        var key = com.mojang.blaze3d.platform.InputConstants.getKey(event);
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null) {
            if (this.minecraft.options.keySwapOffhand.isActiveAndMatches(key)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ClickType.SWAP);
                return true;
            }

            for (int i = 0; i < 9; i++) {
                if (this.minecraft.options.keyHotbarSlots[i].isActiveAndMatches(key)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ClickType.SWAP);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void removed() {
        if (this.minecraft.player != null) {
            this.menu.removed(this.minecraft.player);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public final void tick() {
        super.tick();
        if (this.minecraft.player.isAlive() && !this.minecraft.player.isRemoved()) {
            this.containerTick();
        } else {
            this.minecraft.player.closeContainer();
        }
    }

    protected void containerTick() {
    }

    @Override
    public T getMenu() {
        return this.menu;
    }

    @org.jetbrains.annotations.Nullable
    public Slot getSlotUnderMouse() { return this.hoveredSlot; }
    public int getGuiLeft() { return leftPos; }
    public int getGuiTop() { return topPos; }
    public int getXSize() { return imageWidth; }
    public int getYSize() { return imageHeight; }

    protected int slotColor = -2130706433;
    public int getSlotColor(int index) {
        return slotColor;
    }

    @Override
    public void onClose() {
        this.minecraft.player.closeContainer();
        if (this.hoveredSlot != null) {
            this.onStopHovering(this.hoveredSlot);
        }

        super.onClose();
    }

    @OnlyIn(Dist.CLIENT)
    record SnapbackData(ItemStack item, Vector2i start, Vector2i end, long time) {
    }
}
