package net.goui.cosmicdungeon.playerclass.theurgist;

import java.util.*;
import net.minecraft.nbt.*;
import net.goui.cosmicdungeon.playerclass.resource.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class RevivalPlanTest {
    private static RevivalPlan plan(){
        var root=new CompoundTag();var ledger=ClassResourceLedger.forRun(root,22).withAmount(ClassResourceKind.BREWING_SUPPLIES,240);
        var image=ledger.image();image.putString("future","keep");root.put(ClassResourceLedger.KEY,image);
        return RevivalPlan.create(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),22,ClassResourceLedger.forRun(root,22));
    }
    @Test void exactDebitPreservesUnknownFieldsAndCopies(){
        var p=plan();assertEquals(240,p.tag("before").getCompoundOrEmpty("balances").getIntOr("brewing_supplies",-1));
        assertEquals(120,p.tag("after").getCompoundOrEmpty("balances").getIntOr("brewing_supplies",-1));
        assertEquals("keep",p.tag("after").getStringOr("future",""));
        var copy=p.image();copy.putInt("decision",2);assertEquals(0,p.decision());
        assertNotEquals(p.reservation(p.caster()),p.reservation(p.target()));
        assertEquals(p.reservation(p.caster()),p.decide(true).acknowledge(p.target()).reservation(p.caster()));
    }
    @Test void decisionsAreSingleUseAndReceiptsIdentifyBothOwnersAndLatestDeath(){
        var p=plan();assertThrows(IllegalStateException.class,()->p.acknowledge(p.caster()));
        var decided=p.decide(true);assertThrows(IllegalStateException.class,()->decided.decide(false));
        assertNotEquals(p.receipt(p.target(),true),p.receipt(p.target(),false));
        assertNotEquals(p.receipt(p.caster(),true),p.receipt(p.target(),true));
        assertEquals(p.death().toString(),p.receipt(p.target(),true).getStringOr("death",""));
        assertTrue(decided.acknowledge(p.caster()).acknowledge(p.target()).complete());
    }
    @Test void malformedFutureOrUnrelatedDebitsFailClosed(){
        var p=plan();var image=p.image();image.putInt("schema",2);assertThrows(IllegalArgumentException.class,()->new RevivalPlan(image));
        var wrong=p.image();var after=wrong.getCompoundOrEmpty("after");var balances=after.getCompoundOrEmpty("balances");balances.putInt("brewing_supplies",119);
        assertThrows(IllegalArgumentException.class,()->new RevivalPlan(wrong));
        var ack=p.image();ack.putBoolean("caster_ack",true);assertThrows(IllegalArgumentException.class,()->new RevivalPlan(ack));
        var poor=ClassResourceLedger.forRun(new CompoundTag(),22).withAmount(ClassResourceKind.BREWING_SUPPLIES,119);
        assertThrows(IllegalArgumentException.class,()->RevivalPlan.create(p.caster(),p.target(),p.death(),22,poor));
    }
    @Test void pendingJournalIndexesOwnersAndRetiresOnlyCompletedPlan(){
        var image=new CompoundTag();image.putInt("schema",1);image.putString("unknown","keep");var data=new RevivalData(image);var p=plan();
        data.reserve(p);assertEquals(p.id(),data.pending(p.caster()).id());assertEquals(p.id(),data.pending(p.target()).id());
        assertThrows(IllegalStateException.class,()->data.reserve(p));
        var round=RevivalData.CODEC.parse(NbtOps.INSTANCE,RevivalData.CODEC.encodeStart(NbtOps.INSTANCE,data).getOrThrow()).getOrThrow();
        round.decide(p.id(),false);round.acknowledge(p.id(),p.target());assertNotNull(round.pending(p.caster()));
        round.acknowledge(p.id(),p.caster());assertNull(round.pending(p.target()));assertEquals("keep",round.image().getStringOr("unknown",""));
        var future=image.copy();future.putInt("schema",99);assertThrows(IllegalArgumentException.class,()->new RevivalData(future));
    }
}
