package net.goui.cosmicdungeon.client.screen.skills;

import java.util.Arrays;
import java.util.List;
import net.goui.cosmicdungeon.network.ClassSkillPayloads.*;
import net.goui.cosmicdungeon.playerclass.skill.ClassSkillSlot;
import net.goui.cosmicdungeon.client.screen.crafting.PotionPaletteLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ClassAbilityClientTest {
    private static final ClassSkillSlot PRIMARY = ClassSkillSlot.PRIMARY;
    private static View view(long run, long revision, String cls, SlotView primary, List<Track> tracks) {
        var slots = Arrays.stream(ClassSkillSlot.values()).map(slot -> slot == PRIMARY ? primary : SlotView.unassigned(slot)).toList();
        return new View(run, revision, cls, slots, tracks);
    }
    private static SlotView ready() { return new SlotView(PRIMARY, "test", "Test ability", "Useful effect.", true, 0, 100, 100); }
    @AfterEach void clear() { ClassAbilityClient.clear(); }

    @Test void fallbackAndUnassignedSlotsCannotEmitAnAction() {
        for (String cls : List.of("theurgist", "venefex", "dragoon", "bogatyr")) {
            assertEquals(4, ClassAbilityClient.slots(null, cls).size());
            assertTrue(ClassAbilityClient.slots(null, cls).stream().noneMatch(SlotView::enabled));
            var empty = View.empty(1, cls);
            for (var slot : ClassSkillSlot.values()) {
                assertTrue(ClassAbilityClient.request(empty, cls, slot).isEmpty());
                assertTrue(ClassAbilityClient.unavailable(empty, cls, slot).contains("not assigned"));
            }
        }
    }
    @Test void requestRetainsServerSnapshotAndRejectsClassOrRunChanges() {
        var snapshot = view(13, 8, "dragoon", ready(), List.of());
        assertEquals(new Action(13, 8, "dragoon", PRIMARY),
                ClassAbilityClient.request(snapshot, "dragoon", PRIMARY).orElseThrow());
        assertTrue(ClassAbilityClient.request(snapshot, "theurgist", PRIMARY).isEmpty());
        assertTrue(ClassAbilityClient.slots(snapshot, "theurgist").stream().allMatch(s -> s.abilityId().isBlank()));
        var inactive = view(0, 9, "dragoon", new SlotView(PRIMARY, "test", "Test ability", "", false, 0, 0, 0), List.of());
        assertTrue(ClassAbilityClient.request(inactive, "dragoon", PRIMARY).isEmpty());
        assertTrue(ClassAbilityClient.request(snapshot, "dragoon", ClassSkillSlot.SKILL_1).isEmpty());
    }
    @Test void cooldownAndIncompleteChargeAreRefusedWithUsefulFeedback() {
        var cooling = view(13, 8, "dragoon", new SlotView(PRIMARY, "test", "Test ability", "", false, 21, 0, 0), List.of());
        assertTrue(ClassAbilityClient.request(cooling, "dragoon", PRIMARY).isEmpty());
        assertEquals("Test ability is ready in 2 seconds.", ClassAbilityClient.unavailable(cooling, "dragoon", PRIMARY));
        var charging = view(13, 9, "dragoon", new SlotView(PRIMARY, "test", "Test ability", "", true, 0, 99, 100), List.of());
        assertTrue(ClassAbilityClient.request(charging, "dragoon", PRIMARY).isEmpty());
        assertEquals("Test ability: 99/100 charge.", ClassAbilityClient.unavailable(charging, "dragoon", PRIMARY));
    }
    @Test void panelKeepsExistingActionsResourcesAndSignatureProgress() {
        var oldAction = new SkillsPanelModel.Action("bogatyr_feed_pack", "Feed Pack", "Heal wolves.", true);
        var base = new SkillsPanelModel("bogatyr", "Wolfpack", "Loaded wolves: 3", "Three loaded wolves.", List.of(oldAction));
        ClassAbilityClient.actions(action -> fail("Rendering must not send actions"));
        ClassAbilityClient.receive(view(13, 8, "bogatyr", SlotView.unassigned(PRIMARY),
                List.of(new Track("wolves", "Wolves", "WLF", 2, 1, 3))));
        var model = ClassAbilityClient.augment(base);
        assertEquals(5, model.actions().size());
        assertEquals(oldAction, model.actions().getFirst());
        assertEquals("WLF - 2 | Loaded wolves: 3", model.resource());
        assertEquals("Wolves - Level 2\n1/3 to next level.\nThree loaded wolves.", model.resourceTooltip().getString());
        assertTrue(model.actions().subList(1,5).stream().noneMatch(SkillsPanelModel.Action::enabled));
        assertEquals("Primary class skill", model.actions().get(1).label());
        assertEquals(1, base.actions().size());
        var wrongClass = ClassAbilityClient.augment(SkillsPanelModel.initial("dragoon"));
        assertFalse(wrongClass.resource().contains("WLF"));
    }
    @Test void staleSnapshotsCannotRestoreAnOldAbilityAndClearRemovesCapabilities() {
        ClassAbilityClient.actions(action -> fail("Rendering must not send actions"));
        ClassAbilityClient.receive(view(13, 9, "dragoon", SlotView.unassigned(PRIMARY), List.of()));
        ClassAbilityClient.receive(view(13, 8, "dragoon", ready(), List.of()));
        assertFalse(ClassAbilityClient.augment(SkillsPanelModel.initial("dragoon")).actions().getFirst().enabled());
        ClassAbilityClient.receive(view(13, 10, "dragoon", ready(), List.of()));
        var active = ClassAbilityClient.augment(SkillsPanelModel.initial("dragoon")).actions().getFirst();
        assertTrue(active.enabled());
        assertEquals("Primary class skill: Test ability",active.label());
        ClassAbilityClient.clear();
        assertTrue(ClassAbilityClient.augment(SkillsPanelModel.initial("dragoon")).actions().stream().noneMatch(SkillsPanelModel.Action::enabled));
    }
    @Test void brewerPanelCannotOverlapPotionPaletteAfterSavedPositionResizeOrDrag() {
        for (int width : new int[]{320, 480, 854}) {
            int height = 240, inventoryWidth = 176, left = (width-inventoryWidth)/2;
            var shared = SharedInventoryLayout.of(width,height,left,37,inventoryWidth,166);
            var palette = PotionPaletteLayout.of(width,height,left,37,inventoryWidth,30);
            var reserved = SkillsPanelClient.reservePalette(shared,palette.panel());
            var state = new SkillsPanelState(reserved,new SkillsPanelState.Placement(1,0,false));
            assertTrue(state.geometry().panel().right() <= palette.panel().x());
            var header = state.geometry().header();
            state.press(header.x()+2,header.y()+2,0,false);
            state.drag(width,80,0,false);
            assertTrue(state.geometry().panel().right() <= palette.panel().x());
            state.release();
            assertFalse(state.press(palette.panel().x()+1,palette.panel().y()+1,0,false).consumed());
        }
    }
}
