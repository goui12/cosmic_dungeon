package net.goui.cosmicdungeon.achievement.d1;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.achievement.*;
import net.goui.cosmicdungeon.dungeon.d1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class D1WorldAchievements {
    private record OpenTarget(String dimension,BlockPos pos,long tick){}
    private static final Map<UUID,OpenTarget> OPENS=new HashMap<>();
    private D1WorldAchievements(){}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void clicked(PlayerInteractEvent.RightClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        if (!player.isAlive() || player.isSpectator() || AccessPolicy.isDeveloper(player)) return;
        var run=D1Members.run(player.level()).orElse(null);
        if (!player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                && (run==null||!D1Members.inside(player,run))) return;
        OPENS.put(player.getUUID(),new OpenTarget(player.level().dimension().location().toString(),event.getPos().immutable(),player.level().getGameTime()));
    }
    @SubscribeEvent public static void opened(PlayerContainerEvent.Open event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var pending=OPENS.remove(player.getUUID());if(pending==null||player.level().getGameTime()-pending.tick()>2
                ||!pending.dimension().equals(player.level().dimension().location().toString()))return;
        if (!player.isAlive() || player.isSpectator() || AccessPolicy.isDeveloper(player)
                || net.goui.cosmicdungeon.transaction.InventoryTransactionGuard.blocked(player)) return;
        var run=D1Members.run(player.level()).orElse(null);
        var locations=D1ObjectiveBindings.get(player.level().getServer());
        if(run!=null && D1Members.inside(player,run) && event.getContainer() instanceof LecternMenu&& !event.getContainer().getSlot(0).getItem().isEmpty()){
            for(String key:List.of("journal_1","journal_2","journal_3"))if(locations.matches(player.level(),pending.pos(),key)){
                D1JournalService.opened(player, event.getContainer().getSlot(0).getItem(), key);
            }
        }
        if (!player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) return;
        if(!(event.getContainer() instanceof ChestMenu chest) || !chest.stillValid(player))return;
        var place=locations.location("stairway");
        if(place==null || !locations.matches(player.level(),place.pos(),"stairway") || !player.level().hasChunkAt(place.pos()))return;
        var bound=player.level().getBlockEntity(place.pos());
        boolean correct=chest.getContainer()==bound
                ||(chest.getContainer() instanceof net.minecraft.world.CompoundContainer compound&&bound instanceof net.minecraft.world.Container container&&compound.contains(container));
        if(!correct)return;
        StairwayReward.deliver(player, 0, place.pos());
    }
    @SubscribeEvent public static void path(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var run=D1Members.run(player.level()).orElse(null);if(run==null||!D1Members.inside(player,run))return;
        var locations=D1ObjectiveBindings.get(player.level().getServer());var data=D1RunData.get(player.level().getServer());
        String key="fire_escape:"+player.getUUID();var feet=player.blockPosition();
        if(player.isInLava()){data.setValue(run.runId(),key,"");return;}
        if(locations.matches(player.level(),feet,"fire_start")||locations.matches(player.level(),feet.below(),"fire_start"))
            data.setValue(run.runId(),key,"started");
        if((locations.matches(player.level(),feet,"fire_end")||locations.matches(player.level(),feet.below(),"fire_end"))
                &&!data.values(run.runId(),key).isEmpty()){
            CosmicAdvancementUtil.grant(player,CosmicAchievementIds.FIRE_ESCAPE);data.setValue(run.runId(),key,"");
        }
    }
    @SubscribeEvent public static void shulker(LivingDamageEvent.Post event){
        if(event.getNewDamage()<=0||!(event.getEntity() instanceof ServerPlayer player)||AccessPolicy.isDeveloper(player)
                ||!(event.getSource().getDirectEntity() instanceof ShulkerBullet))return;
        if(D1AchievementRegionService.inRegion(player.level(),player.blockPosition(),"d1_spawn_area"))
            CosmicAdvancementUtil.grant(player,CosmicAchievementIds.SHULKER_EXPRESS);
    }
    @SubscribeEvent public static void left(PlayerEvent.PlayerLoggedOutEvent event){OPENS.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){OPENS.clear();}
    // StairwayReward delivers Q&A D76's ordinary Elytra with a same-player save receipt.
    // D1JournalService checks trusted edition + canonical body on actual book/lectern opens.
    // TODO(M79/M81, licensed TEST): review legacy books with explicit preview/apply and bind the
    // actual uppermost chest. Preserve authored placements/content and verify real opening packets.
    // MASTER Achievements!C20 explicitly means six CHARACTERS wearing Piglin Heads at Camp 4.
    // Linked 1xe-ikZsd0JoNlHZO_4b076AND7W6ow1ax4aWnVfuNPo is flavor, not a six-companion rule.
    // D1PiglinAchievement implements that mechanic; the Bogatyr companion cap remains unchanged.
}
