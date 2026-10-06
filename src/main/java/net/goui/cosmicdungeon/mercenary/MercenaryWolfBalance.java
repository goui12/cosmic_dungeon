package net.goui.cosmicdungeon.mercenary;

/** Requested pack growth; constant command work even when a long-lived pack grows. */
final class MercenaryWolfBalance {
    static final int COMMAND_BUDGET=5, MIN_INTERVAL=400;
    private MercenaryWolfBalance(){}
    private static int level(int level){return Math.clamp(level,1,65536);}
    static int capacity(int level){return MercenaryWolves.CAP+(level(level)-1)/2;}
    static int interval(int level){
        long divisor=100L+6L*(level(level)-1);
        return (int)Math.max(MIN_INTERVAL,(MercenaryWolves.INTERVAL*100L+divisor-1)/divisor);
    }
    static int remaining(int ticks,int elapsed,int level){
        return MercenaryWolves.advance(Math.min(ticks,interval(level)),elapsed);
    }
}
