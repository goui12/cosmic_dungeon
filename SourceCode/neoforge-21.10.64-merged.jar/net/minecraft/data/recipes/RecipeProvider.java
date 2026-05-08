package net.minecraft.data.recipes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.EnterBlockTrigger;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SuspiciousEffectHolder;

public abstract class RecipeProvider {
    protected final HolderLookup.Provider registries;
    protected final HolderGetter<Item> items;
    protected final RecipeOutput output;
    private static final Map<BlockFamily.Variant, RecipeProvider.FamilyRecipeProvider> SHAPE_BUILDERS = ImmutableMap.<BlockFamily.Variant, RecipeProvider.FamilyRecipeProvider>builder()
        .put(BlockFamily.Variant.BUTTON, (p_359442_, p_359443_, p_359444_) -> p_359442_.buttonBuilder(p_359443_, Ingredient.of(p_359444_)))
        .put(
            BlockFamily.Variant.CHISELED,
            (p_359448_, p_359449_, p_359450_) -> p_359448_.chiseledBuilder(RecipeCategory.BUILDING_BLOCKS, p_359449_, Ingredient.of(p_359450_))
        )
        .put(
            BlockFamily.Variant.CUT,
            (p_359439_, p_359440_, p_359441_) -> p_359439_.cutBuilder(RecipeCategory.BUILDING_BLOCKS, p_359440_, Ingredient.of(p_359441_))
        )
        .put(BlockFamily.Variant.DOOR, (p_359456_, p_359457_, p_359458_) -> p_359456_.doorBuilder(p_359457_, Ingredient.of(p_359458_)))
        .put(BlockFamily.Variant.CUSTOM_FENCE, (p_359445_, p_359446_, p_359447_) -> p_359445_.fenceBuilder(p_359446_, Ingredient.of(p_359447_)))
        .put(BlockFamily.Variant.FENCE, (p_359430_, p_359431_, p_359432_) -> p_359430_.fenceBuilder(p_359431_, Ingredient.of(p_359432_)))
        .put(BlockFamily.Variant.CUSTOM_FENCE_GATE, (p_359420_, p_359421_, p_359422_) -> p_359420_.fenceGateBuilder(p_359421_, Ingredient.of(p_359422_)))
        .put(BlockFamily.Variant.FENCE_GATE, (p_359411_, p_359412_, p_359413_) -> p_359411_.fenceGateBuilder(p_359412_, Ingredient.of(p_359413_)))
        .put(BlockFamily.Variant.SIGN, (p_359408_, p_359409_, p_359410_) -> p_359408_.signBuilder(p_359409_, Ingredient.of(p_359410_)))
        .put(
            BlockFamily.Variant.SLAB,
            (p_359405_, p_359406_, p_359407_) -> p_359405_.slabBuilder(RecipeCategory.BUILDING_BLOCKS, p_359406_, Ingredient.of(p_359407_))
        )
        .put(BlockFamily.Variant.STAIRS, (p_359433_, p_359434_, p_359435_) -> p_359433_.stairBuilder(p_359434_, Ingredient.of(p_359435_)))
        .put(
            BlockFamily.Variant.PRESSURE_PLATE,
            (p_359451_, p_359452_, p_359453_) -> p_359451_.pressurePlateBuilder(RecipeCategory.REDSTONE, p_359452_, Ingredient.of(p_359453_))
        )
        .put(
            BlockFamily.Variant.POLISHED,
            (p_359417_, p_359418_, p_359419_) -> p_359417_.polishedBuilder(RecipeCategory.BUILDING_BLOCKS, p_359418_, Ingredient.of(p_359419_))
        )
        .put(BlockFamily.Variant.TRAPDOOR, (p_359414_, p_359415_, p_359416_) -> p_359414_.trapdoorBuilder(p_359415_, Ingredient.of(p_359416_)))
        .put(
            BlockFamily.Variant.WALL,
            (p_359436_, p_359437_, p_359438_) -> p_359436_.wallBuilder(RecipeCategory.DECORATIONS, p_359437_, Ingredient.of(p_359438_))
        )
        .build();

