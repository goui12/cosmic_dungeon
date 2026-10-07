package net.goui.cosmicdungeon.playerclass.theurgist;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.mercenary.*;
import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.resource.*;
import net.goui.cosmicdungeon.transaction.InventoryTransactionGuard;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Ephemeral explicit offers, current-death tokens and server-thread class/resource permissions. */
public final class TheurgistActions {
    private record Pending(UUID id,long run,UUID caster,UUID target,UUID death){}
    private static final class Session {TheurgistPayloads.View sent;long revision,lastAction=Long.MIN_VALUE;}
    private static final class State{
        final Map<UUID,Session> sessions=new HashMap<>();
        final Map<UUID,Pending> offers=new HashMap<>();
        final Set<UUID> warned=new HashSet<>();
    }
    private static final Map<MinecraftServer,State> STATES=new WeakHashMap<>();
    private TheurgistActions(){}
    private static State state(MinecraftServer s){return STATES.computeIfAbsent(s,k->new State());}
    private static Session session(ServerPlayer p){return state(p.level().getServer()).sessions.computeIfAbsent(p.getUUID(),k->new Session());}
    private static boolean connected(ServerPlayer p){return p!=null&&p.connection!=null&&!p.hasDisconnected()&&p.level().getServer().getPlayerList().getPlayer(p.getUUID())==p;}
    private static boolean alive(ServerPlayer p){return connected(p)&&p.isAlive()&&!p.isDeadOrDying();}
    private static boolean caster(ServerPlayer p){return alive(p)&&ClassData.getClassId(p).equals("theurgist")&&ClassResourceService.activeRun(p).isPresent();}
    private static boolean ready(ServerPlayer p){return connected(p)&&InventoryTransactionGuard.beforeCurrentInventoryAction(p);}
    private static int amount(ServerPlayer p,long run){return ClassResourceLedger.forRun(p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG),run).amount(ClassResourceKind.BREWING_SUPPLIES);}
    private static MercenaryResurrectionState.Death death(ServerPlayer target,long run){
        return connected(target)&&target.isDeadOrDying()?MercenaryResurrectionState.death(D1RunData.get(target.level().getServer()),run,target.getUUID()):null;
    }
    private static boolean valid(MinecraftServer server,Pending offer){
        var c=server.getPlayerList().getPlayer(offer.caster());var t=server.getPlayerList().getPlayer(offer.target());
        if(!caster(c)||!connected(t)||!t.isDeadOrDying()||c==t)return false;
        var cr=MercenaryResurrection.deathRun(c);var tr=MercenaryResurrection.deathRun(t);
        if(cr==null||tr==null||cr.runId()!=offer.run()||tr.runId()!=offer.run()||amount(c,offer.run())<RevivalPlan.COST
                ||TheurgistRevival.blocked(c)||TheurgistRevival.blocked(t))return false;
        var death=death(t,offer.run());return death!=null&&death.id().equals(offer.death())&&tr.containsDimension(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.parse(death.dimension())));
    }
    private static Pending offer(ServerPlayer p){
        var state=state(p.level().getServer());var offer=state.offers.get(p.getUUID());
        if(offer!=null&&!valid(p.level().getServer(),offer)){state.offers.remove(p.getUUID());return null;}return offer;
    }
    public static boolean hasOffer(ServerPlayer p){try{return offer(p)!=null;}catch(RuntimeException invalid){return false;}}
    public static void deathNotice(ServerPlayer target,DungeonRunRegistryData.RunRecord run){
        var server=target.level().getServer();
        for(UUID id:run.orderedPlayers()){
            var c=server.getPlayerList().getPlayer(id);
            if(c!=target&&caster(c))c.sendSystemMessage(Component.literal(target.getGameProfile().name()+" has died. Open your inventory to offer resurrection (120 Brewing Supplies on acceptance)."));
        }
    }
    public static void invalidate(ServerPlayer p){
        var st=state(p.level().getServer());var session=st.sessions.get(p.getUUID());if(session!=null)session.sent=null;
        st.offers.values().removeIf(o->o.caster().equals(p.getUUID())||o.target().equals(p.getUUID()));
    }
    public static void forget(ServerPlayer p){invalidate(p);var st=state(p.level().getServer());st.sessions.remove(p.getUUID());st.warned.remove(p.getUUID());}
    public static void stop(){STATES.clear();}
    private static void prune(MinecraftServer server){state(server).offers.values().removeIf(o->!valid(server,o));}
    public static void syncAll(MinecraftServer server){
        for(var p:server.getPlayerList().getPlayers())sync(p,false);
    }
    public static void sync(ServerPlayer p,boolean acknowledge){
        if(!connected(p))return;
        var session=session(p);TheurgistPayloads.View view;
        try{
            var run=MercenaryResurrection.deathRun(p);
            if(run==null||TheurgistRevival.blocked(p))view=TheurgistPayloads.View.empty(session.revision);
            else{
                boolean cls=ClassData.getClassId(p).equals("theurgist"),living=alive(p),available=cls&&living&&ready(p);
                int balance=cls?amount(p,run.runId()):0;boolean space=available&&TheurgistCrafting.freeSlot(p)>=0;
                var targets=new ArrayList<TheurgistPayloads.Target>();
                if(cls&&living)for(UUID id:run.orderedPlayers()){
                    if(id.equals(p.getUUID())||targets.size()>=TheurgistPayloads.MAX_TARGETS)continue;
                    var t=p.level().getServer().getPlayerList().getPlayer(id);var tr=t==null?null:MercenaryResurrection.deathRun(t);
                    var death=tr!=null&&tr.runId()==run.runId()?death(t,run.runId()):null;
                    if(death!=null)targets.add(new TheurgistPayloads.Target(id,t.getGameProfile().name(),death.id(),
                            available&&balance>=RevivalPlan.COST&&ready(t)&&offer(t)==null));
                }
                var pending=offer(p);TheurgistPayloads.Offer shown=null;
                if(pending!=null){var c=p.level().getServer().getPlayerList().getPlayer(pending.caster());shown=new TheurgistPayloads.Offer(pending.id(),pending.caster(),c.getGameProfile().name(),pending.death());}
                view=new TheurgistPayloads.View(run.runId(),session.revision,cls,living,space&&balance>=20,space&&balance>=40,targets,shown);
            }
        }catch(RuntimeException error){
            if(state(p.level().getServer()).warned.add(p.getUUID()))com.mojang.logging.LogUtils.getLogger().error("Theurgist actions disabled without changing data: {}",p.getUUID(),error);
            view=TheurgistPayloads.View.empty(session.revision);
        }
        if(!acknowledge&&view.equals(session.sent))return;
        session.revision=Math.addExact(session.revision,1);
        view=new TheurgistPayloads.View(view.run(),session.revision,view.theurgist(),view.alive(),view.normal(),view.epic(),view.targets(),view.offer());
        session.sent=view;ModNetwork.sendTo(p,view);
    }
    public static void action(ServerPlayer p,TheurgistPayloads.Action request){
        var server=p.level().getServer();var session=session(p);boolean changed=false;
        try{
            long tick=server.getTickCount();
            if(session.lastAction!=Long.MIN_VALUE&&tick>=session.lastAction&&tick-session.lastAction<5)return;
            session.lastAction=tick;
            var sent=session.sent;var run=MercenaryResurrection.deathRun(p);
            if(!connected(p)||sent==null||run==null||request.run()!=run.runId()||request.revision()!=sent.revision())return;
            switch(request.kind()){
                case CRAFT,EPIC->{
                    if(!caster(p))return;
                    changed=TheurgistCrafting.craft(p,run.runId(),request.kind()==TheurgistPayloads.Kind.EPIC);
                }
                case OFFER->{
                    if(!caster(p)||!ready(p)||amount(p,run.runId())<RevivalPlan.COST
                            ||sent.targets().stream().noneMatch(t->t.player().equals(request.target())&&t.death().equals(request.token())&&t.available()))return;
                    var target=server.getPlayerList().getPlayer(request.target());if(target==null||!ready(target)||offer(target)!=null)return;
                    var candidate=new Pending(UUID.randomUUID(),run.runId(),p.getUUID(),target.getUUID(),request.token());
                    if(!valid(server,candidate))return;
                    state(server).offers.put(target.getUUID(),candidate);changed=true;
                    target.sendSystemMessage(Component.literal(p.getGameProfile().name()+" offers resurrection. Accept or decline on your death screen."));
                }
                case ACCEPT,DECLINE->{
                    var offer=offer(p);
                    if(offer==null||!offer.id().equals(request.token())||!offer.caster().equals(request.target())
                            ||sent.offer()==null||!sent.offer().id().equals(offer.id()))return;
                    if(request.kind()==TheurgistPayloads.Kind.DECLINE){state(server).offers.remove(p.getUUID());changed=true;return;}
                    var caster=server.getPlayerList().getPlayer(offer.caster());
                    if(!ready(caster)||!ready(p)||!valid(server,offer)||!InventoryTransactionGuard.beforeInventoryChange(p))return;
                    if(!valid(server,offer)||!ready(caster))return;
                    var death=death(p,run.runId());if(death==null)return;
                    state(server).offers.remove(p.getUUID());changed=true;
                    TheurgistRevival.execute(caster,p,run.runId(),death);
                }
            }
        }catch(RuntimeException failure){
            com.mojang.logging.LogUtils.getLogger().error("Theurgist action rejected for {}",p.getUUID(),failure);
        }finally{
            if(changed)syncAll(server);
            var current=server.getPlayerList().getPlayer(p.getUUID());if(current!=null)sync(current,true);
        }
    }
}
