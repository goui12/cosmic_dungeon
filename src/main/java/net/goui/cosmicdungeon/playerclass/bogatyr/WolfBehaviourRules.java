package net.goui.cosmicdungeon.playerclass.bogatyr;

/** Source-ranked threats and D27's configurable lifetime; arithmetic does not depend on game bootstrap. */
public final class WolfBehaviourRules {
    private WolfBehaviourRules(){}
    public static int threatRank(boolean skeleton,boolean ranged){return skeleton?0:ranged?1:2;}
    public static boolean refresh(long now,long last,int interval){
        return last==Long.MIN_VALUE||now<last||now-last>=Math.max(1,interval);
    }
    public static long activeTick(long elapsed,int minutes,boolean paused){
        long safe=Math.max(0,elapsed);
        return minutes<=0||paused||safe==Long.MAX_VALUE?safe:safe+1;
    }
    public static boolean expired(long elapsed,int minutes){
        return minutes>0&&Math.max(0,elapsed)>=(long)minutes*1200L;
    }
}
