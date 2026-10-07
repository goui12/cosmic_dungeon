package net.goui.cosmicdungeon.client.screen.skills;

import java.util.Optional;

/** Read-only display state plus one pending action token; no optimistic balance changes or client timers. */
public final class ClassResourceState {
    private ClassResourceSnapshot snapshot,pending;
    public ClassResourceSnapshot snapshot(){return snapshot;}
    public void receive(ClassResourceSnapshot next){
        if(next.runId()==0){clear();return;}
        if(snapshot!=null&&snapshot.runId()==next.runId()&&next.revision()<snapshot.revision())return;
        snapshot=next;
        if(pending!=null&&(pending.runId()!=next.runId()||!pending.resourceId().equals(next.resourceId())||pending.revision()!=next.revision()||!next.active()))pending=null;
    }
    public void clear(){snapshot=null;pending=null;}
    public boolean awaitingAction(){return pending!=null;}
    public Optional<ClassResourceSnapshot> recycle(String classId){
        if(snapshot==null||!snapshot.matches(classId)||!snapshot.canRecycle()||pending!=null)return Optional.empty();
        pending=snapshot;return Optional.of(snapshot);
    }
}
