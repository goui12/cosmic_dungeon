package net.minecraft.world.level.block.entity;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import java.util.Collections;
import java.util.SequencedSet;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public class FuelValues {
    private final Object2IntSortedMap<Item> values;

    FuelValues(Object2IntSortedMap<Item> values) {
        this.values = values;
    }

    public boolean isFuel(ItemStack stack) {
        return this.values.containsKey(stack.getItem());
    }

    public SequencedSet<Item> fuelItems() {
        return Collections.unmodifiableSequencedSet(this.values.keySet());
    }

    /**
     * @deprecated Neo: use {@link ItemStack#getBurnTime(
     *             net.minecraft.world.item.crafting.RecipeType, FuelValues)} instead
     */
    @Deprecated
    public int burnDuration(ItemStack stack) {
        return stack.isEmpty() ? 0 : this.values.getInt(stack.getItem());
    }

    public static FuelValues vanillaBurnTimes(HolderLookup.Provider registries, FeatureFlagSet enabledFeatures) {
        return vanillaBurnTimes(registries, enabledFeatures, 200);
    }

    public static FuelValues vanillaBurnTimes(HolderLookup.Provider registries, FeatureFlagSet enabledFeatures, int smeltingTime) {
        return vanillaBurnTimes(new FuelValues.Builder(registries, enabledFeatures), smeltingTime);
    }

    public static FuelValues vanillaBurnTimes(FuelValues.Builder builder, int p_363365_) {
        return builder
            .add(Items.LAVA_BUCKET, p_363365_ * 100)
            .add(Blocks.COAL_BLOCK, p_363365_ * 8 * 10)
            .add(Items.BLAZE_ROD, p_363365_ * 12)
            .add(Items.COAL, p_363365_ * 8)
            .add(Items.CHARCOAL, p_363365_ * 8)
            .add(ItemTags.LOGS, p_363365_ * 3 / 2)
            .add(ItemTags.BAMBOO_BLOCKS, p_363365_ * 3 / 2)
            .add(ItemTags.PLANKS, p_363365_ * 3 / 2)
            .add(Blocks.BAMBOO_MOSAIC, p_363365_ * 3 / 2)
            .add(ItemTags.WOODEN_STAIRS, p_363365_ * 3 / 2)
            .add(Blocks.BAMBOO_MOSAIC_STAIRS, p_363365_ * 3 / 2)
            .add(ItemTags.WOODEN_SLABS, p_363365_ * 3 / 4)
            .add(Blocks.BAMBOO_MOSAIC_SLAB, p_363365_ * 3 / 4)
            .add(ItemTags.WOODEN_TRAPDOORS, p_363365_ * 3 / 2)
            .add(ItemTags.WOODEN_PRESSURE_PLATES, p_363365_ * 3 / 2)
            .add(ItemTags.WOODEN_SHELVES, p_363365_ * 3 / 2)
            .add(ItemTags.WOODEN_FENCES, p_363365_ * 3 / 2)
            .add(ItemTags.FENCE_GATES, p_363365_ * 3 / 2)
            .add(Blocks.NOTE_BLOCK, p_363365_ * 3 / 2)
            .add(Blocks.BOOKSHELF, p_363365_ * 3 / 2)
            .add(Blocks.CHISELED_BOOKSHELF, p_363365_ * 3 / 2)
            .add(Blocks.LECTERN, p_363365_ * 3 / 2)
            .add(Blocks.JUKEBOX, p_363365_ * 3 / 2)
            .add(Blocks.CHEST, p_363365_ * 3 / 2)
            .add(Blocks.TRAPPED_CHEST, p_363365_ * 3 / 2)
            .add(Blocks.CRAFTING_TABLE, p_363365_ * 3 / 2)
            .add(Blocks.DAYLIGHT_DETECTOR, p_363365_ * 3 / 2)
            .add(ItemTags.BANNERS, p_363365_ * 3 / 2)
            .add(Items.BOW, p_363365_ * 3 / 2)
            .add(Items.FISHING_ROD, p_363365_ * 3 / 2)
            .add(Blocks.LADDER, p_363365_ * 3 / 2)
            .add(ItemTags.SIGNS, p_363365_)
            .add(ItemTags.HANGING_SIGNS, p_363365_ * 4)
            .add(Items.WOODEN_SHOVEL, p_363365_)
            .add(Items.WOODEN_SWORD, p_363365_)
            .add(Items.WOODEN_HOE, p_363365_)
            .add(Items.WOODEN_AXE, p_363365_)
            .add(Items.WOODEN_PICKAXE, p_363365_)
            .add(ItemTags.WOODEN_DOORS, p_363365_)
            .add(ItemTags.BOATS, p_363365_ * 6)
            .add(ItemTags.WOOL, p_363365_ / 2)
            .add(ItemTags.WOODEN_BUTTONS, p_363365_ / 2)
            .add(Items.STICK, p_363365_ / 2)
            .add(ItemTags.SAPLINGS, p_363365_ / 2)
            .add(Items.BOWL, p_363365_ / 2)
            .add(ItemTags.WOOL_CARPETS, 1 + p_363365_ / 3)
            .add(Blocks.DRIED_KELP_BLOCK, 1 + p_363365_ * 20)
            .add(Items.CROSSBOW, p_363365_ * 3 / 2)
            .add(Blocks.BAMBOO, p_363365_ / 4)
            .add(Blocks.DEAD_BUSH, p_363365_ / 2)
            .add(Blocks.SHORT_DRY_GRASS, p_363365_ / 2)
            .add(Blocks.TALL_DRY_GRASS, p_363365_ / 2)
            .add(Blocks.SCAFFOLDING, p_363365_ / 4)
            .add(Blocks.LOOM, p_363365_ * 3 / 2)
            .add(Blocks.BARREL, p_363365_ * 3 / 2)
            .add(Blocks.CARTOGRAPHY_TABLE, p_363365_ * 3 / 2)
            .add(Blocks.FLETCHING_TABLE, p_363365_ * 3 / 2)
            .add(Blocks.SMITHING_TABLE, p_363365_ * 3 / 2)
            .add(Blocks.COMPOSTER, p_363365_ * 3 / 2)
            .add(Blocks.AZALEA, p_363365_ / 2)
            .add(Blocks.FLOWERING_AZALEA, p_363365_ / 2)
            .add(Blocks.MANGROVE_ROOTS, p_363365_ * 3 / 2)
            .add(Blocks.LEAF_LITTER, p_363365_ / 2)
            .remove(ItemTags.NON_FLAMMABLE_WOOD)
            .build();
    }

    public static class Builder {
        private final HolderLookup<Item> items;
        private final FeatureFlagSet enabledFeatures;
        private final Object2IntSortedMap<Item> values = new Object2IntLinkedOpenHashMap<>();

        public Builder(HolderLookup.Provider registries, FeatureFlagSet enabledFeatures) {
            this.items = registries.lookupOrThrow(Registries.ITEM);
            this.enabledFeatures = enabledFeatures;
        }

        public FuelValues build() {
            return new FuelValues(this.values);
        }

        public FuelValues.Builder remove(TagKey<Item> tag) {
            this.values.keySet().removeIf(p_363176_ -> p_363176_.builtInRegistryHolder().is(tag));
            return this;
        }

        public FuelValues.Builder add(TagKey<Item> tag, int value) {
            this.items.get(tag).ifPresent(p_364314_ -> {
                for (Holder<Item> holder : p_364314_) {
                    this.putInternal(value, holder.value());
                }
            });
            return this;
        }

        public FuelValues.Builder add(ItemLike p_item, int value) {
            Item item = p_item.asItem();
            this.putInternal(value, item);
            return this;
        }

        private void putInternal(int value, Item item) {
            if (item.isEnabled(this.enabledFeatures)) {
                this.values.put(item, value);
            }
        }
    }
}
