package net.goui.cosmicdungeon.playerclass.dragoon.repair;

import net.goui.cosmicdungeon.economy.CurrencyService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class DragoonRepairTransactionService {
    private DragoonRepairTransactionService() {}
    public enum Result { SUCCESS, INVALID_ITEM, NO_MATERIAL, INSUFFICIENT_BALANCE, CANNOT_RECEIVE_CURRENCY, CURRENCY_FAILED }
    public static Result finalizeRepair(ServerPlayer dragoon, ServerPlayer target, SimpleContainer container, int units, long fee) {
        ItemStack stack = container.getItem(0); var materialOpt = DragoonRepairRules.materialFor(stack); if (materialOpt.isEmpty()) return Result.INVALID_ITEM;
        int validUnits = DragoonRepairRules.clampUnits(stack, units); if (validUnits != units) return Result.INVALID_ITEM; Item material = materialOpt.get();
        if (count(dragoon, material) < units) return Result.NO_MATERIAL; long tb = CurrencyService.getBalanceTrace(target); long db = CurrencyService.getBalanceTrace(dragoon);
        if (fee < 0 || tb < fee) return Result.INSUFFICIENT_BALANCE; if (fee > CurrencyService.getCapacity(dragoon) - db) return Result.CANNOT_RECEIVE_CURRENCY;
        List<ItemSnapshot> materialSnapshot = snapshotMatching(dragoon, material);
        if (fee > 0 && !CurrencyService.tryWithdraw(target, fee)) return Result.CURRENCY_FAILED;
        if (fee > 0 && !CurrencyService.tryDeposit(dragoon, fee)) { rollbackCurrency(target, dragoon, tb, db); return Result.CURRENCY_FAILED; }
        if (!consume(dragoon, material, units)) { rollbackCurrency(target, dragoon, tb, db); restoreSnapshot(materialSnapshot); return Result.NO_MATERIAL; }
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - DragoonRepairRules.projectedRepair(stack, units))); container.setChanged(); return Result.SUCCESS;
    }
    public static int count(ServerPlayer p, Item item) { int n=0; for (int i=0; i<p.getInventory().getContainerSize(); i++) { ItemStack s = p.getInventory().getItem(i); if (s.is(item)) n += s.getCount(); } return n; }
    private static boolean consume(ServerPlayer p, Item item, int count) { if (count(p,item) < count) return false; int left=count; for (int i=0; i<p.getInventory().getContainerSize() && left>0; i++) { ItemStack s = p.getInventory().getItem(i); if (!s.is(item)) continue; int take=Math.min(left, s.getCount()); s.shrink(take); left-=take; } p.getInventory().setChanged(); return left==0; }
    private static void rollbackCurrency(ServerPlayer target, ServerPlayer dragoon, long targetBalance, long dragoonBalance) { CurrencyService.setBalanceTrace(target, targetBalance); CurrencyService.setBalanceTrace(dragoon, dragoonBalance); }
    private static List<ItemSnapshot> snapshotMatching(ServerPlayer p, Item item) { List<ItemSnapshot> out = new ArrayList<>(); for (int i=0; i<p.getInventory().getContainerSize(); i++) { ItemStack s=p.getInventory().getItem(i); if (s.is(item)) out.add(new ItemSnapshot(p, i, s.copy())); } return out; }
    private static void restoreSnapshot(List<ItemSnapshot> snapshots) { for (ItemSnapshot snapshot : snapshots) snapshot.player.getInventory().setItem(snapshot.slot, snapshot.stack.copy()); if (!snapshots.isEmpty()) snapshots.get(0).player.getInventory().setChanged(); }
    private record ItemSnapshot(ServerPlayer player, int slot, ItemStack stack) {}
}
