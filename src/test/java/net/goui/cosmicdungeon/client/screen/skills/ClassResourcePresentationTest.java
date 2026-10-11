package net.goui.cosmicdungeon.client.screen.skills;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Retired Requests/Brewing Supplies stay absent while the pending Bogatyr rework keeps its real resource. */
final class ClassResourcePresentationTest {
    private ClassResourceSnapshot snapshot(String resource, int amount, boolean alive) {
        return new ClassResourceSnapshot(23, resource, amount, 600, true, alive, true, 9);
    }
    @Test void requestsAreAbsentForEveryClassEvenWithLegacyConsent() {
        for (String id : List.of("none", "judicator", "dragoon", "pyroclast", "venefex", "theurgist", "bogatyr")) {
            var panel = ClassResourcePresentation.panel(id, snapshot(id.equals("bogatyr") ? "kibble" : "brewing_supplies", 147, true), false, true);
            assertTrue(panel.actions().stream().noneMatch(action -> action.id().equals("request_supplies")));
        }
    }
    @Test void bogatyrRetainsOnlyMatchingResourceAndRecycleEligibilityUntilItsRework() {
        var value = snapshot("kibble", 147, true);
        var panel = ClassResourcePresentation.panel("bogatyr", value, false);
        assertSame(value, panel.resourceSnapshot()); assertEquals("Kibble: 147/600", panel.resource());
        assertEquals(List.of("recycle", "guide"), panel.actions().stream().map(SkillsPanelModel.Action::id).toList());
        assertTrue(panel.actions().getFirst().enabled());
        assertFalse(ClassResourcePresentation.panel("bogatyr", value, true).actions().getFirst().enabled());
        assertFalse(ClassResourcePresentation.panel("bogatyr", snapshot("kibble", 147, false), false).actions().getFirst().enabled());
        var wrong = ClassResourcePresentation.panel("bogatyr", snapshot("brewing_supplies", 147, true), false);
        assertNull(wrong.resourceSnapshot()); assertFalse(wrong.actions().getFirst().enabled());
    }
    @Test void legacyBrewingSuppliesNeverReappearInTheurgistPresentation() {
        var model = ClassResourcePresentation.panel("theurgist", snapshot("brewing_supplies", 600, true), false, true);
        assertNull(model.resourceSnapshot());
        assertFalse(model.resource().contains("Brewing Supplies"));
        assertTrue(model.actions().stream().noneMatch(action -> action.id().equals("recycle")));
        assertTrue(model.actions().stream().noneMatch(action -> action.tooltip().getString().contains("Brewing Supplies")));
        assertTrue(model.actions().getFirst().tooltip().getString().contains("Craft..."));
    }
}
