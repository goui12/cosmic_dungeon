package net.goui.cosmicdungeon.playerclass.resource;

import java.util.*;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Server-thread resource authority. Regeneration touches memory only; recycling commits one owner snapshot. */
public final class ClassResourceService {
    private static final Map<ServerPlayer,Session> SESSIONS=new WeakHashMap<>();
    private static final Set<ServerPlayer> HOLDS=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<ServerPlayer> WARNED=Collections.newSetFromMap(new WeakHashMap<>());
    private static final class Session{
        final OnlineResourceClock clock=new OnlineResourceClock();
        ClassResourcePayloads.View sent;long lastAction=Long.MIN_VALUE;
    }
    private record Context(long run,ClassResourceKind kind){}
    private ClassResourceService(){}
    private static Session session(ServerPlayer p){return SESSIONS.computeIfAbsent(p,key->new Session());}
    private static CompoundTag root(ServerPlayer p){return p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);}
    private static void write(ServerPlayer p,ClassResourceLedger ledger){
        p.getPersistentData().put(ClassData.ROOT_TAG,ledger.applyTo(root(p)));
    }
    public static boolean blocked(ServerPlayer p){return HOLDS.contains(p)||SupplyTransfers.blocked(p);}
    public static Optional<DungeonRunRegistryData.RunRecord> activeRun(ServerPlayer p){
        if(blocked(p)||p.isSpectator()||AccessPolicy.isDeveloper(p))return Optional.empty();
        return DungeonRunRegistryData.get(p.level().getServer()).findRunForInstanceDimension(p.level().dimension())
                .filter(r->r.stateEnum()==DungeonRunState.ACTIVE&&r.containsPlayer(p.getUUID())&&!r.isCompletionExited(p.getUUID()))
                .filter(r->!r.dungeonId().equals("dungeon_1")||!D1RunData.get(p.level().getServer()).sealed(r.runId()));
    }
    private static Context context(ServerPlayer p){
        if(blocked(p)||p.isSpectator()||AccessPolicy.isDeveloper(p))return null;
        var kind=ClassResourceKind.forClass(ClassData.getClassId(p)).orElse(null);if(kind==null)return null;
        var run=activeRun(p).orElse(null);
        return run==null?null:new Context(run.runId(),kind);
    }
    private static void invalid(ServerPlayer p,RuntimeException error){
        if(WARNED.add(p))com.mojang.logging.LogUtils.getLogger().error("Class resource data preserved without changes for {}",p.getUUID(),error);
        send(p,ClassResourcePayloads.View.empty());session(p).clock.clear();
    }
    public static void forget(ServerPlayer p){SESSIONS.remove(p);WARNED.remove(p);}
    public static void stop(){SESSIONS.clear();HOLDS.clear();WARNED.clear();}
    public static void pulse(ServerPlayer p,long now,boolean regenerate){
        var context=context(p);var session=session(p);
        if(context==null){session.clock.clear();send(p,ClassResourcePayloads.View.empty());return;}
        try{
            var ledger=ClassResourceLedger.forRun(root(p),context.run());
            if(regenerate&&session.clock.elapsed(context.run(),context.kind().id(),now))
                ledger=ledger.credit(context.kind(),1);
            else if(!regenerate)session.clock.bind(context.run(),context.kind().id(),now);
            if(!ledger.image().equals(root(p).getCompoundOrEmpty(ClassResourceLedger.KEY)))write(p,ledger);
            send(p,view(p,context,ledger));
        }catch(RuntimeException error){invalid(p,error);}
    }
    private static ClassResourcePayloads.View view(ServerPlayer p,Context context,ClassResourceLedger ledger){
        int amount=ledger.amount(context.kind());boolean alive=p.isAlive()&&!p.isDeadOrDying();
        boolean recycle=alive&&amount<600&&p.containerMenu==p.inventoryMenu&&p.inventoryMenu.getCarried().isEmpty()
                &&ResourceRecycling.any(p.getInventory(),context.kind());
        return new ClassResourcePayloads.View(context.run(),context.kind().id(),amount,600,true,alive,recycle,ledger.revision());
    }
    private static void send(ServerPlayer p,ClassResourcePayloads.View view){
        var session=session(p);if(view.equals(session.sent))return;
        ModNetwork.sendTo(p,view);session.sent=view;
    }
    public static void recycle(ServerPlayer p,ClassResourcePayloads.Recycle request){
        try{recycleAttempt(p,request);}
        finally{acknowledge(p,request);}
    }
    private static void acknowledge(ServerPlayer p,ClassResourcePayloads.Recycle request){
        var context=context(p);
        if(context==null){send(p,ClassResourcePayloads.View.empty());return;}
        try{
            var ledger=ClassResourceLedger.forRun(root(p),context.run());
            if(context.run()==request.runId()&&context.kind().id().equals(request.resourceId())
                    &&ledger.revision()==request.revision()){
                ledger=ledger.nextRevision();write(p,ledger);
            }
            send(p,view(p,context,ledger));
        }catch(RuntimeException error){invalid(p,error);}
    }
    private static void recycleAttempt(ServerPlayer p,ClassResourcePayloads.Recycle request){
        var session=session(p);long now=p.level().getServer().getTickCount();
        if(session.lastAction!=Long.MIN_VALUE&&now>=session.lastAction&&now-session.lastAction<5)return;
        session.lastAction=now;
        var context=context(p);
        if(context==null||context.run()!=request.runId()||!context.kind().id().equals(request.resourceId())
                ||!p.isAlive()||p.isDeadOrDying()||p.containerMenu!=p.inventoryMenu
                ||!p.inventoryMenu.getCarried().isEmpty()||InventoryTransactionGuard.blocked(p))return;
        ClassResourceLedger ledger;
        try{ledger=ClassResourceLedger.forRun(root(p),context.run());}
        catch(RuntimeException invalid){invalid(p,invalid);return;}
        if(ledger.revision()!=request.revision()){send(p,view(p,context,ledger));return;}
        if(!InventoryTransactionGuard.beforeCurrentInventoryAction(p))return;
        // Recheck after any existing transaction reconciliation; no container or stale state may become authorization.
        context=context(p);
        if(context==null||context.run()!=request.runId()||!context.kind().id().equals(request.resourceId())
                ||p.containerMenu!=p.inventoryMenu||!p.inventoryMenu.getCarried().isEmpty()||!p.isAlive())return;
        try{ledger=ClassResourceLedger.forRun(root(p),context.run());}
        catch(RuntimeException invalid){invalid(p,invalid);return;}
        if(ledger.revision()!=request.revision())return;
        var kind=context.kind();
        var plan=ResourceRecycling.plan(p.getInventory(),ledger.amount(kind),stack->stack.is(kind.tag()));
        var before=root(p).copy();
        try{
            var next=ledger.credit(context.kind(),plan.credit()).nextRevision();
            if(!ResourceRecycling.apply(p.getInventory(),plan))return;
            write(p,next);
            PlayerSaveProof.snapshot(p); // Serialization must succeed before a disk write can begin.
        }catch(RuntimeException stagingFailure){
            ResourceRecycling.rollbackBeforeSave(p.getInventory(),plan);p.getPersistentData().put(ClassData.ROOT_TAG,before);
            invalid(p,stagingFailure);return;
        }
        if(plan.credit()>0&&SingleItemCommit.finish(()->PlayerSaveProof.save(p),()->{})!=SingleItemCommit.Result.COMMITTED){
            // Save outcome is uncertain: keep paired item/resource state, block replacement, and reconnect.
            HOLDS.add(p);
            p.connection.disconnect(Component.literal("Your resource recycling needs a save check. Please reconnect."));
            return;
        }
        p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();
        pulse(p,now,false);
    }
}
