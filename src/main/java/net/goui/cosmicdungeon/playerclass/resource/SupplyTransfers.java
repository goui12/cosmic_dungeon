package net.goui.cosmicdungeon.playerclass.resource;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.ChopTravelRecovery;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Verified two-owner commit using the project's native player/SavedData proof and inventory codecs. */
public final class SupplyTransfers {
    public static final String CUSTODY="supply_reservation_v1",RECEIPT="supply_receipt_v1";
    private static final Set<ServerPlayer> HOLDS=Collections.newSetFromMap(new WeakHashMap<>());
    private SupplyTransfers(){}
    private static CompoundTag root(ServerPlayer p){return p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);}
    private static void root(ServerPlayer p,CompoundTag root){p.getPersistentData().put(ClassData.ROOT_TAG,root);}
    public static void hold(ServerPlayer p,Exception failure){
        if(!HOLDS.add(p))return;
        com.mojang.logging.LogUtils.getLogger().error("Supply transfer preserved for recovery: {}",p.getUUID(),failure);
        p.connection.disconnect(Component.literal("Your supply transfer needs a save check. Its items and resources are preserved; reconnect to recover."));
    }
    public static boolean blocked(ServerPlayer p){
        if(HOLDS.contains(p)||root(p).contains(CUSTODY))return true;
        try{return SupplyTransferData.get(p.level().getServer()).pending(p.getUUID())!=null;}
        catch(RuntimeException error){hold(p,error);return true;}
    }
    public static boolean readyForCleanup(MinecraftServer server,List<UUID> owners){
        for(UUID owner:owners){
            var player=server.getPlayerList().getPlayer(owner);
            if(player!=null&&blocked(player)&&!reconcile(player))return false;
            if(SupplyTransferData.get(server).pending(owner)!=null)return false;
        }return true;
    }
    public static boolean beforeInventoryChange(ServerPlayer p){return !HOLDS.contains(p)&&reconcile(p)&&!blocked(p);}
    /** Decode native images and prove that only the consented tagged item count disappears. */
    public static void validateImages(ServerPlayer registryOwner,SupplyTransferPlan plan){
        var before=ChopTravelRecovery.decode(registryOwner,plan.tag("inventory_before"));
        var after=ChopTravelRecovery.decode(registryOwner,plan.tag("inventory_after"));
        if(before.size()!=after.size())throw new IllegalArgumentException("Supply inventory shape changed");
        long removed=0;
        for(int slot=0;slot<before.size();slot++){
            var old=before.get(slot);var next=after.get(slot);
            if(ItemStack.matches(old,next))continue;
            if(old.isEmpty()||!old.is(plan.kind().tag())
                    ||(!next.isEmpty()&&!ItemStack.isSameItemSameComponents(old,next))
                    ||next.getCount()>=old.getCount())
                throw new IllegalArgumentException("Supply image changes an unconsented stack");
            removed+=old.getCount()-next.getCount();
        }
        if(removed!=plan.amount())throw new IllegalArgumentException("Supply image yield differs from credit");
    }
    public static boolean execute(ServerPlayer donor,ServerPlayer recipient,SupplyTransferPlan plan){
        if(!plan.donor().equals(donor.getUUID())||!plan.recipient().equals(recipient.getUUID())
                ||blocked(donor)||blocked(recipient))return false;
        try{
            if(!ChopTravelRecovery.saveInventory(donor).equals(plan.tag("inventory_before"))
                    ||!root(recipient).getCompoundOrEmpty(ClassResourceLedger.KEY).equals(plan.tag("resource_before")))
                throw new IllegalStateException("Supply owner changed before reservation");
            validateImages(donor,plan);
            var data=SupplyTransferData.get(donor.level().getServer());data.reserve(plan);
            if(!data.flushVerified())throw new IllegalStateException("Supply reservation not verified");
            for(var owner:List.of(donor,recipient)){
                var next=root(owner).copy();next.put(CUSTODY,plan.reservation(owner.getUUID()));root(owner,next);
                if(!PlayerSaveProof.save(owner))throw new IllegalStateException("Supply owner reservation not verified");
            }
            data.commit(plan.id());
            if(!data.flushVerified())throw new IllegalStateException("Supply commit decision not verified");
            if(!reconcile(donor)||!reconcile(recipient))throw new IllegalStateException("Supply owner settlement incomplete");
            return true;
        }catch(RuntimeException error){hold(donor,error);hold(recipient,error);return false;}
    }
    public static boolean reconcile(ServerPlayer p){
        if(HOLDS.contains(p))return false;
        try{
            var data=SupplyTransferData.get(p.level().getServer());var plan=data.pending(p.getUUID());
            if(plan==null){
                if(root(p).contains(CUSTODY))throw new IllegalStateException("Supply owner reservation has no world decision");
                return true;
            }
            validateImages(p,plan);
            var custody=root(p).getCompoundOrEmpty(CUSTODY);var receipt=root(p).getCompoundOrEmpty(RECEIPT);
            if(!plan.recoverable(p.getUUID(),custody,receipt))throw new IllegalStateException("Supply owner proof differs from journal");
            if(!data.flushVerified())throw new IllegalStateException("Supply decision readback failed");
            if(!plan.receipt(p.getUUID()).equals(receipt)){
                if(plan.committed()){
                    if(p.getUUID().equals(plan.donor())){
                        if(!ChopTravelRecovery.saveInventory(p).equals(plan.tag("inventory_before")))
                            throw new IllegalStateException("Donor inventory differs from reserved image");
                        var items=ChopTravelRecovery.decode(p,plan.tag("inventory_after"));
                        for(int slot=0;slot<items.size();slot++)p.getInventory().setItem(slot,items.get(slot).copy());
                        p.getInventory().setChanged();
                    }else{
                        if(!root(p).getCompoundOrEmpty(ClassResourceLedger.KEY).equals(plan.tag("resource_before")))
                            throw new IllegalStateException("Recipient resource differs from reserved image");
                        var next=root(p).copy();next.put(ClassResourceLedger.KEY,plan.tag("resource_after"));root(p,next);
                    }
                }
                var next=root(p).copy();next.remove(CUSTODY);next.put(RECEIPT,plan.receipt(p.getUUID()));root(p,next);
            }
            if(!PlayerSaveProof.save(p))throw new IllegalStateException("Supply owner receipt not verified");
            data.acknowledge(plan.id(),p.getUUID());
            if(!data.flushVerified())throw new IllegalStateException("Supply owner acknowledgement not verified");
            p.inventoryMenu.broadcastChanges();return true;
        }catch(RuntimeException error){hold(p,error);return false;}
    }
    public static void stopped(){HOLDS.clear();}
}
