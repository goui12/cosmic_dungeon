package net.goui.cosmicdungeon.trade;

import net.goui.cosmicdungeon.menu.ModMenus;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class TradeMenu extends AbstractContainerMenu {
    public static final int OFFER_SLOTS = 6;
    private final TradeSessionData.TradeSession session;
    private final Player self;
    private final SimpleContainer ownFallback;
    private final SimpleContainer otherFallback;

    public TradeMenu(int id, Inventory inv, TradeSessionData.TradeSession session) {
        super(ModMenus.TRADE.get(), id);
        this.session = session;
        this.self = inv.player;
        this.ownFallback = new SimpleContainer(OFFER_SLOTS);
        this.otherFallback = new SimpleContainer(OFFER_SLOTS);

        var ownContainer = session != null ? session.getContainerFor(inv.player, true) : ownFallback;
        var otherContainer = session != null ? session.getContainerFor(inv.player, false) : otherFallback;

        int y = 20;
        for (int i = 0; i < OFFER_SLOTS; i++) {
            addSlot(new Slot(ownContainer, i, 17 + i * 18, y));
        }
        for (int i = 0; i < OFFER_SLOTS; i++) {
            addSlot(new Slot(otherContainer, i, 17 + i * 18, y + 24) {
                @Override
                public boolean mayPlace(ItemStack s) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player p) {
                    return false;
                }

                @Override
                public ItemStack remove(int amount) {
                    return ItemStack.EMPTY;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player p) {
        return session == null || session.contains(p);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        if (idx >= OFFER_SLOTS && idx < OFFER_SLOTS * 2) {
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        if (slotId >= OFFER_SLOTS && slotId < OFFER_SLOTS * 2) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (session != null && player instanceof ServerPlayer && session.contains(player)) {
            session.cancel("Menu closed");
        }
    }

    @Override
    public void slotsChanged(net.minecraft.world.Container c) {
        super.slotsChanged(c);
        if (session != null) {
            session.onOfferChanged(self);
        }
    }
}
