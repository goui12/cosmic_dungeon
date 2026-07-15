package net.goui.cosmicdungeon.playerclass.dragoon.repair;

import net.goui.cosmicdungeon.economy.CurrencyService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class DragoonRepairTransactionService {
    private DragoonRepairTransactionService() {}
    public enum Result { SUCCESS, INVALID_ITEM, NO_MATERIAL, INSUFFICIENT_BALANCE, CANNOT_RECEIVE_CURRENCY, CURRENCY_FAILED }
    public static Result finalizeRepair(ServerPlayer dragoon, ServerPlayer target, SimpleContainer container, int units, long fee) {
        ItemStack stack = container.getItem(0); var materialOpt = DragoonRepairRules.materialFor(stack); if (materialOpt.isEmpty()) return Result.INVALID_ITEM;
        int validUnits = DragoonRepairRules.clampUnits(stack, units); if (validUnits != units) return Result.INVALID_ITEM; Item material = materialOpt.get();
        if (count(dragoon, material) < units) return Result.NO_MATERIAL; long tb = CurrencyService.getBalanceTrace(target); long db = CurrencyService.getBalanceTrace(dragoon);
        if (fee < 0 || tb < fee) return Result.INSUFFICIENT_BALANCE; if (fee > CurrencyService.getCapacity(dragoon) - db) return Result.CANNOT_RECEIVE_CURRENCY;
        if (fee > 0 && !CurrencyService.tryWithdraw(target, fee)) return Result.CURRENCY_FAILED;
        if (fee > 0 && !CurrencyService.tryDeposit(dragoon, fee)) { CurrencyService.setBalanceTrace(target, tb); CurrencyService.setBalanceTrace(dragoon, db); return Result.CURRENCY_FAILED; }
        if (!consume(dragoon, material, units)) { CurrencyService.setBalanceTrace(target, tb); CurrencyService.setBalanceTrace(dragoon, db); return Result.NO_MATERIAL; }
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - DragoonRepairRules.projectedRepair(stack, units))); container.setChanged(); return Result.SUCCESS;
    }
    public static int count(ServerPlayer p, Item item) { int n=0; for (int i=0; i<p.getInventory().getContainerSize(); i++) { ItemStack s = p.getInventory().getItem(i); if (s.is(item)) n += s.getCount(); } return n; }
    private static boolean consume(ServerPlayer p, Item item, int count) { if (count(p,item) < count) return false; int left=count; for (int i=0; i<p.getInventory().getContainerSize() && left>0; i++) { ItemStack s = p.getInventory().getItem(i); if (!s.is(item)) continue; int take=Math.min(left, s.getCount()); s.shrink(take); left-=take; } p.getInventory().setChanged(); return left==0; }
}
