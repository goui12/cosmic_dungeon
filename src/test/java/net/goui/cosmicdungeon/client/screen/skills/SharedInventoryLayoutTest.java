package net.goui.cosmicdungeon.client.screen.skills;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SharedInventoryLayoutTest {
    private void inside(SharedInventoryLayout.Rect r,int width,int height){
        assertTrue(r.x()>=0&&r.y()>=0&&r.right()<=width&&r.bottom()<=height,r.toString());
    }
    @Test void survivalAndCreativeDefaultRegionsStayVisibleAndOutsideSlotsAtSupportedScales(){
        for(int width:new int[]{320,426,480,640,854,1920})
            for(int height:new int[]{240,270,480,1080})
                for(int image:new int[]{176,195}){
                    int left=(width-image)/2,top=(height-166)/2;
                    var shared=SharedInventoryLayout.of(width,height,left,top,image,166);
                    var slots=new SharedInventoryLayout.Rect(left,top,image,166);
                    assertFalse(shared.compact(),width+" "+image);
                    var regions=List.of(shared.group(),shared.requests(),shared.skillsDefault(),shared.account());
                    for(var r:regions){inside(r,width,height);assertFalse(r.intersects(slots),r+" overlaps "+slots);}
                    for(int i=0;i<regions.size();i++)for(int j=i+1;j<regions.size();j++)
                        assertFalse(regions.get(i).intersects(regions.get(j)),regions.toString());
                    assertEquals(left+image+8,shared.requests().x());
                    assertTrue(shared.account().width()>=112);
                    assertEquals(29,shared.account().height());
                    inside(shared.worldResource(),width,height);
                }
    }
    @Test void genuinelyUnsupportedViewportKeepsACompactVisibleHeaderWithoutMovingContainer(){
        var shared=SharedInventoryLayout.of(240,160,32,-3,176,166);
        assertTrue(shared.compact());
        var state=new SkillsPanelState(shared,null);
        assertTrue(state.minimized());assertEquals(0,state.geometry().body().height());
        inside(state.geometry().panel(),240,160);
    }
}
