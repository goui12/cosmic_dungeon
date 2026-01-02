// file: src/main/java/net/goui/cosmicdungeon/util/ModTags.java
package net.goui.cosmicdungeon.util;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Canonical tag keys for Cosmic Dungeon.
 *
 * Rules:
 * - All tag keys live here (single source of truth).
 * - Tag paths are always under your namespace unless explicitly vanilla.
 */
public final class ModTags {
    private ModTags() {}

    public static final class Blocks {
        private Blocks() {}

        // ===== Custom tags (your namespace) =====
        public static final TagKey<Block> AMETHYST_BLOCKS      = create("amethyst_blocks");
        public static final TagKey<Block> BUDDING_AMETHYST     = create("budding_amethyst");

        public static final TagKey<Block> AMETHYST_BUDS_SMALL  = create("amethyst_buds_small");
        public static final TagKey<Block> AMETHYST_BUDS_MEDIUM = create("amethyst_buds_medium");
        public static final TagKey<Block> AMETHYST_BUDS_LARGE  = create("amethyst_buds_large");
        public static final TagKey<Block> AMETHYST_CLUSTERS    = create("amethyst_clusters");

        private static TagKey<Block> create(String path) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path));
        }
    }

    public static final class Items {
        private Items() {}

        // ===== Existing tags =====
        public static final TagKey<Item> TRANSFORMABLE_ITEMS = create("transformable_items");
        public static final TagKey<Item> AMETHYST_SHARDS     = create("amethyst_shards");

        public static final TagKey<Item> SHIELDS =
                ItemTags.create(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "shields"));

        public static final TagKey<Item> INFINITE_SHOOTABLES =
                ItemTags.create(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "infinite_shootables"));

        // ===== Class restriction tags (data-driven enforcement) =====
        // Items in these tags should be restricted to that class for USE and (eventually) PICKUP.
        //
        // Path layout:
        //   data/cosmicdungeon/tags/items/class_restricted/<class>.json
        //
        // Example:
        //   class_restricted/metalmancer
        public static final TagKey<Item> CLASS_RESTRICTED_METALMANCER = create("class_restricted/metalmancer");
        public static final TagKey<Item> CLASS_RESTRICTED_JUDICATOR   = create("class_restricted/judicator");
        public static final TagKey<Item> CLASS_RESTRICTED_DRAGOON     = create("class_restricted/dragoon");
        public static final TagKey<Item> CLASS_RESTRICTED_DEADEYE     = create("class_restricted/deadeye");
        public static final TagKey<Item> CLASS_RESTRICTED_PYROCLAST   = create("class_restricted/pyroclast");
        public static final TagKey<Item> CLASS_RESTRICTED_THEURGIST   = create("class_restricted/theurgist");
        public static final TagKey<Item> CLASS_RESTRICTED_VENEFEX     = create("class_restricted/venefex");
        public static final TagKey<Item> CLASS_RESTRICTED_BOGATYR     = create("class_restricted/bogatyr");

        private static TagKey<Item> create(String path) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path));
        }
    }
}
