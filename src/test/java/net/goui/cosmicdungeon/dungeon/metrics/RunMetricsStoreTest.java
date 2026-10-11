package net.goui.cosmicdungeon.dungeon.metrics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RunMetricsStoreTest {
    @TempDir Path temporary;
    private static final UUID WORLD=UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID MEMBER=UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER=UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static RunMetrics.Stats stats(long kills) {
        return new RunMetrics.Stats(kills,1,12.5,4.5,1,Map.of("minecraft:zombie",kills),
                Map.of("minecraft:nether_wart",2L),Map.of("minecraft:glowstone_dust",1L),2);
    }
    private static RunMetrics run(long id,String outcome,UUID member,long kills) {
        return new RunMetrics(WORLD,id,"dungeon_1","HARD",1000+id,2000+id,
                outcome.equals("ACTIVE")?0:3000+id,outcome,"COMPLETE",
                List.of(new RunMetrics.Member(member,"Goui & <Wolf> \"λ\"","bogatyr","ACTIVE",stats(kills))));
    }
    @Test void xmlRoundTripPreservesIdentityNamesMobIdsAndResources() throws Exception {
        var original=run(1,"COMPLETED",MEMBER,3);
        var bytes=RunMetricsXml.encodeRun(original);
        assertEquals(original,RunMetricsXml.decodeRun(bytes));
        assertDoesNotThrow(()->UUID.fromString(original.instanceId()));
        String xml=new String(bytes,StandardCharsets.UTF_8);
        assertTrue(xml.contains("&amp;"));assertTrue(xml.contains("&lt;"));
        assertTrue(bytes.length<RunMetricsXml.MAX_XML_BYTES);
    }
    @Test void activeCheckpointThenFinalizationIsExactlyOnce() throws Exception {
        var store=new RunMetricsStore(temporary,WORLD);
        var active=run(1,"ACTIVE",MEMBER,3);
        store.checkpoint(active);assertEquals(active,store.read(1));
        var finished=run(1,"COMPLETED",MEMBER,3);
        store.finish(finished);store.finish(finished);
        var reopened=new RunMetricsStore(temporary);reopened.recover();
        assertEquals(finished,reopened.read(1));assertEquals(WORLD,reopened.worldId());
        assertEquals(1,reopened.totals(null).runs());assertEquals(1,reopened.totals(MEMBER).completed());
        assertEquals(3,reopened.totals(MEMBER).stats().kills());
        assertEquals(3,reopened.totals(MEMBER).stats().mobs().get("minecraft:zombie"));
        assertEquals(2,reopened.totals(MEMBER).stats().used().get("minecraft:nether_wart"));
        assertEquals(1,reopened.totals(MEMBER).stats().contributed().get("minecraft:glowstone_dust"));
        assertEquals(2,reopened.totals(MEMBER).stats().spawners());
        assertFalse(Files.exists(temporary.resolve("pending.xml")));
        assertTrue(Files.exists(temporary.resolve(finished.instanceId()+".xml")));
    }
    @Test void conflictingReplayAndActiveOverwriteCannotRewriteFinalHistory() throws Exception {
        var store=new RunMetricsStore(temporary,WORLD);
        var finished=run(1,"COMPLETED",MEMBER,3);store.finish(finished);
        assertThrows(IOException.class,()->store.finish(run(1,"COMPLETED",MEMBER,4)));
        assertThrows(IOException.class,()->store.checkpoint(run(1,"ACTIVE",MEMBER,9)));
        assertEquals(finished,store.read(1));assertEquals(3,store.totals(MEMBER).stats().kills());
    }
    @Test void simultaneousInstancesAndPlayersRemainSeparate() throws Exception {
        var store=new RunMetricsStore(temporary,WORLD);
        store.checkpoint(run(1,"ACTIVE",MEMBER,2));store.checkpoint(run(2,"ACTIVE",OTHER,5));
        store.finish(run(2,"ABANDONED",OTHER,5));assertEquals("ACTIVE",store.read(1).outcome());
        store.finish(run(1,"COMPLETED",MEMBER,2));
        assertEquals(7,store.totals(null).stats().kills());
        assertEquals(2,store.totals(MEMBER).stats().kills());assertEquals(5,store.totals(OTHER).stats().kills());
        assertEquals(1,store.totals(OTHER).aborted());assertEquals(0,store.totals(OTHER).completed());
    }
    @Test void crashAfterEveryDurableWriteRecoversAnExactAfterimage() throws Exception {
        var points=new ArrayList<String>();
        var probe=new RunMetricsStore(temporary.resolve("probe"),WORLD,points::add);
        points.clear();probe.finish(run(1,"COMPLETED",MEMBER,3));
        assertTrue(points.size()>=4,"Journal, participant total, server total, and final run must be durable.");
        for(int failAt=0;failAt<points.size();failAt++) {
            final int failurePoint=failAt;
            var armed=new AtomicBoolean(false);var writes=new AtomicInteger();
            Path path=temporary.resolve("crash-"+failAt);
            var crashing=new RunMetricsStore(path,WORLD,step->{
                if(armed.get()&&writes.getAndIncrement()==failurePoint)throw new SimulatedCrash(step);
            });
            armed.set(true);
            assertThrows(SimulatedCrash.class,()->crashing.finish(run(1,"COMPLETED",MEMBER,3)),
                    "Expected injected crash at "+points.get(failAt));
            var resumed=new RunMetricsStore(path);resumed.recover();
            resumed.finish(run(1,"COMPLETED",MEMBER,3));resumed.recover();
            assertEquals(1,resumed.totals(null).runs(),points.get(failAt));
            assertEquals(3,resumed.totals(MEMBER).stats().kills(),points.get(failAt));
            assertEquals(run(1,"COMPLETED",MEMBER,3),resumed.read(1),points.get(failAt));
            assertFalse(Files.exists(path.resolve("pending.xml")),points.get(failAt));
        }
    }
    @Test void rebuildRecoversTotalsAndLastTenFromFinalizedInstancesOnly() throws Exception {
        Path source=temporary.resolve("source"),destination=temporary.resolve("rebuilt");
        var store=new RunMetricsStore(source,WORLD);
        for(int i=1;i<=12;i++)store.finish(run(i,i%2==0?"COMPLETED":"ABANDONED",MEMBER,i));
        store.checkpoint(run(13,"ACTIVE",MEMBER,100));
        var expected=store.totals(MEMBER);
        assertEquals(12,expected.runs());assertEquals(6,expected.completed());assertEquals(6,expected.aborted());
        assertEquals(78,expected.stats().kills());assertEquals(10,expected.recent().size());
        assertEquals(12,expected.recent().getFirst().runId());assertEquals(3,expected.recent().getLast().runId());
        store.rebuild(destination);var rebuilt=new RunMetricsStore(destination);
        assertEquals(expected,rebuilt.totals(MEMBER));assertEquals(store.totals(null),rebuilt.totals(null));
        assertEquals(store.read(1),rebuilt.read(1));assertEquals("ACTIVE",store.read(13).outcome());
    }
    @Test void xmlRejectsUnknownFieldsExternalEntitiesAndOversizeInput() throws Exception {
        byte[] valid=RunMetricsXml.encodeRun(run(1,"ACTIVE",MEMBER,1));
        String xml=new String(valid,StandardCharsets.UTF_8);
        int closing=xml.lastIndexOf("</");
        String future=xml.substring(0,closing)+"<future-field value=\"preserve me\"/>"+xml.substring(closing);
        assertThrows(IOException.class,()->RunMetricsXml.decodeRun(future.getBytes(StandardCharsets.UTF_8)));
        var payload="<?xml version=\"1.0\"?><!DOCTYPE run [<!ENTITY leak SYSTEM \"file:///must-not-be-read\">]><run>&leak;</run>";
        assertThrows(IOException.class,()->RunMetricsXml.decodeRun(payload.getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class,()->RunMetricsXml.decodeRun(new byte[RunMetricsXml.MAX_XML_BYTES+1]));
    }
    @Test void foreignWorldCannotMixStatistics() throws Exception {
        var store=new RunMetricsStore(temporary,WORLD);store.finish(run(1,"COMPLETED",MEMBER,1));
        assertThrows(IOException.class,()->new RunMetricsStore(temporary,UUID.randomUUID()));
        assertEquals(1,store.totals(null).runs());
    }
    @Test void representativeArchiveSizeIsBoundedAndReported() throws Exception {
        var members=new ArrayList<RunMetrics.Member>();var mobs=new TreeMap<String,Long>();
        for(int i=0;i<128;i++)mobs.put("example:hostile_"+i,1L);
        var values=new RunMetrics.Stats(128,2,300,50,0,mobs,Map.of(),Map.of(),4);
        for(int i=0;i<6;i++)members.add(new RunMetrics.Member(new UUID(0,i+1),"Member"+i,"theurgist","ACTIVE",values));
        var run=new RunMetrics(WORLD,1,"dungeon_1","HARD",1,2,3,"COMPLETED","COMPLETE",members);
        long started=System.nanoTime();byte[] bytes=RunMetricsXml.encodeRun(run);
        assertEquals(run,RunMetricsXml.decodeRun(bytes));
        assertTrue(bytes.length<128*1024,"Six players and128 mob types should remain a small bounded snapshot.");
        System.out.println("METRICS_BASELINE members=6 mob_types_each=128 xml_bytes="+bytes.length
                +" encode_decode_micros="+((System.nanoTime()-started)/1000));
    }
    @Test void partyAndStructuredResourceEventsSurviveRoundTrip() throws Exception {
        var base=run(5,"COMPLETED",MEMBER,2);UUID party=UUID.randomUUID(),transaction=UUID.randomUUID();
        var event=new RunMetrics.ResourceEvent(transaction,OTHER,MEMBER,"Healing splash","minecraft:nether_wart",2,false,2004);
        var full=new RunMetrics(WORLD,base.runId(),party,base.dungeon(),base.difficulty(),base.started(),
                base.updated(),base.ended(),base.outcome(),base.coverage(),
                List.of(base.members().getFirst(),new RunMetrics.Member(OTHER,"Donor","theurgist","ACTIVE",RunMetrics.Stats.EMPTY)),List.of(event));
        var decoded=RunMetricsXml.decodeRun(RunMetricsXml.encodeRun(full));
        assertEquals(full,decoded);assertEquals(party,decoded.partyId());
        assertEquals(OTHER,decoded.resourceEvents().getFirst().donor());
        assertEquals(MEMBER,decoded.resourceEvents().getFirst().recipient());
        assertEquals(transaction,decoded.resourceEvents().getFirst().transaction());
    }
    @Test void unknownStoredRunIsRetainedAndCannotBeOverwritten() throws Exception {
        var store=new RunMetricsStore(temporary,WORLD);var original=run(1,"ACTIVE",MEMBER,1);
        String xml=new String(RunMetricsXml.encodeRun(original),StandardCharsets.UTF_8);
        int closing=xml.lastIndexOf("</");
        byte[] future=(xml.substring(0,closing)+"<future-field/>"+xml.substring(closing)).getBytes(StandardCharsets.UTF_8);
        Path path=temporary.resolve(original.instanceId()+".xml");Files.write(path,future);
        assertThrows(IOException.class,()->store.read(1));
        assertThrows(IOException.class,()->store.checkpoint(original));
        assertArrayEquals(future,Files.readAllBytes(path));
    }
    @Test void corruptRecoveryJournalIsRetainedForRepair() throws Exception {
        var store=new RunMetricsStore(temporary,WORLD);
        Path journal=temporary.resolve("pending.xml");byte[] corrupt="<pending future=\"1\"/>".getBytes(StandardCharsets.UTF_8);
        Files.write(journal,corrupt);
        assertThrows(IOException.class,store::recover);
        assertArrayEquals(corrupt,Files.readAllBytes(journal));
    }
    @Test void oldWorldWithoutArchiveStartsEmptyAndStartupAbortIsTruthful() throws Exception {
        var store=new RunMetricsStore(temporary,WORLD);assertNull(store.read(123));
        var aborted=run(1,"STARTUP_ABORTED",MEMBER,0);store.finish(aborted);
        assertEquals("STARTUP_ABORTED",store.read(1).outcome());
        assertEquals(1,store.totals(MEMBER).aborted());assertEquals(0,store.totals(MEMBER).completed());
    }
    @Test void invalidResourceCustodyAndInconsistentTotalsFailClosed() {
        var base=run(5,"COMPLETED",MEMBER,2);
        var outsider=new RunMetrics.ResourceEvent(UUID.randomUUID(),OTHER,MEMBER,"Healing","minecraft:nether_wart",1,false,2004);
        assertThrows(IllegalArgumentException.class,()->new RunMetrics(WORLD,base.runId(),base.partyId(),base.dungeon(),
                base.difficulty(),base.started(),base.updated(),base.ended(),base.outcome(),base.coverage(),base.members(),List.of(outsider)));
        assertThrows(IllegalArgumentException.class,()-> {
            var inconsistentOwn=new RunMetrics.ResourceEvent(UUID.randomUUID(),MEMBER,MEMBER,"Healing","minecraft:nether_wart",1,false,2004);
            new RunMetrics(WORLD,base.runId(),base.partyId(),base.dungeon(),base.difficulty(),base.started(),
                    base.updated(),base.ended(),base.outcome(),base.coverage(),base.members(),List.of(inconsistentOwn));
        });
        assertThrows(IllegalArgumentException.class,()->new RunMetrics.Totals(WORLD,"server",2,1,0,RunMetrics.Stats.EMPTY,List.of()));
        var wrongIdentity=new RunMetrics.History(UUID.randomUUID().toString(),5,"dungeon_1","COMPLETED",3005);
        assertThrows(IllegalArgumentException.class,()->new RunMetrics.Totals(WORLD,"server",1,1,0,RunMetrics.Stats.EMPTY,List.of(wrongIdentity)));
        var history=new RunMetrics.History(base.instanceId(),5,"dungeon_1","COMPLETED",3005);
        assertThrows(IllegalArgumentException.class,()->new RunMetrics.Totals(WORLD,"server",0,0,0,RunMetrics.Stats.EMPTY,List.of(history)));
    }
    private static final class SimulatedCrash extends RuntimeException {
        SimulatedCrash(String point){super(point);}
    }
}
