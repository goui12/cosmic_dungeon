package net.goui.cosmicdungeon.playerclass.bogatyr;

/** Opt-in native test counters. Normal gameplay keeps the probe null; no logging, allocations or packets. */
final class BogatyrWork {
    static Probe probe;
    static final class Probe { long scans,inspected,visited,decisions,paths,nodes,leashes,guards,heals; }
    private BogatyrWork(){}
    static Probe begin(){if(probe!=null)throw new IllegalStateException("Wolf probe already active");return probe=new Probe();}
    static void end(){probe=null;}
}
