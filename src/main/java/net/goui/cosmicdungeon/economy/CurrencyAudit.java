package net.goui.cosmicdungeon.economy;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
/** Console export and developer notifications; authoritative evidence is in the account outbox/ledger. */
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
        var data=PlayerCurrencyData.get(server);
        for(long threshold:new long[]{D1EconomyConfig.WEALTH_EARLY.get(),D1EconomyConfig.WEALTH_HIGH.get(),D1EconomyConfig.WEALTH_MAX.get()}){
            if(before>=threshold||after<threshold)continue;
            LogUtils.getLogger().warn("COSMIC_WEALTH_CROSSING threshold={} transaction={}",threshold,row);
            if(data.markThreshold(player,threshold)){
                String notice="Economy review: "+name+" ("+player+") reached "+after+" Trace; "+type+" "+(after-before)
                        +" Trace, previous "+before+", transaction "+tx+", "+java.time.Instant.now();
                for(ServerPlayer developer:server.getPlayerList().getPlayers())
                    if(AccessPolicy.isDeveloper(developer))developer.sendSystemMessage(Component.literal(notice));
            }
        }
    }
    // TODO(M02/M03, licensed TEST): verify threshold notifications and persistent final-review
    // entries against the native ledger on reconnect, with developers offline and at cap.
    // Final review is diagnostic, never a debit freeze. /currency report exposes daily totals;
    // /currency review <UUID> reads preserved cap-crossing evidence. Reports include active logical death drops; only destruction is a sink.
}
