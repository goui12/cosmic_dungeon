package net.goui.cosmicdungeon.client.screen.skills;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SkillsPanelStateTest {
    private SharedInventoryLayout layout(int width,int height){
        return SharedInventoryLayout.of(width,height,(width-176)/2,(height-166)/2,176,166);
    }
    @Test void dragCaptureClampsAndResizesWithoutInvadingRequestsOrPersistingEveryFrame(){
        var state=new SkillsPanelState(layout(854,480),null);state.actions(20);
        var header=state.geometry().header();
        assertFalse(state.press(header.x()+2,header.y()+5,0,true).consumed());
        for(int button:new int[]{1,2}){
            assertTrue(state.press(header.x()+2,header.y()+5,button,false).consumed());
            assertFalse(state.captured());assertTrue(state.release(button));assertFalse(state.release(button));
            assertFalse(state.press(header.x()+2,header.y()+5,button,true).consumed());
            assertFalse(state.press(500,300,button,false).consumed());
        }
        assertFalse(state.drag(100,100,0,false));assertFalse(state.dirty());
        assertTrue(state.press(header.x()+2,header.y()+5,0,false).consumed());assertTrue(state.captured());
        assertTrue(state.drag(10000,10000,0,false));
        assertTrue(state.dirty());
        assertFalse(state.geometry().panel().intersects(layout(854,480).requests()));
        assertTrue(state.release());assertFalse(state.release());
        var saved=state.placement();state.saved();
        state.resize(layout(320,240));
        var box=state.geometry().panel();
        assertTrue(box.x()>=0&&box.y()>=0&&box.right()<=320&&box.bottom()<=240);
        assertFalse(box.intersects(layout(320,240).requests()));
        assertEquals(saved,state.placement());assertFalse(state.dirty());assertFalse(state.captured());
    }
    @Test void minimizingRetainsHeaderResourceAndExactTopLeftAndResetIsVisible(){
        var state=new SkillsPanelState(layout(640,480),new SkillsPanelState.Placement(.7,.8,false));
        var before=state.geometry().panel();var toggle=state.geometry().minimize();
        var result=state.press(toggle.x()+2,toggle.y()+2,0,false);
        assertTrue(result.changed());assertTrue(state.minimized());
        assertEquals(before.x(),state.geometry().panel().x());assertEquals(before.y(),state.geometry().panel().y());
        assertEquals(18,state.geometry().header().height());assertEquals(18,state.geometry().resource().height());
        assertEquals(0,state.geometry().body().height());assertEquals(36,state.geometry().panel().height());
        state.release();toggle=state.geometry().minimize();state.press(toggle.x()+2,toggle.y()+2,0,false);
        assertFalse(state.minimized());assertEquals(before,state.geometry().panel());
        var reset=new SkillsPanelState(layout(320,240),null);
        assertEquals(layout(320,240).skillsDefault(),reset.geometry().panel());
        assertFalse(reset.minimized());assertFalse(reset.dirty());
    }
    @Test void uniformActionsScrollVisiblyAndOnlyOwnTheirPointerSequence(){
        var state=new SkillsPanelState(layout(320,240),null);state.actions(20);
        var box=state.geometry();
        for(int i=0;i<20;i++){
            assertEquals(box.row(0,0).width(),box.row(i,0).width());
            assertEquals(20,box.row(i,0).height());
        }
        assertTrue(state.scroll().max()>0);
        assertFalse(state.wheel(200,100,-1,false));assertFalse(state.wheel(10,50,-1,true));
        assertTrue(state.wheel(box.body().x()+4,box.body().y()+2,-1000,false));
        assertEquals(state.scroll().max(),state.scroll().offset());
        assertTrue(state.wheel(box.body().x()+4,box.body().y()+2,-1,false));
        assertEquals(state.scroll().max(),state.scroll().offset(),"Endpoint still owns wheel, preventing another pane from scrolling");
        state.scroll().reset();
        assertTrue(state.press(box.track().x()+2,box.track().y()+2,0,false).consumed());
        assertTrue(state.drag(box.track().x()+2,1000,0,false));
        assertEquals(state.scroll().max(),state.scroll().offset());
        assertTrue(state.release());assertFalse(state.drag(10,10,0,false));
        var last=box.row(19,state.scroll().offset());
        assertEquals(19,state.press(last.x()+2,last.y()+2,0,false).action());
        state.release();
        var head=state.geometry().header();state.press(head.x()+2,head.y()+4,0,false);
        assertFalse(state.drag(100,100,0,true));assertFalse(state.captured());
    }
    @Test void invalidSavedCoordinatesAreRejectedAndOutOfRangeValuesRecover(){
        assertThrows(IllegalArgumentException.class,()->new SkillsPanelState.Placement(Double.NaN,0,false));
        var state=new SkillsPanelState(layout(320,240),new SkillsPanelState.Placement(-20,30,false));
        assertEquals(0,state.placement().x());assertEquals(1,state.placement().y());
        assertTrue(state.geometry().panel().x()>=0&&state.geometry().panel().bottom()<=240);
    }
}
