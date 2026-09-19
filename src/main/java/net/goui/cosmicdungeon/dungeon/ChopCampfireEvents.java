package net.goui.cosmicdungeon.dungeon;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.progression.ProgressionService;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;

/** Pending cooking never consumes items; cancellation/restart leaves the Raw Chop untouched. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class ChopCampfireEvents {
    private record Cooking(ResourceKey<Level> dimension,BlockPos pos,InteractionHand hand,ItemStack item,long completes){}
    private static final Map<UUID,Cooking> PENDING=new HashMap<>();
    private ChopCampfireEvents(){}
    @SubscribeEvent public static void campfire(PlayerInteractEvent.RightClickBlock event){
        if(!event.getItemStack().is(ModItems.RAW_FARROWS_CHOP.get())
                ||!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof CampfireBlock))return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        if(!ProgressionService.hasVillageAccess(player)){
            player.sendSystemMessage(Component.literal("Complete Dungeon 1 before using village travel."));return;
        }
        if(!ChopOwnershipService.owned(player,event.getItemStack())||PENDING.containsKey(player.getUUID()))return;
        var state=player.level().getBlockState(event.getPos());
        if(!state.getValue(CampfireBlock.LIT))return;
        var run=DungeonLifecycleService.findActiveRunForPlayer(player);
        if(run.isEmpty()||!run.get().containsDimension(player.level().dimension()))return;
        PENDING.put(player.getUUID(),new Cooking(player.level().dimension(),event.getPos().immutable(),event.getHand(),
                event.getItemStack().copy(),player.level().getServer().overworld().getGameTime()+Config.CHOP_COOK_TICKS.get()));
        player.displayClientMessage(Component.literal("Cooking Farrow's Chop. Stay beside the campfire."),true);
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        var server=event.getServer();long now=server.overworld().getGameTime();
        for(var id:new ArrayList<>(PENDING.keySet())){
            var pending=PENDING.get(id);var player=server.getPlayerList().getPlayer(id);
            if(player==null||!player.isAlive()||!player.level().dimension().equals(pending.dimension())
                    ||player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pending.pos()))>Math.pow(Config.CHOP_COOK_RANGE.get(),2)
                    ||!ItemStack.matches(player.getItemInHand(pending.hand()),pending.item())){
                PENDING.remove(id);continue;
            }
            if(now>=pending.completes()){
                PENDING.remove(id);
                FarrowsChopTravelService.cookAndLeaveDungeon(player,player.getItemInHand(pending.hand()),pending.pos());
            }
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){PENDING.clear();}
}
