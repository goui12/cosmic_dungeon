package net.goui.cosmicdungeon.playerclass.dragoon.repair;

import net.goui.cosmicdungeon.menu.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DragoonRepairMenu extends AbstractContainerMenu implements net.goui.cosmicdungeon.menu.SessionMenu {
    public static final int REPAIR_SLOT = 0, PLAYER_INV_START = 1, PLAYER_INV_COUNT = 27, HOTBAR_START = 28, HOTBAR_COUNT = 9;
    private final DragoonRepairSessionData.RepairSession session; private final Player self; private final Container repairContainer;
    private final java.util.UUID sessionId;
    private final net.goui.cosmicdungeon.menu.MenuBalanceRefresh balanceRefresh = new net.goui.cosmicdungeon.menu.MenuBalanceRefresh();
    @Override public java.util.UUID sessionId() { return sessionId; }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (session != null && self instanceof ServerPlayer sp && sp.containerMenu == this
                && balanceRefresh.due(sp.level().getGameTime(), net.goui.cosmicdungeon.Config.MENU_BALANCE_POLL_TICKS.get()))
            session.refreshBalances(sp, balanceRefresh);
    }
    public DragoonRepairMenu(int id, Inventory inv, DragoonRepairSessionData.RepairSession session) {
        this(id, inv, session, session == null ? new java.util.UUID(0, 0) : session.id());
    }
    public DragoonRepairMenu(int id, Inventory inv, DragoonRepairSessionData.RepairSession session, java.util.UUID sessionId) {
        super(ModMenus.DRAGOON_REPAIR.get(), id); this.sessionId = sessionId; this.session = session; this.self = inv.player; this.repairContainer = session == null ? new SimpleContainer(1) : session.repairContainer();
        addSlot(new RepairSlot(repairContainer, 0, 54, 48));
        for (int row=0; row<3; row++) for (int col=0; col<9; col++) addSlot(new Slot(inv, col + row*9 + 9, 47 + col*18, 174 + row*18));
        for (int col=0; col<9; col++) addSlot(new Slot(inv, col, 47 + col*18, 232));
    }
    public boolean belongsTo(DragoonRepairSessionData.RepairSession expected){return session==expected;}
    @Override public boolean stillValid(Player p) { return session != null && session.isValidFor(p); }
    @Override public ItemStack quickMoveStack(Player p, int idx) {
        if (session!=null && session.locked()) return ItemStack.EMPTY;
        if (idx < 0 || idx >= slots.size()) return ItemStack.EMPTY; Slot slot=slots.get(idx); if (!slot.hasItem()) return ItemStack.EMPTY; ItemStack in=slot.getItem(); ItemStack original=in.copy();
        if (idx == REPAIR_SLOT) { if (!mayEditRepair(p) || !moveItemStackTo(in, PLAYER_INV_START, HOTBAR_START+HOTBAR_COUNT, true)) return ItemStack.EMPTY; }
        else if (mayEditRepair(p) && DragoonRepairRules.isValidRepairItemShape(in) && !moveItemStackTo(in, REPAIR_SLOT, REPAIR_SLOT+1, false)) return ItemStack.EMPTY; else if (!mayEditRepair(p)) return ItemStack.EMPTY;
        if (in.isEmpty()) slot.setByPlayer(ItemStack.EMPTY, original); else slot.setChanged(); if (in.getCount()==original.getCount()) return ItemStack.EMPTY; slot.onTake(p,in); return original;
    }
    @Override public void clicked(int slotId, int button, ClickType clickType, Player player) { if (session!=null && session.locked()) return; if (slotId == REPAIR_SLOT && !mayEditRepair(player)) return; super.clicked(slotId, button, clickType, player); if(player instanceof ServerPlayer sp)RepairCustody.capture(sp); }
    @Override public void removed(Player player) { if (!player.level().isClientSide() && session != null && player instanceof ServerPlayer sp && session.canCancelFromMenuClose(sp)) session.cancelFromMenuClose(sp); super.removed(player); }
    @Override public void slotsChanged(Container c) { super.slotsChanged(c); if (c == repairContainer && session != null) session.onRepairItemChanged(self); }
    private boolean mayEditRepair(Player p) { return session != null && session.canEditRepairSlot(p); }
    private class RepairSlot extends Slot { RepairSlot(Container c,int slot,int x,int y){super(c,slot,x,y);} @Override public boolean mayPlace(ItemStack s){ return mayEditRepair(self) && DragoonRepairRules.isValidRepairItemShape(s); } @Override public boolean mayPickup(Player p){ return mayEditRepair(p); } @Override public int getMaxStackSize(){return 1;} @Override public void setChanged(){super.setChanged(); if (session != null) session.onRepairItemChanged(self);} }
}
