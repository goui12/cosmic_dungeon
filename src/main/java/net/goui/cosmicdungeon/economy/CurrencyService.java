package net.goui.cosmicdungeon.economy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.goui.cosmicdungeon.dungeon.FarrowsChopTravelService;

public final class CurrencyService {
    private CurrencyService() {}

    public static long getAvailableTrace(ServerPlayer player){
        var data=getData(player);return data==null?0:data.availableTrace(player.getUUID());
    }
    public static long getAvailableCapacity(ServerPlayer player){
        var data=getData(player);return data==null?0:data.availableCapacity(player.getUUID());
    }
    public static long getBalanceTrace(ServerPlayer player) {
        PlayerCurrencyData data = getData(player);
        return data == null ? 0L : data.getBalanceTrace(player.getUUID());
    }

    public static String getFormattedBalance(ServerPlayer player) {
        return CurrencyAmount.ofTrace(getBalanceTrace(player)).formatNormalized();
    }

    public static boolean tryDeposit(ServerPlayer player, long traceAmount) {
        if (!transactionsAllowed(player)) return false;
        PlayerCurrencyData data = getData(player);
        return data != null && traceAmount>=0 && data.change(player.getUUID(),player.getName().getString(),traceAmount,
                "credit","",0,java.util.UUID.randomUUID().toString(),false)>=0;
    }


    /** Quote IDs are single-use and menu-local; the shared crash journal remains a separate TODO. */
    public static boolean tryVendorSaleDeposit(ServerPlayer player, long amount, String quoteId, String vendorId) {
        if (!transactionsAllowed(player) || amount < 0) return false;
        PlayerCurrencyData data = getData(player);
        return data != null && data.change(player.getUUID(), player.getName().getString(), amount,
                "vendor_sale", vendorId, 0, "vendor_sale:" + quoteId, false) >= 0;
    }

    public static boolean tryWithdraw(ServerPlayer player, long traceAmount) {
        if (!transactionsAllowed(player)) return false;
        PlayerCurrencyData data = getData(player);
        return data != null && traceAmount>=0 && data.change(player.getUUID(),player.getName().getString(),-traceAmount,
                "debit","",0,java.util.UUID.randomUUID().toString(),false)>=0;
    }

    public static boolean canDeposit(ServerPlayer player, long traceAmount) {
        if (!transactionsAllowed(player)) return false;
        PlayerCurrencyData data = getData(player);
        return data != null && data.canDeposit(player.getUUID(), traceAmount);
    }

    public static long getCapacity(ServerPlayer player) {
        PlayerCurrencyData data = getData(player);
        return data == null ? PlayerCurrencyData.DEFAULT_CAPACITY_TRACE : data.getCapacityTrace(player.getUUID());
    }

    public static void setBalanceTrace(ServerPlayer player, long traceAmount) {
        PlayerCurrencyData data = getData(player);
        if (data != null) {
            long before=data.getBalanceTrace(player.getUUID());
            data.setBalanceTrace(player.getUUID(),traceAmount);
            CurrencyAudit.report(player.level().getServer(),player.getUUID(),player.getName().getString(),
                    java.util.UUID.randomUUID().toString(),"admin_or_rollback",traceAmount-before,before,
                    data.getBalanceTrace(player.getUUID()),0,"",0,"committed");
        }
    }

    public static void setCapacity(ServerPlayer player, long capacityTrace) {
        PlayerCurrencyData data = getData(player);
        if (data != null) data.setCapacityTrace(player.getUUID(), capacityTrace);
    }

    public static void clear(ServerPlayer player) {
        PlayerCurrencyData data = getData(player);
        if (data != null) data.clear(player.getUUID());
    }

    public static boolean transactionsAllowed(ServerPlayer player) {
        if(player==null||net.goui.cosmicdungeon.dungeon.ChopTravelRecovery.blocked(player)||DeathCurrencyService.blocked(player))return false;
        if(net.goui.cosmicdungeon.vendor.CommerceTransactions.blocked(player))return false;
        if(net.goui.cosmicdungeon.trade.TradeCustody.held(player)
                ||(net.goui.cosmicdungeon.trade.TradeSessionData.get(player)==null&&net.goui.cosmicdungeon.trade.TradeTransactions.blocked(player)))return false;
        if(net.goui.cosmicdungeon.playerclass.dragoon.repair.DragoonRepairSessionData.get(player)==null
                &&net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairTransactions.blocked(player))return false;
        return net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData.get(player.level().getServer())
                .findRunForPlayer(player.getUUID())
                .filter(run->run.stateEnum()==net.goui.cosmicdungeon.dungeon.DungeonRunState.RESETTING).isEmpty();
    }

    public static long reward(ServerPlayer player,long requested,String type,String related,long run,String transaction){
        if(!transactionsAllowed(player)||requested<0)return -1;
        var data=getData(player);if(data==null)return -1;
        boolean repeated=data.hasReceipt(transaction,player.getUUID());
        long credited=data.change(player.getUUID(),player.getName().getString(),requested,type,related,run,transaction,true);
        if(!repeated&&credited>=0&&credited<requested)
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Account cap: "+credited+" Trace credited; "
                    +(requested-credited)+" Trace rejected and recorded."));
        return credited;
    }

    private static PlayerCurrencyData getData(ServerPlayer player) {
        if (player == null) return null;
        MinecraftServer server = player.level().getServer();
        if (server == null) return null;
        return PlayerCurrencyData.get(server);
    }
}
