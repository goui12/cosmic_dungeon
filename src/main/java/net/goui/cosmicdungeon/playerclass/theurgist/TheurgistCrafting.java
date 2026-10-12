package net.goui.cosmicdungeon.playerclass.theurgist;
import net.minecraft.server.level.ServerPlayer;
/** Source compatibility only. Random potion skills and Brewing Supplies spending are retired. */
public final class TheurgistCrafting {
    private TheurgistCrafting() {}
    public static int freeSlot(ServerPlayer p) {
        for (int slot = 0; slot < 36; slot++) if (p.getInventory().getItem(slot).isEmpty()) return slot;
        return -1;
    }
    public static boolean craft(ServerPlayer p, long run, boolean epic) { return false; }
}
