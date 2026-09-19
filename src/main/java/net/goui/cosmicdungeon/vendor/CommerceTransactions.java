package net.goui.cosmicdungeon.vendor;
import net.goui.cosmicdungeon.economy.*;
import net.goui.cosmicdungeon.item.identity.ProtectedItemRecovery;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCustody;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairTransactions;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;
/** Vendor sale/retail/direct-repair all use the same account and owner-file recovery protocol. */
public final class CommerceTransactions {
    public static final String KEY="commerce_custody_v1",RECEIPT="commerce_receipt_v1";
    private static final Set<ServerPlayer> HOLDS=Collections.newSetFromMap(new WeakHashMap<>());
    private CommerceTransactions(){}
    private static PlayerCurrencyData data(ServerPlayer p){return PlayerCurrencyData.get(p.level().getServer());}
    private static CompoundTag root(ServerPlayer p){return p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);}
    private static void custody(ServerPlayer p,CompoundTag image){var next=root(p).copy();if(image.isEmpty())next.remove(KEY);else next.put(KEY,image.copy());p.getPersistentData().put(ClassData.ROOT_TAG,next);}
    public static boolean blocked(ServerPlayer p){return HOLDS.contains(p)||root(p).contains(KEY)||data(p).pendingOperation(p.getUUID()).isPresent();}
    private static void hold(ServerPlayer p,String reason){HOLDS.add(p);com.mojang.logging.LogUtils.getLogger().error("Commerce recovery held for {}: {}",p.getUUID(),reason);p.connection.disconnect(Component.literal("Your purchase needs a save check. Items and payment records are preserved; reconnect to recover."));}
    public static boolean beforeInventoryChange(ServerPlayer p){if(HOLDS.contains(p))return false;p.closeContainer();return reconcile(p)&&!blocked(p);}
    public static boolean execute(ServerPlayer p,UUID id,long delta,String kind,String related,CompoundTag plan){
        if(blocked(p)||!CurrencyService.transactionsAllowed(p)||!p.containerMenu.getCarried().isEmpty())return false;
        try{
            CommerceCustodyImages.validate(plan,p.getUUID());
            for(var tag:(ListTag)plan.get("inputs")){var lot=(CompoundTag)tag;int slot=lot.getIntOr("slot",-1);
                if(!RepairCustody.encode(p,p.getInventory().getItem(slot)).equals(lot.getCompoundOrEmpty("item")))return false;}
            var data=data(p);var op=data.reserveOperation(id,p.getUUID(),delta,kind,related,plan.getLongOr("run",-1),plan,System.currentTimeMillis());
            if(!data.flushVerified())throw new IllegalStateException("account reservation not verified");
            if(!op.reserved()){reconcile(p);return false;}
            custody(p,plan);for(var tag:(ListTag)plan.get("inputs"))p.getInventory().setItem(((CompoundTag)tag).getIntOr("slot",-1),ItemStack.EMPTY);
            p.getInventory().setChanged();if(!PlayerSaveProof.save(p))throw new IllegalStateException("owner input custody save not verified");
            data.prepareOperation(id);if(!data.flushVerified())throw new IllegalStateException("prepared owner save not verified");
            op=data.decideOperation(id,true,System.currentTimeMillis());if(!data.flushVerified())throw new IllegalStateException("account/item decision not verified");
            CurrencyAudit.report(p.level().getServer(),p.getUUID(),p.getName().getString(),id.toString(),kind,delta,op.before(),op.after(),0,related,op.run(),op.status());
            return reconcile(p);
        }catch(RuntimeException failure){hold(p,failure.getMessage());return false;}
    }
    public static boolean reconcile(ServerPlayer p){
        if(HOLDS.contains(p))return false;
        try{
            var data=data(p);UUID id=data.pendingOperation(p.getUUID()).orElse(null);
            if(id==null){if(root(p).contains(KEY))throw new IllegalStateException("Owner custody has no account decision");return true;}
            var op=data.operation(id).orElseThrow();
            if(op.reserved())op=data.decideOperation(id,false,System.currentTimeMillis());
            if(!data.flushVerified())throw new IllegalStateException("commerce outcome not verified");
            boolean committed=op.status().equals(AccountTransfer.COMMITTED);String outcome=committed?AccountTransfer.COMMITTED:AccountTransfer.CANCELLED;
            var recoverable=CommerceCustodyImages.recoverable(op,id,p.getUUID(),root(p).getCompoundOrEmpty(KEY),root(p).getCompoundOrEmpty(RECEIPT));
            if(RepairTransactions.receiptMatches(root(p).getCompoundOrEmpty(RECEIPT),p.getUUID(),id,outcome)){
                if(root(p).contains(KEY))throw new IllegalStateException("Applied commerce receipt still has custody");
            }else{
                var current=root(p).getCompoundOrEmpty(KEY);
                if(!current.isEmpty()){
                    if(!current.equals(op.plan()))throw new IllegalStateException("Owner commerce custody differs from prepared plan");
                    if(!apply(p,op.plan(),committed,recoverable))return false;
                }else if(op.prepared()||committed)throw new IllegalStateException("Verified commerce custody missing; manual restore review required");
                var next=root(p).copy();next.put(RECEIPT,RepairTransactions.receiptImage(p.getUUID(),id,outcome));p.getPersistentData().put(ClassData.ROOT_TAG,next);
            }
            if(!PlayerSaveProof.save(p))throw new IllegalStateException("owner commerce receipt not verified");
            data.acknowledgeOperation(id,p.getUUID());if(!data.flushVerified())throw new IllegalStateException("commerce acknowledgement not verified");
            p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();p.containerMenu.broadcastChanges();return true;
        }catch(RuntimeException failure){hold(p,failure.getMessage());return false;}
    }
    private static boolean apply(ServerPlayer p,CompoundTag plan,boolean committed,ListTag lots){
        var before=root(p).copy();var inventory=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)inventory.add(p.getInventory().getItem(i).copy());
        try{
            var decoded=new ArrayList<ItemStack>();
            for(var tag:lots){var item=RepairCustody.decode(p,((CompoundTag)tag).getCompoundOrEmpty("item"));ProtectedItemRecovery.validateSerializable(p,item);decoded.add(item);}
            var details=plan.getCompoundOrEmpty("details");
            if(committed&&details.contains("chop_before")&&!net.goui.cosmicdungeon.dungeon.ChopOwnershipData.get(p.level().getServer())
                    .compareAndSetVerified(p.getUUID(),details.getCompoundOrEmpty("chop_before"),details.getCompoundOrEmpty("chop_after")))throw new IllegalStateException("Chop ownership decision not verified");
            if(committed)VendorStock.apply(p,details);
            custody(p,new CompoundTag());long run=plan.getLongOr("run",-1);
            for(int i=0;i<lots.size();i++){
                var item=decoded.get(i);int slot=((CompoundTag)lots.get(i)).getIntOr("slot",-1);
                if(p.isAlive()&&run==ProtectedItemRecovery.scope(p)){
                    if(slot>=0&&p.getInventory().getItem(slot).isEmpty())p.getInventory().setItem(slot,item);
                    else ProtectedItemRecovery.returnDetached(p,item);
                }else ProtectedItemRecovery.queueDetached(p,item,run);
            }
            return true;
        }catch(RuntimeException failure){
            for(int i=0;i<inventory.size();i++)p.getInventory().setItem(i,inventory.get(i));p.getPersistentData().put(ClassData.ROOT_TAG,before);
            hold(p,failure.getMessage());return false;
        }
    }
    public static CompoundTag plan(ServerPlayer p,UUID id,ListTag inputs,ListTag outputs,CompoundTag details){
        return CommerceCustodyImages.create(id,p.getUUID(),ProtectedItemRecovery.scope(p),inputs,outputs,details);
    }
    public static void describeChop(ServerPlayer p,CompoundTag details,ItemStack issued,boolean sale){
        var data=net.goui.cosmicdungeon.dungeon.ChopOwnershipData.get(p.level().getServer());details.put("chop_before",data.image(p.getUUID()));
        var after=sale?new CompoundTag():net.goui.cosmicdungeon.dungeon.ChopOwnershipData.issuedImage(issued.get(net.goui.cosmicdungeon.component.ModDataComponents.CHOP_TOKEN.get()));details.put("chop_after",after);
    }
    // TODO(M03/M118, licensed TEST): native account/player/Chop write interruptions, full inventory,
    // daily stock across clock rollback/restart and exact zero-Trace surrender. Partial restored
    // backups without matching owner receipts remain held for full-save evidence review.
}
