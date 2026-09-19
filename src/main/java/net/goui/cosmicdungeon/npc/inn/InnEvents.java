package net.goui.cosmicdungeon.npc.inn;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class InnEvents {
    private InnEvents(){}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void bed(PlayerInteractEvent.RightClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player)||AccessPolicy.isDeveloper(player))return;
        if(InnService.claim(player,event.getPos())){event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);}
        else if(InnData.get(player.level().getServer()).contains(player.level(),event.getPos())){
            var item=event.getItemStack().getItem();
            if(item instanceof net.minecraft.world.item.AxeItem||item instanceof net.minecraft.world.item.BucketItem
                    ||item instanceof net.minecraft.world.item.FlintAndSteelItem||item instanceof net.minecraft.world.item.FireChargeItem){
                event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);
            }
        }
    }
    @SubscribeEvent public static void broken(BlockEvent.BreakEvent event){
        if(event.getPlayer() instanceof ServerPlayer player&&!AccessPolicy.isDeveloper(player)
                &&InnData.get(player.level().getServer()).contains(player.level(),event.getPos()))event.setCanceled(true);
    }
    @SubscribeEvent public static void placed(BlockEvent.EntityPlaceEvent event){
        if(event.getLevel() instanceof ServerLevel level
                &&(!(event.getEntity() instanceof ServerPlayer player)||!AccessPolicy.isDeveloper(player))
                &&InnData.get(level.getServer()).contains(level,event.getPos()))event.setCanceled(true);
    }
    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate event){
        if(event.getLevel() instanceof ServerLevel level)
            event.getAffectedBlocks().removeIf(pos->InnData.get(level.getServer()).contains(level,pos));
    }
    @SubscribeEvent public static void spawn(FinalizeSpawnEvent event){
        var level=event.getLevel().getLevel();
        if(event.getEntity() instanceof Enemy&&InnData.get(level.getServer()).contains(level,event.getEntity().blockPosition()))
            event.setSpawnCancelled(true);
    }
    @SubscribeEvent public static void hurt(LivingIncomingDamageEvent event){if(InnService.isBeluzon(event.getEntity()))event.setCanceled(true);}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){InnService.clear();}
    // TODO(M40, protection integration): bind the Inn cuboid to the project's full region movement/
    // fluid/piston controls before public play. Break/place, tool modification, explosions and new
    // hostile spawns are blocked here; test the First Heart for a full night as Dad's document requires.
}
