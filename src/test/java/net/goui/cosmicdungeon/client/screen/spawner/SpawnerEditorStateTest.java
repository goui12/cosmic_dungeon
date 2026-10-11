package net.goui.cosmicdungeon.client.screen.spawner;

import java.util.HashSet;
import java.util.UUID;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEditorDraft;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SpawnerEditorStateTest {
    private static SpawnerEditorState state() {
        return new SpawnerEditorState(UUID.randomUUID(), SpawnerEditorDraft.empty());
    }
    @Test void tabChangesAndResizesCannotLosePartialTextOrSelectedFields() {
        var state = state();
        state.entityId("minecraft:pillager"); state.name("Gate Captain"); state.captain(true);
        state.cap(""); state.minDelay("00020"); state.maxDelay("900");
        state.search("PILL"); state.page(3); state.tab(SpawnerEditorState.Tab.FLAGS);
        state.silent(true); state.noAi(true); state.invulnerable(true); state.persistent(true); state.nameVisible(true);
        SpawnerEditorLayout.fit(320, 180); SpawnerEditorLayout.fit(1920, 1080);
        state.tab(SpawnerEditorState.Tab.ENTITY);
        assertEquals("", state.cap()); assertEquals("00020", state.minDelay());
        assertEquals("PILL", state.search()); assertEquals(3, state.page());
        assertEquals("Gate Captain", state.name()); assertTrue(state.captain());
        assertTrue(state.dirty()); assertFalse(state.error().isEmpty()); assertFalse(state.beginApply());
        state.cap("7");
        assertEquals(new SpawnerEditorDraft("minecraft:pillager", "Gate Captain", true, false, 7, 20, 900,
                true, true, true, true, true), state.draft());
        assertTrue(state.error().isEmpty()); assertTrue(state.beginApply());
    }
    @Test void serverRejectionRetainsEditsAndStaleSessionsCannotReleaseAnInflightApply() {
        var state = state(); state.name("Unapplied");
        assertTrue(state.beginApply()); assertFalse(state.beginApply());
        assertFalse(state.reject(UUID.randomUUID(), "Wrong session"));
        assertTrue(state.pending());
        assertTrue(state.reject(state.session(), "Spawner changed; reopen it."));
        assertFalse(state.pending()); assertTrue(state.dirty());
        assertEquals("Unapplied", state.name()); assertEquals("Spawner changed; reopen it.", state.notice());
        assertTrue(state.beginApply());
    }
    @Test void closeEndsTheSessionWithoutChangingTheOriginalAndCannotReopenIt() {
        var state = state();
        assertFalse(state.dirty()); state.name("Discard me");
        assertFalse(state.close(UUID.randomUUID())); assertFalse(state.closed());
        assertTrue(state.close(state.session())); assertTrue(state.closed());
        assertFalse(state.beginApply()); assertFalse(state.reject(state.session(), "late"));
        var reopened = new SpawnerEditorState(UUID.randomUUID(), SpawnerEditorDraft.empty());
        assertFalse(reopened.dirty()); assertEquals("", reopened.name());
    }
    @Test void numericDraftRejectsOverflowWhitespaceNegativeAndIncompleteInputWithoutThrowing() {
        for (String bad : new String[] {"", "-1", " 2", "1.0", "9999999999", "abc"}) {
            var state = state(); state.cap(bad);
            assertFalse(state.error().isEmpty(), bad); assertFalse(state.beginApply());
        }
        var state = state(); state.minDelay("900"); state.maxDelay("20");
        assertFalse(state.error().isEmpty()); assertFalse(state.beginApply());
        state.minDelay("1"); state.maxDelay(Integer.toString(SpawnerEditorDraft.MAX_DELAY));
        state.cap(Integer.toString(SpawnerEditorDraft.MAX_CAP));
        assertTrue(state.error().isEmpty()); assertTrue(state.beginApply());
    }
    @Test void revertingFieldsRestoresCleanDraftButSearchAndNavigationNeverDirtyIt() {
        var state = state(); state.search("zombie"); state.page(5); state.tab(SpawnerEditorState.Tab.PREVIEW);
        assertFalse(state.dirty());
        state.name("A"); assertTrue(state.dirty()); state.name(""); assertFalse(state.dirty());
        state.cap("000"); assertTrue(state.dirty()); state.cap("0"); assertFalse(state.dirty());
        state.search("pig"); assertEquals(0, state.page());
    }
    @Test void entitySearchFindsRegistryIdsAndLocalizedNamesWithoutCaseOrWhitespaceSensitivity() {
        assertTrue(SpawnerEditorState.matches(" ZOMB ", "minecraft:zombie", "Zombie"));
        assertTrue(SpawnerEditorState.matches("minecraft:", "minecraft:zombie", "Zombie"));
        assertTrue(SpawnerEditorState.matches("guardian", "minecraft:elder_guardian", "Elder Guardian"));
        assertTrue(SpawnerEditorState.matches("", "minecraft:pig", "Pig"));
        assertFalse(SpawnerEditorState.matches("wither", "minecraft:pig", "Pig"));
    }
    @Test void compactAndWideLayoutsKeepFormsAndPaginationInsideTheWindow() {
        for (int[] dimensions : new int[][] {{320,180},{427,240},{640,360},{1920,1080}}) {
            var layout = SpawnerEditorLayout.fit(dimensions[0], dimensions[1]);
            assertTrue(layout.x() >= 0 && layout.y() >= 0);
            assertTrue(layout.x() + layout.width() <= dimensions[0]);
            assertTrue(layout.y() + layout.height() <= dimensions[1]);
            assertTrue(layout.contentY() + 72 <= layout.footerY() - 22);
            assertTrue(layout.contentY() + 25 + layout.entityRows() * 22 + 18 <= layout.footerY() - 22);
            assertTrue(layout.contentX() + layout.contentWidth() < layout.x() + layout.width());
            assertTrue(layout.footerY() + 20 <= dimensions[1]);
        }
    }
    @Test void pagingReachesEveryEntityAndClampsWhenSearchShrinksTheCatalog() {
        var reached = new HashSet<Integer>();
        int total = 137, capacity = SpawnerEditorLayout.fit(320, 180).entityRows();
        for (int requested = 0; requested < total; requested++) {
            int page = SpawnerEditorLayout.page(requested, total, capacity);
            for (int row = 0; row < capacity; row++) {
                int index = page * capacity + row;
                if (index < total) reached.add(index);
            }
        }
        assertEquals(total, reached.size());
        assertEquals(0, SpawnerEditorLayout.page(Integer.MAX_VALUE, 0, capacity));
        assertEquals(0, SpawnerEditorLayout.page(20, 1, capacity));
        assertEquals(0, SpawnerEditorLayout.page(-1, total, capacity));
    }
}
