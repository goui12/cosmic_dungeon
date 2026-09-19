package net.goui.cosmicdungeon.dungeon.d1;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.dungeon.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Instance timestamps survive restart; offline-server wall time does not accelerate removal. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class D1MembershipService {
    private D1MembershipService(){}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        DungeonLifecycleService.findActiveRunForPlayer(player).filter(r->r.dungeonId().equals("dungeon_1")).ifPresent(run->{
            var server=player.level().getServer();var data=D1RunData.get(server);long now=server.overworld().getGameTime();
            data.setValue(run.runId(),"offline:"+player.getUUID(),Long.toString(now));
            data.setValue(run.runId(),"flag_grace_until",Long.toString(now+Config.FLAG_DISCONNECT_GRACE_SECONDS.get()*20L));
        });
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        var server=event.getServer();long now=server.overworld().getGameTime();
        if(now%(Config.ACTIVITY_POLL_SECONDS.get()*20L)!=0)return;
        var data=D1RunData.get(server);
        for(var run:DungeonRunRegistryData.get(server).listAllRuns()){
            if(!run.dungeonId().equals("dungeon_1")||run.stateEnum()!=DungeonRunState.ACTIVE)continue;
            for(UUID id:run.orderedPlayers()){
                String key="offline:"+id;
                if(server.getPlayerList().getPlayer(id)!=null){data.setValue(run.runId(),key,"");continue;}
                var saved=data.values(run.runId(),key);
                long since=now;
                if(!saved.isEmpty())try{since=Long.parseLong(saved.getFirst());}catch(NumberFormatException ignored){}
                if(saved.isEmpty()||since>now){data.setValue(run.runId(),key,Long.toString(now));continue;}
                if(now-since>Config.LINK_DEAD_SECONDS.get()*20L)
                    DungeonLifecycleService.removeLinkDeadD1Member(server,run.runId(),id);
            }
        }
    }
    public static boolean flagsCanComplete(net.minecraft.server.MinecraftServer server,long runId){
        var saved=D1RunData.get(server).values(runId,"flag_grace_until");
        if(saved.isEmpty())return true;
        try{return server.overworld().getGameTime()>=Long.parseLong(saved.getFirst());}
        catch(NumberFormatException invalid){return false;}
    }
}
