package net.goui.cosmicdungeon.playerclass.theurgist;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.mercenary.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.resource.*;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Pending two-owner proof: target success is durable before the caster is ever charged. */
public final class TheurgistRevival {
    public static final String CUSTODY="theurgist_revival_reservation_v1",RECEIPT="theurgist_revival_receipt_v1";
    private static final ThreadLocal<RevivalPlan> ACTIVE=new ThreadLocal<>();
    private static final Set<ServerPlayer> HOLDS=Collections.newSetFromMap(new WeakHashMap<>());
    private TheurgistRevival(){}
    private static CompoundTag root(ServerPlayer p){return p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);}
    private static void root(ServerPlayer p,CompoundTag value){p.getPersistentData().put(ClassData.ROOT_TAG,value);}
    private static boolean internal(ServerPlayer p){var a=ACTIVE.get();return a!=null&&(a.caster().equals(p.getUUID())||a.target().equals(p.getUUID()));}
    public static void hold(ServerPlayer p,Exception failure){
        if(p==null||!HOLDS.add(p))return;
        com.mojang.logging.LogUtils.getLogger().error("Player revival preserved for recovery: {}",p.getUUID(),failure);
        p.connection.disconnect(Component.literal("Your resurrection needs a save check. Reconnect to recover the recorded outcome."));
    }
    public static boolean blocked(ServerPlayer p){
        if(internal(p))return false;
        if(HOLDS.contains(p)||root(p).contains(CUSTODY))return true;
        try{return RevivalData.get(p.level().getServer()).pending(p.getUUID())!=null;}
        catch(RuntimeException failure){hold(p,failure);return true;}
    }
    public static boolean beforeInventoryChange(ServerPlayer p){return internal(p)||!HOLDS.contains(p)&&reconcile(p)&&!blocked(p);}
    public static boolean readyForCleanup(MinecraftServer server,List<UUID> owners){
        for(UUID owner:owners){var p=server.getPlayerList().getPlayer(owner);if(p!=null&&blocked(p)&&!reconcile(p))return false;
            if(RevivalData.get(server).pending(owner)!=null)return false;}return true;
    }
    private static void targetSuccess(ServerPlayer target,RevivalPlan plan){
        var next=root(target).copy();next.remove(CUSTODY);next.put(RECEIPT,plan.receipt(target.getUUID(),true));root(target,next);
    }
    public static boolean execute(ServerPlayer caster,ServerPlayer target,long run,MercenaryResurrectionState.Death death){
        if(ACTIVE.get()!=null||blocked(caster)||blocked(target)||MercenaryResurrection.respawning())return false;
        var server=caster.level().getServer();ServerPlayer actualTarget=target;
        try{
            var before=root(caster).copy();var ledger=ClassResourceLedger.forRun(before,run);
            if(ledger.amount(ClassResourceKind.BREWING_SUPPLIES)<RevivalPlan.COST)return false;
            // Normalize old/default ledger shape before binding the native owner reservation.
            root(caster,ledger.applyTo(before));var plan=RevivalPlan.create(caster.getUUID(),target.getUUID(),death.id(),run,ledger);
            var data=RevivalData.get(server);data.reserve(plan);
            if(!data.flushVerified())throw new IllegalStateException("Revival prepare not verified");
            ACTIVE.set(plan);
            for(var owner:List.of(caster,target)){
                var n=root(owner).copy();n.put(CUSTODY,plan.reservation(owner.getUUID()));root(owner,n);
                if(!PlayerSaveProof.save(owner))throw new IllegalStateException("Revival owner reservation not verified");
            }
            var deaths=D1RunData.get(server);var current=MercenaryResurrectionState.death(deaths,run,target.getUUID());
            if(current==null||!current.id().equals(death.id()))throw new IllegalStateException("Latest death changed before claim");
            MercenaryResurrectionState.clearDeath(deaths,run,target.getUUID());
            if(!deaths.flushVerified())throw new IllegalStateException("Latest death claim not verified");
            actualTarget=MercenaryResurrection.perform(target,death,caster.getGameProfile().name(),p->targetSuccess(p,plan));
            if(actualTarget!=null){
                // The same marker is set before native respawn event dispatch, before a native disconnect save.
                if(!PlayerSaveProof.saveWithLocation(actualTarget))throw new IllegalStateException("Successful revival owner save not verified");
                data.decide(plan.id(),true);
            }else{
                actualTarget=server.getPlayerList().getPlayer(target.getUUID());data.decide(plan.id(),false);
            }
            if(!data.flushVerified())throw new IllegalStateException("Revival outcome not verified");
            ACTIVE.remove();
            boolean targetDone=actualTarget!=null&&reconcile(actualTarget);
            boolean casterDone=reconcile(caster);
            if(casterDone)ClassResourceService.pulse(caster,server.getTickCount(),false);
            return targetDone&&casterDone&&plan.receipt(target.getUUID(),true).equals(root(actualTarget).getCompoundOrEmpty(RECEIPT));
        }catch(RuntimeException failure){
            hold(caster,failure);hold(server.getPlayerList().getPlayer(target.getUUID()),failure);return false;
        }finally{ACTIVE.remove();}
    }
    public static boolean reconcile(ServerPlayer p){
        if(internal(p))return true;if(HOLDS.contains(p))return false;
        try{
            var server=p.level().getServer();var data=RevivalData.get(server);var plan=data.pending(p.getUUID());
            if(plan==null){if(root(p).contains(CUSTODY))throw new IllegalStateException("Orphan revival reservation");return true;}
            var custody=root(p).getCompoundOrEmpty(CUSTODY);var receipt=root(p).getCompoundOrEmpty(RECEIPT);
            if(plan.decision()==0){
                if(p.getUUID().equals(plan.caster())){
                    var target=server.getPlayerList().getPlayer(plan.target());
                    if(target==null||target.hasDisconnected())return false;
                    if(!reconcile(target))return false;plan=data.pending(p.getUUID());
                    if(plan==null)return true;
                }else{
                    boolean succeeded=plan.receipt(p.getUUID(),true).equals(receipt);
                    if(succeeded&&!custody.isEmpty()||!succeeded&&!custody.isEmpty()&&!custody.equals(plan.reservation(p.getUUID())))
                        throw new IllegalStateException("Revival target proof differs from plan");
                    data.decide(plan.id(),succeeded);plan=data.pending(p.getUUID());
                }
            }
            boolean success=plan.decision()==1;
            if(!data.flushVerified())throw new IllegalStateException("Revival decision not verified");
            if(!plan.receipt(p.getUUID(),success).equals(receipt)){
                if(plan.acknowledged(p.getUUID()))throw new IllegalStateException("Acknowledged revival receipt missing");
                if(success){
                    if(p.getUUID().equals(plan.target()))throw new IllegalStateException("Successful target receipt missing");
                    if(!custody.equals(plan.reservation(p.getUUID()))||!root(p).getCompoundOrEmpty(ClassResourceLedger.KEY).equals(plan.tag("before")))
                        throw new IllegalStateException("Revival caster proof differs from plan");
                    var n=root(p).copy();n.put(ClassResourceLedger.KEY,plan.tag("after"));root(p,n);
                }else if(!custody.isEmpty()&&!custody.equals(plan.reservation(p.getUUID())))throw new IllegalStateException("Cancelled revival custody differs");
                var n=root(p).copy();n.remove(CUSTODY);n.put(RECEIPT,plan.receipt(p.getUUID(),success));root(p,n);
            }else if(!custody.isEmpty())throw new IllegalStateException("Settled revival still has custody");
            if(!PlayerSaveProof.save(p))throw new IllegalStateException("Revival settlement not verified");
            data.acknowledge(plan.id(),p.getUUID());
            if(!data.flushVerified())throw new IllegalStateException("Revival acknowledgement not verified");
            return true;
        }catch(RuntimeException failure){hold(p,failure);return false;}
    }
    public static void stop(){HOLDS.clear();ACTIVE.remove();}
}
