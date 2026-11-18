package net.goui.cosmicdungeon.playerclass.skill;

import java.util.*;
import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server-owned slot sessions; current actor/run/charge is revalidated for every activation. */
public final class ClassSkillActions {
    /** One deferred acknowledgement bit coalesces rejected presses until the existing periodic sync. */
    static final class Session {
        ClassSkillPayloads.View sent;
        long revision,lastAction=Long.MIN_VALUE;
        boolean acknowledgementPending;
        boolean acceptAction(long now){
            if(lastAction!=Long.MIN_VALUE&&now>=lastAction&&now-lastAction<5){
                acknowledgementPending=true;
                return false;
            }
            lastAction=now;
            return true;
        }
        ClassSkillPayloads.View response(ClassSkillPayloads.View view,boolean force){
            if(!force&&!acknowledgementPending&&view.equals(sent))return null;
            revision=Math.addExact(revision,1);
            sent=new ClassSkillPayloads.View(view.run(),revision,view.classId(),view.slots(),view.tracks());
            acknowledgementPending=false;
            return sent;
        }
    }
    private static final Map<ServerPlayer,Session> SESSIONS=new WeakHashMap<>();
    private ClassSkillActions(){}
    public static List<ClassSkillPayloads.SlotView> slots(){
        return Arrays.stream(ClassSkillSlot.values()).map(ClassSkillPayloads.SlotView::unassigned).toList();
    }
    public static ClassSkillPayloads.SlotView primary(String cls,int level,int charge,boolean armed,boolean active){
        if(cls.equals("dragoon")){
            String description=!active?"Requires an active dungeon.":armed?"Armed: your next valid trident strike releases Chain Lightning."
                    :"Land 100 direct hostile trident hits, then arm your next strike. Charge: "+charge+"/100.";
            return new ClassSkillPayloads.SlotView(ClassSkillSlot.PRIMARY,"chain_lightning",
                    armed?"Chain Lightning (Armed)":"Chain Lightning",description,active&&charge==100&&!armed,0,charge,100);
        }
        if(cls.equals("judicator")){
            var rank=net.goui.cosmicdungeon.playerclass.judicator.SmiteCombat.rank(level);
            String description=!active?"Requires an active dungeon.":"Kill 10 undead to charge. "+rank.damage()+" damage to undead within "
                    +rank.radius()+" blocks. Only activations that damage undead earn proficiency. Charge: "+charge+"/10.";
            return new ClassSkillPayloads.SlotView(ClassSkillSlot.PRIMARY,"smite","Smite ("+rank.name()+")",
                    description,active&&charge==10,0,charge,10);
        }
        return ClassSkillPayloads.SlotView.unassigned(ClassSkillSlot.PRIMARY);
    }
    public static List<ClassSkillPayloads.SlotView> slots(ServerPlayer p){
        var result=new ArrayList<>(slots());String cls=ClassData.getClassId(p);
        var skill=SignatureSkill.forClass(cls).orElse(null);
        if(skill!=null&&ChargedSkillState.limit(skill)>0)result.set(0,primary(cls,ChargedClassSkills.level(p,skill),
                ChargedClassSkills.charge(p,skill),skill==SignatureSkill.CHAIN_LIGHTNING&&ChargedClassSkills.armed(p),
                ChargedClassSkills.active(p,skill)));
        result.set(1,ClassBuffs.slot(p));
        return List.copyOf(result);
    }
    private static ClassSkillPayloads.View snapshot(ServerPlayer p,long revision){
        var run=ClassSkills.run(p);
        return new ClassSkillPayloads.View(run==null?0:run.runId(),revision,ClassData.getClassId(p),slots(p),SignatureSkills.playerSnapshot(p));
    }
    public static void sync(ServerPlayer p,boolean force){
        if(p.connection==null||p.hasDisconnected())return;
        var session=SESSIONS.computeIfAbsent(p,k->new Session());
        var response=session.response(snapshot(p,session.revision),force);
        if(response!=null)ModNetwork.sendTo(p,response);
    }
    public static void syncAll(MinecraftServer server){for(var p:server.getPlayerList().getPlayers())sync(p,false);}
    public static boolean matches(ClassSkillPayloads.View sent,ClassSkillPayloads.Action request,long run,String cls){
        return sent!=null&&run>0&&sent.run()==run&&request.run()==run&&sent.revision()==request.revision()
                &&sent.classId().equals(cls)&&request.classId().equals(cls);
    }
    public static void action(ServerPlayer p,ClassSkillPayloads.Action request){
        var session=SESSIONS.get(p);
        if(session==null||p.connection==null||p.hasDisconnected())return;
        long now=p.level().getServer().getTickCount();
        if(!session.acceptAction(now))return;
        var run=ClassSkills.run(p);
        if (!matches(session.sent, request, run == null ? 0 : run.runId(), ClassData.getClassId(p))) {
            p.displayClientMessage(Component.literal("Your skill state changed. Check the updated skill and try again."), true);
            sync(p, true);
            return;
        }
        // Resolve from current server catalog, not the old sent snapshot or a client ability ID.
        var current=slots(p).stream().filter(s->s.slot()==request.slot()).findFirst().orElseThrow();
        if(current.enabled()&&current.abilityId().equals("chain_lightning")){
            if(ChargedClassSkills.arm(p))p.displayClientMessage(Component.literal("Chain Lightning armed for your next trident strike."),true);
        }else if(current.enabled()&&current.abilityId().equals("smite"))ChargedClassSkills.smite(p);
        else if(current.enabled()&&request.slot()==ClassSkillSlot.SKILL_1)ClassBuffs.cast(p);
        else p.displayClientMessage(Component.literal(current.abilityId().isEmpty()
                ?request.slot().label()+": no ability assigned.":current.title()+": "+current.description()),true);
        sync(p,true);
    }
    public static void forget(ServerPlayer p){SESSIONS.remove(p);}
    public static void stop(){SESSIONS.clear();ClassBuffs.stop();}
}
