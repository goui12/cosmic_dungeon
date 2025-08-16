package net.goui.cosmicdungeon.util;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {

    public static class Blocks {
        // Custom tags in YOUR namespace
        public static final TagKey<Block> AMETHYST_BLOCKS       = create("amethyst_blocks");       // decorative colored amethyst blocks
        public static final TagKey<Block> BUDDING_AMETHYST      = create("budding_amethyst");      // the budding block(s)

        public static final TagKey<Block> AMETHYST_BUDS_SMALL   = create("amethyst_buds_small");
        public static final TagKey<Block> AMETHYST_BUDS_MEDIUM  = create("amethyst_buds_medium");
        public static final TagKey<Block> AMETHYST_BUDS_LARGE   = create("amethyst_buds_large");
        public static final TagKey<Block> AMETHYST_CLUSTERS     = create("amethyst_clusters");

        // Helper to make a cosmicdungeon:block tag key
        private static TagKey<Block> create(String path) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path));
        }
    }

    public static class Items {
        // You already had this
        public static final TagKey<Item> TRANSFORMABLE_ITEMS = create("transformable_items");

        // Optional: colored amethyst shards (if/when you add the item)
        public static final TagKey<Item> AMETHYST_SHARDS = create("amethyst_shards");

        private static TagKey<Item> create(String path) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path));
        }
    }
}
