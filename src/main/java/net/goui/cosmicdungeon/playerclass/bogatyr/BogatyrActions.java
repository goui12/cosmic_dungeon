package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.*;
import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.resource.*;
import net.goui.cosmicdungeon.transaction.InventoryTransactionGuard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** One loaded-owner roster lookup per second; packets carry changes only, never entity scans or client authority. */
public final class BogatyrActions {
    private static final Map<ServerPlayer,Session> SESSIONS=new WeakHashMap<>();
    private static final class Session{
        long revision,lastAction=Long.MIN_VALUE;BogatyrPayloads.View sent;
        List<BogatyrCommands.Plan> plans=List.of();
    }
    private static long generation;
    private BogatyrActions(){}
    public static void forget(ServerPlayer p){SESSIONS.remove(p);}
    public static void stop(){SESSIONS.clear();generation=0;}
    public static void syncAll(MinecraftServer server){for(var p:server.getPlayerList().getPlayers())sync(p,false);}
    public static void sync(ServerPlayer p,boolean force){
        var s=SESSIONS.computeIfAbsent(p,key->new Session());
        BogatyrPayloads.View next=BogatyrPayloads.View.empty(0);var plans=new ArrayList<BogatyrCommands.Plan>();
        try{
            var run=ClassResourceService.activeRun(p).orElse(null);
            if(run!=null&&"bogatyr".equals(ClassData.getClassId(p))&&BogatyrRunLifecycle.migrate(p.level().getServer())){
                var pack=BogatyrCommands.loaded(p,run.runId());
                var quotes=new ArrayList<BogatyrPayloads.Quote>();
                int amount=ClassResourceLedger.forRun(BogatyrCommands.root(p),run.runId()).amount(ClassResourceKind.KIBBLE);
                boolean able=p.isAlive()&&!p.isDeadOrDying()&&InventoryTransactionGuard.beforeCurrentInventoryAction(p);
                for(var kind:BogatyrPayloads.Kind.values()){
                    var plan=able?BogatyrCommands.plan(p,run.runId(),kind,pack,amount):new BogatyrCommands.Plan(run.runId(),kind,List.of());
                    plans.add(plan);quotes.add(new BogatyrPayloads.Quote(plan.count(),plan.cost(),plan.count()>0));
                }
                var mode=BogatyrCompanionData.get(p.level().getServer()).mode(p.getUUID(),run.runId());
                next=new BogatyrPayloads.View(run.runId(),0,pack.size(),quotes,mode.mode(),able&&mode.writable());
            }
        }catch(RuntimeException failure){
            com.mojang.logging.LogUtils.getLogger().error("Wolfpack view preserved for {}",p.getUUID(),failure);
        }
        boolean same=s.sent!=null&&s.sent.run()==next.run()&&s.sent.loaded()==next.loaded()&&s.sent.quotes().equals(next.quotes())&&s.plans.equals(plans)
                &&s.sent.mode()==next.mode()&&s.sent.modesEnabled()==next.modesEnabled();
        if(force||!same){
            if(generation==Long.MAX_VALUE)throw new IllegalStateException("Wolfpack generation exhausted");
            s.revision=++generation;s.plans=List.copyOf(plans);
            s.sent=new BogatyrPayloads.View(next.run(),s.revision,next.loaded(),next.quotes(),next.mode(),next.modesEnabled());ModNetwork.sendTo(p,s.sent);
        }
    }
    public static void mode(ServerPlayer p,BogatyrPayloads.ModeAction request){
        var s=SESSIONS.get(p);
        try{
            if(s==null||s.sent==null||!s.sent.modesEnabled()||s.sent.run()!=request.run()||s.sent.revision()!=request.revision())return;
            BogatyrModes.select(p,request.run(),request.mode());
        }finally{sync(p,true);}
    }
    public static void action(ServerPlayer p,BogatyrPayloads.Action request){
        var s=SESSIONS.get(p);
        try{
            if(s==null||s.sent==null||s.sent.run()!=request.run()||s.sent.revision()!=request.revision()||s.plans.size()!=4)return;
            long now=p.level().getServer().getTickCount();
            if(s.lastAction!=Long.MIN_VALUE&&now>=s.lastAction&&now-s.lastAction<20)return;
            s.lastAction=now;
            // execute computes current eligibility and placement again; changed previews are harmless no-ops.
            BogatyrCommands.execute(p,s.plans.get(request.kind().ordinal()));
        }finally{sync(p,true);}
    }
}