    protected RecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        this.registries = registries;
        this.items = registries.lookupOrThrow(Registries.ITEM);
        this.output = output;
    }

    protected abstract void buildRecipes();

    protected void generateForEnabledBlockFamilies(FeatureFlagSet enabledFeatures) {
        BlockFamilies.getAllFamilies().filter(BlockFamily::shouldGenerateRecipe).forEach(p_359455_ -> this.generateRecipes(p_359455_, enabledFeatures));
    }

    protected void oneToOneConversionRecipe(ItemLike result, ItemLike ingredient, @Nullable String group) {
        this.oneToOneConversionRecipe(result, ingredient, group, 1);
    }

    protected void oneToOneConversionRecipe(ItemLike result, ItemLike ingredient, @Nullable String group, int resultCount) {
        this.shapeless(RecipeCategory.MISC, result, resultCount)
            .requires(ingredient)
            .group(group)
            .unlockedBy(getHasName(ingredient), this.has(ingredient))
            .save(this.output, getConversionRecipeName(result, ingredient));
    }

    protected void oreSmelting(List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float experience, int cookingTime, String group) {
        this.oreCooking(
            RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, ingredients, category, result, experience, cookingTime, group, "_from_smelting"
        );
    }

    protected void oreBlasting(List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float experience, int cookingTime, String group) {
        this.oreCooking(
            RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, ingredients, category, result, experience, cookingTime, group, "_from_blasting"
        );
    }

    protected <T extends AbstractCookingRecipe> void oreCooking(
        RecipeSerializer<T> serializer,
        AbstractCookingRecipe.Factory<T> recipeFactory,
        List<ItemLike> ingredients,
        RecipeCategory category,
        ItemLike result,
        float experience,
        int cookingTime,
        String group,
        String suffix
    ) {
        for (ItemLike itemlike : ingredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), category, result, experience, cookingTime, serializer, recipeFactory)
                .group(group)
                .unlockedBy(getHasName(itemlike), this.has(itemlike))
                .save(this.output, getItemName(result) + suffix + "_" + getItemName(itemlike));
        }
    }

    protected void netheriteSmithing(Item ingredientItem, RecipeCategory category, Item resultItem) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ingredientItem),
                this.tag(ItemTags.NETHERITE_TOOL_MATERIALS),
                category,
                resultItem
            )
            .unlocks("has_netherite_ingot", this.has(ItemTags.NETHERITE_TOOL_MATERIALS))
            .save(this.output, getItemName(resultItem) + "_smithing");
    }

    protected void trimSmithing(Item template, ResourceKey<TrimPattern> pattern, ResourceKey<Recipe<?>> recipe) {
        Holder.Reference<TrimPattern> reference = this.registries.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(pattern);
        SmithingTrimRecipeBuilder.smithingTrim(
                Ingredient.of(template), this.tag(ItemTags.TRIMMABLE_ARMOR), this.tag(ItemTags.TRIM_MATERIALS), reference, RecipeCategory.MISC
            )
            .unlocks("has_smithing_trim_template", this.has(template))
            .save(this.output, recipe);
    }

    protected void twoByTwoPacker(RecipeCategory category, ItemLike packed, ItemLike unpacked) {
        this.shaped(category, packed, 1)
            .define('#', unpacked)
            .pattern("##")
            .pattern("##")
            .unlockedBy(getHasName(unpacked), this.has(unpacked))
            .save(this.output);
    }

    protected void threeByThreePacker(RecipeCategory category, ItemLike packed, ItemLike unpacked, String criterionName) {
        this.shapeless(category, packed).requires(unpacked, 9).unlockedBy(criterionName, this.has(unpacked)).save(this.output);
    }

    protected void threeByThreePacker(RecipeCategory category, ItemLike packed, ItemLike unpacked) {
        this.threeByThreePacker(category, packed, unpacked, getHasName(unpacked));
    }

    protected void planksFromLog(ItemLike planks, TagKey<Item> logs, int resultCount) {
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, planks, resultCount)
            .requires(logs)
            .group("planks")
            .unlockedBy("has_log", this.has(logs))
            .save(this.output);
    }

    protected void planksFromLogs(ItemLike planks, TagKey<Item> logs, int result) {
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, planks, result)
            .requires(logs)
            .group("planks")
            .unlockedBy("has_logs", this.has(logs))
            .save(this.output);
    }

    protected void woodFromLogs(ItemLike wood, ItemLike log) {
        this.shaped(RecipeCategory.BUILDING_BLOCKS, wood, 3)
            .define('#', log)
            .pattern("##")
            .pattern("##")
            .group("bark")
            .unlockedBy("has_log", this.has(log))
            .save(this.output);
    }

    protected void woodenBoat(ItemLike boat, ItemLike material) {
        this.shaped(RecipeCategory.TRANSPORTATION, boat)
            .define('#', material)
            .pattern("# #")
            .pattern("###")
            .group("boat")
            .unlockedBy("in_water", insideOf(Blocks.WATER))
            .save(this.output);
    }

    protected void chestBoat(ItemLike boat, ItemLike material) {
        this.shapeless(RecipeCategory.TRANSPORTATION, boat)
            .requires(Blocks.CHEST)
            .requires(material)
            .group("chest_boat")
            .unlockedBy("has_boat", this.has(ItemTags.BOATS))
            .save(this.output);
    }

    protected RecipeBuilder buttonBuilder(ItemLike button, Ingredient material) {
        return this.shapeless(RecipeCategory.REDSTONE, button).requires(material);
    }

    protected RecipeBuilder doorBuilder(ItemLike door, Ingredient material) {
        return this.shaped(RecipeCategory.REDSTONE, door, 3).define('#', material).pattern("##").pattern("##").pattern("##");
    }

    protected RecipeBuilder fenceBuilder(ItemLike fence, Ingredient material) {
        int i = fence == Blocks.NETHER_BRICK_FENCE ? 6 : 3;
        Item item = fence == Blocks.NETHER_BRICK_FENCE ? Items.NETHER_BRICK : Items.STICK;
        return this.shaped(RecipeCategory.DECORATIONS, fence, i).define('W', material).define('#', item).pattern("W#W").pattern("W#W");
    }

    protected RecipeBuilder fenceGateBuilder(ItemLike fenceGate, Ingredient material) {
        return this.shaped(RecipeCategory.REDSTONE, fenceGate).define('#', Items.STICK).define('W', material).pattern("#W#").pattern("#W#");
    }

    protected void pressurePlate(ItemLike pressurePlate, ItemLike material) {
        this.pressurePlateBuilder(RecipeCategory.REDSTONE, pressurePlate, Ingredient.of(material))
            .unlockedBy(getHasName(material), this.has(material))
            .save(this.output);
    }

    protected RecipeBuilder pressurePlateBuilder(RecipeCategory category, ItemLike pressurePlate, Ingredient material) {
        return this.shaped(category, pressurePlate).define('#', material).pattern("##");
    }

    protected void slab(RecipeCategory category, ItemLike slab, ItemLike material) {
        this.slabBuilder(category, slab, Ingredient.of(material)).unlockedBy(getHasName(material), this.has(material)).save(this.output);
    }

    protected void shelf(ItemLike shelf, ItemLike strippedLog) {
        this.shaped(RecipeCategory.DECORATIONS, shelf, 6)
            .define('#', strippedLog)
            .pattern("###")
            .pattern("   ")
            .pattern("###")
            .group("shelf")
            .unlockedBy(getHasName(strippedLog), this.has(strippedLog))
            .save(this.output);
    }

    protected RecipeBuilder slabBuilder(RecipeCategory category, ItemLike slab, Ingredient material) {
        return this.shaped(category, slab, 6).define('#', material).pattern("###");
    }

    protected RecipeBuilder stairBuilder(ItemLike stairs, Ingredient material) {
        return this.shaped(RecipeCategory.BUILDING_BLOCKS, stairs, 4).define('#', material).pattern("#  ").pattern("## ").pattern("###");
    }

    protected RecipeBuilder trapdoorBuilder(ItemLike trapdoor, Ingredient material) {
        return this.shaped(RecipeCategory.REDSTONE, trapdoor, 2).define('#', material).pattern("###").pattern("###");
    }

    protected RecipeBuilder signBuilder(ItemLike sign, Ingredient material) {
        return this.shaped(RecipeCategory.DECORATIONS, sign, 3)
            .group("sign")
            .define('#', material)
            .define('X', Items.STICK)
            .pattern("###")
            .pattern("###")
            .pattern(" X ");
    }

    protected void hangingSign(ItemLike sign, ItemLike material) {
        this.shaped(RecipeCategory.DECORATIONS, sign, 6)
            .group("hanging_sign")
            .define('#', material)
            .define('X', Items.IRON_CHAIN)
            .pattern("X X")
            .pattern("###")
            .pattern("###")
            .unlockedBy("has_stripped_logs", this.has(material))
            .save(this.output);
    }

    protected void colorItemWithDye(List<Item> dyeItems, List<Item> dyeableItems, String group, RecipeCategory category) {
        this.colorWithDye(dyeItems, dyeableItems, null, group, category);
    }

    protected void colorWithDye(List<Item> dyes, List<Item> dyeableItems, @Nullable Item dye, String group, RecipeCategory category) {
        for (int i = 0; i < dyes.size(); i++) {
            Item item = dyes.get(i);
            Item item1 = dyeableItems.get(i);
            Stream<Item> stream = dyeableItems.stream().filter(p_288265_ -> !p_288265_.equals(item1));
            if (dye != null) {
                stream = Stream.concat(stream, Stream.of(dye));
            }

            this.shapeless(category, item1)
                .requires(item)
                .requires(Ingredient.of(stream))
                .group(group)
                .unlockedBy("has_needed_dye", this.has(item))
                .save(this.output, "dye_" + getItemName(item1));
        }
    }

    protected void carpet(ItemLike carpet, ItemLike material) {
        this.shaped(RecipeCategory.DECORATIONS, carpet, 3)
            .define('#', material)
            .pattern("##")
            .group("carpet")
            .unlockedBy(getHasName(material), this.has(material))
            .save(this.output);
    }

    protected void bedFromPlanksAndWool(ItemLike bed, ItemLike wool) {
        this.shaped(RecipeCategory.DECORATIONS, bed)
            .define('#', wool)
            .define('X', ItemTags.PLANKS)
            .pattern("###")
            .pattern("XXX")
            .group("bed")
            .unlockedBy(getHasName(wool), this.has(wool))
            .save(this.output);
    }

    protected void banner(ItemLike banner, ItemLike material) {
        this.shaped(RecipeCategory.DECORATIONS, banner)
            .define('#', material)
            .define('|', Items.STICK)
            .pattern("###")
            .pattern("###")
            .pattern(" | ")
            .group("banner")
            .unlockedBy(getHasName(material), this.has(material))
            .save(this.output);
    }

    protected void stainedGlassFromGlassAndDye(ItemLike stainedGlass, ItemLike dye) {
        this.shaped(RecipeCategory.BUILDING_BLOCKS, stainedGlass, 8)
            .define('#', Blocks.GLASS)
            .define('X', dye)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .group("stained_glass")
            .unlockedBy("has_glass", this.has(Blocks.GLASS))
            .save(this.output);
    }

    protected void dryGhast(ItemLike dryGhast) {
        this.shaped(RecipeCategory.BUILDING_BLOCKS, dryGhast, 1)
            .define('#', Items.GHAST_TEAR)
            .define('X', Items.SOUL_SAND)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .group("dry_ghast")
            .unlockedBy(getHasName(Items.GHAST_TEAR), this.has(Items.GHAST_TEAR))
            .save(this.output);
    }

    protected void harness(ItemLike harness, ItemLike wool) {
        this.shaped(RecipeCategory.COMBAT, harness)
            .define('#', wool)
            .define('G', Items.GLASS)
            .define('L', Items.LEATHER)
            .pattern("LLL")
            .pattern("G#G")
            .group("harness")
            .unlockedBy("has_dried_ghast", this.has(Blocks.DRIED_GHAST))
            .save(this.output);
    }

    protected void stainedGlassPaneFromStainedGlass(ItemLike stainedGlassPane, ItemLike stainedGlass) {
        this.shaped(RecipeCategory.DECORATIONS, stainedGlassPane, 16)
            .define('#', stainedGlass)
            .pattern("###")
            .pattern("###")
            .group("stained_glass_pane")
            .unlockedBy("has_glass", this.has(stainedGlass))
            .save(this.output);
    }

    protected void stainedGlassPaneFromGlassPaneAndDye(ItemLike stainedGlassPane, ItemLike dye) {
        this.shaped(RecipeCategory.DECORATIONS, stainedGlassPane, 8)
            .define('#', Blocks.GLASS_PANE)
            .define('$', dye)
            .pattern("###")
            .pattern("#$#")
            .pattern("###")
            .group("stained_glass_pane")
            .unlockedBy("has_glass_pane", this.has(Blocks.GLASS_PANE))
            .unlockedBy(getHasName(dye), this.has(dye))
            .save(this.output, getConversionRecipeName(stainedGlassPane, Blocks.GLASS_PANE));
    }

    protected void coloredTerracottaFromTerracottaAndDye(ItemLike terracotta, ItemLike dye) {
        this.shaped(RecipeCategory.BUILDING_BLOCKS, terracotta, 8)
            .define('#', Blocks.TERRACOTTA)
            .define('X', dye)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .group("stained_terracotta")
            .unlockedBy("has_terracotta", this.has(Blocks.TERRACOTTA))
            .save(this.output);
    }

    protected void concretePowder(ItemLike concretePowder, ItemLike dye) {
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, concretePowder, 8)
            .requires(dye)
            .requires(Blocks.SAND, 4)
            .requires(Blocks.GRAVEL, 4)
            .group("concrete_powder")
            .unlockedBy("has_sand", this.has(Blocks.SAND))
            .unlockedBy("has_gravel", this.has(Blocks.GRAVEL))
            .save(this.output);
    }

    protected void candle(ItemLike candle, ItemLike dye) {
        this.shapeless(RecipeCategory.DECORATIONS, candle)
            .requires(Blocks.CANDLE)
            .requires(dye)
            .group("dyed_candle")
            .unlockedBy(getHasName(dye), this.has(dye))
            .save(this.output);
    }

    protected void wall(RecipeCategory category, ItemLike wall, ItemLike material) {
        this.wallBuilder(category, wall, Ingredient.of(material)).unlockedBy(getHasName(material), this.has(material)).save(this.output);
    }

    protected RecipeBuilder wallBuilder(RecipeCategory category, ItemLike wall, Ingredient material) {
        return this.shaped(category, wall, 6).define('#', material).pattern("###").pattern("###");
    }

    protected void polished(RecipeCategory category, ItemLike result, ItemLike material) {
        this.polishedBuilder(category, result, Ingredient.of(material)).unlockedBy(getHasName(material), this.has(material)).save(this.output);
    }

    protected RecipeBuilder polishedBuilder(RecipeCategory category, ItemLike result, Ingredient material) {
        return this.shaped(category, result, 4).define('S', material).pattern("SS").pattern("SS");
    }

    protected void cut(RecipeCategory category, ItemLike cutResult, ItemLike material) {
        this.cutBuilder(category, cutResult, Ingredient.of(material)).unlockedBy(getHasName(material), this.has(material)).save(this.output);
    }

    protected ShapedRecipeBuilder cutBuilder(RecipeCategory category, ItemLike cutResult, Ingredient material) {
        return this.shaped(category, cutResult, 4).define('#', material).pattern("##").pattern("##");
    }

    protected void chiseled(RecipeCategory category, ItemLike chiseledResult, ItemLike material) {
        this.chiseledBuilder(category, chiseledResult, Ingredient.of(material)).unlockedBy(getHasName(material), this.has(material)).save(this.output);
    }

    protected void mosaicBuilder(RecipeCategory category, ItemLike result, ItemLike material) {
        this.shaped(category, result)
            .define('#', material)
            .pattern("#")
            .pattern("#")
            .unlockedBy(getHasName(material), this.has(material))
            .save(this.output);
    }

    protected ShapedRecipeBuilder chiseledBuilder(RecipeCategory category, ItemLike chiseledResult, Ingredient material) {
        return this.shaped(category, chiseledResult).define('#', material).pattern("#").pattern("#");
    }

    protected void stonecutterResultFromBase(RecipeCategory category, ItemLike result, ItemLike material) {
        this.stonecutterResultFromBase(category, result, material, 1);
    }

    protected void stonecutterResultFromBase(RecipeCategory category, ItemLike result, ItemLike material, int resultCount) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(material), category, result, resultCount)
            .unlockedBy(getHasName(material), this.has(material))
            .save(this.output, getConversionRecipeName(result, material) + "_stonecutting");
    }

    protected void smeltingResultFromBase(ItemLike result, ItemLike ingredient) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ingredient), RecipeCategory.BUILDING_BLOCKS, result, 0.1F, 200)
            .unlockedBy(getHasName(ingredient), this.has(ingredient))
            .save(this.output);
    }

    protected void nineBlockStorageRecipes(RecipeCategory unpackedCategory, ItemLike unpacked, RecipeCategory packedCategory, ItemLike packed) {
        this.nineBlockStorageRecipes(unpackedCategory, unpacked, packedCategory, packed, getSimpleRecipeName(packed), null, getSimpleRecipeName(unpacked), null);
    }

    protected void nineBlockStorageRecipesWithCustomPacking(
        RecipeCategory unpackedCategory, ItemLike unpacked, RecipeCategory packedCategory, ItemLike packed, String packedName, String packedGroup
    ) {
        this.nineBlockStorageRecipes(unpackedCategory, unpacked, packedCategory, packed, packedName, packedGroup, getSimpleRecipeName(unpacked), null);
    }

    protected void nineBlockStorageRecipesRecipesWithCustomUnpacking(
        RecipeCategory unpackedCategory, ItemLike unpacked, RecipeCategory packedCategory, ItemLike packed, String unpackedName, String unpackedGroup
    ) {
        this.nineBlockStorageRecipes(unpackedCategory, unpacked, packedCategory, packed, getSimpleRecipeName(packed), null, unpackedName, unpackedGroup);
    }

    protected void nineBlockStorageRecipes(
        RecipeCategory unpackedCategory,
        ItemLike unpacked,
        RecipeCategory packedCategory,
        ItemLike packed,
        String packedName,
        @Nullable String packedGroup,
        String unpackedName,
        @Nullable String unpackedGroup
    ) {
        this.shapeless(unpackedCategory, unpacked, 9)
            .requires(packed)
            .group(unpackedGroup)
            .unlockedBy(getHasName(packed), this.has(packed))
            .save(this.output, ResourceKey.create(Registries.RECIPE, ResourceLocation.parse(unpackedName)));
        this.shaped(packedCategory, packed)
            .define('#', unpacked)
            .pattern("###")
            .pattern("###")
            .pattern("###")
            .group(packedGroup)
            .unlockedBy(getHasName(unpacked), this.has(unpacked))
            .save(this.output, ResourceKey.create(Registries.RECIPE, ResourceLocation.parse(packedName)));
    }

    protected void copySmithingTemplate(ItemLike template, ItemLike baseItem) {
        this.shaped(RecipeCategory.MISC, template, 2)
            .define('#', Items.DIAMOND)
            .define('C', baseItem)
            .define('S', template)
            .pattern("#S#")
            .pattern("#C#")
            .pattern("###")
            .unlockedBy(getHasName(template), this.has(template))
            .save(this.output);
    }

    protected void copySmithingTemplate(ItemLike template, Ingredient baseItem) {
        this.shaped(RecipeCategory.MISC, template, 2)
            .define('#', Items.DIAMOND)
            .define('C', baseItem)
            .define('S', template)
            .pattern("#S#")
            .pattern("#C#")
            .pattern("###")
            .unlockedBy(getHasName(template), this.has(template))
            .save(this.output);
    }

    protected <T extends AbstractCookingRecipe> void cookRecipes(
        String cookingMethod, RecipeSerializer<T> cookingSerializer, AbstractCookingRecipe.Factory<T> recipeFactory, int cookingTime
    ) {
        this.simpleCookingRecipe(cookingMethod, cookingSerializer, recipeFactory, cookingTime, Items.BEEF, Items.COOKED_BEEF, 0.35F);
        this.simpleCookingRecipe(cookingMethod, cookingSerializer, recipeFactory, cookingTime, Items.CHICKEN, Items.COOKED_CHICKEN, 0.35F);
        this.simpleCookingRecipe(cookingMethod, cookingSerializer, recipeFactory, cookingTime, Items.COD, Items.COOKED_COD, 0.35F);
        this.simpleCookingRecipe(cookingMethod, cookingSerializer, recipeFactory, cookingTime, Items.KELP, Items.DRIED_KELP, 0.1F);
        this.simpleCookingRecipe(cookingMethod, cookingSerializer, recipeFactory, cookingTime, Items.SALMON, Items.COOKED_SALMON, 0.35F);
        this.simpleCookingRecipe(cookingMethod, cookingSerializer, recipeFactory, cookingTime, Items.MUTTON, Items.COOKED_MUTTON, 0.35F);
        this.simpleCookingRecipe(cookingMethod, cookingSerializer, recipeFactory, cookingTime, Items.PORKCHOP, Items.COOKED_PORKCHOP, 0.35F);
        this.simpleCookingRecipe(cookingMethod, cookingSerializer, recipeFactory, cookingTime, Items.POTATO, Items.BAKED_POTATO, 0.35F);
        this.simpleCookingRecipe(cookingMethod, cookingSerializer, recipeFactory, cookingTime, Items.RABBIT, Items.COOKED_RABBIT, 0.35F);
    }

    protected <T extends AbstractCookingRecipe> void simpleCookingRecipe(
        String cookingMethod,
        RecipeSerializer<T> cookingSerializer,
        AbstractCookingRecipe.Factory<T> recipeFactory,
        int cookingTime,
        ItemLike material,
        ItemLike result,
        float experience
    ) {
        SimpleCookingRecipeBuilder.generic(Ingredient.of(material), RecipeCategory.FOOD, result, experience, cookingTime, cookingSerializer, recipeFactory)
            .unlockedBy(getHasName(material), this.has(material))
            .save(this.output, getItemName(result) + "_from_" + cookingMethod);
    }

    protected void waxRecipes(FeatureFlagSet requiredFeatures) {
        net.neoforged.neoforge.common.DataMapHooks.INVERSE_WAXABLES_DATAMAP
            .forEach(
                (p_392602_, p_392603_) -> {
                    if (p_392603_.requiredFeatures().isSubsetOf(requiredFeatures)) {
                        this.shapeless(RecipeCategory.BUILDING_BLOCKS, p_392603_)
                            .requires(p_392602_)
                            .requires(Items.HONEYCOMB)
                            .group(getItemName(p_392603_))
                            .unlockedBy(getHasName(p_392602_), this.has(p_392602_))
                            .save(this.output, getConversionRecipeName(p_392603_, Items.HONEYCOMB));
                    }
                }
            );
    }

    protected void grate(Block grateBlock, Block material) {
        this.shaped(RecipeCategory.BUILDING_BLOCKS, grateBlock, 4)
            .define('M', material)
            .pattern(" M ")
            .pattern("M M")
            .pattern(" M ")
            .unlockedBy(getHasName(material), this.has(material))
            .save(this.output);
    }

    protected void copperBulb(Block bulbBlock, Block material) {
        this.shaped(RecipeCategory.REDSTONE, bulbBlock, 4)
            .define('C', material)
            .define('R', Items.REDSTONE)
            .define('B', Items.BLAZE_ROD)
            .pattern(" C ")
            .pattern("CBC")
            .pattern(" R ")
            .unlockedBy(getHasName(material), this.has(material))
            .save(this.output);
    }

    protected void suspiciousStew(Item flowerItem, SuspiciousEffectHolder effect) {
        ItemStack itemstack = new ItemStack(
            Items.SUSPICIOUS_STEW.builtInRegistryHolder(),
            1,
            DataComponentPatch.builder().set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effect.getSuspiciousEffects()).build()
        );
        this.shapeless(RecipeCategory.FOOD, itemstack)
            .requires(Items.BOWL)
            .requires(Items.BROWN_MUSHROOM)
            .requires(Items.RED_MUSHROOM)
            .requires(flowerItem)
            .group("suspicious_stew")
            .unlockedBy(getHasName(flowerItem), this.has(flowerItem))
            .save(this.output, getItemName(itemstack.getItem()) + "_from_" + getItemName(flowerItem));
    }

    protected void generateRecipes(BlockFamily blockFamily, FeatureFlagSet requiredFeatures) {
        blockFamily.getVariants()
            .forEach(
                (p_359428_, p_359429_) -> {
                    if (p_359429_.requiredFeatures().isSubsetOf(requiredFeatures)) {
                        RecipeProvider.FamilyRecipeProvider recipeprovider$familyrecipeprovider = SHAPE_BUILDERS.get(p_359428_);
                        ItemLike itemlike = this.getBaseBlock(blockFamily, p_359428_);
                        if (recipeprovider$familyrecipeprovider != null) {
                            RecipeBuilder recipebuilder = recipeprovider$familyrecipeprovider.create(this, p_359429_, itemlike);
                            blockFamily.getRecipeGroupPrefix()
                                .ifPresent(
                                    p_293701_ -> recipebuilder.group(p_293701_ + (p_359428_ == BlockFamily.Variant.CUT ? "" : "_" + p_359428_.getRecipeGroup()))
                                );
                            recipebuilder.unlockedBy(blockFamily.getRecipeUnlockedBy().orElseGet(() -> getHasName(itemlike)), this.has(itemlike));
                            recipebuilder.save(this.output);
                        }

                        if (p_359428_ == BlockFamily.Variant.CRACKED) {
                            this.smeltingResultFromBase(p_359429_, itemlike);
                        }
                    }
                }
            );
    }

    protected Block getBaseBlock(BlockFamily family, BlockFamily.Variant variant) {
        if (variant == BlockFamily.Variant.CHISELED) {
            if (!family.getVariants().containsKey(BlockFamily.Variant.SLAB)) {
                throw new IllegalStateException("Slab is not defined for the family.");
            } else {
                return family.get(BlockFamily.Variant.SLAB);
            }
        } else {
            return family.getBaseBlock();
        }
    }

    protected static Criterion<EnterBlockTrigger.TriggerInstance> insideOf(Block block) {
        return CriteriaTriggers.ENTER_BLOCK
            .createCriterion(new EnterBlockTrigger.TriggerInstance(Optional.empty(), Optional.of(block.builtInRegistryHolder()), Optional.empty()));
    }

    protected Criterion<InventoryChangeTrigger.TriggerInstance> has(MinMaxBounds.Ints count, ItemLike item) {
        return inventoryTrigger(ItemPredicate.Builder.item().of(this.items, item).withCount(count));
    }

    protected Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike itemLike) {
        return inventoryTrigger(ItemPredicate.Builder.item().of(this.items, itemLike));
    }

    protected Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> tag) {
        return inventoryTrigger(ItemPredicate.Builder.item().of(this.items, tag));
    }

    protected static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryTrigger(ItemPredicate.Builder... items) {
        return inventoryTrigger(Arrays.stream(items).map(ItemPredicate.Builder::build).toArray(ItemPredicate[]::new));
    }

    protected static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryTrigger(ItemPredicate... predicates) {
        return CriteriaTriggers.INVENTORY_CHANGED
            .createCriterion(new InventoryChangeTrigger.TriggerInstance(Optional.empty(), InventoryChangeTrigger.TriggerInstance.Slots.ANY, List.of(predicates)));
    }

    protected static String getHasName(ItemLike itemLike) {
        return "has_" + getItemName(itemLike);
    }

    protected static String getItemName(ItemLike itemLike) {
        return BuiltInRegistries.ITEM.getKey(itemLike.asItem()).getPath();
    }

    protected static String getSimpleRecipeName(ItemLike itemLike) {
        return getItemName(itemLike);
    }

    protected static String getConversionRecipeName(ItemLike result, ItemLike ingredient) {
        return getItemName(result) + "_from_" + getItemName(ingredient);
    }

    protected static String getSmeltingRecipeName(ItemLike itemLike) {
        return getItemName(itemLike) + "_from_smelting";
    }

    protected static String getBlastingRecipeName(ItemLike itemLike) {
        return getItemName(itemLike) + "_from_blasting";
    }

    protected Ingredient tag(TagKey<Item> tag) {
        return Ingredient.of(this.items.getOrThrow(tag));
    }

    protected ShapedRecipeBuilder shaped(RecipeCategory category, ItemStack stack) {
        return ShapedRecipeBuilder.shaped(this.items, category, stack);
    }

    protected ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result) {
        return ShapedRecipeBuilder.shaped(this.items, category, result);
    }

    protected ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result, int count) {
        return ShapedRecipeBuilder.shaped(this.items, category, result, count);
    }

    protected ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemStack result) {
        return ShapelessRecipeBuilder.shapeless(this.items, category, result);
    }

    protected ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result) {
        return ShapelessRecipeBuilder.shapeless(this.items, category, result);
    }

    protected ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result, int count) {
        return ShapelessRecipeBuilder.shapeless(this.items, category, result, count);
    }

    @FunctionalInterface
    interface FamilyRecipeProvider {
        RecipeBuilder create(RecipeProvider recipeProvider, ItemLike ingredient, ItemLike result);
    }

    public abstract static class Runner implements DataProvider {
        private final PackOutput packOutput;
        private final CompletableFuture<HolderLookup.Provider> registries;

        protected Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            this.packOutput = packOutput;
            this.registries = registries;
        }

        @Override
        public final CompletableFuture<?> run(CachedOutput output) {
            return this.registries
                .thenCompose(
                    p_364211_ -> {
                        final PackOutput.PathProvider packoutput$pathprovider = this.packOutput.createRegistryElementsPathProvider(Registries.RECIPE);
                        final PackOutput.PathProvider packoutput$pathprovider1 = this.packOutput.createRegistryElementsPathProvider(Registries.ADVANCEMENT);
                        final Set<ResourceKey<Recipe<?>>> set = Sets.newHashSet();
                        final List<CompletableFuture<?>> list = new ArrayList<>();
                        RecipeOutput recipeoutput = new RecipeOutput() {
                            @Override
                            public void accept(ResourceKey<Recipe<?>> p_380360_, Recipe<?> p_362882_, @Nullable AdvancementHolder p_364507_, net.neoforged.neoforge.common.conditions.ICondition... conditions) {
                                if (!set.add(p_380360_)) {
                                    throw new IllegalStateException("Duplicate recipe " + p_380360_.location());
                                } else {
                                    this.saveRecipe(p_380360_, p_362882_, conditions);
                                    if (p_364507_ != null) {
                                        this.saveAdvancement(p_364507_, conditions);
                                    }
                                }
                            }

                            @Override
                            public Advancement.Builder advancement() {
                                return Advancement.Builder.recipeAdvancement().parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
                            }

                            @Override
                            public void includeRootAdvancement() {
                                AdvancementHolder advancementholder = Advancement.Builder.recipeAdvancement()
                                    .addCriterion("impossible", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                                    .build(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
                                this.saveAdvancement(advancementholder);
                            }

                            private void saveRecipe(ResourceKey<Recipe<?>> p_380099_, Recipe<?> p_364792_) {
                                saveRecipe(p_380099_, p_364792_, new net.neoforged.neoforge.common.conditions.ICondition[0]);
                            }
                            private void saveRecipe(ResourceKey<Recipe<?>> p_380099_, Recipe<?> p_364792_, net.neoforged.neoforge.common.conditions.ICondition... conditions) {
                                list.add(
                                    DataProvider.saveStable(output, p_364211_, Recipe.CONDITIONAL_CODEC, Optional.of(new net.neoforged.neoforge.common.conditions.WithConditions<>(p_364792_, conditions)), packoutput$pathprovider.json(p_380099_.location()))
                                );
                            }

                            private void saveAdvancement(AdvancementHolder p_363148_) {
                                saveAdvancement(p_363148_, new net.neoforged.neoforge.common.conditions.ICondition[0]);
                            }
                            private void saveAdvancement(AdvancementHolder p_363148_, net.neoforged.neoforge.common.conditions.ICondition... conditions) {
                                list.add(
                                    DataProvider.saveStable(
                                        output, p_364211_, Advancement.CONDITIONAL_CODEC, Optional.of(new net.neoforged.neoforge.common.conditions.WithConditions<>(p_363148_.value(), conditions)), packoutput$pathprovider1.json(p_363148_.id())
                                    )
                                );
                            }
                        };
                        this.createRecipeProvider(p_364211_, recipeoutput).buildRecipes();
                        return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
                    }
                );
        }

        protected abstract RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output);
    }
}
