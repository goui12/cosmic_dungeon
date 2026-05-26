package net.goui.cosmicdungeon.trade;
import net.goui.cosmicdungeon.menu.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TradeMenu extends AbstractContainerMenu {
    public static final int OFFER_SLOTS = 6;
    private final TradeSessionData.TradeSession session;
    private final Player self;
    public TradeMenu(int id, Inventory inv, TradeSessionData.TradeSession session) {
        super(ModMenus.TRADE.get(), id);
        this.session = session; this.self = inv.player;
        int y=20;
        for(int i=0;i<OFFER_SLOTS;i++) addSlot(new Slot(session.getContainerFor(inv.player, true), i, 17+i*18, y));
        for(int i=0;i<OFFER_SLOTS;i++) addSlot(new Slot(session.getContainerFor(inv.player, false), i, 17+i*18, y+24){@Override public boolean mayPlace(ItemStack s){return false;}});
        int idx=12;
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(inv, col+row*9+9, 8+col*18, 84+row*18));
        for(int col=0;col<9;col++) addSlot(new Slot(inv,col,8+col*18,142));
    }
    @Override public boolean stillValid(Player p){ return session != null && session.contains(p); }
    @Override public ItemStack quickMoveStack(Player p, int idx){ return ItemStack.EMPTY; }
    @Override public void slotsChanged(net.minecraft.world.Container c){ super.slotsChanged(c); session.onOfferChanged(self); }
}
