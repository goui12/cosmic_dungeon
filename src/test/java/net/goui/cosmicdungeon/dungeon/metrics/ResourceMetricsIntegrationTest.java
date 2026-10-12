package net.goui.cosmicdungeon.dungeon.metrics;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Real store writes and bounded state at increasing craft counts; timings are diagnostic only. */
class ResourceMetricsIntegrationTest {
    @TempDir Path directory;
    @Test void durableCostAndReplayRemainBoundedAcrossSixMemberWorkloads() throws Exception {
        UUID world=new UUID(1700,1), owner=new UUID(1700,2);
        var writes=new ArrayList<String>();var store=new RunMetricsStore(directory,world,writes::add);
        int baseline=ResourceMetricsStateXml.encode(ResourceMetricsState.empty(world,17)).length;
        long cursor=0;ResourceMetricsBatch last=null;long started=System.nanoTime();
        for(int count=1;count<=120;count++) {
            UUID tx=new UUID(1717,count);var ranks=new LinkedHashMap<UUID,Long>();
            var events=new ArrayList<RunMetrics.ResourceEvent>();
            events.add(new RunMetrics.ResourceEvent(tx,owner,owner,"Brew","minecraft:potion",1,true,count));
            for(int i=1;i<=5;i++) {
                UUID donor=new UUID(1700,2+i);ranks.put(donor,cursor+i);
                events.add(new RunMetrics.ResourceEvent(tx,donor,owner,"Brew","example:ingredient_"+i,1,false,count));
            }
            last=new ResourceMetricsBatch(world,17,tx,count,cursor,cursor+5,ranks,events);cursor+=5;
            var state=store.commitResources(last);
            assertEquals(count,state.sequence());assertEquals(6,state.members().size());
            assertEquals(Math.min(64,count*6),state.recent().size());
            assertEquals(count,state.members().get(owner).own().get("minecraft:potion"));
            if(count==1||count==30||count==120) {
                int bytes=ResourceMetricsStateXml.encode(state).length;
                assertTrue(bytes<65536,"Bounded six-member afterimage, no growth with full transaction history");
                long diskBytes;try(var paths=Files.walk(directory)) {diskBytes=paths.filter(Files::isRegularFile).mapToLong(path->{try{return Files.size(path);}catch(Exception ex){throw new RuntimeException(ex);}}).sum();}
                System.out.println("METRICS_INTEGRATION_COST crafts="+count+" members=6 recent="+state.recent().size()
                        +" baseline_empty_bytes="+baseline+" state_bytes="+bytes+" disk_bytes="+diskBytes
                        +" writes="+writes.size()+" cumulative_us="+(System.nanoTime()-started)/1000);
            }
        }
        var expected=store.readResourceState(17);int written=writes.size();
        assertEquals(expected,store.commitResources(last));assertEquals(written,writes.size(),"Settled replay does no writes");
        var restarted=new RunMetricsStore(directory);assertEquals(expected,restarted.readResourceState(17));
        assertEquals(ResourceMetricsState.empty(world,18),restarted.readResourceState(18),"Another run remains independent");
        try(var paths=Files.list(directory.resolve("resources").resolve(RunMetrics.instanceId(world,17)))) {assertEquals(120,paths.count());}
        assertEquals(241,writes.size(),"One identity plus two durable writes per successful craft; no hidden per-member files");
    }
}
