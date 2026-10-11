package net.goui.cosmicdungeon.crafting;

import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedCraftPlanTest {
    private static final UUID RECIPIENT=new UUID(0,1), A=new UUID(0,2), B=new UUID(0,3), OTHER=new UUID(0,4);
    private static CompoundTag debit(String item,String key,int count) {
        var row=new CompoundTag();row.putString("item",item);row.putString("ingredient",key);row.putInt("count",count);return row;
    }
    private static CompoundTag owner(String item,String ingredient,int count) {
        var row=new CompoundTag();row.putBoolean("ack",false);
        var before=new CompoundTag();before.putString("native_fixture","before");
        var after=new CompoundTag();after.putString("native_fixture","after");
        var debits=new CompoundTag();debits.put("0",debit(item,ingredient,count));
        row.put("before",before);row.put("after",after);row.put("debits",debits);return row;
    }
    private static SharedCraftPlan plan() {return plan(RECIPIENT,A,B,9);}
    private static SharedCraftPlan plan(UUID recipient,UUID first,UUID second,long run) {
        var tag=new CompoundTag();tag.putInt("schema",1);tag.putString("id",UUID.randomUUID().toString());
        tag.putString("recipient",recipient.toString());tag.putLong("run",run);tag.putLong("sequence",2);tag.putLong("at",1234);
        tag.putLong("donation_before",10);tag.putLong("donation_after",13);
        tag.putString("recipe","positive:healing");tag.putString("skill","Healing potion");tag.putString("recipient_name","Crafter");
        tag.putBoolean("committed",false);tag.putBoolean("metrics_ack",false);
        var owners=new CompoundTag();owners.put(recipient.toString(),owner("minecraft:nether_wart","wart",1));
        owners.put(first.toString(),owner("minecraft:sugar","sugar",2));owners.put(second.toString(),owner("minecraft:glowstone_dust","glowstone",1));
        tag.put("owners",owners);var ranks=new CompoundTag();ranks.putLong(first.toString(),12);ranks.putLong(second.toString(),13);
        tag.put("donated",ranks);var output=new CompoundTag();output.putString("native_fixture","potion");tag.put("output",output);
        return new SharedCraftPlan(tag);
    }
    @Test void supportedNbtRoundTripPreservesUnknownDataAndImmutableImages() {
        var source=plan().image();source.putString("future","preserved");
        source.getCompoundOrEmpty("owners").getCompoundOrEmpty(A.toString()).putString("future_owner","preserved");
        var decoded=SharedCraftPlan.CODEC.parse(NbtOps.INSTANCE,source).getOrThrow();
        assertEquals(source,SharedCraftPlan.CODEC.encodeStart(NbtOps.INSTANCE,decoded).getOrThrow());
        source.putString("future","changed");assertEquals("preserved",decoded.image().getStringOr("future",""));
        var copy=decoded.inventory(A,false);copy.putString("native_fixture","tampered");
        assertEquals("before",decoded.inventory(A,false).getStringOr("native_fixture",""));
        assertEquals("preserved",decoded.commit().image().getStringOr("future",""));
        assertThrows(UnsupportedOperationException.class,()->decoded.owners().clear());
        assertThrows(UnsupportedOperationException.class,()->decoded.donated().clear());
    }
    @Test void preparedCrashPrefixesCancelWithOrWithoutAnOwnerReservation() {
        var prepared=plan();var empty=new CompoundTag();
        for(var owner:prepared.owners()) {
            assertTrue(prepared.recoverable(owner,empty,empty));
            assertTrue(prepared.recoverable(owner,prepared.reservation(owner),empty));
            assertTrue(prepared.recoverable(owner,empty,prepared.receipt(owner)));
        }
        assertFalse(prepared.recoverable(A,prepared.reservation(B),empty));
        var cancelled=prepared.acknowledge(A).acknowledge(B).acknowledge(RECIPIENT);
        assertTrue(cancelled.complete());assertFalse(cancelled.committed());assertFalse(cancelled.metricsAcknowledged());
        assertThrows(IllegalStateException.class,()->prepared.acknowledge(A).commit());
    }
    @Test void committedRecoveryRequiresExactCustodyOrMatchingSavedReceipt() {
        var committed=plan().commit();var empty=new CompoundTag();
        assertFalse(committed.recoverable(A,empty,empty));
        assertTrue(committed.recoverable(A,committed.reservation(A),empty));
        assertTrue(committed.recoverable(A,empty,committed.receipt(A)));
        assertFalse(committed.recoverable(A,committed.reservation(A),committed.receipt(A)));
        assertFalse(committed.recoverable(A,empty,committed.receipt(B)));
        var tampered=committed.reservation(A);tampered.getCompoundOrEmpty("output").putString("native_fixture","foreign");
        assertFalse(committed.recoverable(A,tampered,empty));
        var acknowledged=committed.acknowledge(A);
        assertFalse(acknowledged.recoverable(A,committed.reservation(A),empty));
        assertTrue(acknowledged.recoverable(A,empty,committed.receipt(A)));
    }
    @Test void committedPlanCannotRetireBeforeBothOwnerAndMetricsAcknowledgements() {
        var plan=plan().commit();
        assertThrows(IllegalArgumentException.class,plan::acknowledgeMetrics);
        plan=plan.acknowledge(A).acknowledge(B).acknowledge(RECIPIENT);
        assertTrue(plan.ownersAcknowledged());assertFalse(plan.complete());
        var done=plan.acknowledgeMetrics();assertTrue(done.complete());assertTrue(done.metricsAcknowledged());
        assertTrue(done.recoverable(A,new CompoundTag(),done.receipt(A)));
    }
    @Test void eventProjectionPreservesExactOwnAndSharedQuantities() {
        var plan=plan();var events=plan.events();
        assertEquals(3,events.size());assertTrue(events.stream().allMatch(e->e.transaction().equals(plan.id())&&e.recipient().equals(RECIPIENT)));
        assertEquals(1,events.stream().filter(e->e.own()).mapToLong(e->e.quantity()).sum());
        assertEquals(3,events.stream().filter(e->!e.own()).mapToLong(e->e.quantity()).sum());
        assertTrue(events.stream().anyMatch(e->e.donor().equals(A)&&e.item().equals("minecraft:sugar")&&e.quantity()==2));
    }
    @Test void malformedSchemaCursorsAndInventoriesFailWithoutMutatingInput() {
        for(int mode=0;mode<8;mode++) {
            var image=plan().image();
            switch(mode) {
                case 0->image.putInt("schema",2);
                case 1->image.putInt("run",9);
                case 2->image.putLong("run",0);
                case 3->image.putLong("sequence",0);
                case 4->image.putLong("donation_after",14);
                case 5->image.putBoolean("metrics_ack",true);
                case 6->image.getCompoundOrEmpty("owners").getCompoundOrEmpty(A.toString()).remove("before");
                default->image.getCompoundOrEmpty("owners").getCompoundOrEmpty(A.toString()).getCompoundOrEmpty("debits")
                        .getCompoundOrEmpty("0").putInt("count",65);
            }
            var before=image.copy();assertThrows(RuntimeException.class,()->new SharedCraftPlan(image));assertEquals(before,image);
        }
    }
    @Test void noncanonicalDuplicateSlotsAndImpossibleDonationRanksAreRejected() {
        var duplicate=plan().image();
        var debits=duplicate.getCompoundOrEmpty("owners").getCompoundOrEmpty(RECIPIENT.toString()).getCompoundOrEmpty("debits");
        debits.put("00",debits.getCompoundOrEmpty("0").copy());
        assertThrows(IllegalArgumentException.class,()->new SharedCraftPlan(duplicate));
        var duplicateRanks=plan().image();duplicateRanks.getCompoundOrEmpty("donated").putLong(A.toString(),13);
        assertThrows(IllegalArgumentException.class,()->new SharedCraftPlan(duplicateRanks));
        var missingLast=plan().image();missingLast.getCompoundOrEmpty("donated").putLong(B.toString(),11);
        assertThrows(IllegalArgumentException.class,()->new SharedCraftPlan(missingLast));
    }
    @Test void journalRetainsDecisionUntilOwnerAndMetricsRecoveryFinish() {
        var raw=new CompoundTag();raw.putInt("schema",1);raw.putString("future","retained");
        var data=new SharedCraftData(raw);var plan=plan();data.reserve(plan);
        assertEquals(plan.id(),data.pending(A).id());assertEquals(plan.id(),data.pendingRun(9).id());
        data.commit(plan.id());data.acknowledge(plan.id(),A);
        var recovered=SharedCraftData.CODEC.parse(NbtOps.INSTANCE,data.image()).getOrThrow();
        assertTrue(recovered.pending(A).acknowledged(A));assertFalse(recovered.pending(B).acknowledged(B));
        assertEquals("retained",recovered.image().getStringOr("future",""));
        assertThrows(IllegalStateException.class,()->recovered.retire(plan.id()));
        recovered.acknowledge(plan.id(),B);recovered.acknowledge(plan.id(),RECIPIENT);
        assertThrows(IllegalStateException.class,()->recovered.retire(plan.id()));
        recovered.acknowledgeMetrics(plan.id());recovered.retire(plan.id());
        assertNull(recovered.pending(A));assertNull(recovered.pendingRun(9));assertTrue(recovered.pending().isEmpty());
    }
    @Test void journalSerializesEachRunAndOwnerButAllowsIndependentRuns() {
        var raw=new CompoundTag();raw.putInt("schema",1);var data=new SharedCraftData(raw);data.reserve(plan());
        var d=new UUID(1,1);var e=new UUID(1,2);var f=new UUID(1,3);
        assertThrows(IllegalStateException.class,()->data.reserve(plan(d,e,f,9)));
        assertThrows(IllegalStateException.class,()->data.reserve(plan(RECIPIENT,e,f,10)));
        var independent=plan(d,e,f,10);data.reserve(independent);
        assertEquals(2,data.pending().size());assertEquals(independent.id(),data.pending(d).id());
        raw.putInt("schema",2);assertThrows(IllegalArgumentException.class,()->new SharedCraftData(raw));
    }

    @Test void cursorSchemaIsAdditiveImmutableAndRequiresBothOwnerImages() {
        var old=plan();assertFalse(old.cursor());assertFalse(old.hasFuel());
        var source=old.image();source.putInt("schema",3);source.put("fuel",BrewingFuel.plan(BrewingFuel.empty(),3));
        var before=new CompoundTag();var after=new CompoundTag();after.putString("fixture","cursor_after");
        source.put("cursor_before",before);source.put("cursor_after",after);source.putString("future_cursor","preserved");
        var cursor=new SharedCraftPlan(source);assertTrue(cursor.cursor());assertTrue(cursor.hasFuel());
        assertEquals(source,SharedCraftPlan.CODEC.encodeStart(NbtOps.INSTANCE,cursor).getOrThrow());
        var exposed=cursor.cursor(true);exposed.putString("fixture","tampered");assertEquals(after,cursor.cursor(true));
        var committed=cursor.commit();assertEquals("preserved",committed.image().getStringOr("future_cursor",""));
        for(String key:List.of("cursor_before","cursor_after","fuel")){
            var missing=source.copy();missing.remove(key);assertThrows(IllegalArgumentException.class,()->new SharedCraftPlan(missing));
        }
        source.putInt("schema",4);assertThrows(IllegalArgumentException.class,()->new SharedCraftPlan(source));
    }
}
