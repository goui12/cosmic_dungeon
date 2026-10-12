package net.goui.cosmicdungeon.client.screen.spawner;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.IntrinsicDropRule.Kind;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEditorDraft;
import net.goui.cosmicdungeon.spawner.editor.SpawnerLootDraft;
import net.goui.cosmicdungeon.spawner.editor.SpawnerLootDraft.DefaultDrop;
import net.goui.cosmicdungeon.spawner.editor.SpawnerLootDraft.Rule;
import net.goui.cosmicdungeon.spawner.editor.SpawnerLootDraft.Snapshot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SpawnerLootStateTest {
    private static Rule row(String id, String item, float chance) {
        return new Rule(id, item, chance, 1, Kind.CUSTOM_ADDITION);
    }
    private static Snapshot baseline(List<Rule> rules) {
        return new Snapshot(List.of(-1f, 0f, 2f, .085f, .5f, 1f), rules,
                List.of(new DefaultDrop("minecraft:rotten_flesh", "Conditional / complex")), "Conditions apply.", "minecraft:zombie");
    }
    @Test void malformedNonFiniteSnapshotDisplaysSafelyWithoutRewritingAuthoredValues() {
        var legacy = new Snapshot(List.of(Float.NaN, -1f, -1f, -1f, -1f, -1f),
                List.of(row("legacy", "minecraft:bone", Float.NaN)), List.of(), "", "minecraft:zombie");
        var state = new SpawnerLootState(legacy, SpawnerLootDraft.unchanged());
        assertEquals("Invalid", state.gear(0));
        assertEquals("Invalid", SpawnerLootState.percent(Float.NaN));
        assertEquals("Invalid", SpawnerLootState.percent(Float.POSITIVE_INFINITY));
        assertEquals("Invalid", SpawnerLootState.percent(Float.NEGATIVE_INFINITY));
        assertTrue(state.error().isEmpty());
        assertFalse(state.draft().dirty());
        assertEquals(-1f, state.draft().gear().getFirst());
        assertTrue(Float.isNaN(state.draft().rules().getFirst().chance()));
        state.gear(0, "25");
        assertEquals(.25f, state.draft().gear().getFirst());
        state.resetGear(0); assertFalse(state.draft().dirty());
    }
    @Test void untouchedAndResetGearPreserveInheritedAndLegacyForcedDropValues() {
        var state = new SpawnerLootState(baseline(List.of()), SpawnerLootDraft.unchanged());
        assertEquals("", state.gear(0));
        assertEquals("200", state.gear(2));
        assertEquals("8.5", state.gear(3));
        assertFalse(state.draft().dirty());
        state.gear(2, "25");
        assertEquals(.25f, state.draft().gear().get(2));
        state.resetGear(2);
        assertEquals("200", state.gear(2));
        assertFalse(state.draft().dirty());
        state.gear(0, "0");
        assertEquals(0f, state.draft().gear().get(0));
        assertEquals(-1f, state.draft().gear().get(1));
    }
    @Test void percentageValidationRejectsNonFiniteOverflowAndPartialNumbers() {
        var state = new SpawnerLootState(baseline(List.of()), SpawnerLootDraft.unchanged());
        for (String invalid : List.of("", "-", "NaN", "Infinity", "1e999", "-1", "100.1", " 3", "3 ")) {
            state.gear(4, invalid);
            assertFalse(state.error().isEmpty(), invalid);
            assertThrows(IllegalArgumentException.class, state::draft);
        }
        state.gear(4, "33.333");
        assertEquals(.33333f, state.draft().gear().get(4), .000001f);
        assertTrue(state.error().isEmpty());
    }
    @Test void duplicateItemsRemainIndependentByStableRowIdentity() {
        Rule a = row("a", "minecraft:diamond", .1f), b = row("b", "minecraft:diamond", .2f);
        var state = new SpawnerLootState(baseline(List.of(a, b)), SpawnerLootDraft.unchanged());
        assertFalse(state.draft().replaceRules());
        state.save(row("b", "minecraft:diamond", .75f));
        assertEquals(a, state.rules().getFirst());
        assertEquals(.75f, state.rules().get(1).chance());
        state.remove("a");
        assertEquals(List.of(row("b", "minecraft:diamond", .75f)), state.rules());
        assertTrue(state.draft().replaceRules());
        state.remove("b");
        assertTrue(state.draft().replaceRules()); assertTrue(state.draft().rules().isEmpty());
    }
    @Test void openingAStagedDraftAndInspectingAnotherEntityCannotEraseLootEdits() {
        Rule saved = row("saved", "minecraft:emerald", .5f);
        var staged = new SpawnerLootDraft(List.of(-1f, .75f, -1f, -1f, -1f, -1f), true, List.of(saved));
        var state = new SpawnerLootState(baseline(List.of()), staged);
        var before = state.draft();
        state.inspection(new Snapshot(Collections.nCopies(6, 0f), List.of(row("foreign", "minecraft:dirt", 1f)),
                List.of(new DefaultDrop("minecraft:bone", "Conditional")), "Server table.", "minecraft:skeleton"));
        assertEquals(before, state.draft());
        assertEquals("minecraft:skeleton", state.snapshot().entityId());
        assertEquals("minecraft:bone", state.snapshot().defaults().getFirst().itemId());
        assertEquals("75", state.gear(1));
        assertEquals(List.of(saved), state.rules());
    }
    @Test void boundedRowsAllowExistingRowEditsAtCapacityAndProduceDistinctIds() {
        var state = new SpawnerLootState(baseline(List.of()), SpawnerLootDraft.unchanged());
        for (int i = 0; i < 128; i++) assertTrue(state.save(row("r" + i, "minecraft:diamond", .1f)));
        assertFalse(state.save(row("overflow", "minecraft:diamond", .1f)));
        assertTrue(state.save(row("r0", "minecraft:emerald", .9f)));
        assertEquals(128, state.rules().size());
        assertNotEquals(SpawnerLootState.newId(), SpawnerLootState.newId());
        assertTrue(SpawnerLootState.newId().matches("[A-Za-z0-9_-]{1,64}"));
    }
    @Test void loadedPresetRequiresDiscardConfirmationEvenWhenVisibleFieldsEqualBaseline() {
        var state = new SpawnerEditorState(UUID.randomUUID(), SpawnerEditorDraft.empty());
        assertFalse(state.dirty()); state.presetLoaded(true); assertTrue(state.dirty());
        assertTrue(state.beginApply());
        assertTrue(state.reject(state.session(), "Conflict."));
        assertTrue(state.dirty());
    }
    @Test void lootIsIncludedInMainDirtyStateAndRetainedAfterRejectedApply() {
        var state = new SpawnerEditorState(UUID.randomUUID(), SpawnerEditorDraft.empty());
        var loot = new SpawnerLootDraft(Collections.nCopies(6, -1f), true, List.of(row("r1", "minecraft:bone", .25f)));
        state.loot(loot); assertTrue(state.dirty());
        assertTrue(state.beginApply()); state.reject(state.session(), "Changed target.");
        assertEquals(loot, state.loot()); assertTrue(state.dirty());
    }
}
