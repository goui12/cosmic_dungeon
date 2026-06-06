package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Shared Controls-menu categories for Cosmic Dungeon client keybindings.
 */
public final class CosmicDungeonKeybindCategories {
    public static final KeyMapping.Category COSMIC_DUNGEON =
            new KeyMapping.Category(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "cosmicdungeon"));

    private CosmicDungeonKeybindCategories() {}

    /**
     * MOD BUS event.
     *
     * Register this category exactly once before individual key mappings are registered.
     */
    public static void registerCategories(RegisterKeyMappingsEvent event) {
        event.registerCategory(COSMIC_DUNGEON);
    }
}
