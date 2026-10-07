package net.goui.cosmicdungeon.client.screen.skills;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.goui.cosmicdungeon.client.screen.requests.SupplyRequestAction.Decision;
import net.goui.cosmicdungeon.client.screen.requests.SupplyRequestsSnapshot;
import net.goui.cosmicdungeon.client.screen.requests.SupplyRequestsState;
import net.goui.cosmicdungeon.network.BogatyrPayloads;
import net.goui.cosmicdungeon.network.TheurgistPayloads;
import net.goui.cosmicdungeon.playerclass.bogatyr.WolfMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Set C composition checks; native drawing, GUI-scale rendering and licensed play remain manual QA. */
final class SetCSkillsIntegrationTest {
    @TempDir Path directory;

    @BeforeEach void prepare() {
        BogatyrClient.clear();
        TheurgistClient.clear();
        BogatyrClient.actions(action -> fail("Geometry must not send care actions"));
        BogatyrClient.modeActions(action -> fail("Geometry must not send mode actions"));
        TheurgistClient.actions(action -> fail("Geometry must not send crafting or resurrection actions"));
        BogatyrClient.accept(new BogatyrPayloads.View(26,1,4,List.of(
                new BogatyrPayloads.Quote(2,10,true),new BogatyrPayloads.Quote(1,30,true),
                new BogatyrPayloads.Quote(4,4,true),new BogatyrPayloads.Quote(1,5,true)),
                WolfMode.DANGER_CLOSE,true));
        var targets=new ArrayList<TheurgistPayloads.Target>();
        for(int i=0;i<TheurgistPayloads.MAX_TARGETS;i++)
            targets.add(new TheurgistPayloads.Target(new UUID(26,i+1),"Teammate "+i,new UUID(27,i+1),i%2==0));
        TheurgistClient.receive(new TheurgistPayloads.View(26,1,true,true,true,true,targets,null));
    }

    @AfterEach void clear() {
        BogatyrClient.clear();
        TheurgistClient.clear();
    }

    private static SharedInventoryLayout layout(int width,int height,int image) {
        return SharedInventoryLayout.of(width,height,(width-image)/2,(height-166)/2,image,166);
    }

    private static SkillsPanelModel model(String id) {
        var resource=new ClassResourceSnapshot(26,id.equals("bogatyr")?"kibble":"brewing_supplies",
                177,600,true,true,true,7);
        var base=ClassResourcePresentation.panel(id,resource,false,true);
        var combined=id.equals("bogatyr")?BogatyrClient.augment(base):TheurgistClient.augment(base);
        assertSame(resource,combined.resourceSnapshot(),"Class actions must retain the authoritative shared resource");
        assertEquals(base.resourceTooltip(),combined.resourceTooltip());
        assertEquals(List.of("request_supplies","recycle"),
                combined.actions().subList(0,2).stream().map(SkillsPanelModel.Action::id).toList());
        return combined;
    }

    private static SupplyRequestsState requests(SharedInventoryLayout shared) {
        var cards=new ArrayList<SupplyRequestsSnapshot.Card>();
        for(int i=0;i<SupplyRequestsSnapshot.MAX_CARDS;i++) {
            boolean bogatyr=i%2==0;
            cards.add(new SupplyRequestsSnapshot.Card(new UUID(28,i+1),new UUID(29,i+1),"Requester "+i,
                    bogatyr?"bogatyr":"theurgist",bogatyr?"kibble":"brewing_supplies",0,List.of(),false));
        }
        var requests=new SupplyRequestsState();
        requests.connected(true);
        requests.resize(shared.requests());
        requests.receive(new SupplyRequestsSnapshot(26,7,true,true,true,cards));
        return requests;
    }

    private static void bounded(SkillsPanelState panel,SharedInventoryLayout shared) {
        var box=panel.geometry().panel();
        assertTrue(box.x()>=0&&box.y()>=0&&box.right()<=shared.screenWidth()
                &&box.bottom()<=shared.screenHeight(),box.toString());
        assertFalse(box.intersects(shared.requests()),"A saved or dragged Skills panel must leave Requests reachable");
    }

    @Test void fullyComposedPanelsKeepEveryActionReachableAtSupportedSurvivalAndCreativeScales() {
        for(var id:List.of("bogatyr","theurgist")) {
            var combined=model(id);
            assertEquals(id.equals("bogatyr")?12:69,combined.actions().size());
            for(int[] size:new int[][]{{320,240},{426,240},{640,360},{854,480}})
                for(int image:new int[]{176,195}) {
                    var shared=layout(size[0],size[1],image);
                    var panel=new SkillsPanelState(shared,null);
                    var requests=requests(shared);
                    panel.actions(combined.actions().size());
                    bounded(panel,shared);
                    assertTrue(requests.visible());
                    for(int i=0;i<combined.actions().size();i++) {
                        panel.scroll().reset();
                        var body=panel.geometry().body();
                        assertTrue(panel.wheel(body.x()+2,body.y()+2,-i,false));
                        var row=panel.geometry().row(i,panel.scroll().offset());
                        double x=row.x()+1,y=Math.max(row.y(),body.y())+1;
                        assertTrue(body.contains(x,y),id+" action "+i+" must have a visible hit area");
                        assertFalse(requests.owns(x,y,false));
                        assertEquals(i,panel.press(x,y,0,false).action(),combined.actions().get(i).id());
                        assertTrue(panel.release());
                    }
                    assertFalse(panel.captured());
                    assertEquals(0,requests.scroll().offset(),"Reviewing class actions cannot scroll supply requests");
                }
        }
    }

