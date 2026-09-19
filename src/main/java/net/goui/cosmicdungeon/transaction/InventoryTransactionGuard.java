package net.goui.cosmicdungeon.transaction;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairTransactions;
import net.goui.cosmicdungeon.trade.TradeCustody;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import java.util.UUID;
/** Shared gate for inventory replacement; recovery precedes dungeon/Chop escrow transitions. */
public final class InventoryTransactionGuard {
    private InventoryTransactionGuard(){}
    public static boolean blocked(ServerPlayer p){return net.goui.cosmicdungeon.dungeon.DungeonInventoryHandoffs.blocked(p)||net.goui.cosmicdungeon.dungeon.ChopTravelRecovery.blocked(p)||net.goui.cosmicdungeon.economy.DeathCurrencyService.blocked(p)||net.goui.cosmicdungeon.vendor.CommerceTransactions.blocked(p)||RepairTransactions.blocked(p)||net.goui.cosmicdungeon.trade.TradeTransactions.blocked(p);}
    public static boolean beforeInventoryChange(ServerPlayer p){return !net.goui.cosmicdungeon.dungeon.DungeonInventoryHandoffs.blocked(p)&&otherTransactionsReady(p);}
    public static boolean otherTransactionsReady(ServerPlayer p){return !net.goui.cosmicdungeon.dungeon.ChopTravelRecovery.blocked(p)&&!net.goui.cosmicdungeon.economy.DeathCurrencyService.blocked(p)&&net.goui.cosmicdungeon.vendor.CommerceTransactions.beforeInventoryChange(p)&&RepairTransactions.beforeInventoryChange(p)&&TradeCustody.beforeInventoryChange(p);}
    public static boolean readyForCleanup(MinecraftServer server,List<UUID> owners){
        if(!net.goui.cosmicdungeon.dungeon.ChopTravelRecovery.readyForCleanup(server,owners))return false;
        if(owners.stream().anyMatch(owner->net.goui.cosmicdungeon.economy.PlayerCurrencyData.get(server).pendingDeath(owner)))return false;
        if(!RepairTransactions.readyForCleanup(server,owners))return false;
        for(UUID owner:owners){var p=server.getPlayerList().getPlayer(owner);
            if(p!=null&&net.goui.cosmicdungeon.vendor.CommerceTransactions.blocked(p)&&!net.goui.cosmicdungeon.vendor.CommerceTransactions.beforeInventoryChange(p))return false;
            if(net.goui.cosmicdungeon.economy.PlayerCurrencyData.get(server).pendingOperation(owner).isPresent())return false;
            if(p!=null&&net.goui.cosmicdungeon.trade.TradeTransactions.blocked(p)&&!TradeCustody.beforeInventoryChange(p))return false;
            if(net.goui.cosmicdungeon.economy.PlayerCurrencyData.get(server).pendingTrade(owner).isPresent())return false;}
        return net.goui.cosmicdungeon.dungeon.DungeonInventoryHandoffs.readyForCleanup(server,owners);
    }
}
