package net.goui.cosmicdungeon.playerclass.skill;

import java.util.*;
import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server-owned slot sessions. Later batches attach abilities; unassigned slots never fall through to old actions. */
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
    private static ClassSkillPayloads.View snapshot(ServerPlayer p,long revision){
        var run=ClassSkills.run(p);
        return new ClassSkillPayloads.View(run==null?0:run.runId(),revision,ClassData.getClassId(p),slots(),SignatureSkills.playerSnapshot(p));
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
        if(!matches(session.sent,request,run==null?0:run.runId(),ClassData.getClassId(p))){sync(p,true);return;}
        // Resolve from current server catalog, not the old sent snapshot or a client ability ID.
        var current=slots().stream().filter(s->s.slot()==request.slot()).findFirst().orElseThrow();
        p.displayClientMessage(Component.literal(current.abilityId().isEmpty()
                ?request.slot().label()+": no ability assigned.":current.title()+": unavailable."),true);
        sync(p,true);
    }
    public static void forget(ServerPlayer p){SESSIONS.remove(p);}
    public static void stop(){SESSIONS.clear();}
}
