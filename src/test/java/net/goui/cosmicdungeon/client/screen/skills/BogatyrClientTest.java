package net.goui.cosmicdungeon.client.screen.skills;

import java.util.ArrayList;
import java.util.List;
import net.goui.cosmicdungeon.network.BogatyrPayloads;
import net.goui.cosmicdungeon.network.BogatyrPayloads.Quote;
import net.goui.cosmicdungeon.network.BogatyrPayloads.View;
import net.goui.cosmicdungeon.playerclass.bogatyr.WolfMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BogatyrClientTest {
    private final List<BogatyrPayloads.Action> sent=new ArrayList<>();
    private final List<BogatyrPayloads.ModeAction> modes=new ArrayList<>();
    private static SkillsPanelModel model(){
        return BogatyrClient.augment(SkillsPanelModel.initial("bogatyr"));
    }
    private static View view(long revision,WolfMode mode){return view(23,revision,mode,0);}
    private static View view(long run,long revision,WolfMode mode,int feedInputs){
        return new View(run,revision,7,List.of(new Quote(1,5,true),new Quote(1,30,true),
                new Quote(7,7,true),new Quote(2,10,true)),mode,true,feedInputs);
    }
    private static SkillsPanelModel.Action feed(){return model().actions().getLast();}
    @BeforeEach void setup(){
        BogatyrClient.clear();BogatyrClient.actions(sent::add);BogatyrClient.modeActions(modes::add);
    }
    @AfterEach void cleanup(){BogatyrClient.clear();}
    @Test void onlyModesAndFeedRemainEvenWithLegacyQuotesAndInjectedResourceActions(){
        BogatyrClient.accept(view(1,WolfMode.PATROL));
        var legacy=new ClassResourceSnapshot(23,"kibble",105,600,true,true,true,3);
        var old=new SkillsPanelModel("bogatyr","Old","Kibble: 105",net.minecraft.network.chat.Component.literal("Old resource"),
                List.of(new SkillsPanelModel.Action("recycle","Recycle","Old action",true)),legacy);
        var result=BogatyrClient.augment(old);
        assertEquals("Wolfpack",result.title());assertNull(result.resourceSnapshot());
        assertEquals("Loaded wolves: 7",result.resource());
        assertEquals(List.of("Patrol","Stay","Feed Pack"),result.actions().stream().map(SkillsPanelModel.Action::label).toList());
        assertEquals(List.of(BogatyrClient.PATROL,BogatyrClient.STAY,BogatyrClient.FEED_PACK),
                result.actions().stream().map(SkillsPanelModel.Action::id).toList());
        assertFalse(result.resourceTooltip().getString().contains("Kibble"));
        for(var row:result.actions()){
            assertFalse(row.tooltip().getString().contains("Kibble"));
            assertTrue(row.tooltip().getString().startsWith(row.label()+"\n"));
        }
        assertFalse(feed().enabled(),"Legacy paid quotes cannot grant a Feed Pack action");
    }
    @Test void retiredActionsNeverSendEvenWhenOldQuotesAndNewFoodClaimEligibility(){
        BogatyrClient.accept(view(23,2,WolfMode.PATROL,10));
        for(String id:List.of("bogatyr_breed","bogatyr_summon","bogatyr_regroup","bogatyr_heal",
                "recycle","request_supplies","guide","bogatyr_mode_0","bogatyr_mode_1",
                "bogatyr_mode_2","bogatyr_mode_3","bogatyr_mode_4","bogatyr_mode_5")){
            assertFalse(BogatyrClient.activate(id),id);
        }
        for(var mode:WolfMode.values())if(mode!=WolfMode.PATROL&&mode!=WolfMode.STAY)
            assertFalse(BogatyrClient.activate(BogatyrClient.modeId(mode)));
        assertFalse(BogatyrClient.pending());assertTrue(sent.isEmpty());assertTrue(modes.isEmpty());
    }
    @Test void tooltipExplainsDefenseStayAndFoodPriorityWithoutInventingKeys(){
        BogatyrClient.accept(view(23,3,WolfMode.STAY,10));
        var patrol=model().actions().getFirst().tooltip().getString();
        assertTrue(patrol.contains("defend you and themselves when attacked"));
        assertTrue(patrol.contains("forgets earlier enemies"));
        var stay=model().actions().get(1).tooltip().getString();
        assertTrue(stay.contains("within 5 blocks"));assertTrue(stay.contains("even when you move"));
        assertTrue(stay.contains("ignore attacks until you select Patrol"));
        var food=feed().tooltip().getString();
        assertTrue(food.contains("up to 10 food items"));
        assertTrue(food.contains("heal injured wolves first, then breed healthy adults"));
        assertTrue(food.contains("bones and rotten flesh first, then raw meat"));
        assertTrue(food.contains("Bones work as food only through Feed Pack"));
        assertTrue(food.contains("return to their saved positions"));
        assertTrue(food.contains("Ready to use: 10 items."));
        assertFalse(food.contains("ALT"));assertFalse(feed().selected());
        assertEquals("Living, loaded wolves you own in this dungeon: 7",model().resourceTooltip().getString());
    }
    @Test void unavailableStateDoesNotInventPackOrSelectionAndOtherClassesAreUntouched(){
        assertEquals("Waiting for wolfpack state",model().resource());
        assertTrue(model().actions().stream().noneMatch(SkillsPanelModel.Action::enabled));
        assertTrue(model().actions().stream().noneMatch(SkillsPanelModel.Action::selected));
        assertFalse(BogatyrClient.activate(BogatyrClient.PATROL));
        assertFalse(BogatyrClient.activate(BogatyrClient.STAY));
        assertFalse(BogatyrClient.activate(BogatyrClient.FEED_PACK));
        BogatyrClient.accept(view(5,WolfMode.PATROL));
        assertFalse(BogatyrClient.activate(BogatyrClient.FEED_PACK));
        BogatyrClient.clear();
        assertTrue(model().actions().stream().noneMatch(SkillsPanelModel.Action::selected));
        var other=SkillsPanelModel.initial("theurgist");assertSame(other,BogatyrClient.augment(other));
    }
    @Test void feedingSendsCapturedGenerationAndWaitsForNewAuthority(){
        var offered=view(23,20,WolfMode.STAY,2);BogatyrClient.accept(offered);
        assertTrue(feed().enabled());assertTrue(BogatyrClient.activate(BogatyrClient.FEED_PACK));
        assertEquals(List.of(new BogatyrPayloads.Action(23,20,BogatyrPayloads.Kind.FEED_PACK)),sent);
        assertTrue(BogatyrClient.pending());assertTrue(model().actions().get(1).selected());
        assertTrue(model().actions().stream().noneMatch(SkillsPanelModel.Action::enabled));
        assertTrue(feed().tooltip().getString().contains("Waiting for the server."));
        for(var row:model().actions())assertFalse(BogatyrClient.activate(row.id()));
        BogatyrClient.accept(offered);BogatyrClient.accept(view(23,19,WolfMode.PATROL,10));
        assertTrue(BogatyrClient.pending());assertEquals(1,sent.size());assertTrue(modes.isEmpty());
        BogatyrClient.accept(view(23,21,WolfMode.STAY,1));
        assertFalse(BogatyrClient.pending());assertTrue(feed().tooltip().getString().contains("Ready to use: 1 item."));
        assertTrue(BogatyrClient.activate(BogatyrClient.FEED_PACK));
        assertEquals(new BogatyrPayloads.Action(23,21,BogatyrPayloads.Kind.FEED_PACK),sent.getLast());
    }
    @Test void pendingModeBlocksFeedAndNewRunCannotReuseOldFoodQuote(){
        BogatyrClient.accept(view(23,30,WolfMode.PATROL,3));
        assertTrue(BogatyrClient.activate(BogatyrClient.STAY));
        assertFalse(feed().enabled());assertFalse(BogatyrClient.activate(BogatyrClient.FEED_PACK));
        BogatyrClient.accept(view(23,31,WolfMode.STAY,3));
        assertTrue(feed().enabled());assertTrue(BogatyrClient.activate(BogatyrClient.FEED_PACK));
        BogatyrClient.accept(view(24,32,WolfMode.PATROL,0));
        assertFalse(BogatyrClient.pending());assertFalse(feed().enabled());
        BogatyrClient.accept(view(23,31,WolfMode.STAY,3));
        assertFalse(BogatyrClient.activate(BogatyrClient.FEED_PACK));assertTrue(model().actions().getFirst().selected());
        assertEquals(1,sent.size());assertEquals(1,modes.size());
        BogatyrClient.accept(View.empty(33));
        assertFalse(BogatyrClient.activate(BogatyrClient.FEED_PACK));
    }
    @Test void failedFeedTransportReleasesPendingAndKeepsServerSelectionAndAvailability(){
        BogatyrClient.accept(view(23,40,WolfMode.STAY,4));
        BogatyrClient.actions(action->{throw new IllegalStateException("Disconnected");});
        assertThrows(IllegalStateException.class,()->BogatyrClient.activate(BogatyrClient.FEED_PACK));
        assertFalse(BogatyrClient.pending());assertTrue(feed().enabled());
        assertTrue(model().actions().get(1).selected());assertFalse(feed().selected());assertTrue(sent.isEmpty());
    }
    @Test void threeRowsRemainReachableByScrollingTheSmallPanelWithoutAResource(){
        BogatyrClient.accept(view(23,50,WolfMode.PATROL,2));
        var panel=new SkillsPanelState(SharedInventoryLayout.of(320,240,72,37,176,166),null);
        panel.actions(model().actions().size());
        assertEquals(3,model().actions().size());assertEquals(0,panel.scroll().offset());
        var body=panel.geometry().body();
        assertTrue(panel.scroll().max()>0);
        var hidden=panel.geometry().row(2,0);
        assertFalse(body.contains(hidden.x(),hidden.y()));
        assertEquals(-1,panel.press(hidden.x()+1,hidden.y()+1,0,false).action());panel.release();
        for(int i=0;i<3;i++){
            panel.scroll().reset();assertTrue(panel.wheel(body.x()+2,body.y()+2,-i,false));
            var row=panel.geometry().row(i,panel.scroll().offset());
            assertTrue(body.contains(row.x(),row.y())&&body.contains(row.right()-1,row.bottom()-1),
                    "Each complete button must become visible through normal panel scrolling");
            assertEquals(i,panel.press(row.x()+row.width()/2.0,row.y()+row.height()/2.0,0,false).action());
            assertTrue(panel.release());
        }
        var row=panel.geometry().row(2,panel.scroll().offset());
        assertFalse(panel.press(row.x()+1,row.y()+1,0,true).consumed(),"Carried items cannot activate Feed Pack");
        assertTrue(sent.isEmpty());assertTrue(modes.isEmpty());
    }
}
