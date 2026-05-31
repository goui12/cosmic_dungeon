package net.goui.cosmicdungeon.trade;

import net.goui.cosmicdungeon.menu.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class TradeMenu extends AbstractContainerMenu {
    public static final int OFFER_SLOTS = 9;

    public static final int OTHER_OFFER_START = 0;
    public static final int OTHER_OFFER_COUNT = OFFER_SLOTS;
    public static final int OWN_OFFER_START = OTHER_OFFER_START + OTHER_OFFER_COUNT;
    public static final int OWN_OFFER_COUNT = OFFER_SLOTS;
    public static final int PLAYER_INV_START = OWN_OFFER_START + OWN_OFFER_COUNT;
    public static final int PLAYER_INV_COUNT = 27;
    public static final int HOTBAR_START = PLAYER_INV_START + PLAYER_INV_COUNT;
    public static final int HOTBAR_COUNT = 9;

    private static final int OTHER_OFFER_X = 56;
    private static final int OTHER_OFFER_Y = 28;
    private static final int OWN_OFFER_X = 55;
    private static final int OWN_OFFER_Y = 89;
    private static final int PLAYER_INV_X = 55;
    private static final int PLAYER_INV_Y = 126;
    private static final int HOTBAR_X = 55;
    private static final int HOTBAR_Y = 184;
    private static final int SLOT_SPACING = 18;

    private final TradeSessionData.TradeSession session;
    private final Player self;
    private final SimpleContainer ownFallback;
    private final SimpleContainer otherFallback;
    private final Container ownContainer;

    public TradeMenu(int id, Inventory inv, TradeSessionData.TradeSession session) {
        super(ModMenus.TRADE.get(), id);
        this.session = session;
        this.self = inv.player;
        this.ownFallback = new SimpleContainer(OFFER_SLOTS);
        this.otherFallback = new SimpleContainer(OFFER_SLOTS);

        this.ownContainer = session != null ? session.getContainerFor(inv.player, true) : ownFallback;
        Container otherContainer = session != null ? session.getContainerFor(inv.player, false) : otherFallback;

        for (int i = 0; i < OTHER_OFFER_COUNT; i++) {
            addSlot(new ReadOnlyTradeSlot(otherContainer, i, OTHER_OFFER_X + i * SLOT_SPACING, OTHER_OFFER_Y));
        }

        for (int i = 0; i < OWN_OFFER_COUNT; i++) {
            addSlot(new OwnOfferSlot(ownContainer, i, OWN_OFFER_X + i * SLOT_SPACING, OWN_OFFER_Y));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, PLAYER_INV_X + col * SLOT_SPACING, PLAYER_INV_Y + row * SLOT_SPACING));
            }
        }

        for (int col = 0; col < HOTBAR_COUNT; col++) {
            addSlot(new Slot(inv, col, HOTBAR_X + col * SLOT_SPACING, HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(Player p) {
        return session != null && session.isValidFor(p);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        if (idx < 0 || idx >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        if (isOtherOfferSlot(idx)) {
            return ItemStack.EMPTY;
        }

        Slot slot = this.slots.get(idx);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack inSlot = slot.getItem();
        ItemStack original = inSlot.copy();

        if (isOwnOfferSlot(idx)) {
            if (!moveItemStackTo(inSlot, PLAYER_INV_START, HOTBAR_START + HOTBAR_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else if (isPlayerInventorySlot(idx) || isHotbarSlot(idx)) {
            if (!canEditOwnOffer(p)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(inSlot, OWN_OFFER_START, OWN_OFFER_START + OWN_OFFER_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (inSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, original);
        } else {
            slot.setChanged();
        }
        if (inSlot.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(p, inSlot);
        return original;
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        if (isOtherOfferSlot(slotId)) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide() && session != null && player instanceof ServerPlayer serverPlayer && session.canCancelFromMenuClose(serverPlayer)) {
            session.cancelFromMenuClose(serverPlayer);
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == ownContainer) {
            notifyOwnOfferChanged();
        }
    }

    private boolean canEditOwnOffer(Player player) {
        return session == null || session.canEditOffer(player);
    }

    private void notifyOwnOfferChanged() {
        if (session != null) {
            session.onOfferChanged(self);
        }
    }

    private static boolean isOtherOfferSlot(int idx) {
        return idx >= OTHER_OFFER_START && idx < OTHER_OFFER_START + OTHER_OFFER_COUNT;
    }

    private static boolean isOwnOfferSlot(int idx) {
        return idx >= OWN_OFFER_START && idx < OWN_OFFER_START + OWN_OFFER_COUNT;
    }

    private static boolean isPlayerInventorySlot(int idx) {
        return idx >= PLAYER_INV_START && idx < PLAYER_INV_START + PLAYER_INV_COUNT;
    }

    private static boolean isHotbarSlot(int idx) {
        return idx >= HOTBAR_START && idx < HOTBAR_START + HOTBAR_COUNT;
    }

    private final class OwnOfferSlot extends Slot {
        private OwnOfferSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return canEditOwnOffer(self);
        }

        @Override
        public boolean mayPickup(Player player) {
            return canEditOwnOffer(player);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            notifyOwnOfferChanged();
        }
    }

    private static final class ReadOnlyTradeSlot extends Slot {
        private ReadOnlyTradeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public ItemStack remove(int amount) {
            return ItemStack.EMPTY;
        }
    }
}
