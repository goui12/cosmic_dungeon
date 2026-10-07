package net.goui.cosmicdungeon.client;

import net.minecraft.world.inventory.RecipeBookMenu;

/** Presentation only, for every recipe-book menu; server CraftingPolicy still authorizes crafting. */
public final class DungeonInventoryRecipeBook {
    private DungeonInventoryRecipeBook() {}

    public static boolean hidden(RecipeBookMenu menu) {
        return true;
    }
}
