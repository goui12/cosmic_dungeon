package net.goui.cosmicdungeon.playerclass.skill;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid="cosmicdungeon")
public final class ClassSkillActionEvents {
    private ClassSkillActionEvents(){}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        if(e.getServer().getTickCount()%20==0)ClassSkillActions.syncAll(e.getServer());
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){
        if(e.getEntity() instanceof ServerPlayer p){ClassSkillActions.forget(p);ClassSkillActions.sync(p,true);}
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)ClassSkillActions.forget(p);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)ClassSkillActions.sync(p,true);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)ClassSkillActions.sync(p,true);}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){ClassSkillActions.stop();}
}
