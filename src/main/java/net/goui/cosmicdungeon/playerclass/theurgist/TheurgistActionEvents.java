package net.goui.cosmicdungeon.playerclass.theurgist;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid="cosmicdungeon")
public final class TheurgistActionEvents {
    private TheurgistActionEvents(){}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){if(event.getServer().getTickCount()%20==0)TheurgistActions.syncAll(event.getServer());}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        TheurgistActions.forget(p);TheurgistRevival.reconcile(p);
        // A target's fresh authoritative load can decide the outcome for a caster who rejoined first.
        for(var other:p.level().getServer().getPlayerList().getPlayers())if(other!=p&&TheurgistRevival.blocked(other))TheurgistRevival.reconcile(other);
        TheurgistActions.syncAll(p.level().getServer());
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)TheurgistActions.forget(p);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){
        if(event.getEntity() instanceof ServerPlayer p){TheurgistActions.invalidate(p);TheurgistActions.sync(p,true);}
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p)TheurgistActions.syncAll(p.level().getServer());}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void protect(LivingDeathEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&TheurgistRevival.blocked(p)){event.setCanceled(true);p.setHealth(Math.max(1,p.getHealth()));}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void died(LivingDeathEvent event){
        if(!event.isCanceled()&&event.getEntity() instanceof ServerPlayer p)TheurgistActions.invalidate(p);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void pickup(ItemEntityPickupEvent.Pre event){
        if(event.getPlayer() instanceof ServerPlayer p&&TheurgistRevival.blocked(p))event.setCanPickup(TriState.FALSE);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){TheurgistActions.stop();TheurgistRevival.stop();}
}
