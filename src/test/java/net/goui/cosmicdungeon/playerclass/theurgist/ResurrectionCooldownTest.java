package net.goui.cosmicdungeon.playerclass.theurgist;

import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ResurrectionCooldownTest {
    @Test void exactBoundaryAndRounding(){
        var empty=new CompoundTag();var started=ResurrectionCooldown.start(empty,100);
        assertTrue(empty.isEmpty());assertEquals(3700,ResurrectionCooldown.deadline(started));
        assertEquals(180,ResurrectionCooldown.seconds(started,100));
        assertEquals(180,ResurrectionCooldown.seconds(started,119));
        assertEquals(179,ResurrectionCooldown.seconds(started,120));
        assertEquals(1,ResurrectionCooldown.seconds(started,3699));
        assertEquals(0,ResurrectionCooldown.seconds(started,3700));
        assertThrows(IllegalStateException.class,()->ResurrectionCooldown.start(started,3699));
        assertEquals(7300,ResurrectionCooldown.deadline(ResurrectionCooldown.start(started,3700)));
    }
    @Test void independentCopiesPreserveUnknownDataAndRejectMalformedState(){
        var prior=ResurrectionCooldown.start(new CompoundTag(),0);prior.putString("future","keep");
        var next=ResurrectionCooldown.start(prior,3600);
        assertEquals("keep",next.getStringOr("future",""));assertEquals(3600,ResurrectionCooldown.deadline(prior));
        var root=new CompoundTag();root.put(ResurrectionCooldown.KEY,next);var copy=ResurrectionCooldown.read(root);
        copy.putLong("ready_at",1);assertEquals(7200,ResurrectionCooldown.deadline(ResurrectionCooldown.read(root)));
        root.putString(ResurrectionCooldown.KEY,"corrupt");assertThrows(IllegalArgumentException.class,()->ResurrectionCooldown.read(root));
        var wrong=next.copy();wrong.putInt("ready_at",7200);assertThrows(IllegalArgumentException.class,()->ResurrectionCooldown.deadline(wrong));
        var future=next.copy();future.putInt("schema",99);assertThrows(IllegalArgumentException.class,()->ResurrectionCooldown.deadline(future));
        assertThrows(IllegalArgumentException.class,()->ResurrectionCooldown.seconds(next,-1));
        assertThrows(ArithmeticException.class,()->ResurrectionCooldown.start(new CompoundTag(),Long.MAX_VALUE));
    }
    @Test void freePlanRecordsOnlyCooldownAndRetainsExactDecisionAcrossJournalCodec(){
        var p=RevivalPlan.free(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),22,new CompoundTag(),100);
        assertEquals(2,p.schema());assertEquals(ResurrectionCooldown.KEY,p.stateKey());
        assertTrue(p.tag("before").isEmpty());assertEquals(3700,ResurrectionCooldown.deadline(p.tag("after")));
        assertEquals(2,p.receipt(p.caster(),true).getIntOr("schema",0));
        var base=new CompoundTag();base.putInt("schema",1);var journal=new RevivalData(base);journal.reserve(p);
        var round=RevivalData.CODEC.parse(NbtOps.INSTANCE,RevivalData.CODEC.encodeStart(NbtOps.INSTANCE,journal).getOrThrow()).getOrThrow();
        assertEquals(p.image(),round.pending(p.target()).image());
        round.decide(p.id(),true);assertEquals(p.reservation(p.caster()),round.pending(p.caster()).reservation(p.caster()));
        round.acknowledge(p.id(),p.target());round.acknowledge(p.id(),p.caster());assertNull(round.pending(p.caster()));
    }
    @Test void forgedCooldownPlansCannotChangeDurationOrConsumeAnActiveCooldown(){
        var p=RevivalPlan.free(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),22,new CompoundTag(),100);
        var wrong=p.image();wrong.getCompoundOrEmpty("after").putLong("ready_at",3699);
        assertThrows(IllegalArgumentException.class,()->new RevivalPlan(wrong));
        var active=p.image();active.put("before",p.tag("after"));
        assertThrows(IllegalStateException.class,()->new RevivalPlan(active));
        var future=p.image();future.putInt("schema",99);assertThrows(IllegalArgumentException.class,()->new RevivalPlan(future));
    }
    @Test void boundedOfferExpiryRejectsExpiredAndRolledBackClock(){
        assertTrue(TheurgistActions.unexpired(1200,0));assertTrue(TheurgistActions.unexpired(1200,1199));
        assertFalse(TheurgistActions.unexpired(1200,1200));assertFalse(TheurgistActions.unexpired(1200,1201));
        assertFalse(TheurgistActions.unexpired(1201,0));assertFalse(TheurgistActions.unexpired(10,-1));
    }
}
