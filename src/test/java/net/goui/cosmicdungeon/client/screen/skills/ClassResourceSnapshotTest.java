package net.goui.cosmicdungeon.client.screen.skills;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ClassResourceSnapshotTest {
    private ClassResourceSnapshot value(int amount){return new ClassResourceSnapshot(7,"brewing_supplies",amount,600,true,true,true,2);}
    @Test void onlyServerRangeAndKnownResourceIdentityCanReachPresentation(){
        assertEquals("theurgist",value(0).classId());
        assertEquals("bogatyr",new ClassResourceSnapshot(7,"kibble",0,600,true,true,false,2).classId());
        assertTrue(value(0).matches("theurgist"));assertFalse(value(0).matches("bogatyr"));
        assertEquals("599/600",value(599).count());
        for(int amount:new int[]{-1,601,Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class,()->value(amount));
        assertThrows(IllegalArgumentException.class,()->new ClassResourceSnapshot(7,"other",0,600,true,true,true,2));
        assertThrows(IllegalArgumentException.class,()->new ClassResourceSnapshot(7,"kibble",0,599,true,true,true,2));
        assertThrows(IllegalArgumentException.class,()->new ClassResourceSnapshot(-1,"kibble",0,600,true,true,true,2));
        assertThrows(IllegalArgumentException.class,()->new ClassResourceSnapshot(7,"kibble",0,600,true,true,true,-1));
        var empty=new ClassResourceSnapshot(0,"",0,600,false,false,false,0);
        assertFalse(empty.matches("none"));assertFalse(empty.canRecycle());
        assertThrows(IllegalArgumentException.class,()->new ClassResourceSnapshot(0,"kibble",0,600,false,false,false,0));
        assertThrows(IllegalArgumentException.class,()->new ClassResourceSnapshot(0,"",1,600,false,false,false,0));
    }
    @Test void fillRepresentsExactCapacityWithoutOverflowOrInventedMinimum(){
        for(int width:new int[]{0,1,34,156,182,Integer.MAX_VALUE}){
            assertEquals(0,value(0).filledPixels(width));
            assertEquals(width,value(600).filledPixels(width));
            assertEquals((int)((long)width*599/600),value(599).filledPixels(width));
            assertEquals(width/600,value(1).filledPixels(width));
        }
        assertEquals(0,value(600).filledPixels(-1));
    }
}
