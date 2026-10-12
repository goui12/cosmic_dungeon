package net.goui.cosmicdungeon.client.screen.crafting;

import java.util.*;
import net.goui.cosmicdungeon.network.PotionCraftPayloads.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PotionPaletteTest {
    private Entry entry(String id, int own, int shared) {
        var potion = id.contains("strong_") ? Potions.STRONG_HEALING : Potions.HEALING;
        return new Entry("minecraft:" + id + "_splash", PotionContents.createItemStack(Items.SPLASH_POTION, potion), own, shared, 3, 1);
    }
    private View view(long run, long revision, Entry... entries) { return new View(run, revision, "theurgist", 17, 20, List.of(entries)); }
    @Test void variantsKeepAllFamiliesAndOnlySelectRealServerEntries() {
        var normal = entry("healing", 2, 4); var strong = entry("strong_healing", 0, 1); var other = entry("night_vision", 0, 0);
        var entries = List.of(normal, strong, other);
        assertEquals(List.of(normal, other), PotionPaletteModel.visible(entries, PotionPaletteModel.Variant.NORMAL));
        assertEquals(List.of(strong, other), PotionPaletteModel.visible(entries, PotionPaletteModel.Variant.STRONG));
        assertEquals(List.of(normal, other), PotionPaletteModel.visible(entries, PotionPaletteModel.Variant.EXTENDED));
        assertEquals("minecraft:turtle_master_drinkable", PotionPaletteModel.family("minecraft:long_turtle_master_drinkable"));
        assertEquals(PotionPaletteModel.Variant.EXTENDED, PotionPaletteModel.variant("minecraft:long_turtle_master_drinkable"));
    }
    @Test void ownSharedAndMissingCountsHaveDistinctPresentations() {
        var own = entry("healing", 2, 5); var shared = entry("healing", 0, 5); var missing = entry("healing", 0, 0);
        assertEquals(2, PotionPaletteModel.count(own)); assertEquals(5, PotionPaletteModel.count(shared)); assertEquals(0, PotionPaletteModel.count(missing));
        assertTrue(PotionPaletteModel.source(own).contains("your own inventory"));
        assertTrue(PotionPaletteModel.source(shared).contains("other players' inventories"));
        assertTrue(PotionPaletteModel.source(missing).contains("red 0"));
        assertNotEquals(PotionPaletteModel.color(shared), PotionPaletteModel.color(missing));
        assertNotEquals(PotionPaletteModel.background(own), PotionPaletteModel.background(shared));
        assertNotEquals(PotionPaletteModel.background(missing), PotionPaletteModel.background(shared));
    }
    @Test void minimumAndLargeScreensKeepEveryTileVisibleOutsideNativeSlots() {
        for (int[] size : new int[][] {{320,240}, {426,240}, {640,360}, {854,480}})
            for (int inventoryHeight : new int[] {166,184})
                for (int count : new int[] {9,11}) {
                    int left = (size[0] - 176) / 2, top = (size[1] - inventoryHeight) / 2;
                    var layout = PotionPaletteLayout.of(size[0], size[1], left, top, 176, count);
                    assertTrue(layout.panel().x() >= left + 176);
                    assertTrue(layout.panel().right() <= size[0] && layout.panel().bottom() <= size[1]);
                    assertEquals(count, layout.tiles().size());
                    for (var tile : layout.tiles()) {
                        assertTrue(tile.x() >= layout.panel().x() && tile.right() <= layout.panel().right());
                        assertTrue(tile.y() >= layout.panel().y() && tile.bottom() <= layout.panel().bottom());
                        assertFalse(tile.intersects(layout.fuel()));
                        for (var toggle : layout.variants()) assertFalse(tile.intersects(toggle));
                    }
                    assertEquals(count, layout.tiles().stream().distinct().count());
                    if (size[0] >= 640) assertEquals(2, layout.tiles().stream().map(box -> box.y()).distinct().count());
                }
    }
    @Test void exactGenerationsAndAcknowledgementsProtectRepeatedClicks() {
        var state = new PotionPaletteState(); var sent = new ArrayList<Action>(); var requests = new ArrayList<Request>();
        state.actions(sent::add, requests::add); assertTrue(state.toggle()); assertEquals(List.of(new Request(true)), requests);
        var recipe = entry("healing", 3, 3); state.receive(view(8, 10, recipe));
        assertTrue(state.craft(recipe.targetId(), false));
        assertEquals(new Action(8, 10, recipe.targetId(), false), sent.getFirst());
        assertFalse(state.craft(recipe.targetId(), false)); assertTrue(state.pending());
        state.receive(view(8, 9, recipe)); assertTrue(state.pending(), "An older snapshot cannot acknowledge a newer click");
        state.receive(view(8, 10, recipe)); assertFalse(state.pending());
        assertTrue(state.craft(recipe.targetId(), true)); assertTrue(sent.getLast().shift());
        state.receive(view(9, 1, recipe)); assertTrue(state.craft(recipe.targetId(), false));
        assertEquals(9, sent.getLast().run());
        state.close(); assertEquals(new Request(false), requests.getLast()); assertFalse(state.craft(recipe.targetId(), false));
        state.receive(view(9, 2, recipe)); assertEquals(0, state.view().run(), "Late closed-palette packets cannot restore actions");
    }
    @Test void zeroCountsMissingTargetsAndUnavailableViewsNeverSend() {
        var state = new PotionPaletteState(); var sent = new ArrayList<Action>();
        state.actions(sent::add, request -> {}); state.toggle();
        var zero = entry("healing", 0, 0); state.receive(view(8, 1, zero));
        assertFalse(state.craft(zero.targetId(), false)); assertFalse(state.craft("minecraft:invented", true));
        state.receive(new View(0, 2, "", 0, 20, List.of()));
        assertFalse(state.craft(zero.targetId(), false)); assertTrue(sent.isEmpty());
        state.clear(); assertFalse(state.open()); assertFalse(state.pending());
    }
    @Test void changingVariantsWhilePendingCannotQueueAnotherPurchase() {
        var state = new PotionPaletteState(); var sent = new ArrayList<Action>();
        state.actions(sent::add, request -> {}); state.toggle();
        var normal = entry("healing", 1, 1); var strong = entry("strong_healing", 1, 1);
        state.receive(view(8, 1, normal, strong)); state.craft(normal.targetId(), false);
        state.variant(PotionPaletteModel.Variant.STRONG);
        assertFalse(state.craft(strong.targetId(), false));
        state.receive(view(8, 2, normal, strong));
        assertFalse(state.craft(normal.targetId(), false), "Only the displayed variant is actionable");
        assertTrue(state.craft(strong.targetId(), false)); assertEquals(2, sent.size());
    }
}
