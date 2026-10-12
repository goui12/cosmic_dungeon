package net.goui.cosmicdungeon.client.screen.skills;

import java.util.ArrayList;
import java.util.Collections;
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
    private static final List<WolfMode> MODES=List.of(WolfMode.PATROL,WolfMode.STAY);
    private final List<BogatyrPayloads.ModeAction> sent=new ArrayList<>();
    private static View view(long run,long revision,WolfMode mode,boolean enabled){
        return new View(run,revision,3,Collections.nCopies(4,Quote.empty()),mode,enabled);
    }
    private static SkillsPanelModel model(){return BogatyrClient.augment(SkillsPanelModel.initial("bogatyr"));}
    private static SkillsPanelModel.Action row(WolfMode mode){
        return model().actions().stream().filter(a->a.id().equals(BogatyrClient.modeId(mode))).findFirst().orElseThrow();
    }
    private static void selected(WolfMode mode){
        assertEquals(List.of(BogatyrClient.modeId(mode)),model().actions().stream().filter(SkillsPanelModel.Action::selected)
                .map(SkillsPanelModel.Action::id).toList());
    }
    @BeforeEach void setup(){BogatyrClient.clear();BogatyrClient.modeActions(sent::add);}
    @AfterEach void cleanup(){BogatyrClient.clear();}
    @Test void exactlyOneAuthoritativeModeRemainsSelectedWhenControlsAreDisabled(){
        assertTrue(model().actions().stream().noneMatch(SkillsPanelModel.Action::selected));
        int revision=0;
        for(var mode:MODES){
            BogatyrClient.accept(view(24,++revision,mode,true));selected(mode);
            for(var candidate:MODES)assertTrue(row(candidate).enabled());
            BogatyrClient.accept(view(24,++revision,mode,false));selected(mode);
            for(var candidate:MODES){
                assertFalse(row(candidate).enabled());assertFalse(BogatyrClient.activate(BogatyrClient.modeId(candidate)));
            }
            assertTrue(row(mode).tooltip().getString().contains("Selected mode."));
        }
        assertTrue(sent.isEmpty());BogatyrClient.accept(View.empty(++revision));
        assertTrue(model().actions().stream().noneMatch(SkillsPanelModel.Action::selected));
    }
    @Test void choiceUsesCapturedGenerationWithoutOptimisticSelectionOrDuplicateSends(){
        var initial=view(24,10,WolfMode.PATROL,true);BogatyrClient.accept(initial);
        assertTrue(BogatyrClient.activate(BogatyrClient.STAY));
        assertEquals(new BogatyrPayloads.ModeAction(24,10,WolfMode.STAY),sent.getFirst());
        selected(WolfMode.PATROL);assertTrue(BogatyrClient.pending());
        for(var mode:MODES){assertFalse(row(mode).enabled());assertFalse(BogatyrClient.activate(BogatyrClient.modeId(mode)));}
        BogatyrClient.accept(initial);assertTrue(BogatyrClient.pending());
        BogatyrClient.accept(view(24,9,WolfMode.STAY,true));selected(WolfMode.PATROL);
        assertTrue(BogatyrClient.pending());assertEquals(1,sent.size());
        assertTrue(row(WolfMode.STAY).tooltip().getString().contains("Waiting for the server."));
        BogatyrClient.accept(view(24,11,WolfMode.STAY,true));selected(WolfMode.STAY);
        assertFalse(BogatyrClient.pending());assertTrue(BogatyrClient.activate(BogatyrClient.PATROL));
        assertEquals(new BogatyrPayloads.ModeAction(24,11,WolfMode.PATROL),sent.getLast());
    }
    @Test void selectedRadioChoiceNeverResendsOrRecallsExistingStayPositions(){
        for(var mode:MODES){
            BogatyrClient.accept(view(25,mode==WolfMode.PATROL?1:2,mode,true));selected(mode);
            assertFalse(BogatyrClient.activate(BogatyrClient.modeId(mode)));
            assertFalse(BogatyrClient.pending());assertTrue(sent.isEmpty());
        }
    }
    @Test void rejectionAcknowledgementAndRunChangesRequireNoInventedCooldown(){
        BogatyrClient.accept(view(24,100,WolfMode.PATROL,true));
        assertTrue(BogatyrClient.activate(BogatyrClient.STAY));
        BogatyrClient.accept(view(24,101,WolfMode.PATROL,true));selected(WolfMode.PATROL);
        assertTrue(BogatyrClient.activate(BogatyrClient.STAY),"New acknowledgement permits immediate retry");
        BogatyrClient.accept(view(25,102,WolfMode.STAY,true));selected(WolfMode.STAY);
        assertFalse(BogatyrClient.pending());assertTrue(BogatyrClient.activate(BogatyrClient.PATROL));
        assertEquals(new BogatyrPayloads.ModeAction(25,102,WolfMode.PATROL),sent.getLast());
        BogatyrClient.accept(View.empty(103));
        assertFalse(BogatyrClient.pending());assertFalse(BogatyrClient.activate(BogatyrClient.STAY));
        assertTrue(model().actions().stream().noneMatch(SkillsPanelModel.Action::selected));
    }
    @Test void delayedOtherRunSnapshotsCannotRestoreOldSelectionOrAcknowledgePending(){
        BogatyrClient.accept(view(24,100,WolfMode.PATROL,true));
        BogatyrClient.accept(view(25,101,WolfMode.STAY,true));selected(WolfMode.STAY);
        assertTrue(BogatyrClient.activate(BogatyrClient.PATROL));
        BogatyrClient.accept(view(24,100,WolfMode.PATROL,true));BogatyrClient.accept(View.empty(99));
        selected(WolfMode.STAY);assertTrue(BogatyrClient.pending());
        assertEquals(new BogatyrPayloads.ModeAction(25,101,WolfMode.PATROL),sent.getLast());
        BogatyrClient.accept(view(25,102,WolfMode.PATROL,true));selected(WolfMode.PATROL);
        assertFalse(BogatyrClient.pending());
        BogatyrClient.clear();BogatyrClient.accept(view(24,1,WolfMode.PATROL,true));selected(WolfMode.PATROL);
    }
    @Test void failedTransportReleasesPendingWithoutChangingSelection(){
        BogatyrClient.accept(view(24,1,WolfMode.PATROL,true));
        BogatyrClient.modeActions(action->{throw new IllegalStateException("Disconnected");});
        assertThrows(IllegalStateException.class,()->BogatyrClient.activate(BogatyrClient.STAY));
        assertFalse(BogatyrClient.pending());selected(WolfMode.PATROL);assertTrue(sent.isEmpty());
    }
}
