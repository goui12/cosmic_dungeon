package net.goui.cosmicdungeon.economy;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Bounded delivery after verified account persistence. A reconnect replays still-unacknowledged notices. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class WealthReviewEvents {
    private static final class Session {
        final WealthReviewDelivery<ServerPlayer> delivery=new WealthReviewDelivery<>();
    }
    private static final Map<MinecraftServer,Session> SESSIONS=new WeakHashMap<>();
    private WealthReviewEvents(){}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        var server=event.getServer();if(server.getTickCount()%D1EconomyConfig.WEALTH_REVIEW_TICKS.get()!=0)return;
        var session=SESSIONS.computeIfAbsent(server,ignored->new Session());
        int budget=D1EconomyConfig.WEALTH_REVIEW_BUDGET.get();
        try{
            var data=PlayerCurrencyData.get(server);boolean observed=data.observeLegacyWealth(budget,System.currentTimeMillis());
            var developers=server.getPlayerList().getPlayers().stream().filter(AccessPolicy::isDeveloper).toList();
            session.delivery.deliver(developers,data.wealthReviews(),budget,observed,data::flushVerified,(developer,notice)->
                    developer.sendSystemMessage(Component.literal(WealthReviewCommands.describe(notice)
                            +". Acknowledge: /currency review "+notice.owner()+" ack "+notice.threshold()+" "+notice.revision())));

        }catch(RuntimeException error){
            com.mojang.logging.LogUtils.getLogger().warn("Wealth review processing held; account evidence retained",error);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            var session=SESSIONS.get(player.level().getServer());if(session!=null)session.delivery.forget(player);
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){SESSIONS.remove(event.getServer());}
}
