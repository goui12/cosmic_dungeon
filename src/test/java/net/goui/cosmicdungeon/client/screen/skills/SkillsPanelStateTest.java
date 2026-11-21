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
        assertEquals(SkillsPanelState.HEADER,state.geometry().header().height());
        assertEquals(SkillsPanelState.RESOURCE,state.geometry().resource().height());
        assertEquals(0,state.geometry().body().height());
        assertEquals(SkillsPanelState.HEADER+SkillsPanelState.RESOURCE,state.geometry().panel().height());
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
            assertEquals(SkillsPanelState.BUTTON,box.row(i,0).height());
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
    @Test void contentGrowthUsesOnlyTheRequiredSpaceBeforeScrolling(){
        var panel=new SkillsPanelState(layout(854,480),null);
        panel.content(1,96);
        var small=panel.geometry();
        assertEquals(96,small.panel().width());
        assertEquals(SkillsPanelState.ROW,small.body().height());
        panel.content(7,128);
        assertEquals(128,panel.geometry().panel().width());
        assertEquals(7*SkillsPanelState.ROW,panel.geometry().body().height());
        assertEquals(0,panel.scroll().max());assertEquals(0,panel.geometry().track().width());
        panel.actions(110);
        assertTrue(panel.scroll().max()>0);assertTrue(panel.geometry().track().width()>0);
        panel.content(1,96);
        assertEquals(small,panel.geometry());assertEquals(0,panel.scroll().offset());
    }
    @Test void draggedSkillsLeaveAContiguousLobbyAndThreeButtonFooter() {
        var shared = layout(400,240);
        var state = new SkillsPanelState(shared,null);
        state.actions(14);
        state.reserveGroup(96,234,112);
        var header = state.geometry().header();
        assertTrue(state.press(header.x()+2,header.y()+3,0,false).consumed());
        for (int requestedY = 0; requestedY <= 240; requestedY++) {
            assertTrue(state.drag(12, requestedY, 0, false));
            var panel = state.geometry().panel();
            int above = panel.y() - 4 - 8;
            int below = 234 - panel.bottom() - 4;
            assertTrue(Math.max(above,below) >= 112, "Footer and first row must fit at y=" + requestedY);
        }
        state.release();
    }
    @Test void emptyRightSideAcceptsDragsAcrossTheInventoryAtSmallAndLargeGuiScales() {
        for (int width : new int[]{320, 480, 854, 1696}) {
            var shared = SkillsPanelClient.reservePalette(layout(width,240), new SharedInventoryLayout.Rect(0,0,0,0));
            var state = new SkillsPanelState(shared,null);
            state.content(3,92);
            var header = state.geometry().header();
            assertTrue(state.press(header.x()+2,header.y()+3,0,false).consumed());
            assertTrue(state.drag(width,100,0,false));
            var right = state.geometry().panel();
            assertEquals(width-4,right.right());
            assertTrue(right.x() >= (width+176)/2, "Skills must fit to the right of the native inventory");
            assertTrue(state.release());
            var restored = new SkillsPanelState(shared,state.placement());
            restored.content(3,92);
            assertEquals(right,restored.geometry().panel());
            assertTrue(restored.press(right.x()+2,right.y()+3,0,false).consumed());
            assertTrue(restored.drag(0,100,0,false));
            assertEquals(4,restored.geometry().panel().x());
            assertTrue(restored.release());
        }
    }
    @Test void anOccupiedPanelOnlyReservesItsOwnRectangleAfterDraggingOrResizing() {
        var room = layout(1200,480);
        var state = new SkillsPanelState(room,null);
        state.content(2,110);
        var header = state.geometry().header();
        assertTrue(state.press(header.x()+2,header.y()+3,0,false).consumed());
        assertTrue(state.drag(1200,room.requests().y()+10,0,false));
        assertTrue(state.geometry().panel().x() >= room.requests().right());
        assertTrue(state.drag(room.requests().x()+15,4,0,false));
        assertTrue(state.geometry().panel().x() >= room.requests().x());
        assertTrue(state.geometry().panel().bottom() <= room.requests().y());
        assertTrue(state.drag(room.requests().x()+15,room.requests().y()+8,0,false));
        assertFalse(state.geometry().panel().intersects(room.requests()));
        state.release();
        var smaller = layout(320,240);
        state.resize(smaller);
        assertFalse(state.geometry().panel().intersects(smaller.requests()));
    }
    @Test void invalidSavedCoordinatesAreRejectedAndOutOfRangeValuesRecover(){
        assertThrows(IllegalArgumentException.class,()->new SkillsPanelState.Placement(Double.NaN,0,false));
        var state=new SkillsPanelState(layout(320,240),new SkillsPanelState.Placement(-20,30,false));
        assertEquals(0,state.placement().x());assertEquals(1,state.placement().y());
        assertTrue(state.geometry().panel().x()>=0&&state.geometry().panel().bottom()<=240);
    }
}
