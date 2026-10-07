package net.goui.cosmicdungeon.playerclass.theurgist;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TheurgistPotionProtectionTest {
    @Test void positiveHostilesAndAlliedInversionAreBlockedWithoutChangingNegativeNeutralOrOtherClasses(){
        assertFalse(TheurgistPotionProtection.permits(true,true,true,false,true));
        assertFalse(TheurgistPotionProtection.permits(true,true,true,false,false));
        assertFalse(TheurgistPotionProtection.permits(true,true,false,true,false));
        assertTrue(TheurgistPotionProtection.permits(true,true,false,true,true));
        assertTrue(TheurgistPotionProtection.permits(true,true,false,false,true));
        assertTrue(TheurgistPotionProtection.permits(true,true,false,false,false));
        for(boolean helpful:new boolean[]{false,true}){
            assertTrue(TheurgistPotionProtection.permits(true,false,true,false,helpful));
            assertTrue(TheurgistPotionProtection.permits(false,true,true,false,helpful));
            assertTrue(TheurgistPotionProtection.permits(false,true,false,true,helpful));
        }
    }
    @Test void bothPositiveAndNegativeClassSnapshotsSurviveCopyAndUnknownFields(){
        var owner=UUID.randomUUID();
        for(boolean theurgist:new boolean[]{false,true}){
            var original=TheurgistPotionProtection.snapshot(owner,theurgist);original.putString("future","retained");
            var restored=original.copy();
            assertEquals(theurgist,TheurgistPotionProtection.marked(restored));
            assertEquals(owner.toString(),restored.getStringOr("owner",""));
            assertEquals("retained",restored.getStringOr("future",""));
            restored.putBoolean("theurgist",!theurgist);
            assertEquals(theurgist,TheurgistPotionProtection.marked(original));
        }
        assertNull(TheurgistPotionProtection.marked(new CompoundTag()));
        var malformed=TheurgistPotionProtection.snapshot(owner,true);malformed.putString("owner","bad");
        assertNull(TheurgistPotionProtection.marked(malformed));
    }
}
