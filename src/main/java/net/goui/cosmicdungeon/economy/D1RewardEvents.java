package net.goui.cosmicdungeon.economy;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.dungeon.d1.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class D1RewardEvents {
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        D1Members.run(player.level()).filter(run->run.containsPlayer(player.getUUID())).ifPresent(run->{
            var p=player.position();D1RunData.get(player.level().getServer()).setValue(run.runId(),"death_position:"+player.getUUID(),p.x+","+p.y+","+p.z);
        });
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var registry=net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData.get(player.level().getServer());
        registry.findRunForPlayer(player.getUUID()).ifPresent(run->D1RunData.get(player.level().getServer()).setValue(run.runId(),"death_position:"+player.getUUID(),""));
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){net.goui.cosmicdungeon.dungeon.DungeonGroupSplitService.clear();}
}
