package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryLightningTest {
    @Test void fiftyInitialHitsAndOneLessPerLevelWithBoundedMinimum(){
        assertEquals(50,MercenaryLightningState.threshold(1));
        for(int level=2;level<=46;level++)assertEquals(51-level,MercenaryLightningState.threshold(level));
        assertEquals(5,MercenaryLightningState.threshold(65536));
        assertEquals(50,MercenaryLightningState.threshold(Integer.MIN_VALUE));
        assertEquals(5,MercenaryLightningState.threshold(Integer.MAX_VALUE));
        var state=MercenaryLightningState.initial();var id=UUID.randomUUID();
        for(int i=0;i<49;i++)state=state.primary(8,id);
        assertFalse(state.ready(1));assertTrue(state.primary(8,id).ready(1));
    }
    @Test void eachSecondaryHitCountsOnceWithoutChangingPowerOrVictim(){
        var id=UUID.randomUUID();var state=new MercenaryLightningState(50,8,Optional.of(id),0).spend(1);
        for(int i=0;i<64;i++)state=state.secondary();
        assertEquals(64,state.hits());assertEquals(8,state.damage());assertEquals(Optional.of(id),state.excluded());
        assertFalse(state.ready(2));assertFalse(state.advance(19).ready(2));
        state=state.advance(20);assertTrue(state.ready(2));
        assertEquals(15,state.spend(2).hits());assertFalse(state.spend(2).ready(2));
    }
    @Test void successfulCastsUseTriangularProgressRatherThanTargetCount(){
        for(int casts=0;casts<=10;casts++){
            int expected=casts==0?1:casts<3?2:casts<6?3:casts<10?4:5;
            assertEquals(expected,MercenarySkill.level(casts));
        }
        assertEquals(10,MercenarySkill.threshold(5));
    }
    @Test void failedCastRefundsChargeWithoutLosingConcurrentSecondaryHits(){
        var state=new MercenaryLightningState(50,3,Optional.empty(),0).spend(1);
        assertEquals(50,state.refund(50).hits());assertEquals(20,state.refund(50).cooldown());
        assertEquals(51,state.secondary().refund(50).hits());
        assertThrows(IllegalStateException.class,()->MercenaryLightningState.initial().spend(1));
    }
    @Test void integerSaturationAndInvalidDamageCannotCreateExplosivePower(){
        var state=new MercenaryLightningState(Integer.MAX_VALUE,8,Optional.empty(),0);
        assertEquals(Integer.MAX_VALUE,state.secondary().hits());
        assertEquals(Integer.MAX_VALUE,state.refund(50).hits());
        assertEquals(state,state.primary(Float.NaN,UUID.randomUUID()));
        assertEquals(state,state.primary(Float.POSITIVE_INFINITY,UUID.randomUUID()));
        assertEquals(state,state.primary(0,UUID.randomUUID()));
        assertEquals(0,new MercenaryLightningState(-1,Float.NaN,Optional.empty(),100).hits());
        assertEquals(0,new MercenaryLightningState(50,Float.NaN,Optional.empty(),0).damage());
        assertFalse(new MercenaryLightningState(50,0,Optional.empty(),0).ready(1));
    }
    @Test void nativeCodecRetainsChargeDamageVictimAndActiveCooldown(){
        for(var state:List.of(MercenaryLightningState.initial(),
                new MercenaryLightningState(73,7.25f,Optional.of(UUID.randomUUID()),13),
                new MercenaryLightningState(Integer.MAX_VALUE,8,Optional.empty(),20))){
            var encoded=MercenaryLightningState.CODEC.encodeStart(NbtOps.INSTANCE,state).getOrThrow();
            assertEquals(state,MercenaryLightningState.CODEC.parse(NbtOps.INSTANCE,encoded).getOrThrow());
        }
        var corrupt=new CompoundTag();corrupt.putInt("hits",-1);corrupt.putFloat("damage",8);corrupt.putInt("cooldown",0);
        assertTrue(MercenaryLightningState.CODEC.parse(NbtOps.INSTANCE,corrupt).error().isPresent());
    }
}
