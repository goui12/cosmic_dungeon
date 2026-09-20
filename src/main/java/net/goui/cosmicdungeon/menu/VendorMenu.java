package net.goui.cosmicdungeon.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class VendorMenu extends AbstractContainerMenu implements SessionMenu {
    private final java.util.UUID sessionId;
    private final Player owner;
    private final MenuBalanceRefresh refresh = new MenuBalanceRefresh();
    @Override public java.util.UUID sessionId() { return sessionId; }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (owner instanceof net.minecraft.server.level.ServerPlayer sp && sp.containerMenu == this
                && refresh.due(sp.level().getGameTime(), net.goui.cosmicdungeon.Config.MENU_BALANCE_POLL_TICKS.get())) {
            long balance = net.goui.cosmicdungeon.economy.CurrencyService.getBalanceTrace(sp);
            long capacity = net.goui.cosmicdungeon.economy.CurrencyService.getCapacity(sp);
            if (refresh.changed(balance, capacity))
                sp.connection.send(new net.goui.cosmicdungeon.network.VendorPayloads.S2C_VendorBalance(
                        containerId, sessionId, balance, capacity));
        }
    }
    private java.util.UUID vendor;
    private net.goui.cosmicdungeon.vendor.VendorSaleQuote<ItemStack> saleQuote;
    public void setSaleQuote(net.goui.cosmicdungeon.vendor.VendorSaleQuote<ItemStack> quote) { saleQuote = quote; }
    public net.goui.cosmicdungeon.vendor.VendorSaleQuote<ItemStack> takeSaleQuote() {
        var quote = saleQuote; saleQuote = null; return quote;
    }
    private String dimension,profile;
    public VendorMenu(int id,Inventory inv,net.minecraft.world.entity.Entity entity){
        this(id,inv);vendor=entity.getUUID();dimension=entity.level().dimension().location().toString();
        profile=String.valueOf(net.goui.cosmicdungeon.vendor.VendorAssignmentService.getProfileId(entity));
    }
    public boolean matches(net.minecraft.world.entity.Entity entity){
        return entity!=null&&entity.getUUID().equals(vendor)&&entity.level().dimension().location().toString().equals(dimension)
                &&String.valueOf(net.goui.cosmicdungeon.vendor.VendorAssignmentService.getProfileId(entity)).equals(profile);
    }
    public VendorMenu(int id, Inventory inv) { this(id, inv, java.util.UUID.randomUUID()); }
    public VendorMenu(int id, Inventory inv, java.util.UUID sessionId) {
        super(ModMenus.VENDOR.get(), id);
        this.owner = inv.player; this.sessionId = sessionId;
    }

    @Override
    public boolean stillValid(Player player) {
        if(!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer))return true;
        var entity=vendor==null?null:serverPlayer.level().getEntity(vendor);
        return matches(entity)&&entity.isAlive()&&player.distanceToSqr(entity)<=64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }
}
