package net.goui.cosmicdungeon.client.screen;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class PartyHealthLayoutTest {
    @Test void everyWorldCardFitsAndLeavesHotbarAndCurrencyRoom() {
        for(int width:new int[]{320,426,640,960,1920})for(int height:new int[]{240,360,540,1080})for(int count=1;count<=9;count++){
            var box=PartyHealthLayout.world(width,height,count);
            assertTrue(box.x()+box.columns()*box.width()+(box.columns()-1)*4<=width-8);
            assertTrue(box.y()+box.height()+37<=height-48);
            assertTrue(box.rowsPerColumn()*box.columns()>=count);
            assertTrue(box.width()>=80);
        }
    }
    @Test void inventoryScrollReachesAllEffectsWithoutCoveringSlotsOrFooter() {
        for(int width:new int[]{320,426,640,960})for(int inventoryWidth:new int[]{176,195}){
            int left=(width-inventoryWidth)/2;
            var box=PartyHealthLayout.inventory(left,240);
            assertTrue(box.x()+box.width()+8<=left);
            assertTrue(box.y()+box.height()<214);
            int content=9*box.rowHeight(64);
            assertTrue(box.maxScroll(content)>0);
            assertEquals(0,box.clampScroll(-100,content));
            assertEquals(box.maxScroll(content),box.clampScroll(Integer.MAX_VALUE,content));
            assertEquals(box.maxScroll(content),box.scrollAt(box.y()+box.height()-box.thumbHeight(content),content));
            assertEquals(box.y()+box.height(),box.thumbY(box.maxScroll(content),content)+box.thumbHeight(content));
            assertEquals(0,box.scrollAt(-999,content));
            assertFalse(box.contains(left,50));
            assertTrue(box.contains(10,50));
        }
    }
    @Test void shortContentNeverDividesByZeroOrScrolls() {
        var box=PartyHealthLayout.inventory(200,540);
        assertEquals(0,box.maxScroll(37));
        assertEquals(0,box.scrollAt(1000,37));
        assertEquals(box.viewport(),box.thumbHeight(37));
    }
}
