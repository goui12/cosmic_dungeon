package net.goui.cosmicdungeon.dungeon;

import java.util.*;
import java.util.function.LongSupplier;

/** One bounded synchronous-startup measurement. Nested phases are inclusive, never additive. */
public final class DungeonStartupTiming implements AutoCloseable {
    public enum Phase { SNAPSHOT_COPY, WORLD_OPEN, CHUNK_ACCESS, SCHEMATIC_LOAD, SCHEMATIC_PASTE,
        CHEST_BINDING, MIGRATION, WOLF_ADMISSION, RUN_REGISTRATION, MERCENARY_ADMISSION, TELEPORT }
    private static final ThreadLocal<DungeonStartupTiming> ACTIVE=new ThreadLocal<>();
    private final long[] nanos=new long[Phase.values().length],counts=new long[Phase.values().length];
    private final LongSupplier clock;
    private final long started;
    private final String context;
    private final DungeonStartupTiming previous;
    private long run;
    private boolean success;
    private int chunkDepth;
    private long chunkStarted;
    public DungeonStartupTiming(String context){this(context,System::nanoTime);}
    DungeonStartupTiming(String context,LongSupplier clock){
        this.context=context;this.clock=clock;started=clock.getAsLong();previous=ACTIVE.get();ACTIVE.set(this);
    }
    public void run(long run){this.run=run;}
    public void success(){success=true;}
    public static long start(){var active=ACTIVE.get();return active==null?0:active.clock.getAsLong();}
    public static void record(Phase phase,long start,long operations){
        var active=ACTIVE.get();if(active==null)return;
        active.add(phase,Math.max(0,active.clock.getAsLong()-start),operations);
    }
    private void add(Phase phase,long elapsed,long operations){
        nanos[phase.ordinal()]+=elapsed;counts[phase.ordinal()]+=operations;
    }
    public static void chunkStarted(){
        var active=ACTIVE.get();if(active!=null&&active.chunkDepth++==0)active.chunkStarted=active.clock.getAsLong();
    }
    public static void chunkFinished(){
        var active=ACTIVE.get();if(active!=null&&active.chunkDepth>0&&--active.chunkDepth==0)
            active.add(Phase.CHUNK_ACCESS,active.clock.getAsLong()-active.chunkStarted,1);
    }
    long operations(Phase phase){return counts[phase.ordinal()];}
    long nanos(Phase phase){return nanos[phase.ordinal()];}
    @Override public void close(){
        if(ACTIVE.get()!=this)throw new IllegalStateException("Startup timing scope mismatch");
        ACTIVE.set(previous);if(previous==null)ACTIVE.remove();
        boolean incompleteChunk=chunkDepth>0;
        if(incompleteChunk)add(Phase.CHUNK_ACCESS,clock.getAsLong()-chunkStarted,1);
        var summary=new LinkedHashMap<String,String>();
        for(var phase:Phase.values())summary.put(phase.name(),counts[phase.ordinal()]+" operations/"+(nanos[phase.ordinal()]/1_000_000.0)+"ms");
        com.mojang.logging.LogUtils.getLogger().info(
                "[DungeonStartupTiming] context={} run={} success={} totalMs={} inclusivePhases={} incompleteChunk={} "
                +"(chunk access includes cached hits, initialization and waits; nested phase times overlap; no whole-world diagnosis)",
                context,run,success,(clock.getAsLong()-started)/1_000_000.0,summary,incompleteChunk);
    }
}
