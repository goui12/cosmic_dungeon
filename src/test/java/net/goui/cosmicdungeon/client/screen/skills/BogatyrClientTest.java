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
    private final List<BogatyrPayloads.Action> retired=new ArrayList<>();
    private static SkillsPanelModel model(){
        return BogatyrClient.augment(SkillsPanelModel.initial("bogatyr"));
    }
    private static View view(long revision,WolfMode mode){
        return new View(23,revision,7,List.of(new Quote(1,5,true),new Quote(1,30,true),
                new Quote(7,7,true),new Quote(2,10,true)),mode,true);
    }
    @BeforeEach void setup(){
        BogatyrClient.clear();BogatyrClient.actions(retired::add);
        BogatyrClient.modeActions(action->fail("Presentation must not send mode actions"));
    }
    @AfterEach void cleanup(){BogatyrClient.clear();}
    @Test void onlyTwoModesRemainEvenWithLegacyQuotesAndInjectedResourceActions(){
        BogatyrClient.accept(view(1,WolfMode.PATROL));
        var legacy=new ClassResourceSnapshot(23,"kibble",105,600,true,true,true,3);
        var old=new SkillsPanelModel("bogatyr","Old","Kibble: 105",net.minecraft.network.chat.Component.literal("Old resource"),
                List.of(new SkillsPanelModel.Action("recycle","Recycle","Old action",true)),legacy);
        var result=BogatyrClient.augment(old);
        assertEquals("Wolfpack",result.title());assertNull(result.resourceSnapshot());
        assertEquals("Loaded wolves: 7",result.resource());
        assertEquals(List.of("Patrol","Stay"),result.actions().stream().map(SkillsPanelModel.Action::label).toList());
        assertEquals(List.of(BogatyrClient.PATROL,BogatyrClient.STAY),
                result.actions().stream().map(SkillsPanelModel.Action::id).toList());
        assertFalse(result.resourceTooltip().getString().contains("Kibble"));
        for(var row:result.actions()){
            assertFalse(row.tooltip().getString().contains("Kibble"));
            assertTrue(row.tooltip().getString().startsWith(row.label()+"\n"));
        }
    }
    @Test void retiredActionsNeverSendEvenWhenOldQuotesClaimEligibility(){
        BogatyrClient.accept(view(2,WolfMode.PATROL));
        for(String id:List.of("bogatyr_breed","bogatyr_summon","bogatyr_regroup","bogatyr_heal",
                "recycle","request_supplies","guide","bogatyr_mode_0","bogatyr_mode_1",
                "bogatyr_mode_2","bogatyr_mode_3","bogatyr_mode_4","bogatyr_mode_5")){
            assertFalse(BogatyrClient.activate(id),id);
        }
        for(var mode:WolfMode.values())if(mode!=WolfMode.PATROL&&mode!=WolfMode.STAY)
            assertFalse(BogatyrClient.activate(BogatyrClient.modeId(mode)));
        assertFalse(BogatyrClient.pending());assertTrue(retired.isEmpty());
    }
    @Test void tooltipExplainsNativeDefenseFixedStayAndAggroReset(){
        BogatyrClient.accept(view(3,WolfMode.STAY));
        var patrol=model().actions().getFirst().tooltip().getString();
        assertTrue(patrol.contains("defend you and themselves when attacked"));
        assertTrue(patrol.contains("forgets earlier enemies"));
        var stay=model().actions().getLast().tooltip().getString();
        assertTrue(stay.contains("within 5 blocks"));assertTrue(stay.contains("even when you move"));
        assertTrue(stay.contains("ignore attacks until you select Patrol"));
        assertEquals("Living, loaded wolves you own in this dungeon: 7",model().resourceTooltip().getString());
    }
    @Test void unavailableStateDoesNotInventPackOrSelectionAndOtherClassesAreUntouched(){
        assertEquals("Waiting for wolfpack state",model().resource());
        assertTrue(model().actions().stream().noneMatch(SkillsPanelModel.Action::enabled));
        assertTrue(model().actions().stream().noneMatch(SkillsPanelModel.Action::selected));
        assertFalse(BogatyrClient.activate(BogatyrClient.PATROL));
        assertFalse(BogatyrClient.activate(BogatyrClient.STAY));
        BogatyrClient.accept(view(5,WolfMode.PATROL));
        BogatyrClient.clear();
        assertTrue(model().actions().stream().noneMatch(SkillsPanelModel.Action::selected));
        var other=SkillsPanelModel.initial("theurgist");assertSame(other,BogatyrClient.augment(other));
    }
    @Test void twoModesRemainReachableWithoutAResourceOrLongActionList(){
        BogatyrClient.accept(view(6,WolfMode.PATROL));
        var panel=new SkillsPanelState(SharedInventoryLayout.of(320,240,72,37,176,166),null);
        panel.actions(model().actions().size());
        assertEquals(2,model().actions().size());assertEquals(0,panel.scroll().offset());
        var body=panel.geometry().body();
        for(int i=0;i<2;i++){
            var row=panel.geometry().row(i,0);
            assertTrue(body.contains(row.x(),row.y())&&body.contains(row.right()-1,row.bottom()-1),
                    "Each complete radio button fits before scrolling");
            assertEquals(i,panel.press(row.x()+row.width()/2.0,row.y()+row.height()/2.0,0,false).action());
            assertTrue(panel.release());
        }
        assertTrue(retired.isEmpty());
    }
}
