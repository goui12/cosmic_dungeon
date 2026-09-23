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
    /** Completion commits the decision only; owner-local images are reconciled after both menus close. */
    public static Result finalizeRepair(ServerPlayer dragoon,ServerPlayer target,SimpleContainer container,
                                        int units,java.util.UUID transaction,
                                        net.goui.cosmicdungeon.economy.AccountTransfer.Terms terms){
        if(dragoon==null||target==null||transaction==null||terms==null)return Result.CURRENCY_FAILED;
        try{
            var data=net.goui.cosmicdungeon.economy.PlayerCurrencyData.get(target.level().getServer());
            var transfer=data.transfer(transaction).orElse(null);var plan=data.repairPlan(transaction).orElse(null);
            if(transfer==null||plan==null||!transfer.terms().equals(terms)||units!=plan.units()
                    ||!plan.customer().equals(target.getUUID())||!plan.provider().equals(dragoon.getUUID()))return Result.CURRENCY_FAILED;
            if(transfer.status().equals(net.goui.cosmicdungeon.economy.AccountTransfer.COMMITTED))
                return data.flushVerified()?Result.SUCCESS:Result.CURRENCY_FAILED;
            if(!CurrencyService.transactionsAllowed(target)||!CurrencyService.transactionsAllowed(dragoon)
                    ||!data.reservationValid(transaction,terms))return Result.CURRENCY_FAILED;
            var stack=container.getItem(0);var material=DragoonRepairRules.materialFor(stack);
            if(material.isEmpty()||DragoonRepairRules.clampUnits(stack,units)!=units)return Result.INVALID_ITEM;
            if(!RepairCustody.componentsValid(dragoon,transaction,material.get(),DragoonRepairRules.componentCount(stack,units)))return Result.NO_MATERIAL;
            return RepairTransactions.decide(target,dragoon,transaction)?Result.SUCCESS:Result.CURRENCY_FAILED;
        }catch(RuntimeException failure){
            com.mojang.logging.LogUtils.getLogger().error("Repair decision needs reconciliation: {}",transaction,failure);
            return Result.CURRENCY_FAILED;
        }
    }
    // Repair 2.0: account decision is saved before any owner applies an item result. Each owner
    // saves inventory/recovery plus a receipt, then acknowledges. An uncertain write never refunds
    // a committed payment or substitutes a new item for missing/corrupt participant evidence.
    public static int count(ServerPlayer p, Item item) { int n=0; for (int i=0; i<p.getInventory().getContainerSize(); i++) { ItemStack s = p.getInventory().getItem(i); if (RepairComponents.validFor(s,item)) n += s.getCount(); } return n; }
}
