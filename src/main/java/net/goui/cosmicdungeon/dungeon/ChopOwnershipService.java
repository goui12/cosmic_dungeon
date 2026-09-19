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
        if(net.goui.cosmicdungeon.transaction.InventoryTransactionGuard.blocked(player)||hasStoredRecovery(player)
                ||ChopOwnershipData.get(player.level().getServer()).entry(player.getUUID())!=null)return false;
        for(int i=0;i<player.getInventory().getContainerSize();i++)if(isChop(player.getInventory().getItem(i)))return false;
        for(int i=0;i<player.getEnderChestInventory().getContainerSize();i++)if(isChop(player.getEnderChestInventory().getItem(i)))return false;
        return true;
    }
    /** Do not issue/adopt another token while older custody may still contain its original. */
    public static boolean hasStoredRecovery(ServerPlayer player){
        var server=player.level().getServer();
        return DungeonInventoryEscrowData.get(server).hasOwner(player.getUUID())
                ||net.goui.cosmicdungeon.dungeon.d1.D1StoredInventoryData.get(server).hasPending(player.getUUID())
                ||net.goui.cosmicdungeon.item.identity.ProtectedItemRecovery.pending(player);
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
        if(!isChop(stack)||stack.getCount()!=1||ChopTravelRecovery.blocked(player))return false;
        UUID owner=stack.get(ModDataComponents.CHOP_OWNER.get()),token=stack.get(ModDataComponents.CHOP_TOKEN.get());
        var entry=ChopOwnershipData.get(player.level().getServer()).entry(player.getUUID());
        if(!player.getUUID().equals(owner)||token==null||entry==null||entry.deliver()||!entry.token().equals(token.toString()))return false;
        // Checking ownership must never stamp, consume or merge an old stack.
        for(int i=0;i<player.getInventory().getContainerSize();i++){
            var other=player.getInventory().getItem(i);if(other!=stack&&isChop(other))return false;
        }
        for(int i=0;i<player.getEnderChestInventory().getContainerSize();i++)if(isChop(player.getEnderChestInventory().getItem(i)))return false;
        return true;
    }
    public static void consumed(ServerPlayer player,ItemStack before){
        UUID token=before.get(ModDataComponents.CHOP_TOKEN.get());
        if(token!=null)ChopOwnershipData.get(player.level().getServer()).release(player.getUUID(),token);
    }
    public static DungeonPlayerRunSnapshot snapshotForRun(ServerPlayer player,DungeonPlayerRunSnapshot snapshot){
        var items=ChopTravelRecovery.decode(player,snapshot.inventoryNbt());
        for(int i=0;i<items.size();i++){
            var live=player.getInventory().getItem(i);
            if(isChop(live)&&owned(player,live)){items.get(i).shrink(1);break;}
        }
        return new DungeonPlayerRunSnapshot(player.getUUID(),ChopTravelRecovery.encode(player,items));
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
        if(!(event.getEntity() instanceof ServerPlayer player)
                ||player.tickCount%net.goui.cosmicdungeon.Config.CHOP_RECOVERY_POLL_TICKS.get()!=0
                ||!player.isAlive()||player.containerMenu!=player.inventoryMenu
                ||net.goui.cosmicdungeon.transaction.InventoryTransactionGuard.blocked(player))return;
        var data=ChopOwnershipData.get(player.level().getServer());var entry=data.entry(player.getUUID());
        int slot=-1;
        for(int i=0;i<player.getInventory().getContainerSize();i++){
            var stack=player.getInventory().getItem(i);
            if(!isChop(stack))continue;
            if(slot!=-1||stack.getCount()!=1)return; // Preserve every ambiguous legacy stack exactly.
            slot=i;
        }
        for(int i=0;i<player.getEnderChestInventory().getContainerSize();i++)if(isChop(player.getEnderChestInventory().getItem(i)))return;
        boolean tracked=DungeonRunRegistryData.get(player.level().getServer()).findRunForPlayer(player.getUUID()).isPresent();
        if(!tracked&&(hasStoredRecovery(player)||DungeonInstanceSlots.slotOf(player.level().dimension()).isPresent()
                ||DungeonDefinitions.byDimension(player.level().dimension()).isPresent()))return;
        if(entry==null&&(tracked||slot<0||!player.getInventory().getItem(slot).is(ModItems.RAW_FARROWS_CHOP.get())
                ||player.getInventory().getItem(slot).has(ModDataComponents.CHOP_OWNER.get())
                ||player.getInventory().getItem(slot).has(ModDataComponents.CHOP_TOKEN.get())
                ||player.getInventory().getItem(slot).has(ModDataComponents.DUNGEON_RETURN_TARGET.get())
                ||player.getInventory().getItem(slot).has(ModDataComponents.COORDINATES.get())))return;
        if(entry!=null&&(entry.deliver()?tracked:tracked||slot<0||!player.getInventory().getItem(slot).is(ModItems.FARROWS_CHOP.get())))return;
        var before=ChopTravelRecovery.saveInventory(player);
        var items=ChopTravelRecovery.inventory(player);
        String kind;ChopOwnershipData.Entry next;
        if(entry==null&&slot>=0){
            var stack=items.get(slot);
            if(stack.has(ModDataComponents.CHOP_OWNER.get())||stack.has(ModDataComponents.CHOP_TOKEN.get()))return;
            // Only an unambiguous held Raw Chop is auto-adopted. A legacy return needs its old run/escrow reviewed.
            if(!stack.is(ModItems.RAW_FARROWS_CHOP.get()))return;
            var token=UUID.randomUUID();next=new ChopOwnershipData.Entry(token.toString(),0,false);
            stack.set(ModDataComponents.CHOP_OWNER.get(),player.getUUID());stack.set(ModDataComponents.CHOP_TOKEN.get(),token);
            kind="adopt";
        }else if(entry!=null&&entry.deliver()&&!tracked){
            next=new ChopOwnershipData.Entry(entry.token(),0,false);kind="refresh";
            if(slot>=0){
                var old=items.get(slot);
                if(!player.getUUID().equals(old.get(ModDataComponents.CHOP_OWNER.get())))return;
                items.set(slot,rawFrom(old,player.getUUID(),next));
            }else if(!ChopTravelRecovery.insert(items,raw(player.getUUID(),next)))return;
        }else if(entry!=null&&slot>=0&&!tracked){
            var old=player.getInventory().getItem(slot);
            if(!old.is(ModItems.FARROWS_CHOP.get())||!owned(player,old))return;
            var target=old.get(ModDataComponents.DUNGEON_RETURN_TARGET.get());
            var runs=DungeonRunRegistryData.get(player.level().getServer());
            boolean retained=entry.runId()>0&&runs.getRun(entry.runId()).isPresent()
                    ||target!=null&&runs.getRun(target.runId()).isPresent();
            if(!ChopRecoveryReview.rejection(new ChopRecoveryReview.Evidence(player.getUUID(),1,1,0,
                    old.get(ModDataComponents.CHOP_OWNER.get()),old.get(ModDataComponents.CHOP_TOKEN.get()),entry,
                    target==null?null:target.owner(),target==null?0:target.runId(),retained,false,false)).isEmpty())return;
            next=new ChopOwnershipData.Entry(entry.token(),0,false);kind="refresh";
            items.set(slot,rawFrom(old,player.getUUID(),next));
        }else return;
        if(!net.goui.cosmicdungeon.transaction.InventoryTransactionGuard.beforeInventoryChange(player))return;
        if(!ChopTravelRecovery.saveInventory(player).equals(before))return; // A guard may have returned old custody items.
        var pose=ChopTravelRecovery.pose(player);
        var plan=ChopTravelPlan.create(player.getUUID(),0,kind,before,ChopTravelRecovery.encode(player,items),
                pose,pose,new CompoundTag(),new CompoundTag(),data.image(player.getUUID()),ChopOwnershipData.entryImage(next),null);
        ChopTravelRecovery.execute(player,plan);
    }
    static ItemStack rawFrom(ItemStack previous,UUID owner,ChopOwnershipData.Entry entry){
        var raw=previous.transmuteCopy(ModItems.RAW_FARROWS_CHOP.get(),1);
        raw.remove(ModDataComponents.DUNGEON_RETURN_TARGET.get());raw.remove(ModDataComponents.COORDINATES.get());
        raw.set(ModDataComponents.CHOP_OWNER.get(),owner);raw.set(ModDataComponents.CHOP_TOKEN.get(),UUID.fromString(entry.token()));return raw;
    }
    // TODO(M20/M43, legacy review): Q&A D02 allows ONE personal Chop. Do not delete extras,
    // split an overstack, adopt an unknown dropped owner, or replace a Cooked stack with one Raw.
    // /d1 chop inspect/preview/apply handles a single reviewed orphan outside any retained run.
    // Duplicate, overstacked, partial/foreign markers, stored/orphan escrow and unknown dropped
    // ownership still need complete-save review. Never use a force-delete or ownership guess.
    // Q&A D24 changes unused Cooked to Raw only after its old run is gone; D04 protects its owner.
    // Automatic adoption accepts only one untagged Raw without old return metadata or stored custody.
    @SubscribeEvent public static void toss(ItemTossEvent event){
        if(!(event.getPlayer() instanceof ServerPlayer player)||!isChop(event.getEntity().getItem()))return;
        var stack=event.getEntity().getItem();
        event.getEntity().setTarget(player.getUUID());
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event){
        if(event.getEntity() instanceof ItemEntity item&&isChop(item.getItem())){
            var owner=item.getItem().get(ModDataComponents.CHOP_OWNER.get());if(owner!=null)item.setTarget(owner);
        }
    }
    @SubscribeEvent public static void pickup(ItemEntityPickupEvent.Pre event){
        if(!(event.getPlayer() instanceof ServerPlayer player)||!isChop(event.getItemEntity().getItem()))return;
        var stack=event.getItemEntity().getItem();
        boolean recoverLegacy=stack.getCount()==1&&stack.is(ModItems.RAW_FARROWS_CHOP.get())
                &&!stack.has(ModDataComponents.CHOP_OWNER.get())&&!stack.has(ModDataComponents.CHOP_TOKEN.get())
                &&player.getUUID().equals(event.getItemEntity().getTarget())&&canBuy(player);
        if(!recoverLegacy&&!owned(player,stack))event.setCanPickup(TriState.FALSE);
    }
    @SubscribeEvent public static void removed(EntityLeaveLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getEntity() instanceof ItemEntity item)
                ||item.getRemovalReason()!=Entity.RemovalReason.DISCARDED||!isChop(item.getItem()))return;
        var stack=item.getItem();var owner=stack.get(ModDataComponents.CHOP_OWNER.get());var token=stack.get(ModDataComponents.CHOP_TOKEN.get());
        if(owner!=null&&token!=null&&stack.getCount()==1&&!ChopTravelRecovery.pending(level.getServer(),owner))
            ChopOwnershipData.get(level.getServer()).release(owner,token);
    }
    @SubscribeEvent public static void eating(LivingEntityUseItemEvent.Start event){
        if(event.getEntity() instanceof ServerPlayer player&&isChop(event.getItem())&&!owned(player,event.getItem()))event.setCanceled(true);
    }
    @SubscribeEvent public static void eaten(LivingEntityUseItemEvent.Finish event){
        if(event.getEntity() instanceof ServerPlayer player&&event.getItem().is(ModItems.RAW_FARROWS_CHOP.get()))
            consumed(player,event.getItem());
    }
}
