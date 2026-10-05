package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1Members;
import net.goui.cosmicdungeon.economy.*;
import net.goui.cosmicdungeon.network.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server-thread recovery requests; reuses the original entity, run locator and account journal. */
public final class MercenaryRevival {
    private record Request(long run,MercenaryContract contract,MercenaryRest rest,long announced){}
    private static final Map<UUID,Request> REQUESTS=new HashMap<>();
    private static final Map<UUID,Request> UNVERIFIED=new HashMap<>();
    private MercenaryRevival(){}
    public static void clear(){REQUESTS.clear();UNVERIFIED.clear();}
    public static long price(MercenaryContract contract){return contract.fee()/2;}
    public static boolean admitted(DungeonRunRegistryData.RunRecord run,MercenaryContract contract,
            MercenaryRest rest,UUID actor,String dimension){
        return contract!=null&&rest!=null&&MercenaryLifecycle.admitted(run,contract,contract.id(),rest.dimension())
                &&run.containsPlayer(actor)&&!run.isCompletionExited(actor)
                &&run.dungeonDimensionIds().contains(dimension)&&rest.dimension().equals(dimension);
    }
    public static boolean paid(MinecraftServer server,long run,UUID id,MercenaryRest rest){
        return !UNVERIFIED.containsKey(MercenaryRevivePayment.id(run,id,rest.death(),rest.until()))
                &&MercenaryRevivePayment.paid(PlayerCurrencyData.get(server),run,id,rest.death(),rest.until());
    }
    private static Request current(ServerPlayer actor,UUID mercenary,long deadline){
        var run=D1Members.run(actor.level()).orElse(null);
        if(run==null||!D1Members.inside(actor,run))return null;
        var contract=run.mercenaries().stream().filter(c->c.id().equals(mercenary)).findFirst().orElse(null);
        if(contract==null)return null;
        var rest=run.mercenaryRests().get(mercenary);
        var nativeEntity=actor.level().getEntity(mercenary);
        if(nativeEntity instanceof MercenaryEntity entity){
            if(entity.runId()!=run.runId()||!contract.equals(entity.contract())||!entity.dormant())return null;
            rest=entity.rest();
        }
        if(rest==null||rest.until()!=deadline||!admitted(run,contract,rest,actor.getUUID(),actor.level().dimension().location().toString()))return null;
        var owner=actor.level().getServer().getPlayerList().getPlayer(contract.hirer());
        if(owner==null||owner.level()!=actor.level()||!D1Members.inside(owner,run))return null;
        var recovery=PendingDungeonRecoveryData.get(actor.level().getServer());
        var handoff=recovery.handoff(contract.hirer());
        if(recovery.completed(contract.hirer())>=run.runId()
                ||handoff!=null&&handoff.run()==run.runId()&&handoff.kind().equals("cleanup"))return null;
        return new Request(run.runId(),contract,rest,0);
    }
    private static void message(ServerPlayer player,String text){player.sendSystemMessage(Component.literal(text));}
    public static void action(ServerPlayer player,PartyPayloads.Action action){
        UUID id;try{id=UUID.fromString(action.target());}catch(IllegalArgumentException invalid){return;}
        var run=D1Members.run(player.level()).orElse(null);
        var own=run==null?null:run.mercenaries().stream().filter(c->c.hirer().equals(player.getUUID())).findFirst().orElse(null);
        var request=own==null?null:current(player,own.id(),action.revision());
        if(request==null||!id.equals(MercenaryRevivePayment.id(request.run(),own.id(),request.rest().death(),request.rest().until()))){
            message(player,"That revival is no longer available.");return;
        }
        if(action.action().equals("revive"))pay(player,request,true);
        else if(action.action().equals("revive_help"))ask(player,request);
    }
    private static void ask(ServerPlayer player,Request request){
        var server=player.level().getServer();long now=server.overworld().getGameTime();
        if(request.rest().due(now)||paid(server,request.run(),request.contract().id(),request.rest())){
            message(player,"This companion is already awaiting a safe return.");return;
        }
        REQUESTS.entrySet().removeIf(e->e.getValue().rest().due(now)
                ||DungeonRunRegistryData.get(server).getRun(e.getValue().run()).filter(r->r.stateEnum()==DungeonRunState.ACTIVE).isEmpty());
        UUID token=MercenaryRevivePayment.id(request.run(),request.contract().id(),request.rest().death(),request.rest().until());
        var previous=REQUESTS.get(token);
        if(previous!=null&&now-previous.announced()<600){message(player,"The group has already been asked. Please wait before asking again.");return;}
        if(previous==null&&REQUESTS.size()>=30){message(player,"Please try asking the group again shortly.");return;}
        REQUESTS.put(token,new Request(request.run(),request.contract(),request.rest(),now));
        var text=Component.literal("click here to donate to revive "+request.contract().name()).withStyle(style->style
                .withColor(net.minecraft.ChatFormatting.GREEN).withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand("/d1 party donate "+token))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Donate "+price(request.contract())+" Trace for one revival"))));
        var run=DungeonRunRegistryData.get(server).getRun(request.run()).orElseThrow();
        for(UUID id:run.orderedPlayers()){
            var member=server.getPlayerList().getPlayer(id);
            if(member!=null&&D1Members.inside(member,run))member.sendSystemMessage(text);
        }
    }
    public static int donate(ServerPlayer player,String tokenText){
        UUID token;try{token=UUID.fromString(tokenText);}catch(IllegalArgumentException invalid){return 0;}
        var old=REQUESTS.get(token);
        if(old==null){message(player,"That donation request has expired or was already paid.");return 0;}
        var request=current(player,old.contract().id(),old.rest().until());
        if(request==null||request.run()!=old.run()||!request.rest().death().equals(old.rest().death())){
            message(player,"That donation request is no longer available to you.");return 0;
        }
        return pay(player,request,false)?1:0;
    }
    private static boolean pay(ServerPlayer player,Request request,boolean offerHelp){
        var server=player.level().getServer();var contract=request.contract();var rest=request.rest();
        UUID token=MercenaryRevivePayment.id(request.run(),contract.id(),rest.death(),rest.until());
        if(rest.due(server.overworld().getGameTime())||paid(server,request.run(),contract.id(),rest)){
            message(player,"This companion is already awaiting a safe return. No Trace charged.");return false;
        }
        if(!CurrencyService.transactionsAllowed(player)){message(player,"Finish your current account recovery before reviving a companion.");return false;}
        UNVERIFIED.entrySet().removeIf(e->e.getValue().rest().due(server.overworld().getGameTime())
                ||DungeonRunRegistryData.get(server).getRun(e.getValue().run()).filter(r->r.stateEnum()==DungeonRunState.ACTIVE).isEmpty());
        if(!UNVERIFIED.containsKey(token)&&UNVERIFIED.size()>=30){message(player,"Revival recovery is busy. Please try again shortly.");return false;}
        var accounts=PlayerCurrencyData.get(server);
        if(!accounts.payMercenaryRevive(request.run(),contract.id(),rest.death(),rest.until(),player.getUUID(),price(contract))){
            if(offerHelp)ModNetwork.sendTo(player,new PartyPayloads.RevivePrompt(token.toString(),rest.until(),contract.name()));
            else message(player,"You do not have "+price(contract)+" Trace to donate.");
            return false;
        }
        UNVERIFIED.put(token,request);
        if(!accounts.flushVerified()){
            message(player,"Revival payment is waiting for a verified save. Try again; you will not be charged twice.");return false;
        }
        UNVERIFIED.remove(token);REQUESTS.remove(token);
        message(player,contract.name()+" will return as soon as there is a safe place nearby.");
        MercenaryRespawns.tick(server);return true;
    }
}
