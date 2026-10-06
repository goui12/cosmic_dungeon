package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.npc.tamsin.TamsinFlow;
import net.goui.cosmicdungeon.npc.tamsin.TamsinFlow.Stage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TamsinReplayTest {
    @Test void firstVisitStillUsesTheRealOnboardingFlow() {
        var replay = new TamsinReplay();
        assertFalse(replay.start(Stage.AGREEMENT));
        assertFalse(replay.action("yes"));
        assertEquals(Stage.MAP, TamsinFlow.advance(Stage.AGREEMENT, "yes", false));
        assertFalse(replay.active());
    }
    @Test void readyPlayerReturnsToGroupAfterTheSameLoreAndMap() {
        var replay = new TamsinReplay();
        assertTrue(replay.start(Stage.READY));
        assertEquals(Stage.AGREEMENT, replay.display(Stage.READY));
        assertTrue(replay.action("yes"));
        assertEquals(Stage.MAP, replay.display(Stage.READY));
        assertTrue(replay.action("continue"));
        assertFalse(replay.active());
        assertEquals(Stage.READY, replay.display(Stage.READY));
    }
    @Test void noReturnsWithoutRevokingAnExistingAgreement() {
        var replay = new TamsinReplay();
        replay.start(Stage.READY);
        assertTrue(replay.action("no"));
        assertEquals(Stage.READY, replay.display(Stage.READY));
        assertTrue(TamsinFlow.canReady(true, replay.display(Stage.READY)));
    }
    @Test void developerSelectorReturnsWithoutChoosingAClass() {
        var replay = new TamsinReplay();
        assertTrue(replay.start(Stage.SELECTOR));
        replay.action("yes"); replay.action("continue");
        assertEquals(Stage.SELECTOR, replay.display(Stage.SELECTOR));
    }
    @Test void refreshAndResizeResponsesPreserveMapPreview() {
        var replay = new TamsinReplay();
        replay.start(Stage.READY); replay.action("yes");
        replay.receive(Stage.READY); replay.receive(Stage.READY);
        assertEquals(Stage.MAP, replay.display(Stage.READY));
    }
    @Test void repeatedReplayAndObsoleteClicksStayLocal() {
        var replay = new TamsinReplay();
        replay.start(Stage.READY); replay.action("yes");
        assertTrue(replay.start(Stage.READY));
        assertEquals(Stage.AGREEMENT, replay.display(Stage.READY));
        assertTrue(replay.action("continue"));
        assertEquals(Stage.AGREEMENT, replay.display(Stage.READY));
        replay.action("no");
        assertFalse(replay.action("continue"));
    }
    @Test void authoritativeTaxOrOnboardingSupersedesPreview() {
        for (var stage : new Stage[]{Stage.TAX, Stage.AGREEMENT, Stage.MAP}) {
            var replay = new TamsinReplay();
            replay.start(Stage.READY);
            replay.receive(stage);
            assertFalse(replay.active());
            assertFalse(replay.start(stage));
            assertEquals(stage, replay.display(stage));
        }
    }
    @Test void returningUsesLatestAuthoritativeStage() {
        var replay = new TamsinReplay();
        replay.start(Stage.READY); replay.action("yes");
        replay.receive(Stage.SELECTOR);
        replay.action("continue");
        assertEquals(Stage.SELECTOR, replay.display(Stage.SELECTOR));
    }
}
