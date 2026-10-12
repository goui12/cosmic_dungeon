package net.goui.cosmicdungeon.dungeon.metrics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourceMetricsXmlTest {
    private static final UUID WORLD=new UUID(7,7), TX=new UUID(8,8), RECIPIENT=new UUID(0,1), A=new UUID(0,2), B=new UUID(0,3);
    private static RunMetrics.ResourceEvent event(UUID donor,String item,long quantity,boolean own) {
        return new RunMetrics.ResourceEvent(TX,donor,RECIPIENT,"Healing & <Light> potion",item,quantity,own,1234);
    }
    private static ResourceMetricsBatch batch() {
        return new ResourceMetricsBatch(WORLD,9,TX,2,10,13,Map.of(A,12L,B,13L),
                List.of(event(RECIPIENT,"minecraft:nether_wart",1,true),event(A,"minecraft:sugar",2,false),
                        event(B,"minecraft:glowstone_dust",1,false)));
    }
    @Test void segmentRoundTripRetainsTransactionIdentityAndEveryQuantity() throws Exception {
        var original=batch();byte[] xml=ResourceMetricsXml.encode(original);
        assertEquals(original,ResourceMetricsXml.decode(xml));assertTrue(xml.length<ResourceMetricsXml.MAX_BYTES);
        assertTrue(new String(xml,StandardCharsets.UTF_8).contains("&amp;"));
        assertThrows(UnsupportedOperationException.class,()->original.events().clear());
        assertThrows(UnsupportedOperationException.class,()->original.donated().clear());
    }
    @Test void ownOnlyCraftDoesNotAdvanceDonationHistory() throws Exception {
        var own=new ResourceMetricsBatch(WORLD,9,TX,3,13,13,Map.of(),List.of(event(RECIPIENT,"minecraft:sugar",2,true)));
        assertEquals(own,ResourceMetricsXml.decode(ResourceMetricsXml.encode(own)));
        assertTrue(own.donated().isEmpty());assertEquals(own.donationBefore(),own.donationAfter());
    }
    @Test void donationDeltaAndActualDonorSetMustMatchEvents() {
        var events=batch().events();
        assertThrows(IllegalArgumentException.class,()->new ResourceMetricsBatch(WORLD,9,TX,2,10,14,Map.of(A,12L,B,13L),events));
        assertThrows(IllegalArgumentException.class,()->new ResourceMetricsBatch(WORLD,9,TX,2,10,13,Map.of(A,13L),events));
        assertThrows(IllegalArgumentException.class,()->new ResourceMetricsBatch(WORLD,9,TX,2,10,13,Map.of(A,9L,B,13L),events));
        assertThrows(IllegalArgumentException.class,()->new ResourceMetricsBatch(WORLD,9,TX,2,10,13,Map.of(A,13L,B,13L),events));
        assertThrows(IllegalArgumentException.class,()->new ResourceMetricsBatch(WORLD,9,TX,2,10,13,Map.of(A,12L,B,11L),events));
    }
    @Test void duplicatesForeignTransactionsAndMixedRecipientsFailClosed() {
        var existing=batch();var duplicates=new ArrayList<>(existing.events());duplicates.add(existing.events().getFirst());
        assertThrows(IllegalArgumentException.class,()->new ResourceMetricsBatch(WORLD,9,TX,2,10,13,existing.donated(),duplicates));
        var foreign=new RunMetrics.ResourceEvent(UUID.randomUUID(),A,RECIPIENT,"Healing","minecraft:sugar",1,false,1234);
        assertThrows(IllegalArgumentException.class,()->new ResourceMetricsBatch(WORLD,9,TX,2,10,11,Map.of(A,11L),List.of(foreign)));
        var otherRecipient=new RunMetrics.ResourceEvent(TX,A,B,"Healing","minecraft:sugar",1,false,1234);
        assertThrows(IllegalArgumentException.class,()->new ResourceMetricsBatch(WORLD,9,TX,2,10,12,Map.of(A,12L),
                List.of(event(A,"minecraft:nether_wart",1,false),otherRecipient)));
    }
    @Test void oversizedOrImpossibleTransactionBoundsFailClosed() {
        assertThrows(IllegalArgumentException.class,()->new ResourceMetricsBatch(WORLD,9,TX,0,0,1,Map.of(A,1L),List.of(event(A,"minecraft:sugar",1,false))));
        assertThrows(IllegalArgumentException.class,()->new ResourceMetricsBatch(WORLD,0,TX,1,0,1,Map.of(A,1L),List.of(event(A,"minecraft:sugar",1,false))));
        assertThrows(IllegalArgumentException.class,()->new ResourceMetricsBatch(WORLD,9,TX,1,0,65,Map.of(A,65L),List.of(event(A,"minecraft:sugar",65,false))));
        assertThrows(IllegalArgumentException.class,()->new ResourceMetricsBatch(WORLD,9,TX,1,0,0,Map.of(),List.of()));
        assertThrows(IllegalArgumentException.class,()->new ResourceMetricsBatch(WORLD,9,TX,1,0,0,Map.of(),List.of(event(RECIPIENT,"minecraft:sugar",65,true))));
    }
    @Test void futureSchemaUnknownShapeMalformedFlagsAndDuplicateRanksAreRejected() throws Exception {
        String xml=new String(ResourceMetricsXml.encode(batch()),StandardCharsets.UTF_8);
        assertThrows(IOException.class,()->ResourceMetricsXml.decode(xml.replace("version=\"1\"","version=\"2\"").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class,()->ResourceMetricsXml.decode(xml.replace("own=\"false\"","own=\"maybe\"").getBytes(StandardCharsets.UTF_8)));
        int closing=xml.lastIndexOf("</");
        String future=xml.substring(0,closing)+"<future/>"+xml.substring(closing);
        assertThrows(IOException.class,()->ResourceMetricsXml.decode(future.getBytes(StandardCharsets.UTF_8)));
        int first=xml.indexOf("<donated "),end=xml.indexOf("</donated>",first)+"</donated>".length();
        String duplicate=xml.substring(0,end)+xml.substring(first,end)+xml.substring(end);
        assertThrows(IOException.class,()->ResourceMetricsXml.decode(duplicate.getBytes(StandardCharsets.UTF_8)));
    }
    @Test void xmlRejectsExternalEntitiesAndOversizeWithoutReadingTargets() {
        var malicious="<?xml version=\"1.0\"?><!DOCTYPE resource-transaction [<!ENTITY leak SYSTEM \"file:///must-not-read\">]><resource-transaction>&leak;</resource-transaction>";
        assertThrows(IOException.class,()->ResourceMetricsXml.decode(malicious.getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class,()->ResourceMetricsXml.decode(new byte[ResourceMetricsXml.MAX_BYTES+1]));
    }
}
