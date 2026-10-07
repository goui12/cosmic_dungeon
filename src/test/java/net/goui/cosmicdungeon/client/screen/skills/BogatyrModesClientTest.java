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

final class BogatyrModesClientTest {
    private final List<BogatyrPayloads.Action> care=new ArrayList<>();
    private final List<BogatyrPayloads.ModeAction> modes=new ArrayList<>();
    private static View view(long run,long revision,WolfMode mode,boolean enabled){
        return new View(run,revision,3,List.of(new Quote(1,5,true),Quote.empty(),Quote.empty(),Quote.empty()),mode,enabled);
    }
    private static SkillsPanelModel model(){return BogatyrClient.augment(SkillsPanelModel.initial("bogatyr"));}
    private static SkillsPanelModel.Action row(WolfMode mode){
        return model().actions().stream().filter(a->a.id().equals(BogatyrClient.modeId(mode))).findFirst().orElseThrow();
    }
    private static void selected(WolfMode mode){
        assertEquals(List.of(BogatyrClient.modeId(mode)),model().actions().stream().filter(SkillsPanelModel.Action::selected)
                .map(SkillsPanelModel.Action::id).toList());
    }
    @BeforeEach void setup(){BogatyrClient.clear();BogatyrClient.actions(care::add);BogatyrClient.modeActions(modes::add);}
    @AfterEach void cleanup(){BogatyrClient.clear();}
    @Test void exactlyOneAuthoritativeCoreModeRemainsSelectedWhenControlsAreDisabled(){
        assertTrue(model().actions().stream().noneMatch(SkillsPanelModel.Action::selected));
        int revision=0;
        for(var mode:WolfMode.values()){
            BogatyrClient.accept(view(24,++revision,mode,true));selected(mode);
            for(var candidate:WolfMode.values())assertTrue(row(candidate).enabled());
            BogatyrClient.accept(view(24,++revision,mode,false));selected(mode);
            for(var candidate:WolfMode.values()){
                assertFalse(row(candidate).enabled());assertFalse(BogatyrClient.activate(BogatyrClient.modeId(candidate)));
            }
            assertTrue(row(mode).tooltip().getString().contains("Selected mode."));
        }
        assertTrue(modes.isEmpty());BogatyrClient.accept(View.empty(++revision));
        assertTrue(model().actions().stream().noneMatch(SkillsPanelModel.Action::selected));
    }
    @Test void modeAndCareSharePendingWithoutOptimisticSelectionOrDuplicateSends(){
        var initial=view(24,10,WolfMode.DEFENSIVE,true);BogatyrClient.accept(initial);
        assertTrue(BogatyrClient.activate(BogatyrClient.modeId(WolfMode.STAND_GROUND)));
        assertEquals(new BogatyrPayloads.ModeAction(24,10,WolfMode.STAND_GROUND),modes.getFirst());
        selected(WolfMode.DEFENSIVE);assertTrue(BogatyrClient.pending());
        assertFalse(row(WolfMode.DEFENSIVE).enabled());
        assertFalse(BogatyrClient.activate(BogatyrClient.BREED));
        assertFalse(BogatyrClient.activate(BogatyrClient.modeId(WolfMode.AGGRESSIVE)));
        BogatyrClient.accept(initial);assertTrue(BogatyrClient.pending());
        BogatyrClient.accept(view(24,9,WolfMode.AGGRESSIVE,true));selected(WolfMode.DEFENSIVE);
        assertTrue(BogatyrClient.pending());assertEquals(1,modes.size());assertTrue(care.isEmpty());
        BogatyrClient.accept(view(24,11,WolfMode.STAND_GROUND,true));selected(WolfMode.STAND_GROUND);
        assertFalse(BogatyrClient.pending());assertTrue(BogatyrClient.activate(BogatyrClient.BREED));
        assertFalse(BogatyrClient.activate(BogatyrClient.modeId(WolfMode.DEFENSIVE)));
        assertEquals(1,care.size());assertEquals(1,modes.size());
    }
    @Test void zeroResourceModesNeedOnlyServerCapabilityAndAdvancedRowsStayUnavailable(){
        BogatyrClient.accept(new View(24,1,0,List.of(Quote.empty(),Quote.empty(),Quote.empty(),Quote.empty()),
                WolfMode.DEFENSIVE,true));
        for(var mode:WolfMode.values()){
            assertTrue(row(mode).enabled());assertTrue(row(mode).tooltip().getString().contains("Cost: 0 Kibble"));
        }
        for(int index=3;index<6;index++){
            String id="bogatyr_mode_"+index;
            var action=model().actions().stream().filter(a->a.id().equals(id)).findFirst().orElseThrow();
            assertFalse(action.enabled());assertFalse(action.selected());assertFalse(BogatyrClient.activate(id));
        }
        assertTrue(BogatyrClient.activate(BogatyrClient.modeId(WolfMode.AGGRESSIVE)));
        assertEquals(new BogatyrPayloads.ModeAction(24,1,WolfMode.AGGRESSIVE),modes.getFirst());
        assertTrue(care.isEmpty());
    }
    @Test void RejectionAcknowledgmentAndRunChangesRequireNoInventedCooldown(){
        BogatyrClient.accept(view(24,100,WolfMode.DEFENSIVE,true));
        assertTrue(BogatyrClient.activate(BogatyrClient.modeId(WolfMode.STAND_GROUND)));
        BogatyrClient.accept(view(24,101,WolfMode.DEFENSIVE,true));selected(WolfMode.DEFENSIVE);
        assertTrue(BogatyrClient.activate(BogatyrClient.modeId(WolfMode.AGGRESSIVE)),"New acknowledgment permits immediate retry");
        BogatyrClient.accept(view(25,102,WolfMode.STAND_GROUND,true));selected(WolfMode.STAND_GROUND);
        assertFalse(BogatyrClient.pending());assertTrue(BogatyrClient.activate(BogatyrClient.modeId(WolfMode.DEFENSIVE)));
        assertEquals(new BogatyrPayloads.ModeAction(25,102,WolfMode.DEFENSIVE),modes.getLast());
        BogatyrClient.clear();assertFalse(BogatyrClient.pending());
        assertTrue(model().actions().stream().noneMatch(SkillsPanelModel.Action::selected));
        assertFalse(BogatyrClient.activate(BogatyrClient.modeId(WolfMode.AGGRESSIVE)));
    }
    @Test void delayedOtherRunSnapshotsCannotRestoreAnOldSelectionOrAcknowledgePending(){
        BogatyrClient.accept(view(24,100,WolfMode.DEFENSIVE,true));
        BogatyrClient.accept(view(25,101,WolfMode.AGGRESSIVE,true));selected(WolfMode.AGGRESSIVE);
        assertTrue(BogatyrClient.activate(BogatyrClient.modeId(WolfMode.STAND_GROUND)));
        BogatyrClient.accept(view(24,100,WolfMode.STAND_GROUND,true));
        BogatyrClient.accept(View.empty(99));
        selected(WolfMode.AGGRESSIVE);assertTrue(BogatyrClient.pending());
        assertEquals(new BogatyrPayloads.ModeAction(25,101,WolfMode.STAND_GROUND),modes.getLast());
        BogatyrClient.accept(view(25,102,WolfMode.STAND_GROUND,true));
        selected(WolfMode.STAND_GROUND);assertFalse(BogatyrClient.pending());
        BogatyrClient.clear();
        BogatyrClient.accept(view(24,1,WolfMode.DEFENSIVE,true));selected(WolfMode.DEFENSIVE);
    }
    @Test void standGroundExplainsWhyBreedingRequiresAnotherMode(){
        BogatyrClient.accept(new View(24,1,3,List.of(Quote.empty(),Quote.empty(),Quote.empty(),Quote.empty()),
                WolfMode.STAND_GROUND,true));
        var breed=model().actions().stream().filter(a->a.id().equals(BogatyrClient.BREED)).findFirst().orElseThrow();
        assertFalse(breed.enabled());assertFalse(BogatyrClient.activate(BogatyrClient.BREED));
        assertTrue(breed.tooltip().getString().contains("Switch to Defensive or Aggressive before breeding."));
        selected(WolfMode.STAND_GROUND);
        assertTrue(BogatyrClient.activate(BogatyrClient.modeId(WolfMode.DEFENSIVE)));
        assertTrue(care.isEmpty());
    }
    @Test void failedModeTransportReleasesPendingWithoutChangingTheSelectedBorder(){
        BogatyrClient.accept(view(24,1,WolfMode.DEFENSIVE,true));
        BogatyrClient.modeActions(action->{throw new IllegalStateException("Disconnected");});
        assertThrows(IllegalStateException.class,()->BogatyrClient.activate(BogatyrClient.modeId(WolfMode.STAND_GROUND)));
        assertFalse(BogatyrClient.pending());selected(WolfMode.DEFENSIVE);assertTrue(modes.isEmpty());
    }
}
