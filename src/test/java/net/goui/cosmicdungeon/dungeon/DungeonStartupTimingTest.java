package net.goui.cosmicdungeon.dungeon;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class DungeonStartupTimingTest {
    @Test void failedPhaseKeepsItsElapsedAttemptAndClearsTheActiveScope(){
        var time=new AtomicLong();
        try(var timing=new DungeonStartupTiming("failed-fixture",time::get)){
            assertThrows(IllegalStateException.class,()->{
                long start=DungeonStartupTiming.start();
                try{time.addAndGet(17);throw new IllegalStateException("fixture failure");}
                finally{DungeonStartupTiming.record(DungeonStartupTiming.Phase.TELEPORT,start,1);}
            });
            assertEquals(17,timing.nanos(DungeonStartupTiming.Phase.TELEPORT));
            assertEquals(1,timing.operations(DungeonStartupTiming.Phase.TELEPORT));
        }
        assertEquals(0,DungeonStartupTiming.start());
    }
    @Test void failedNestedChunkUnwindsBeforeCleanupAndTheNextRequest(){
        var time=new AtomicLong();
        var failure=new IllegalStateException("native request failure");
        try(var timing=new DungeonStartupTiming("failed-chunk",time::get)){
            assertTrue(DungeonStartupTiming.active());
            assertSame(failure,assertThrows(IllegalStateException.class,()->DungeonStartupTiming.chunk(()->{
                time.addAndGet(10);
                return DungeonStartupTiming.chunk(()->{
                    time.addAndGet(20);
                    throw failure;
                });
            })));
            assertEquals(30,timing.nanos(DungeonStartupTiming.Phase.CHUNK_ACCESS));
            assertEquals(1,timing.operations(DungeonStartupTiming.Phase.CHUNK_ACCESS));
            time.addAndGet(1000); // unrelated cleanup after the caught native failure
            assertEquals("loaded",DungeonStartupTiming.chunk(()->{
                time.addAndGet(7);
                return "loaded";
            }));
            assertEquals(37,timing.nanos(DungeonStartupTiming.Phase.CHUNK_ACCESS));
            assertEquals(2,timing.operations(DungeonStartupTiming.Phase.CHUNK_ACCESS));
        }
        assertFalse(DungeonStartupTiming.active());
        assertEquals("inactive",DungeonStartupTiming.chunk(()->"inactive"));
    }
    @Test void comparablePhaseCountsUseIndependentDurationsAndNestedChunkCallsCountOnce(){
        for(int pass=0;pass<2;pass++){
            var time=new AtomicLong();
            try(var timing=new DungeonStartupTiming("fixture-"+pass,time::get)){
                long start=DungeonStartupTiming.start();
                DungeonStartupTiming.chunk(()->{
                    time.addAndGet(10);
                    DungeonStartupTiming.chunk(()->time.addAndGet(20));
                    return time.addAndGet(30);
                });
                DungeonStartupTiming.record(DungeonStartupTiming.Phase.SCHEMATIC_PASTE,start,36);
                assertEquals(60,timing.nanos(DungeonStartupTiming.Phase.CHUNK_ACCESS));
                assertEquals(1,timing.operations(DungeonStartupTiming.Phase.CHUNK_ACCESS));
                assertEquals(36,timing.operations(DungeonStartupTiming.Phase.SCHEMATIC_PASTE));
                assertEquals(60,timing.nanos(DungeonStartupTiming.Phase.SCHEMATIC_PASTE));
            }
        }
        assertEquals(0,DungeonStartupTiming.start());
    }
}
