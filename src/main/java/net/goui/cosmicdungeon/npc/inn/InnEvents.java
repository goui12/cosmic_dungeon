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
                &&(InnData.get(level.getServer()).contains(level,event.getPos())
                ||event instanceof BlockEvent.EntityMultiPlaceEvent multi&&multi.getReplacedBlockSnapshots().stream()
                    .anyMatch(snapshot->InnData.get(level.getServer()).contains(level,snapshot.getPos()))))event.setCanceled(true);
    }
    @SubscribeEvent public static void fluidBlock(BlockEvent.FluidPlaceBlockEvent event){
        if(event.getLevel() instanceof ServerLevel level&&InnData.get(level.getServer()).contains(level,event.getPos()))
            event.setCanceled(true);
    }
    @SubscribeEvent public static void mobBreak(net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent event){
        if(event.getEntity().level() instanceof ServerLevel level&&InnData.get(level.getServer()).contains(level,event.getPos()))
            event.setCanceled(true);
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
    @SubscribeEvent public static void piston(net.neoforged.neoforge.event.level.PistonEvent.Pre event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var inn=InnData.get(level.getServer());
        if(inn.contains(level,event.getPos())||inn.contains(level,event.getFaceOffsetPos())){event.setCanceled(true);return;}
        var resolver=event.getStructureHelper();if(resolver==null||!resolver.resolve())return;
        var direction=event.getPistonMoveType().isExtend?event.getDirection():event.getDirection().getOpposite();
        if(resolver.getToDestroy().stream().anyMatch(pos->inn.contains(level,pos))
                ||resolver.getToPush().stream().anyMatch(pos->inn.contains(level,pos)||inn.contains(level,pos.relative(direction))))
            event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void itemUse(net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent event){
        var context=event.getUseOnContext();
        if(!(context.getPlayer() instanceof ServerPlayer player)||AccessPolicy.isDeveloper(player))return;
        var inn=InnData.get(player.level().getServer());
        var item=context.getItemInHand().getItem();
        if((item instanceof net.minecraft.world.item.BlockItem||item instanceof net.minecraft.world.item.BucketItem
                ||item instanceof net.minecraft.world.item.FlintAndSteelItem||item instanceof net.minecraft.world.item.FireChargeItem)
                &&(inn.contains(player.level(),context.getClickedPos())
                ||inn.contains(player.level(),context.getClickedPos().relative(context.getClickedFace()))))
            event.cancelWithResult(InteractionResult.FAIL);
    }
    // TODO(M40, native runtime QA): full-night Heart activation and every Inn boundary must be
    // exercised on an authored-world COPY. NPC Beluzon Internal (Aug 29) requires a native
    // Creaking and protected Pale Oak pillar/Heart/beds; no automatic placement or NPC replacement.
    // These hooks protect local mechanisms, not other mods' direct world writes or operator commands.
}
