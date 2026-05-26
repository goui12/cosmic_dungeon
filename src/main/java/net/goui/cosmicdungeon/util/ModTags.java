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

        // ===== Dungeon protection allowlists =====
        // ONLY consulted when a player is inside a protected region.
        public static final TagKey<Block> DUNGEONEER_BREAKABLE     = create("dungeoneer_breakable");
        public static final TagKey<Block> DUNGEONEER_PLACEABLE     = create("dungeoneer_placeable");
        public static final TagKey<Block> DUNGEONEER_INTERACTABLE  = create("dungeoneer_interactable");

        // ===== Region exception block-sets =====
        // These define WHAT qualifies for an exception; whether allowed is controlled by region flags.
        public static final TagKey<Block> EX_PLACE_TORCH   = create("ex/place/torch");
        public static final TagKey<Block> EX_PLACE_LADDER  = create("ex/place/ladder");
        public static final TagKey<Block> EX_PLACE_WATER   = create("ex/place/water");

        public static final TagKey<Block> EX_BREAK_TORCH   = create("ex/break/torch");
        public static final TagKey<Block> EX_BREAK_LADDER  = create("ex/break/ladder");

        // Legacy/aggregate tags (optional to keep; harmless if present)
        public static final TagKey<Block> REGION_EX_TORCH_BLOCKS   = create("region_ex_torch_blocks");
        public static final TagKey<Block> REGION_EX_LADDER_BLOCKS  = create("region_ex_ladder_blocks");

        private static TagKey<Block> create(String path) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path));
        }
    }

    public static final class Items {
        private Items() {}

        // ===== Existing tags =====
        public static final TagKey<Item> TRANSFORMABLE_ITEMS = create("transformable_items");
        public static final TagKey<Item> AMETHYST_SHARDS     = create("amethyst_shards");

        public static final TagKey<Item> ATTUNEMENT_CURRENCY = create("attunement_currency");

        public static final TagKey<Item> SHIELDS =
                ItemTags.create(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "shields"));

        public static final TagKey<Item> INFINITE_SHOOTABLES =
                ItemTags.create(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "infinite_shootables"));

        // ===== Class restriction tags =====
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