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

/** Target success is durable before committing the free cooldown (or settling a legacy debit). */
public final class TheurgistRevival {
    public static final String CUSTODY="theurgist_revival_reservation_v1",RECEIPT="theurgist_revival_receipt_v1";
    private static final ThreadLocal<RevivalPlan> ACTIVE=new ThreadLocal<>();
    private static final Set<ServerPlayer> HOLDS=Collections.newSetFromMap(new WeakHashMap<>());
    private TheurgistRevival(){}
    private static CompoundTag root(ServerPlayer p){return p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);}
    private static void root(ServerPlayer p,CompoundTag value){p.getPersistentData().put(ClassData.ROOT_TAG,value);}
    private static void validateReceipt(ServerPlayer player,CompoundTag receipt){
        if(receipt.isEmpty())return;
        try{
            var caster=UUID.fromString(receipt.getStringOr("caster",""));var target=UUID.fromString(receipt.getStringOr("target",""));
            UUID.fromString(receipt.getStringOr("death",""));
            if(caster.equals(target)||!player.getUUID().equals(caster)&&!player.getUUID().equals(target)
                    ||receipt.getLongOr("run",0)<=0||receipt.getBoolean("success").isEmpty())
                throw new IllegalArgumentException("Unsupported revival receipt shape");
        }catch(RuntimeException unsupported){
            throw new IllegalStateException("Unsupported owner receipt field "+RECEIPT+"; original save requires review.",unsupported);
        }
    }
    /** Previous operation receipts may be superseded; this operation's recorded terminal outcome may not. */
    private static int currentReceipt(ServerPlayer owner,RevivalPlan plan,CompoundTag custody,CompoundTag receipt){
        if(receipt.isEmpty()||!UUID.fromString(receipt.getStringOr("id","")).equals(plan.id()))return 0;
        int outcome=plan.receipt(owner.getUUID(),true).equals(receipt)?1:plan.receipt(owner.getUUID(),false).equals(receipt)?2:0;
        if(outcome==0||!custody.isEmpty()||plan.decision()!=0&&outcome!=plan.decision())
            throw new IllegalStateException("Revival current-operation receipt conflicts with journal for "+owner.getUUID()+"; original save requires review");
        return outcome;
    }
    private record OwnerProof(CompoundTag owner,CompoundTag custody,int outcome){}
    /** Read only: an online peer's conflicting proof must not be hidden by this owner's settlement. */
    private static OwnerProof preflightOwner(ServerPlayer owner,RevivalPlan plan){
        var saved=PlayerSaveProof.ownerCompound(owner.getPersistentData(),ClassData.ROOT_TAG);
        var custody=PlayerSaveProof.ownerCompound(saved,CUSTODY);
        var receipt=PlayerSaveProof.ownerReceipt(saved,RECEIPT,owner.getUUID(),2,"id");
        validateReceipt(owner,receipt);
        if(!custody.isEmpty()&&!custody.equals(plan.reservation(owner.getUUID())))
            throw new IllegalStateException("Revival owner reservation conflicts before peer settlement; original saves require review");
        int outcome=currentReceipt(owner,plan,custody,receipt);
        if(outcome==0&&plan.acknowledged(owner.getUUID()))
            throw new IllegalStateException("Acknowledged revival peer receipt missing; original saves require review");
        if(plan.decision()==1&&outcome==0){
            if(owner.getUUID().equals(plan.target()))
                throw new IllegalStateException("Successful target peer receipt missing; original saves require review");
            if(!custody.equals(plan.reservation(owner.getUUID()))||!casterSource(saved,plan).equals(plan.tag("before")))
                throw new IllegalStateException("Successful revival caster peer proof differs from plan; original saves require review");
        }
        return new OwnerProof(saved,custody,outcome);
    }
    /** Inspect available peers in either arrival order before any decision, flush or acknowledgement. */
    private static void preflightAvailableOwners(MinecraftServer server,RevivalPlan plan){
        var caster=server.getPlayerList().getPlayer(plan.caster());
        var target=server.getPlayerList().getPlayer(plan.target());
        var casterProof=caster==null||caster.hasDisconnected()?null:preflightOwner(caster,plan);
        var targetProof=target==null||target.hasDisconnected()?null:preflightOwner(target,plan);
        if(plan.decision()!=0||casterProof==null)return; // Valid target proof may settle while its caster is offline.
        boolean alreadySucceeded=casterProof.outcome()==1;
        if(!casterSource(casterProof.owner(),plan).equals(plan.tag(alreadySucceeded?"after":"before")))
            throw new IllegalStateException("Revival caster source differs from plan before peer settlement");
        if(targetProof==null)return;
        int targetOutcome=targetProof.outcome()==1?1:2;
        if(casterProof.outcome()!=0&&casterProof.outcome()!=targetOutcome
                ||targetOutcome==1&&!alreadySucceeded&&!casterProof.custody().equals(plan.reservation(caster.getUUID())))
            throw new IllegalStateException("Revival owner outcomes conflict before peer settlement; original saves require review");
    }
    private static CompoundTag casterSource(CompoundTag owner,RevivalPlan plan){
        if(plan.schema()==2)return ResurrectionCooldown.read(owner);
        var source=PlayerSaveProof.ownerCompound(owner,ClassResourceLedger.KEY);
        ClassResourceLedger.forRun(owner,plan.run()); // Validate legacy shape without normalizing the saved image.
        return source;
    }
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
            var before=ResurrectionCooldown.read(root(caster));long now=server.overworld().getGameTime();
            if(ResurrectionCooldown.seconds(before,now)!=0)return false;
            var plan=RevivalPlan.free(caster.getUUID(),target.getUUID(),death.id(),run,before,now);
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
            var owner=PlayerSaveProof.ownerCompound(p.getPersistentData(),ClassData.ROOT_TAG);
            var custody=PlayerSaveProof.ownerCompound(owner,CUSTODY);
            var receipt=PlayerSaveProof.ownerReceipt(owner,RECEIPT,p.getUUID(),2,"id");validateReceipt(p,receipt);
            var server=p.level().getServer();var data=RevivalData.get(server);var plan=data.pending(p.getUUID());
            if(plan==null){if(root(p).contains(CUSTODY))throw new IllegalStateException("Orphan revival reservation");return true;}
            // Reject this owner's unsupported reservation before recursively settling the peer.
            if(!custody.isEmpty()&&!custody.equals(plan.reservation(p.getUUID())))
                throw new IllegalStateException("Unsupported owner proof field "+CUSTODY+"; differs from the world plan. Original save requires review.");
            currentReceipt(p,plan,custody,receipt);
            preflightAvailableOwners(server,plan);
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
                    if(!custody.equals(plan.reservation(p.getUUID()))||!casterSource(root(p),plan).equals(plan.tag("before")))
                        throw new IllegalStateException("Revival caster proof differs from plan");
                    var n=root(p).copy();n.put(plan.stateKey(),plan.tag("after"));root(p,n);
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
