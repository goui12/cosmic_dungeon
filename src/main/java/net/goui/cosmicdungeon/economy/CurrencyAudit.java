package net.goui.cosmicdungeon.economy;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import java.util.*;
/** Console export only; the account journal captures durable reviews before any balance changes. */
public final class CurrencyAudit {
    private CurrencyAudit(){}
    public static void report(MinecraftServer server,UUID player,String name,String tx,String type,long requested,
                              long before,long after,long rejected,String related,long run,String status){
        var row=new JsonObject();
        row.addProperty("transaction",tx);row.addProperty("player",player.toString());row.addProperty("name",name);
        row.addProperty("category",EconomyLedger.category(type,after-before));
        row.addProperty("type",type);row.addProperty("requested_trace",requested);row.addProperty("balance_before",before);
        row.addProperty("balance_after",after);row.addProperty("cap_rejected_trace",rejected);
        row.addProperty("related",related);row.addProperty("run",run);row.addProperty("status",status);
        row.addProperty("timestamp",java.time.Instant.now().toString());
        LogUtils.getLogger().info("COSMIC_ECONOMY {}",row);
        if(server==null||after<=before)return;
        for(long threshold:new long[]{D1EconomyConfig.WEALTH_EARLY.get(),D1EconomyConfig.WEALTH_HIGH.get(),D1EconomyConfig.WEALTH_MAX.get()}){
            if(before>=threshold||after<threshold)continue;
            LogUtils.getLogger().warn("COSMIC_WEALTH_CROSSING threshold={} transaction={}",threshold,row);

        }
    }
    // Durable notices and operator dispositions: WealthReviewState / WealthReviewCommands.
    // Every committed crossing remains in the existing account ledger even after acknowledgment.
}
