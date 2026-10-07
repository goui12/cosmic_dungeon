package net.goui.cosmicdungeon.playerclass.resource;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.InventoryTransactionGuard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Ephemeral consent only. Server thread; deduplicated per requester/resource/donor and bounded per inbox/server. */
public final class SupplyRequests {
    private static final int MAX_REQUESTS=512;
    private static final Map<MinecraftServer,State> STATES=new WeakHashMap<>();
    private record Request(UUID id,long run,UUID requester,UUID donor,ClassResourceKind kind){}
    private static final class Session{
        SupplyRequestPayloads.View sent;long revision,lastAction=Long.MIN_VALUE;
        List<ItemStack> inventory=List.of();Map<UUID,ResourceRecycling.Plan> quotes=Map.of();
    }
    private static final class State{
        final Map<UUID,Request> requests=new LinkedHashMap<>();
        final Map<UUID,Session> sessions=new HashMap<>();
        final Set<UUID> warned=new HashSet<>();
    }
    private SupplyRequests(){}
    private static State state(MinecraftServer server){return STATES.computeIfAbsent(server,key->new State());}
    private static Session session(ServerPlayer p){return state(p.level().getServer()).sessions.computeIfAbsent(p.getUUID(),key->new Session());}
    private static boolean live(ServerPlayer p){return p!=null&&p.connection!=null&&!p.hasDisconnected()&&p.isAlive()&&!p.isDeadOrDying();}
    private static boolean inventoryReady(ServerPlayer p){
        return live(p)&&p.containerMenu==p.inventoryMenu&&p.inventoryMenu.getCarried().isEmpty()&&!InventoryTransactionGuard.blocked(p);
    }
    private static ClassResourceLedger ledger(ServerPlayer p,long run){
        return ClassResourceLedger.forRun(p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG),run);
    }
    private static void ledger(ServerPlayer p,ClassResourceLedger value){
        p.getPersistentData().put(ClassData.ROOT_TAG,value.applyTo(p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG)));
    }
    private static boolean valid(MinecraftServer server,Request request){
        var requester=server.getPlayerList().getPlayer(request.requester());var donor=server.getPlayerList().getPlayer(request.donor());
        if(!live(requester)||!live(donor)||requester==donor||!request.kind().classId().equals(ClassData.getClassId(requester)))return false;
        var a=ClassResourceService.activeRun(requester).orElse(null);var b=ClassResourceService.activeRun(donor).orElse(null);
        return a!=null&&b!=null&&a.runId()==request.run()&&b.runId()==request.run()
                &&a.containsPlayer(request.donor())&&a.containsPlayer(request.requester())
                &&ledger(requester,request.run()).amount(request.kind())<600;
    }
    private static boolean hasOutgoing(State state,UUID owner,ClassResourceKind kind,long run){
        return state.requests.values().stream().anyMatch(r->r.run()==run&&r.requester().equals(owner)&&r.kind()==kind);
    }
    private static boolean canRequest(ServerPlayer p,State state,DungeonRunRegistryData.RunRecord run){
        var kind=ClassResourceKind.forClass(ClassData.getClassId(p)).orElse(null);
        return kind!=null&&inventoryReady(p)&&ledger(p,run.runId()).amount(kind)<600
                &&!hasOutgoing(state,p.getUUID(),kind,run.runId())
                &&run.orderedPlayers().stream().filter(id->!id.equals(p.getUUID())).map(id->p.level().getServer().getPlayerList().getPlayer(id))
                .anyMatch(peer->live(peer)&&ClassResourceService.activeRun(peer).map(other->other.runId()==run.runId()).orElse(false));
    }
    private static void prune(MinecraftServer server){
        state(server).requests.values().removeIf(request->{
            try{return !valid(server,request);}catch(RuntimeException invalid){return true;}
        });
    }
    public static void syncAll(MinecraftServer server){
        prune(server);for(var p:server.getPlayerList().getPlayers())sync(p,false);
    }
    private static void warn(ServerPlayer p,RuntimeException error){
        if(state(p.level().getServer()).warned.add(p.getUUID()))
            com.mojang.logging.LogUtils.getLogger().error("Supply requests disabled without changing items for {}",p.getUUID(),error);
    }
    public static void sync(ServerPlayer p,boolean force){
        try{buildView(p,force);}catch(RuntimeException error){warn(p,error);send(p,SupplyRequestPayloads.View.empty(),force);}
    }
    private static void buildView(ServerPlayer p,boolean force){
        var state=state(p.level().getServer());var session=session(p);var run=ClassResourceService.activeRun(p).orElse(null);
        if(run==null){session.inventory=List.of();session.quotes=Map.of();send(p,SupplyRequestPayloads.View.empty(),force);return;}
        var cards=new ArrayList<SupplyRequestPayloads.Card>();var quotes=new LinkedHashMap<UUID,ResourceRecycling.Plan>();
        boolean ready=inventoryReady(p);
        for(var request:state.requests.values()){
            if(!request.donor().equals(p.getUUID())||!valid(p.level().getServer(),request))continue;
            var owner=p.level().getServer().getPlayerList().getPlayer(request.requester());
            var plan=ResourceRecycling.plan(p.getInventory(),ledger(owner,run.runId()).amount(request.kind()),stack->stack.is(request.kind().tag()));
            var items=plan.changes().stream().map(change->new SupplyRequestPayloads.Ingredient(
                    change.before().copyWithCount(change.before().getCount()-change.after().getCount()))).toList();
            String name=owner.getGameProfile().name();if(name.length()>64)name=name.substring(0,64);
            cards.add(new SupplyRequestPayloads.Card(request.id(),owner.getUUID(),name,request.kind().classId(),request.kind().id(),
                    plan.credit(),items,ready&&inventoryReady(owner)&&plan.credit()>0));quotes.put(request.id(),plan);
            if(cards.size()==SupplyRequestPayloads.MAX_CARDS)break;
        }
        session.inventory=cards.isEmpty()?List.of():List.copyOf(ChopTravelRecovery.inventory(p));session.quotes=Map.copyOf(quotes);
        var next=new SupplyRequestPayloads.View(run.runId(),session.revision,true,live(p),canRequest(p,state,run),cards);
        if(session.sent==null||!next.equals(session.sent)){
            session.revision=Math.incrementExact(session.revision);
            next=new SupplyRequestPayloads.View(run.runId(),session.revision,true,live(p),next.canRequest(),cards);
        }
        send(p,next,force);
    }
    private static void send(ServerPlayer p,SupplyRequestPayloads.View view,boolean force){
        var session=session(p);if(force||!view.equals(session.sent)){ModNetwork.sendTo(p,view);session.sent=view;}
    }
    private static boolean inventoryMatches(ServerPlayer p,List<ItemStack> expected){
        if(expected.size()!=p.getInventory().getContainerSize())return false;
        for(int i=0;i<expected.size();i++)if(!ItemStack.matches(expected.get(i),p.getInventory().getItem(i)))return false;return true;
    }
    private static void request(ServerPlayer p,DungeonRunRegistryData.RunRecord run){
        var state=state(p.level().getServer());if(!canRequest(p,state,run))return;
        var kind=ClassResourceKind.forClass(ClassData.getClassId(p)).orElseThrow();
        for(UUID id:run.orderedPlayers()){
            if(id.equals(p.getUUID()))continue;
            var donor=p.level().getServer().getPlayerList().getPlayer(id);
            if(!live(donor)||ClassResourceService.activeRun(donor).map(other->other.runId()!=run.runId()).orElse(true))continue;
            if(state.requests.size()>=MAX_REQUESTS)break;
            if(state.requests.values().stream().filter(r->r.donor().equals(id)).count()>=SupplyRequestPayloads.MAX_CARDS)continue;
            if(state.requests.values().stream().anyMatch(r->r.run()==run.runId()&&r.requester().equals(p.getUUID())&&r.donor().equals(id)&&r.kind()==kind))continue;
            var value=new Request(UUID.randomUUID(),run.runId(),p.getUUID(),id,kind);state.requests.put(value.id(),value);
        }
    }
    /** Reuse only quoted slots/counts; prior accepted cards in this same server call may reduce the remaining stacks. */
    public static ResourceRecycling.Plan remaining(net.minecraft.world.Container inventory,ResourceRecycling.Plan quote,int headroom,ClassResourceKind kind){
        return remaining(inventory,quote,headroom,stack->stack.is(kind.tag()));
    }
    public static ResourceRecycling.Plan remaining(net.minecraft.world.Container inventory,ResourceRecycling.Plan quote,int headroom,
            java.util.function.Predicate<ItemStack> eligible){
        if(headroom<0||headroom>600)throw new IllegalArgumentException("Invalid headroom");
        var changes=new ArrayList<ResourceRecycling.Change>();int credit=0;
        for(var change:quote.changes()){
            if(headroom<=0)break;
            var current=inventory.getItem(change.slot());
            if(current.isEmpty()||!eligible.test(current)||!ItemStack.isSameItemSameComponents(current,change.before()))continue;
            int consent=change.before().getCount()-change.after().getCount();int take=Math.min(headroom,Math.min(consent,current.getCount()));
            if(take<=0)continue;var after=current.copy();after.shrink(take);
            changes.add(new ResourceRecycling.Change(change.slot(),current.copy(),after));credit+=take;headroom-=take;
        }return new ResourceRecycling.Plan(changes,credit);
    }
    public static void action(ServerPlayer p,SupplyRequestPayloads.Action action){
        var server=p.level().getServer();var state=state(server);var session=session(p);
        boolean changed=false;
        try{
            long now=server.getTickCount();
            if(session.lastAction!=Long.MIN_VALUE&&now>=session.lastAction&&now-session.lastAction<5)return;
            session.lastAction=now;prune(server);
            var run=ClassResourceService.activeRun(p).orElse(null);
            if(run==null||run.runId()!=action.runId()||session.sent==null||session.sent.runId()!=action.runId()
                    ||session.sent.revision()!=action.revision()||!live(p))return;
            if(action.decision()==SupplyRequestPayloads.Decision.REQUEST){int count=state.requests.size();request(p,run);changed=state.requests.size()!=count;return;}
            var visible=session.sent.cards().stream().map(SupplyRequestPayloads.Card::requestId).toList();
            if(!visible.containsAll(action.requestIds()))return;
            var ordered=visible.stream().filter(action.requestIds()::contains).toList();
            boolean accept=action.decision()==SupplyRequestPayloads.Decision.ACCEPT||action.decision()==SupplyRequestPayloads.Decision.ACCEPT_ALL;
            if(!accept){changed=true;ordered.forEach(id->{var r=state.requests.get(id);if(r!=null&&r.donor().equals(p.getUUID()))state.requests.remove(id);});return;}
            if(!inventoryReady(p)||!inventoryMatches(p,session.inventory))return;
            var quotes=session.quotes;var expected=List.copyOf(ChopTravelRecovery.inventory(p));
            for(UUID id:ordered){
                var request=state.requests.get(id);if(request==null||!request.donor().equals(p.getUUID())||!valid(server,request))continue;
                var recipient=server.getPlayerList().getPlayer(request.requester());var quote=quotes.get(id);
                if(quote==null||!inventoryReady(p)||!inventoryReady(recipient))continue;
                if(!InventoryTransactionGuard.beforeCurrentInventoryAction(p)||!InventoryTransactionGuard.beforeCurrentInventoryAction(recipient))break;
                if(!inventoryMatches(p,expected))break;
                if(!valid(server,request)||!inventoryReady(p)||!inventoryReady(recipient))continue;
                var resource=ledger(recipient,run.runId());var plan=remaining(p.getInventory(),quote,600-resource.amount(request.kind()),request.kind());
                if(plan.credit()==0)continue;
                // Normalize additive old resource data before reserving its exact owner image.
                ledger(recipient,resource);
                var before=ChopTravelRecovery.inventory(p);var after=ChopTravelRecovery.inventory(p);
                for(var change:plan.changes())after.set(change.slot(),change.after().copy());
                var transaction=SupplyTransferPlan.create(p.getUUID(),recipient.getUUID(),run.runId(),request.kind(),plan.credit(),
                        ChopTravelRecovery.encode(p,before),ChopTravelRecovery.encode(p,after),resource);
                changed=true;state.requests.remove(id); // A consent token is single-use even if native settlement must recover.
                if(!SupplyTransfers.execute(p,recipient,transaction))break;
                expected=List.copyOf(ChopTravelRecovery.inventory(p));ClassResourceService.pulse(recipient,now,false);
            }
        }catch(RuntimeException error){warn(p,error);}
        finally{
            prune(server);
            if(changed)for(var viewer:server.getPlayerList().getPlayers())sync(viewer,viewer==p);
            else sync(p,true);
        }
    }
    public static void invalidate(ServerPlayer p){
        var state=state(p.level().getServer());var session=session(p);
        session.sent=null;session.inventory=List.of();session.quotes=Map.of();
        state.requests.values().removeIf(request->request.requester().equals(p.getUUID())||request.donor().equals(p.getUUID()));
    }
    public static void forget(ServerPlayer p){
        var state=state(p.level().getServer());state.sessions.remove(p.getUUID());state.warned.remove(p.getUUID());
        state.requests.values().removeIf(request->request.requester().equals(p.getUUID())||request.donor().equals(p.getUUID()));
    }
    public static void stopped(){STATES.clear();}
}