    @Test void simultaneousRequestsAndSkillsKeepIndependentScrollConsentAndItemDragOwnership() {
        var shared=layout(640,360,176);
        var panel=new SkillsPanelState(shared,null);
        panel.actions(model("theurgist").actions().size());
        var requests=requests(shared);
        var requestBody=requests.geometry().body();
        requests.wheel(requestBody.x()+2,requestBody.y()+2,-10000,false);
        int requestOffset=requests.scroll().offset();
        assertTrue(requestOffset>0);
        var body=panel.geometry().body();
        panel.wheel(body.x()+2,body.y()+2,-3,false);
        assertEquals(requestOffset,requests.scroll().offset());

        var header=panel.geometry().header();
        panel.press(header.x()+2,header.y()+4,0,false);
        panel.drag(10000,10000,0,false);
        bounded(panel,shared);
        assertFalse(panel.drag(20,20,0,true),"Picking up an item cancels overlay capture");
        assertFalse(panel.captured());
        assertEquals(requestOffset,requests.scroll().offset());

        int last=requests.snapshot().cards().size()-1;
        var deny=requests.card(last).deny();
        double x=deny.x()+1,y=deny.y()+1;
        assertTrue(requests.geometry().body().contains(x,y));
        assertFalse(panel.geometry().panel().contains(x,y));
        assertFalse(requests.press(x,y,0,true).consumed());
        var press=requests.press(x,y,0,false);
        assertTrue(press.consumed());
        assertEquals(Decision.DENY,press.action().decision());
        assertEquals(List.of(requests.snapshot().cards().get(last).requestId()),press.action().requestIds());
        assertEquals(26,press.action().runId());
        assertEquals(7,press.action().revision());
        assertTrue(requests.pending());
        assertEquals(SupplyRequestsSnapshot.MAX_CARDS,requests.snapshot().cards().size(),"Consent is not optimistic removal");
        assertFalse(requests.drag(y,0,true));
        assertFalse(requests.captured());

        var smaller=layout(320,240,195);
        panel.resize(smaller);
        requests.resize(smaller.requests());
        bounded(panel,smaller);
        assertTrue(requests.pending(),"A GUI resize cannot manufacture server acknowledgement");
        requests.receive(requests.snapshot());
        assertFalse(requests.pending());
    }

    @Test void perClassDragMinimizeReloadAndResetPreserveBothModelsAndRequestsSpace() throws Exception {
        var path=directory.resolve("skills.json");
        var preferences=new SkillsPanelPreferences(path);
        var shared=layout(854,480,176);
        for(var id:List.of("bogatyr","theurgist")) {
            var combined=model(id);
            var panel=new SkillsPanelState(shared,null);
            panel.actions(combined.actions().size());
            var header=panel.geometry().header();
            panel.press(header.x()+2,header.y()+4,0,false);
            panel.drag(id.equals("bogatyr")?300:200,id.equals("bogatyr")?380:170,0,false);
            panel.release();
            var toggle=panel.geometry().minimize();
            panel.press(toggle.x()+2,toggle.y()+2,0,false);
            panel.release();
            assertTrue(panel.minimized());
            assertEquals(0,panel.geometry().body().height());
            assertTrue(preferences.put(id,panel.placement()));
            assertEquals(177,combined.resourceSnapshot().amount(),"Moving or minimizing is presentation only");
        }
        var loaded=new SkillsPanelPreferences(path);
        assertNotEquals(loaded.get("bogatyr"),loaded.get("theurgist"));
        var smaller=layout(320,240,195);
        for(var id:List.of("bogatyr","theurgist")) {
            var panel=new SkillsPanelState(smaller,loaded.get(id).orElseThrow());
            panel.actions(model(id).actions().size());
            bounded(panel,smaller);
            assertTrue(panel.minimized());
            var toggle=panel.geometry().minimize();
            panel.press(toggle.x()+2,toggle.y()+2,0,false);
            panel.release();
            assertFalse(panel.minimized());
            assertTrue(panel.scroll().max()>0);
            bounded(panel,smaller);
        }
        assertTrue(loaded.reset());
        var reset=new SkillsPanelPreferences(path);
        for(var id:List.of("bogatyr","theurgist")) {
            assertTrue(reset.get(id).isEmpty());
            var panel=new SkillsPanelState(shared,reset.get(id).orElse(null));
            panel.actions(model(id).actions().size());
            assertEquals(shared.skillsDefault(),panel.geometry().panel());
            assertFalse(panel.minimized());
            assertEquals(0,panel.scroll().offset());
            bounded(panel,shared);
        }
    }
}
