package net.goui.cosmicdungeon.client.screen.spawner;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.Slot;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEditorDraft;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft.Edit;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft.Enchant;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft.Origin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SpawnerEquipmentStateTest {
    @Test void nestedCancelCannotMutateParentAndSaveTouchesOnlySelectedSlot() {
        var parent = new SpawnerEditorState(UUID.randomUUID(), SpawnerEditorDraft.empty());
        var nested = new SpawnerEquipmentState(parent.equipment().get(Slot.MAINHAND));
        nested.accept(nested.item("minecraft:diamond_sword"));
        nested.accept(nested.enchant("minecraft:sharpness", 4));
        assertFalse(parent.dirty(), "Selecting nested items must not save without Save item.");
        var cancelled = new SpawnerEquipmentState(parent.equipment().get(Slot.MAINHAND));
        assertEquals(Edit.keep(), cancelled.edit());
        parent.equipment(Slot.MAINHAND, nested.edit());
        assertTrue(parent.dirty());
        for (Slot slot : Slot.values())
            assertEquals(slot == Slot.MAINHAND ? nested.edit() : Edit.keep(), parent.equipment().get(slot));
    }
    @Test void rejectionNavigationAndResizeRetainSavedEquipmentAlongsideCoreDraft() {
        var parent = new SpawnerEditorState(UUID.randomUUID(), SpawnerEditorDraft.empty());
        var edit = new Edit(Origin.HELD, "", true, List.of(new Enchant("minecraft:unbreaking", 20)));
        parent.equipment(Slot.HEAD, edit); parent.name("Unapplied");
        parent.tab(SpawnerEditorState.Tab.PREVIEW);
        SpawnerEquipmentLayout.fit(320, 180);
        assertTrue(parent.beginApply());
        assertTrue(parent.reject(parent.session(), "Held item changed; reopen."));
        parent.tab(SpawnerEditorState.Tab.EQUIPMENT);
        assertEquals(edit, parent.equipment().get(Slot.HEAD));
        assertEquals("Unapplied", parent.name()); assertFalse(parent.pending()); assertTrue(parent.dirty());
        parent.equipment(Slot.HEAD, Edit.keep()); parent.name("");
        assertFalse(parent.dirty());
    }
    @Test void candidateConstructionDoesNotSilentlyCommitARejectedValidation() {
        var original = new Edit(Origin.KEEP, "", true, List.of(new Enchant("minecraft:sharpness", 20)));
        var state = new SpawnerEquipmentState(original);
        Edit normal = state.mode(false);
        assertFalse(normal.advanced()); assertEquals(original.enchantments(), normal.enchantments());
        assertEquals(original, state.edit(), "Only accept() commits after shared validation.");
        state.notice("Normal maximum exceeded");
        state.page(SpawnerEquipmentState.Page.BOOKS); state.query("damage"); state.index(4);
        assertEquals(original, state.edit());
    }
    @Test void bookGesturesUseAbsoluteOverridesAndRemoveWithoutDuplicatingEntries() {
        var state = new SpawnerEquipmentState(Edit.keep());
        state.accept(state.enchant("minecraft:unbreaking", 1));
        state.accept(state.enchant("minecraft:sharpness", 3));
        state.accept(state.enchant("minecraft:unbreaking", 0));
        assertEquals(List.of(new Enchant("minecraft:sharpness", 3), new Enchant("minecraft:unbreaking", 0)),
                state.edit().enchantments());
        assertEquals(4, SpawnerEquipmentState.level(3, 5, 0, false, false));
        assertEquals(5, SpawnerEquipmentState.level(3, 5, 0, true, false));
        assertEquals(2, SpawnerEquipmentState.level(3, 5, 1, false, false));
        assertEquals(0, SpawnerEquipmentState.level(3, 5, 1, true, false));
        assertEquals(0, SpawnerEquipmentState.level(0, 5, 1, false, false));
    }
    @Test void advancedLevelsAreBoundedAndShiftLeftAlwaysMeansTheNormalMaximum() {
        assertEquals(5, SpawnerEquipmentState.level(5, 5, 0, false, false));
        assertEquals(6, SpawnerEquipmentState.level(5, 5, 0, false, true));
        assertEquals(255, SpawnerEquipmentState.level(255, 5, 0, false, true));
        assertEquals(5, SpawnerEquipmentState.level(200, 5, 0, true, true));
        assertEquals(200, SpawnerEquipmentState.level(200, 5, 2, false, true));
    }
    @Test void replacementAndClearResetOldOverridesButKeepExplicitMode() {
        var state = new SpawnerEquipmentState(new Edit(Origin.KEEP, "", true,
                List.of(new Enchant("minecraft:sharpness", 4))));
        assertEquals(new Edit(Origin.ITEM, "minecraft:bow", true, List.of()), state.item("minecraft:bow"));
        assertEquals(new Edit(Origin.HELD, "", true, List.of()), state.held());
        assertEquals(new Edit(Origin.CLEAR, "", true, List.of()), state.clear());
    }
    @Test void compactCatalogsPreviewAndButtonsStaySeparatedAndEveryItemIsReachable() {
        for (int[] dimensions : new int[][] {{320,180},{427,240},{640,360},{1920,1080}}) {
            var layout = SpawnerEquipmentLayout.fit(dimensions[0], dimensions[1]);
            assertTrue(layout.capacity() > 0);
            assertTrue(layout.tabsY() + 20 < layout.searchY());
            assertTrue(layout.searchY() + 20 < layout.gridY());
            for (int i = 0; i < layout.capacity(); i++) {
                assertTrue(layout.tileX(i) >= layout.x());
                assertTrue(layout.tileX(i) + 20 <= layout.x() + layout.width());
                assertTrue(layout.tileY(i) + 20 < layout.previewY());
            }
            assertTrue(layout.previewY() + 20 < layout.panel().footerY());
            assertTrue(layout.panel().footerY() + 20 <= dimensions[1]);
            var reached = new HashSet<Integer>();
            for (int requested = 0; requested < 2000; requested++) {
                int page = SpawnerEditorLayout.page(requested, 2000, layout.capacity());
                for (int i = 0; i < layout.capacity(); i++)
                    if (page * layout.capacity() + i < 2000) reached.add(page * layout.capacity() + i);
            }
            assertEquals(2000, reached.size());
        }
    }
}
