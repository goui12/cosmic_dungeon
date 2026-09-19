package net.goui.cosmicdungeon.dungeon;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.item.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.TriState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;

/** Q&A D01-D04/D24-D25: owner-only pickup, one concurrent Chop, stale returns become Raw. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class ChopOwnershipService {
    private ChopOwnershipService(){}
    public static boolean isChop(ItemStack stack){return stack.is(ModItems.RAW_FARROWS_CHOP.get())||stack.is(ModItems.FARROWS_CHOP.get());}
    public static boolean canBuy(ServerPlayer player){
        if(ChopOwnershipData.get(player.level().getServer()).entry(player.getUUID())!=null)return false;
        for(int i=0;i<player.getInventory().getContainerSize();i++)if(isChop(player.getInventory().getItem(i)))return false;
        for(int i=0;i<player.getEnderChestInventory().getContainerSize();i++)if(isChop(player.getEnderChestInventory().getItem(i)))return false;
        return true;
    }
    public static ItemStack preparePurchase(ServerPlayer player,ItemStack stack){
        stack.set(ModDataComponents.CHOP_OWNER.get(),player.getUUID());
        stack.set(ModDataComponents.CHOP_TOKEN.get(),UUID.randomUUID());
        return stack;
    }
    public static void purchased(ServerPlayer player,ItemStack stack){
        ChopOwnershipData.get(player.level().getServer()).issue(player.getUUID(),stack.get(ModDataComponents.CHOP_TOKEN.get()));
    }
    public static boolean owned(ServerPlayer player,ItemStack stack){
        if(!isChop(stack))return false;
        UUID owner=stack.get(ModDataComponents.CHOP_OWNER.get()), token=stack.get(ModDataComponents.CHOP_TOKEN.get());
        var data=ChopOwnershipData.get(player.level().getServer());
        if(owner==null&&token==null&&data.entry(player.getUUID())==null){
            preparePurchase(player,stack);purchased(player,stack);owner=player.getUUID();token=stack.get(ModDataComponents.CHOP_TOKEN.get());
        }
        var entry=data.entry(player.getUUID());
        return player.getUUID().equals(owner)&&token!=null&&entry!=null&&entry.token().equals(token.toString());
    }
    public static void consumed(ServerPlayer player,ItemStack before){
        UUID token=before.get(ModDataComponents.CHOP_TOKEN.get());
        if(token!=null)ChopOwnershipData.get(player.level().getServer()).release(player.getUUID(),token);
    }
    public static DungeonPlayerRunSnapshot snapshotForRun(ServerPlayer player,DungeonPlayerRunSnapshot snapshot){
        var items=NonNullList.withSize(player.getInventory().getContainerSize(),ItemStack.EMPTY);
        ContainerHelper.loadAllItems(TagValueInput.create(ProblemReporter.DISCARDING,player.level().registryAccess(),snapshot.inventoryNbt()),items);
        for(int i=0;i<items.size();i++){
            var live=player.getInventory().getItem(i);
            if(isChop(live)&&owned(player,live)){items.get(i).shrink(1);break;}
        }
        var output=TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);ContainerHelper.saveAllItems(output,items);
        return new DungeonPlayerRunSnapshot(player.getUUID(),output.buildResult());
    }
    public static void clearForDungeonEntry(ServerPlayer player){
        ItemStack carry=ItemStack.EMPTY;
        for(int i=0;i<player.getInventory().getContainerSize();i++){
            var stack=player.getInventory().getItem(i);
            if(isChop(stack)&&owned(player,stack)){carry=stack.copyWithCount(1);break;}
        }
        DungeonLifecycleService.clearPlayerInventory(player);
        if(!carry.isEmpty())player.getInventory().add(carry);
    }
    private static ItemStack raw(UUID owner,ChopOwnershipData.Entry entry){
        var stack=new ItemStack(ModItems.RAW_FARROWS_CHOP.get());
        stack.set(ModDataComponents.CHOP_OWNER.get(),owner);
        stack.set(ModDataComponents.CHOP_TOKEN.get(),UUID.fromString(entry.token()));
        return stack;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player)||player.tickCount%20!=0)return;
        if(net.goui.cosmicdungeon.vendor.CommerceTransactions.blocked(player))return;
        var data=ChopOwnershipData.get(player.level().getServer());
        var entry=data.entry(player.getUUID());
        boolean tracked=DungeonRunRegistryData.get(player.level().getServer()).findRunForPlayer(player.getUUID()).isPresent();
        if(entry!=null&&entry.deliver()&&!tracked){
            boolean replaced=false;
            for(int i=0;i<player.getInventory().getContainerSize();i++){
                var stack=player.getInventory().getItem(i);
                if(isChop(stack)&&player.getUUID().equals(stack.get(ModDataComponents.CHOP_OWNER.get()))){
                    player.getInventory().setItem(i,replaced?ItemStack.EMPTY:raw(player.getUUID(),entry));replaced=true;
                }
            }
            if(replaced||player.getInventory().add(raw(player.getUUID(),entry)))data.delivered(player.getUUID());
        }
        for(int i=0;i<player.getInventory().getContainerSize();i++){
            var stack=player.getInventory().getItem(i);
            if(!isChop(stack))continue;
            if(!owned(player,stack))continue;
            var target=stack.get(ModDataComponents.DUNGEON_RETURN_TARGET.get());
            if(stack.is(ModItems.FARROWS_CHOP.get())&&(target==null||DungeonRunRegistryData.get(player.level().getServer())
                    .getRun(target.runId()).filter(r->r.stateEnum()==DungeonRunState.ACTIVE&&r.containsPlayer(player.getUUID())).isEmpty()))
                player.getInventory().setItem(i,raw(player.getUUID(),data.entry(player.getUUID())));
        }
    }
    @SubscribeEvent public static void toss(ItemTossEvent event){
        if(!(event.getPlayer() instanceof ServerPlayer player)||!isChop(event.getEntity().getItem()))return;
        var stack=event.getEntity().getItem();owned(player,stack);
        event.getEntity().setTarget(player.getUUID());
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event){
        if(event.getEntity() instanceof ItemEntity item&&isChop(item.getItem())){
            var owner=item.getItem().get(ModDataComponents.CHOP_OWNER.get());if(owner!=null)item.setTarget(owner);
        }
    }
    @SubscribeEvent public static void pickup(ItemEntityPickupEvent.Pre event){
        if(!(event.getPlayer() instanceof ServerPlayer player)||!isChop(event.getItemEntity().getItem()))return;
        if(!owned(player,event.getItemEntity().getItem()))event.setCanPickup(TriState.FALSE);
    }
    @SubscribeEvent public static void removed(EntityLeaveLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getEntity() instanceof ItemEntity item)
                ||item.getRemovalReason()!=Entity.RemovalReason.DISCARDED||!isChop(item.getItem()))return;
        var stack=item.getItem();var owner=stack.get(ModDataComponents.CHOP_OWNER.get());var token=stack.get(ModDataComponents.CHOP_TOKEN.get());
        if(owner!=null&&token!=null)ChopOwnershipData.get(level.getServer()).release(owner,token);
    }
    @SubscribeEvent public static void eaten(LivingEntityUseItemEvent.Finish event){
        if(event.getEntity() instanceof ServerPlayer player&&event.getItem().is(ModItems.RAW_FARROWS_CHOP.get()))
            consumed(player,event.getItem());
    }
}
