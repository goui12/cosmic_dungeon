package net.goui.cosmicdungeon.client.screen.requests;

import java.util.*;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SupplyRequestsLayoutTest {
    private SupplyRequestsState state(int width,int height,int imageWidth,int imageHeight){
        var state=new SupplyRequestsState();state.connected(true);
        state.resize(SharedInventoryLayout.of(width,height,(width-imageWidth)/2,(height-imageHeight)/2,imageWidth,imageHeight).requests());
        var cards=new ArrayList<SupplyRequestsSnapshot.Card>();
        for(int i=0;i<8;i++)cards.add(SupplyRequestsSnapshotTest.card(UUID.randomUUID(),4,true));
        state.receive(new SupplyRequestsSnapshot(8,2,true,true,false,cards));return state;
    }
    @Test void survivalAndCreativeColumnsKeepControlsOutsideSlotsAcrossGuiScales(){
        for(int width:new int[]{320,426,640,854})for(int imageWidth:new int[]{176,195}){
            var state=state(width,240,imageWidth,166);var box=state.geometry();
            var inventory=new Rect((width-imageWidth)/2,37,imageWidth,166);
            assertTrue(state.visible());assertFalse(box.panel().intersects(inventory));
            assertTrue(box.panel().right()<=width&&box.panel().bottom()<=240);
            assertTrue(box.acceptAll().y()>=box.body().bottom());assertTrue(box.denyAll().bottom()<=box.panel().bottom());
            var card=state.card(0);
            for(var button:List.of(card.accept(),card.deny())){
                assertTrue(button.x()>=card.panel().x()&&button.right()<=card.panel().right());
                assertTrue(button.bottom()<=card.panel().bottom());
            }
            assertFalse(card.accept().intersects(card.deny()));
        }
    }
    @Test void everyQuotedStackHasAReviewableIconBeforeConsentEvenInNarrowCreativeLane(){
        var state=state(320,240,195,136);var ingredients=new ArrayList<SupplyRequestsSnapshot.Ingredient>();
        for(int i=0;i<64;i++)ingredients.add(new SupplyRequestsSnapshot.Ingredient(new ItemStack(Items.SUGAR)));
        var card=new SupplyRequestsSnapshot.Card(UUID.randomUUID(),UUID.randomUUID(),"Long requester name","theurgist",
                "brewing_supplies",64,ingredients,true);
        state.receive(new SupplyRequestsSnapshot(8,3,true,true,false,List.of(card)));var box=state.card(0);
        assertEquals(64,box.ingredients().size());assertTrue(state.scroll().max()>0);
        for(var icon:box.ingredients()){
            assertTrue(icon.x()>=box.panel().x()&&icon.right()<=box.panel().right());
            assertTrue(icon.bottom()<=box.yield().y());assertFalse(icon.intersects(box.accept()));
        }
        var footer=state.geometry().acceptAll();state.scroll().wheel(-10000);
        assertEquals(footer,state.geometry().acceptAll());assertTrue(state.card(0).deny().bottom()<=state.geometry().body().bottom());
    }
    @Test void wheelThumbCaptureAndAuxiliaryClicksNeverFallThroughToInventory(){
        var state=state(640,480,176,166);var body=state.geometry().body();var track=state.geometry().track();
        assertFalse(state.press(body.x()+3,body.y()+3,0,true).consumed());
        assertFalse(state.wheel(body.x()+3,body.y()+3,-1,true));
        for(int button:new int[]{1,2}){
            assertTrue(state.press(body.x()+3,body.y()+3,button,false).consumed());
            assertFalse(state.captured());assertFalse(state.pending());assertTrue(state.release(button));assertFalse(state.release(button));
        }
        assertTrue(state.wheel(body.x()+3,body.y()+3,-10000,false));assertEquals(state.scroll().max(),state.scroll().offset());
        assertTrue(state.wheel(body.x()+3,body.y()+3,-1,false),"Endpoint still owns wheel");
        assertTrue(state.press(track.x()+2,track.y()+2,0,false).consumed());assertTrue(state.captured());
        assertTrue(state.owns(0,0,false));assertTrue(state.drag(10000,0,false));assertEquals(state.scroll().max(),state.scroll().offset());
        assertTrue(state.release());assertFalse(state.drag(0,0,false));
        state.press(track.x()+2,track.y()+2,0,false);assertFalse(state.drag(0,0,true));assertFalse(state.captured());
        assertFalse(state.press(0,0,0,false).consumed());assertFalse(state.wheel(0,0,1,false));
    }
    @Test void footerConsentUsesWholeSnapshotAndResizeClampsWithoutSavingOrHiddenCapture(){
        var state=state(640,480,176,166);var footer=state.geometry().acceptAll();
        var press=state.press(footer.x()+2,footer.y()+2,0,false);
        assertTrue(press.consumed());assertNotNull(press.action());assertEquals(8,press.action().requestIds().size());
        state.resize(new Rect(0,0,35,160));assertFalse(state.captured());assertFalse(state.visible());
        assertFalse(state.press(2,2,0,false).consumed());assertFalse(state.wheel(2,2,1,false));assertFalse(state.owns(2,2,false));
        state.resize(new Rect(265,37,47,136));assertTrue(state.visible());
        assertTrue(state.scroll().offset()<=state.scroll().max());
    }
}
