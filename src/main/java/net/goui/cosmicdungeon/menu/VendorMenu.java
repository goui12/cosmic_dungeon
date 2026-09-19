package net.goui.cosmicdungeon.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class VendorMenu extends AbstractContainerMenu {
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
    public VendorMenu(int id, Inventory inv) {
        super(ModMenus.VENDOR.get(), id);
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
