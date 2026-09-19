package net.goui.cosmicdungeon.playerclass.dragoon.repair;
import net.goui.cosmicdungeon.economy.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;
/** Commit decision first; apply/acknowledge each owner only after an exact player-file readback.
 * No new balance store, no refund of an uncertain commit, no reconstruction from an absent owner image. */
public final class RepairTransactions {
    public static final String RECEIPT="repair_receipt_v1";
    private static final Set<ServerPlayer> HOLDS=Collections.newSetFromMap(new WeakHashMap<>());
    private RepairTransactions(){}
    private static PlayerCurrencyData data(ServerPlayer player){return PlayerCurrencyData.get(player.level().getServer());}
    public static boolean prepare(ServerPlayer customer,ServerPlayer provider,UUID id,int units,long deadline){
        RepairCustody.capture(customer);RepairCustody.capture(provider);
        var before=RepairCustody.read(customer);var parts=RepairCustody.read(provider);
        var repaired=RepairCustody.decode(customer,before.getCompoundOrEmpty("target"));
        repaired.setDamageValue(Math.max(0,repaired.getDamageValue()-DragoonRepairRules.projectedRepair(repaired,units)));
        var plan=new RepairCommitPlan(customer.getUUID(),provider.getUUID(),before,parts,
                RepairCustody.encode(customer,repaired),units,deadline,-1,DragoonRepairRules.durationTicks(units),false,false);
        data(customer).prepareRepair(id,plan);return data(customer).flushVerified();
    }
    public static boolean decide(ServerPlayer customer,ServerPlayer provider,UUID id){
        var data=data(customer);var plan=data.repairPlan(id).orElseThrow();
        RepairCustody.capture(customer);RepairCustody.capture(provider);
        if(!plan.customerBefore().equals(RepairCustody.read(customer))||!plan.providerBefore().equals(RepairCustody.read(provider)))return false;
        var transfer=data.commitRepair(id,customer.level().getServer().overworld().getGameTime(),System.currentTimeMillis());
        if(!transfer.status().equals(AccountTransfer.COMMITTED))return false;
        // A false save result is uncertain, never a reason to refund/release pre-commit items.
        boolean verified=data.flushVerified();
        if(verified)data.reportTransfer(id,transfer);
        return verified;
    }
    private static CompoundTag receipt(ServerPlayer player){
        return player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).getCompoundOrEmpty(RECEIPT);
    }
    public static CompoundTag receiptImage(UUID owner,UUID transaction,String outcome){
        if(!Set.of(AccountTransfer.COMMITTED,AccountTransfer.CANCELLED).contains(outcome))throw new IllegalArgumentException("Unsettled repair receipt");
        var receipt=new CompoundTag();receipt.putInt("schema",1);receipt.putString("owner",owner.toString());
        receipt.putString("transaction",transaction.toString());receipt.putString("outcome",outcome);return receipt;
    }
    private static void writeReceipt(ServerPlayer player,UUID transaction,String outcome){
        var receipt=receiptImage(player.getUUID(),transaction,outcome);
        var root=player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();root.put(RECEIPT,receipt);
        player.getPersistentData().put(ClassData.ROOT_TAG,root);
    }
    public static boolean receiptMatches(CompoundTag receipt,UUID owner,UUID transaction,String outcome){
        return receipt.getIntOr("schema",0)==1&&owner.toString().equals(receipt.getStringOr("owner",""))
                &&transaction.toString().equals(receipt.getStringOr("transaction",""))&&outcome.equals(receipt.getStringOr("outcome",""));
    }
    public static void hold(ServerPlayer player,String detail){
        HOLDS.add(player);
        com.mojang.logging.LogUtils.getLogger().error("Repair reconciliation held for {}: {}",player.getUUID(),detail);
        player.connection.disconnect(Component.literal("Your repair needs a save check. Its items and payment records are preserved; reconnect to recover."));
    }
    public static boolean blocked(ServerPlayer player){
        return HOLDS.contains(player)||RepairCustody.pending(player)||data(player).pendingRepair(player.getUUID()).isPresent();
    }
    public static boolean beforeInventoryChange(ServerPlayer player){
        if(HOLDS.contains(player)||!net.goui.cosmicdungeon.vendor.CommerceTransactions.beforeInventoryChange(player))return false;
        player.closeContainer();return reconcile(player)&&!blocked(player);
    }
    public static boolean readyForCleanup(net.minecraft.server.MinecraftServer server,java.util.List<UUID> owners){
        // Index lookups only; no world scan or asynchronous player-file rewriting.
        for(UUID owner:owners){
            var player=server.getPlayerList().getPlayer(owner);
            if(player!=null&&blocked(player)&&!beforeInventoryChange(player))return false;
            if(PlayerCurrencyData.get(server).pendingRepair(owner).isPresent())return false;
        }
        return true;
    }
    /** Run before ordinary recovery, menu reopening, or dungeon inventory replacement. */
    public static boolean reconcile(ServerPlayer player){
        try{
            if(HOLDS.contains(player)||DragoonRepairSessionData.get(player)!=null)return false;
            var data=data(player);var owner=player.getUUID();var state=RepairCustody.read(player);
            UUID pending=data.pendingRepair(owner).orElse(null);
            if(pending!=null){
                var plan=data.repairPlan(pending).orElseThrow();var transfer=data.transfer(pending).orElseThrow();
                if(transfer.reserved()){
                    transfer=data.cancelTransfer(pending,transfer.terms(),System.currentTimeMillis(),"repair interface ended before commit");
                    data.reportTransfer(pending,transfer);
                }
                // Verify any uncertain in-memory decision before modifying either owner's escrow.
                if(!data.flushVerified()){hold(player,"account decision save not verified");return false;}
                String outcome=transfer.status();
                if(!Set.of(AccountTransfer.COMMITTED,AccountTransfer.CANCELLED).contains(outcome))throw new IllegalStateException("Invalid repair decision");
                if(receiptMatches(receipt(player),owner,pending,outcome)){
                    if(RepairCustody.pending(player))throw new IllegalStateException("Applied receipt still has custody");
                }else{
                    if(!RepairCustody.pending(player))throw new IllegalStateException("Verified owner reservation is missing; manual restore review required");
                    var projected=plan.project(owner,state,outcome.equals(AccountTransfer.COMMITTED));
                    // Encode/decode every return image before touching the owner. Local rollback below
                    // is safe only before PlayerSaveProof starts writing; the global decision is retained.
                    for(var image:RepairCustodyImages.held(projected,false))RepairCustody.decode(player,image);
                    var rootBefore=player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
                    RepairCustody.write(player,projected);
                    if(!RepairCustody.release(player,false,false)){
                        player.getPersistentData().put(ClassData.ROOT_TAG,rootBefore);hold(player,"item projection failed");return false;
                    }
                    writeReceipt(player,pending,outcome);
                }
                if(!PlayerSaveProof.save(player)){hold(player,"owner item/receipt save not verified");return false;}
                data.acknowledgeRepair(pending,owner);
                if(!data.flushVerified()){hold(player,"owner acknowledgement not verified");return false;}
            }else if(RepairCustody.pending(player)){
                RepairCustodyImages.validate(state,owner);
                String tx=state.getStringOr("transaction","");
                if(!tx.isEmpty()){
                    UUID id=UUID.fromString(tx);var transfer=data.transfer(id).orElseThrow(()->new IllegalStateException("Custody has no account reservation"));
                    if(!transfer.terms().first().equals(owner)&&!transfer.terms().second().equals(owner))throw new IllegalStateException("Foreign account reservation");
                    if(transfer.status().equals(AccountTransfer.COMMITTED))throw new IllegalStateException("Committed legacy repair has no item decision");
                    if(transfer.reserved())data.cancelTransfer(id,transfer.terms(),System.currentTimeMillis(),"incomplete repair preparation");
                    if(!data.flushVerified()){hold(player,"preparation cancellation not verified");return false;}
                }
                if(!RepairCustody.release(player,false,false)){hold(player,"uncommitted custody invalid");return false;}
                if(!PlayerSaveProof.save(player)){hold(player,"cancelled custody save not verified");return false;}
            }
            // The next Ready flushes account state before writing a new reservation/receipt.
            // No disk write is needed for an already-acknowledged ordinary /repair claim.
            return true;
        }catch(RuntimeException failure){hold(player,failure.getMessage());return false;}
    }
}
