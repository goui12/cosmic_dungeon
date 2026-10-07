package net.goui.cosmicdungeon.playerclass.resource;

/** Session-local ticks only: never serializes a timestamp and never accrues offline catch-up. */
public final class OnlineResourceClock {
    private long run,tick;private String kind="";
    public void bind(long activeRun,String activeKind,long now){
        if(activeRun<=0){clear();return;}
        if(activeRun!=run||!activeKind.equals(kind)||now<tick){run=activeRun;kind=activeKind;tick=now;}
    }
    public boolean elapsed(long activeRun,String activeKind,long now){
        if(activeRun<=0){clear();return false;}
        if(activeRun!=run||!activeKind.equals(kind)||now<tick){run=activeRun;kind=activeKind;tick=now;return false;}
        if(now-tick<20)return false;
        tick=now;return true; // One credit at most, including pauses, lag and sparse observations.
    }
    public void clear(){run=0;tick=0;kind="";}
}
