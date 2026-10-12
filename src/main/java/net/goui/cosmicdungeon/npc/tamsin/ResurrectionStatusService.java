package net.goui.cosmicdungeon.npc.tamsin;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.mercenary.MercenaryResurrection;
import net.goui.cosmicdungeon.network.TheurgistPayloads;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.theurgist.*;
import net.minecraft.server.level.ServerPlayer;

/** Same-run, bounded read-only status shared by inventory, party inspection and death screens. */
public final class ResurrectionStatusService {
    private ResurrectionStatusService(){}
    public static TheurgistPayloads.Resurrection player(ServerPlayer p,DungeonRunRegistryData.RunRecord run){
        if(p==null||p.connection==null||p.hasDisconnected())return new TheurgistPayloads.Resurrection("OFFLINE",-1);
        if(!ClassData.getClassId(p).equals("theurgist"))return TheurgistPayloads.Resurrection.UNAVAILABLE;
        try{
            int seconds=ResurrectionCooldown.seconds(p);String state;
            var actual=MercenaryResurrection.deathRun(p);
            if(actual==null||actual.runId()!=run.runId())state="OUT_OF_RUN";
            else if(TheurgistRevival.blocked(p))state="PENDING";
            else if(!p.isAlive()||p.isDeadOrDying())state="DEAD";
            else state=seconds>0?"COOLDOWN":"READY";
            return new TheurgistPayloads.Resurrection(state,seconds);
        }catch(RuntimeException invalid){return TheurgistPayloads.Resurrection.UNAVAILABLE;}
    }
    public static List<TheurgistPayloads.Caster> roster(ServerPlayer viewer,DungeonRunRegistryData.RunRecord run){
        var server=viewer.level().getServer();var data=D1RunData.get(server);
        var result=new ArrayList<TheurgistPayloads.Caster>();
        for(var id:run.orderedPlayers()){
            if(run.isCompletionExited(id)||result.size()>=TheurgistPayloads.MAX_TARGETS)continue;
            var p=server.getPlayerList().getPlayer(id);var known=D1PartyHudService.identity(server,id);
            // Existing metrics identity survives restart; no offline player file or inventory is loaded.
            String cls=p==null?remembered(data,run.runId(),"class",id,known.classId()):ClassData.getClassId(p);
            if(!cls.equals("theurgist"))continue;
            String name=p==null?remembered(data,run.runId(),"name",id,known.name()):p.getGameProfile().name();
            if(name.isBlank()||name.length()>64)name="Theurgist";
            result.add(new TheurgistPayloads.Caster(id,name,false,player(p,run)));
        }
        for(var hire:run.mercenaries()){
            if(result.size()>=TheurgistPayloads.MAX_CASTERS)break;
            if(net.goui.cosmicdungeon.mercenary.MercenarySkill.POSITIVE_POTIONS.supports(hire)&&run.containsPlayer(hire.hirer())&&!run.isCompletionExited(hire.hirer()))
                result.add(new TheurgistPayloads.Caster(hire.id(),hire.name(),true,MercenaryResurrection.status(viewer,run,hire)));
        }
        return List.copyOf(result);
    }
    public static boolean offlineTheurgist(D1RunData data,long run,UUID id){
        return remembered(data,run,"class",id,"none").equals("theurgist");
    }
    private static String remembered(D1RunData data,long run,String field,UUID id,String fallback){
        var values=data.values(run,"metrics_v1:"+field+":"+id);return values.size()==1?values.getFirst():fallback;
    }
    public static String describe(TheurgistPayloads.Resurrection status){
        String label=switch(status.state()){
            case "READY"->"Ready";case "COOLDOWN"->"Cooldown";case "DEAD"->"Dead";
            case "OFFLINE"->"Offline";case "OUT_OF_RUN"->"Outside active dungeon";
            case "LOCKED"->"Unlocks at level 10";case "PENDING"->"Save recovery pending";
            case "HIRER_UNAVAILABLE"->"Hirer unavailable";case "DIFFERENT_DIMENSION"->"Different dimension";
            default->"Unavailable";
        };
        return label+(status.seconds()>0?" ("+status.seconds()+"s remaining)":"");
    }
}
