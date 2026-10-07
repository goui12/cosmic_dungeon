package net.goui.cosmicdungeon.client.screen.skills;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ClassResourceBarTest {
    @Test void worldGeometryClearsNativeHudAndActionTextAcrossGuiScales(){
        for(int width:new int[]{320,426,640,854,1920})for(int height:new int[]{240,360,480,1080})
            for(int stack:new int[]{39,49,59,79,119}){
                var box=ClassResourceBar.world(width,height,stack);
                assertEquals(182,box.width());assertEquals(18,box.height());
                assertEquals((width-182)/2,box.x());
                assertTrue(box.bottom()<=height-Math.max(68,stack+9)-8);
                assertTrue(box.y()>=0&&box.right()<=width&&box.bottom()<=height);
            }
        var shared=SharedInventoryLayout.of(854,480,339,157,176,166);
        assertEquals(ClassResourceBar.world(854,480,39),shared.worldResource());
    }
    @Test void tinyViewportsAndExtremeNativeStacksStayBoundedWithoutArithmeticOverflow(){
        for(int width:new int[]{0,1,8,40,320})for(int height:new int[]{0,1,18,60,240})
            for(int stack:new int[]{0,39,Integer.MAX_VALUE}){
                var box=ClassResourceBar.world(width,height,stack);
                assertTrue(box.x()>=0&&box.y()>=0&&box.width()>=0&&box.height()>=0);
                assertTrue(box.right()<=width&&box.bottom()<=height);
            }
    }
    @Test void compactResourceCountsScaleToTheirAvailableWidth(){
        assertEquals(100,ClassResourceBar.textScalePercent(0,20));
        assertEquals(100,ClassResourceBar.textScalePercent(38,80));
        assertEquals(50,ClassResourceBar.textScalePercent(80,40));
        assertEquals(1,ClassResourceBar.textScalePercent(80,0));
        assertEquals(1,ClassResourceBar.textScalePercent(Integer.MAX_VALUE,1));
        assertEquals(100,ClassResourceBar.textScalePercent(1,Integer.MAX_VALUE));
        for(int available=24;available<=170;available++){
            int percent=ClassResourceBar.textScalePercent(44,available);
            assertTrue(44*percent/100.0<=available);
        }
    }
}
