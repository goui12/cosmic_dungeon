package net.minecraft.world.item;

import com.mojang.datafixers.util.Pair;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.InstrumentTags;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraft.world.level.block.TestBlock;
import net.minecraft.world.level.block.state.properties.TestBlockMode;

public class CreativeModeTabs {
    private static final ResourceLocation INVENTORY_BACKGROUND = CreativeModeTab.createTextureLocation("inventory");
    private static final ResourceLocation SEARCH_BACKGROUND = CreativeModeTab.createTextureLocation("item_search");
    public static final ResourceKey<CreativeModeTab> BUILDING_BLOCKS = createKey("building_blocks");
    public static final ResourceKey<CreativeModeTab> COLORED_BLOCKS = createKey("colored_blocks");
    public static final ResourceKey<CreativeModeTab> NATURAL_BLOCKS = createKey("natural_blocks");
    public static final ResourceKey<CreativeModeTab> FUNCTIONAL_BLOCKS = createKey("functional_blocks");
    public static final ResourceKey<CreativeModeTab> REDSTONE_BLOCKS = createKey("redstone_blocks");
    public static final ResourceKey<CreativeModeTab> HOTBAR = createKey("hotbar");
    public static final ResourceKey<CreativeModeTab> SEARCH = createKey("search");
    public static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES = createKey("tools_and_utilities");
    public static final ResourceKey<CreativeModeTab> COMBAT = createKey("combat");
    public static final ResourceKey<CreativeModeTab> FOOD_AND_DRINKS = createKey("food_and_drinks");
    public static final ResourceKey<CreativeModeTab> INGREDIENTS = createKey("ingredients");
    public static final ResourceKey<CreativeModeTab> SPAWN_EGGS = createKey("spawn_eggs");
    public static final ResourceKey<CreativeModeTab> OP_BLOCKS = createKey("op_blocks");
    public static final ResourceKey<CreativeModeTab> INVENTORY = createKey("inventory");
    private static final Comparator<Holder<PaintingVariant>> PAINTING_COMPARATOR = Comparator.comparing(
        Holder::value, Comparator.comparingInt(PaintingVariant::area).thenComparing(PaintingVariant::width)
    );
    @Nullable
    private static CreativeModeTab.ItemDisplayParameters CACHED_PARAMETERS;

    private static ResourceKey<CreativeModeTab> createKey(String name) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.withDefaultNamespace(name));
    }

    public static CreativeModeTab bootstrap(Registry<CreativeModeTab> registry) {
        Registry.register(
            registry,
            BUILDING_BLOCKS,
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.buildingBlocks"))
                .icon(() -> new ItemStack(Blocks.BRICKS))
                .displayItems((p_450886_, p_450887_) -> {
                    p_450887_.accept(Items.OAK_LOG);
                    p_450887_.accept(Items.OAK_WOOD);
                    p_450887_.accept(Items.STRIPPED_OAK_LOG);
                    p_450887_.accept(Items.STRIPPED_OAK_WOOD);
                    p_450887_.accept(Items.OAK_PLANKS);
                    p_450887_.accept(Items.OAK_STAIRS);
                    p_450887_.accept(Items.OAK_SLAB);
                    p_450887_.accept(Items.OAK_FENCE);
                    p_450887_.accept(Items.OAK_FENCE_GATE);
                    p_450887_.accept(Items.OAK_DOOR);
                    p_450887_.accept(Items.OAK_TRAPDOOR);
                    p_450887_.accept(Items.OAK_PRESSURE_PLATE);
                    p_450887_.accept(Items.OAK_BUTTON);
                    p_450887_.accept(Items.SPRUCE_LOG);
                    p_450887_.accept(Items.SPRUCE_WOOD);
                    p_450887_.accept(Items.STRIPPED_SPRUCE_LOG);
                    p_450887_.accept(Items.STRIPPED_SPRUCE_WOOD);
                    p_450887_.accept(Items.SPRUCE_PLANKS);
                    p_450887_.accept(Items.SPRUCE_STAIRS);
                    p_450887_.accept(Items.SPRUCE_SLAB);
                    p_450887_.accept(Items.SPRUCE_FENCE);
                    p_450887_.accept(Items.SPRUCE_FENCE_GATE);
                    p_450887_.accept(Items.SPRUCE_DOOR);
                    p_450887_.accept(Items.SPRUCE_TRAPDOOR);
                    p_450887_.accept(Items.SPRUCE_PRESSURE_PLATE);
                    p_450887_.accept(Items.SPRUCE_BUTTON);
                    p_450887_.accept(Items.BIRCH_LOG);
                    p_450887_.accept(Items.BIRCH_WOOD);
                    p_450887_.accept(Items.STRIPPED_BIRCH_LOG);
                    p_450887_.accept(Items.STRIPPED_BIRCH_WOOD);
                    p_450887_.accept(Items.BIRCH_PLANKS);
                    p_450887_.accept(Items.BIRCH_STAIRS);
                    p_450887_.accept(Items.BIRCH_SLAB);
                    p_450887_.accept(Items.BIRCH_FENCE);
                    p_450887_.accept(Items.BIRCH_FENCE_GATE);
                    p_450887_.accept(Items.BIRCH_DOOR);
                    p_450887_.accept(Items.BIRCH_TRAPDOOR);
                    p_450887_.accept(Items.BIRCH_PRESSURE_PLATE);
                    p_450887_.accept(Items.BIRCH_BUTTON);
                    p_450887_.accept(Items.JUNGLE_LOG);
                    p_450887_.accept(Items.JUNGLE_WOOD);
                    p_450887_.accept(Items.STRIPPED_JUNGLE_LOG);
                    p_450887_.accept(Items.STRIPPED_JUNGLE_WOOD);
                    p_450887_.accept(Items.JUNGLE_PLANKS);
                    p_450887_.accept(Items.JUNGLE_STAIRS);
                    p_450887_.accept(Items.JUNGLE_SLAB);
                    p_450887_.accept(Items.JUNGLE_FENCE);
                    p_450887_.accept(Items.JUNGLE_FENCE_GATE);
                    p_450887_.accept(Items.JUNGLE_DOOR);
                    p_450887_.accept(Items.JUNGLE_TRAPDOOR);
                    p_450887_.accept(Items.JUNGLE_PRESSURE_PLATE);
                    p_450887_.accept(Items.JUNGLE_BUTTON);
                    p_450887_.accept(Items.ACACIA_LOG);
                    p_450887_.accept(Items.ACACIA_WOOD);
                    p_450887_.accept(Items.STRIPPED_ACACIA_LOG);
                    p_450887_.accept(Items.STRIPPED_ACACIA_WOOD);
                    p_450887_.accept(Items.ACACIA_PLANKS);
                    p_450887_.accept(Items.ACACIA_STAIRS);
                    p_450887_.accept(Items.ACACIA_SLAB);
                    p_450887_.accept(Items.ACACIA_FENCE);
                    p_450887_.accept(Items.ACACIA_FENCE_GATE);
                    p_450887_.accept(Items.ACACIA_DOOR);
                    p_450887_.accept(Items.ACACIA_TRAPDOOR);
                    p_450887_.accept(Items.ACACIA_PRESSURE_PLATE);
                    p_450887_.accept(Items.ACACIA_BUTTON);
                    p_450887_.accept(Items.DARK_OAK_LOG);
                    p_450887_.accept(Items.DARK_OAK_WOOD);
                    p_450887_.accept(Items.STRIPPED_DARK_OAK_LOG);
                    p_450887_.accept(Items.STRIPPED_DARK_OAK_WOOD);
                    p_450887_.accept(Items.DARK_OAK_PLANKS);
                    p_450887_.accept(Items.DARK_OAK_STAIRS);
                    p_450887_.accept(Items.DARK_OAK_SLAB);
                    p_450887_.accept(Items.DARK_OAK_FENCE);
                    p_450887_.accept(Items.DARK_OAK_FENCE_GATE);
                    p_450887_.accept(Items.DARK_OAK_DOOR);
                    p_450887_.accept(Items.DARK_OAK_TRAPDOOR);
                    p_450887_.accept(Items.DARK_OAK_PRESSURE_PLATE);
                    p_450887_.accept(Items.DARK_OAK_BUTTON);
                    p_450887_.accept(Items.MANGROVE_LOG);
                    p_450887_.accept(Items.MANGROVE_WOOD);
                    p_450887_.accept(Items.STRIPPED_MANGROVE_LOG);
                    p_450887_.accept(Items.STRIPPED_MANGROVE_WOOD);
                    p_450887_.accept(Items.MANGROVE_PLANKS);
                    p_450887_.accept(Items.MANGROVE_STAIRS);
                    p_450887_.accept(Items.MANGROVE_SLAB);
                    p_450887_.accept(Items.MANGROVE_FENCE);
                    p_450887_.accept(Items.MANGROVE_FENCE_GATE);
                    p_450887_.accept(Items.MANGROVE_DOOR);
                    p_450887_.accept(Items.MANGROVE_TRAPDOOR);
                    p_450887_.accept(Items.MANGROVE_PRESSURE_PLATE);
                    p_450887_.accept(Items.MANGROVE_BUTTON);
                    p_450887_.accept(Items.CHERRY_LOG);
                    p_450887_.accept(Items.CHERRY_WOOD);
                    p_450887_.accept(Items.STRIPPED_CHERRY_LOG);
                    p_450887_.accept(Items.STRIPPED_CHERRY_WOOD);
                    p_450887_.accept(Items.CHERRY_PLANKS);
                    p_450887_.accept(Items.CHERRY_STAIRS);
                    p_450887_.accept(Items.CHERRY_SLAB);
                    p_450887_.accept(Items.CHERRY_FENCE);
                    p_450887_.accept(Items.CHERRY_FENCE_GATE);
                    p_450887_.accept(Items.CHERRY_DOOR);
                    p_450887_.accept(Items.CHERRY_TRAPDOOR);
                    p_450887_.accept(Items.CHERRY_PRESSURE_PLATE);
                    p_450887_.accept(Items.CHERRY_BUTTON);
                    p_450887_.accept(Items.PALE_OAK_LOG);
                    p_450887_.accept(Items.PALE_OAK_WOOD);
                    p_450887_.accept(Items.STRIPPED_PALE_OAK_LOG);
                    p_450887_.accept(Items.STRIPPED_PALE_OAK_WOOD);
                    p_450887_.accept(Items.PALE_OAK_PLANKS);
                    p_450887_.accept(Items.PALE_OAK_STAIRS);
                    p_450887_.accept(Items.PALE_OAK_SLAB);
                    p_450887_.accept(Items.PALE_OAK_FENCE);
                    p_450887_.accept(Items.PALE_OAK_FENCE_GATE);
                    p_450887_.accept(Items.PALE_OAK_DOOR);
                    p_450887_.accept(Items.PALE_OAK_TRAPDOOR);
                    p_450887_.accept(Items.PALE_OAK_PRESSURE_PLATE);
                    p_450887_.accept(Items.PALE_OAK_BUTTON);
                    p_450887_.accept(Items.BAMBOO_BLOCK);
                    p_450887_.accept(Items.STRIPPED_BAMBOO_BLOCK);
                    p_450887_.accept(Items.BAMBOO_PLANKS);
                    p_450887_.accept(Items.BAMBOO_MOSAIC);
                    p_450887_.accept(Items.BAMBOO_STAIRS);
                    p_450887_.accept(Items.BAMBOO_MOSAIC_STAIRS);
                    p_450887_.accept(Items.BAMBOO_SLAB);
                    p_450887_.accept(Items.BAMBOO_MOSAIC_SLAB);
                    p_450887_.accept(Items.BAMBOO_FENCE);
                    p_450887_.accept(Items.BAMBOO_FENCE_GATE);
                    p_450887_.accept(Items.BAMBOO_DOOR);
                    p_450887_.accept(Items.BAMBOO_TRAPDOOR);
                    p_450887_.accept(Items.BAMBOO_PRESSURE_PLATE);
                    p_450887_.accept(Items.BAMBOO_BUTTON);
                    p_450887_.accept(Items.CRIMSON_STEM);
                    p_450887_.accept(Items.CRIMSON_HYPHAE);
                    p_450887_.accept(Items.STRIPPED_CRIMSON_STEM);
                    p_450887_.accept(Items.STRIPPED_CRIMSON_HYPHAE);
                    p_450887_.accept(Items.CRIMSON_PLANKS);
                    p_450887_.accept(Items.CRIMSON_STAIRS);
                    p_450887_.accept(Items.CRIMSON_SLAB);
                    p_450887_.accept(Items.CRIMSON_FENCE);
                    p_450887_.accept(Items.CRIMSON_FENCE_GATE);
                    p_450887_.accept(Items.CRIMSON_DOOR);
                    p_450887_.accept(Items.CRIMSON_TRAPDOOR);
                    p_450887_.accept(Items.CRIMSON_PRESSURE_PLATE);
                    p_450887_.accept(Items.CRIMSON_BUTTON);
                    p_450887_.accept(Items.WARPED_STEM);
                    p_450887_.accept(Items.WARPED_HYPHAE);
                    p_450887_.accept(Items.STRIPPED_WARPED_STEM);
                    p_450887_.accept(Items.STRIPPED_WARPED_HYPHAE);
                    p_450887_.accept(Items.WARPED_PLANKS);
                    p_450887_.accept(Items.WARPED_STAIRS);
                    p_450887_.accept(Items.WARPED_SLAB);
                    p_450887_.accept(Items.WARPED_FENCE);
                    p_450887_.accept(Items.WARPED_FENCE_GATE);
                    p_450887_.accept(Items.WARPED_DOOR);
                    p_450887_.accept(Items.WARPED_TRAPDOOR);
                    p_450887_.accept(Items.WARPED_PRESSURE_PLATE);
                    p_450887_.accept(Items.WARPED_BUTTON);
                    p_450887_.accept(Items.STONE);
                    p_450887_.accept(Items.STONE_STAIRS);
                    p_450887_.accept(Items.STONE_SLAB);
                    p_450887_.accept(Items.STONE_PRESSURE_PLATE);
                    p_450887_.accept(Items.STONE_BUTTON);
                    p_450887_.accept(Items.COBBLESTONE);
                    p_450887_.accept(Items.COBBLESTONE_STAIRS);
                    p_450887_.accept(Items.COBBLESTONE_SLAB);
                    p_450887_.accept(Items.COBBLESTONE_WALL);
                    p_450887_.accept(Items.MOSSY_COBBLESTONE);
                    p_450887_.accept(Items.MOSSY_COBBLESTONE_STAIRS);
                    p_450887_.accept(Items.MOSSY_COBBLESTONE_SLAB);
                    p_450887_.accept(Items.MOSSY_COBBLESTONE_WALL);
                    p_450887_.accept(Items.SMOOTH_STONE);
                    p_450887_.accept(Items.SMOOTH_STONE_SLAB);
                    p_450887_.accept(Items.STONE_BRICKS);
                    p_450887_.accept(Items.CRACKED_STONE_BRICKS);
                    p_450887_.accept(Items.STONE_BRICK_STAIRS);
                    p_450887_.accept(Items.STONE_BRICK_SLAB);
                    p_450887_.accept(Items.STONE_BRICK_WALL);
                    p_450887_.accept(Items.CHISELED_STONE_BRICKS);
                    p_450887_.accept(Items.MOSSY_STONE_BRICKS);
                    p_450887_.accept(Items.MOSSY_STONE_BRICK_STAIRS);
                    p_450887_.accept(Items.MOSSY_STONE_BRICK_SLAB);
                    p_450887_.accept(Items.MOSSY_STONE_BRICK_WALL);
                    p_450887_.accept(Items.GRANITE);
                    p_450887_.accept(Items.GRANITE_STAIRS);
                    p_450887_.accept(Items.GRANITE_SLAB);
                    p_450887_.accept(Items.GRANITE_WALL);
                    p_450887_.accept(Items.POLISHED_GRANITE);
                    p_450887_.accept(Items.POLISHED_GRANITE_STAIRS);
                    p_450887_.accept(Items.POLISHED_GRANITE_SLAB);
                    p_450887_.accept(Items.DIORITE);
                    p_450887_.accept(Items.DIORITE_STAIRS);
                    p_450887_.accept(Items.DIORITE_SLAB);
                    p_450887_.accept(Items.DIORITE_WALL);
                    p_450887_.accept(Items.POLISHED_DIORITE);
                    p_450887_.accept(Items.POLISHED_DIORITE_STAIRS);
                    p_450887_.accept(Items.POLISHED_DIORITE_SLAB);
                    p_450887_.accept(Items.ANDESITE);
                    p_450887_.accept(Items.ANDESITE_STAIRS);
                    p_450887_.accept(Items.ANDESITE_SLAB);
                    p_450887_.accept(Items.ANDESITE_WALL);
                    p_450887_.accept(Items.POLISHED_ANDESITE);
                    p_450887_.accept(Items.POLISHED_ANDESITE_STAIRS);
                    p_450887_.accept(Items.POLISHED_ANDESITE_SLAB);
                    p_450887_.accept(Items.DEEPSLATE);
                    p_450887_.accept(Items.COBBLED_DEEPSLATE);
                    p_450887_.accept(Items.COBBLED_DEEPSLATE_STAIRS);
                    p_450887_.accept(Items.COBBLED_DEEPSLATE_SLAB);
                    p_450887_.accept(Items.COBBLED_DEEPSLATE_WALL);
                    p_450887_.accept(Items.CHISELED_DEEPSLATE);
                    p_450887_.accept(Items.POLISHED_DEEPSLATE);
                    p_450887_.accept(Items.POLISHED_DEEPSLATE_STAIRS);
                    p_450887_.accept(Items.POLISHED_DEEPSLATE_SLAB);
                    p_450887_.accept(Items.POLISHED_DEEPSLATE_WALL);
                    p_450887_.accept(Items.DEEPSLATE_BRICKS);
                    p_450887_.accept(Items.CRACKED_DEEPSLATE_BRICKS);
                    p_450887_.accept(Items.DEEPSLATE_BRICK_STAIRS);
                    p_450887_.accept(Items.DEEPSLATE_BRICK_SLAB);
                    p_450887_.accept(Items.DEEPSLATE_BRICK_WALL);
                    p_450887_.accept(Items.DEEPSLATE_TILES);
                    p_450887_.accept(Items.CRACKED_DEEPSLATE_TILES);
                    p_450887_.accept(Items.DEEPSLATE_TILE_STAIRS);
                    p_450887_.accept(Items.DEEPSLATE_TILE_SLAB);
                    p_450887_.accept(Items.DEEPSLATE_TILE_WALL);
                    p_450887_.accept(Items.REINFORCED_DEEPSLATE);
                    p_450887_.accept(Items.TUFF);
                    p_450887_.accept(Items.TUFF_STAIRS);
                    p_450887_.accept(Items.TUFF_SLAB);
                    p_450887_.accept(Items.TUFF_WALL);
                    p_450887_.accept(Items.CHISELED_TUFF);
                    p_450887_.accept(Items.POLISHED_TUFF);
                    p_450887_.accept(Items.POLISHED_TUFF_STAIRS);
                    p_450887_.accept(Items.POLISHED_TUFF_SLAB);
                    p_450887_.accept(Items.POLISHED_TUFF_WALL);
                    p_450887_.accept(Items.TUFF_BRICKS);
                    p_450887_.accept(Items.TUFF_BRICK_STAIRS);
                    p_450887_.accept(Items.TUFF_BRICK_SLAB);
                    p_450887_.accept(Items.TUFF_BRICK_WALL);
                    p_450887_.accept(Items.CHISELED_TUFF_BRICKS);
                    p_450887_.accept(Items.BRICKS);
                    p_450887_.accept(Items.BRICK_STAIRS);
                    p_450887_.accept(Items.BRICK_SLAB);
                    p_450887_.accept(Items.BRICK_WALL);
                    p_450887_.accept(Items.PACKED_MUD);
                    p_450887_.accept(Items.MUD_BRICKS);
                    p_450887_.accept(Items.MUD_BRICK_STAIRS);
                    p_450887_.accept(Items.MUD_BRICK_SLAB);
                    p_450887_.accept(Items.MUD_BRICK_WALL);
                    p_450887_.accept(Items.RESIN_BRICKS);
                    p_450887_.accept(Items.RESIN_BRICK_STAIRS);
                    p_450887_.accept(Items.RESIN_BRICK_SLAB);
                    p_450887_.accept(Items.RESIN_BRICK_WALL);
                    p_450887_.accept(Items.CHISELED_RESIN_BRICKS);
                    p_450887_.accept(Items.SANDSTONE);
                    p_450887_.accept(Items.SANDSTONE_STAIRS);
                    p_450887_.accept(Items.SANDSTONE_SLAB);
                    p_450887_.accept(Items.SANDSTONE_WALL);
                    p_450887_.accept(Items.CHISELED_SANDSTONE);
                    p_450887_.accept(Items.SMOOTH_SANDSTONE);
                    p_450887_.accept(Items.SMOOTH_SANDSTONE_STAIRS);
                    p_450887_.accept(Items.SMOOTH_SANDSTONE_SLAB);
                    p_450887_.accept(Items.CUT_SANDSTONE);
                    p_450887_.accept(Items.CUT_STANDSTONE_SLAB);
                    p_450887_.accept(Items.RED_SANDSTONE);
                    p_450887_.accept(Items.RED_SANDSTONE_STAIRS);
                    p_450887_.accept(Items.RED_SANDSTONE_SLAB);
                    p_450887_.accept(Items.RED_SANDSTONE_WALL);
                    p_450887_.accept(Items.CHISELED_RED_SANDSTONE);
                    p_450887_.accept(Items.SMOOTH_RED_SANDSTONE);
                    p_450887_.accept(Items.SMOOTH_RED_SANDSTONE_STAIRS);
                    p_450887_.accept(Items.SMOOTH_RED_SANDSTONE_SLAB);
                    p_450887_.accept(Items.CUT_RED_SANDSTONE);
                    p_450887_.accept(Items.CUT_RED_SANDSTONE_SLAB);
                    p_450887_.accept(Items.SEA_LANTERN);
                    p_450887_.accept(Items.PRISMARINE);
                    p_450887_.accept(Items.PRISMARINE_STAIRS);
                    p_450887_.accept(Items.PRISMARINE_SLAB);
                    p_450887_.accept(Items.PRISMARINE_WALL);
                    p_450887_.accept(Items.PRISMARINE_BRICKS);
                    p_450887_.accept(Items.PRISMARINE_BRICK_STAIRS);
                    p_450887_.accept(Items.PRISMARINE_BRICK_SLAB);
                    p_450887_.accept(Items.DARK_PRISMARINE);
                    p_450887_.accept(Items.DARK_PRISMARINE_STAIRS);
                    p_450887_.accept(Items.DARK_PRISMARINE_SLAB);
                    p_450887_.accept(Items.NETHERRACK);
                    p_450887_.accept(Items.NETHER_BRICKS);
                    p_450887_.accept(Items.CRACKED_NETHER_BRICKS);
                    p_450887_.accept(Items.NETHER_BRICK_STAIRS);
                    p_450887_.accept(Items.NETHER_BRICK_SLAB);
                    p_450887_.accept(Items.NETHER_BRICK_WALL);
                    p_450887_.accept(Items.NETHER_BRICK_FENCE);
                    p_450887_.accept(Items.CHISELED_NETHER_BRICKS);
                    p_450887_.accept(Items.RED_NETHER_BRICKS);
                    p_450887_.accept(Items.RED_NETHER_BRICK_STAIRS);
                    p_450887_.accept(Items.RED_NETHER_BRICK_SLAB);
                    p_450887_.accept(Items.RED_NETHER_BRICK_WALL);
                    p_450887_.accept(Items.BASALT);
                    p_450887_.accept(Items.SMOOTH_BASALT);
                    p_450887_.accept(Items.POLISHED_BASALT);
                    p_450887_.accept(Items.BLACKSTONE);
                    p_450887_.accept(Items.GILDED_BLACKSTONE);
                    p_450887_.accept(Items.BLACKSTONE_STAIRS);
                    p_450887_.accept(Items.BLACKSTONE_SLAB);
                    p_450887_.accept(Items.BLACKSTONE_WALL);
                    p_450887_.accept(Items.CHISELED_POLISHED_BLACKSTONE);
                    p_450887_.accept(Items.POLISHED_BLACKSTONE);
                    p_450887_.accept(Items.POLISHED_BLACKSTONE_STAIRS);
                    p_450887_.accept(Items.POLISHED_BLACKSTONE_SLAB);
                    p_450887_.accept(Items.POLISHED_BLACKSTONE_WALL);
                    p_450887_.accept(Items.POLISHED_BLACKSTONE_PRESSURE_PLATE);
                    p_450887_.accept(Items.POLISHED_BLACKSTONE_BUTTON);
                    p_450887_.accept(Items.POLISHED_BLACKSTONE_BRICKS);
                    p_450887_.accept(Items.CRACKED_POLISHED_BLACKSTONE_BRICKS);
                    p_450887_.accept(Items.POLISHED_BLACKSTONE_BRICK_STAIRS);
                    p_450887_.accept(Items.POLISHED_BLACKSTONE_BRICK_SLAB);
                    p_450887_.accept(Items.POLISHED_BLACKSTONE_BRICK_WALL);
                    p_450887_.accept(Items.END_STONE);
                    p_450887_.accept(Items.END_STONE_BRICKS);
                    p_450887_.accept(Items.END_STONE_BRICK_STAIRS);
                    p_450887_.accept(Items.END_STONE_BRICK_SLAB);
                    p_450887_.accept(Items.END_STONE_BRICK_WALL);
                    p_450887_.accept(Items.PURPUR_BLOCK);
                    p_450887_.accept(Items.PURPUR_PILLAR);
                    p_450887_.accept(Items.PURPUR_STAIRS);
                    p_450887_.accept(Items.PURPUR_SLAB);
                    p_450887_.accept(Items.COAL_BLOCK);
                    p_450887_.accept(Items.IRON_BLOCK);
                    p_450887_.accept(Items.IRON_BARS);
                    p_450887_.accept(Items.IRON_DOOR);
                    p_450887_.accept(Items.IRON_TRAPDOOR);
                    p_450887_.accept(Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
                    p_450887_.accept(Items.IRON_CHAIN);
                    p_450887_.accept(Items.GOLD_BLOCK);
                    p_450887_.accept(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
                    p_450887_.accept(Items.REDSTONE_BLOCK);
                    p_450887_.accept(Items.EMERALD_BLOCK);
                    p_450887_.accept(Items.LAPIS_BLOCK);
                    p_450887_.accept(Items.DIAMOND_BLOCK);
                    p_450887_.accept(Items.NETHERITE_BLOCK);
                    p_450887_.accept(Items.QUARTZ_BLOCK);
                    p_450887_.accept(Items.QUARTZ_STAIRS);
                    p_450887_.accept(Items.QUARTZ_SLAB);
                    p_450887_.accept(Items.CHISELED_QUARTZ_BLOCK);
                    p_450887_.accept(Items.QUARTZ_BRICKS);
                    p_450887_.accept(Items.QUARTZ_PILLAR);
                    p_450887_.accept(Items.SMOOTH_QUARTZ);
                    p_450887_.accept(Items.SMOOTH_QUARTZ_STAIRS);
                    p_450887_.accept(Items.SMOOTH_QUARTZ_SLAB);
                    p_450887_.accept(Items.AMETHYST_BLOCK);
                    p_450887_.accept(Items.COPPER_BLOCK);
                    p_450887_.accept(Items.CHISELED_COPPER);
                    p_450887_.accept(Items.COPPER_GRATE);
                    p_450887_.accept(Items.CUT_COPPER);
                    p_450887_.accept(Items.CUT_COPPER_STAIRS);
                    p_450887_.accept(Items.CUT_COPPER_SLAB);
                    p_450887_.accept(Items.COPPER_BARS.unaffected());
                    p_450887_.accept(Items.COPPER_DOOR);
                    p_450887_.accept(Items.COPPER_TRAPDOOR);
                    p_450887_.accept(Items.COPPER_BULB);
                    p_450887_.accept(Items.COPPER_CHAIN.unaffected());
                    p_450887_.accept(Items.EXPOSED_COPPER);
                    p_450887_.accept(Items.EXPOSED_CHISELED_COPPER);
                    p_450887_.accept(Items.EXPOSED_COPPER_GRATE);
                    p_450887_.accept(Items.EXPOSED_CUT_COPPER);
                    p_450887_.accept(Items.EXPOSED_CUT_COPPER_STAIRS);
                    p_450887_.accept(Items.EXPOSED_CUT_COPPER_SLAB);
                    p_450887_.accept(Items.COPPER_BARS.exposed());
                    p_450887_.accept(Items.EXPOSED_COPPER_DOOR);
                    p_450887_.accept(Items.EXPOSED_COPPER_TRAPDOOR);
                    p_450887_.accept(Items.EXPOSED_COPPER_BULB);
                    p_450887_.accept(Items.COPPER_CHAIN.exposed());
                    p_450887_.accept(Items.WEATHERED_COPPER);
                    p_450887_.accept(Items.WEATHERED_CHISELED_COPPER);
                    p_450887_.accept(Items.WEATHERED_COPPER_GRATE);
                    p_450887_.accept(Items.WEATHERED_CUT_COPPER);
                    p_450887_.accept(Items.WEATHERED_CUT_COPPER_STAIRS);
                    p_450887_.accept(Items.WEATHERED_CUT_COPPER_SLAB);
                    p_450887_.accept(Items.COPPER_BARS.weathered());
                    p_450887_.accept(Items.WEATHERED_COPPER_DOOR);
                    p_450887_.accept(Items.WEATHERED_COPPER_TRAPDOOR);
                    p_450887_.accept(Items.WEATHERED_COPPER_BULB);
                    p_450887_.accept(Items.COPPER_CHAIN.weathered());
                    p_450887_.accept(Items.OXIDIZED_COPPER);
                    p_450887_.accept(Items.OXIDIZED_CHISELED_COPPER);
                    p_450887_.accept(Items.OXIDIZED_COPPER_GRATE);
                    p_450887_.accept(Items.OXIDIZED_CUT_COPPER);
                    p_450887_.accept(Items.OXIDIZED_CUT_COPPER_STAIRS);
                    p_450887_.accept(Items.OXIDIZED_CUT_COPPER_SLAB);
                    p_450887_.accept(Items.COPPER_BARS.oxidized());
                    p_450887_.accept(Items.OXIDIZED_COPPER_DOOR);
                    p_450887_.accept(Items.OXIDIZED_COPPER_TRAPDOOR);
                    p_450887_.accept(Items.OXIDIZED_COPPER_BULB);
                    p_450887_.accept(Items.COPPER_CHAIN.oxidized());
                    p_450887_.accept(Items.WAXED_COPPER_BLOCK);
                    p_450887_.accept(Items.WAXED_CHISELED_COPPER);
                    p_450887_.accept(Items.WAXED_COPPER_GRATE);
                    p_450887_.accept(Items.WAXED_CUT_COPPER);
                    p_450887_.accept(Items.WAXED_CUT_COPPER_STAIRS);
                    p_450887_.accept(Items.WAXED_CUT_COPPER_SLAB);
                    p_450887_.accept(Items.COPPER_BARS.waxed());
                    p_450887_.accept(Items.WAXED_COPPER_DOOR);
                    p_450887_.accept(Items.WAXED_COPPER_TRAPDOOR);
                    p_450887_.accept(Items.WAXED_COPPER_BULB);
                    p_450887_.accept(Items.COPPER_CHAIN.waxed());
                    p_450887_.accept(Items.WAXED_EXPOSED_COPPER);
                    p_450887_.accept(Items.WAXED_EXPOSED_CHISELED_COPPER);
                    p_450887_.accept(Items.WAXED_EXPOSED_COPPER_GRATE);
                    p_450887_.accept(Items.WAXED_EXPOSED_CUT_COPPER);
                    p_450887_.accept(Items.WAXED_EXPOSED_CUT_COPPER_STAIRS);
                    p_450887_.accept(Items.WAXED_EXPOSED_CUT_COPPER_SLAB);
                    p_450887_.accept(Items.COPPER_BARS.waxedExposed());
                    p_450887_.accept(Items.WAXED_EXPOSED_COPPER_DOOR);
                    p_450887_.accept(Items.WAXED_EXPOSED_COPPER_TRAPDOOR);
                    p_450887_.accept(Items.WAXED_EXPOSED_COPPER_BULB);
                    p_450887_.accept(Items.COPPER_CHAIN.waxedExposed());
                    p_450887_.accept(Items.WAXED_WEATHERED_COPPER);
                    p_450887_.accept(Items.WAXED_WEATHERED_CHISELED_COPPER);
                    p_450887_.accept(Items.WAXED_WEATHERED_COPPER_GRATE);
                    p_450887_.accept(Items.WAXED_WEATHERED_CUT_COPPER);
                    p_450887_.accept(Items.WAXED_WEATHERED_CUT_COPPER_STAIRS);
                    p_450887_.accept(Items.WAXED_WEATHERED_CUT_COPPER_SLAB);
                    p_450887_.accept(Items.COPPER_BARS.waxedWeathered());
                    p_450887_.accept(Items.WAXED_WEATHERED_COPPER_DOOR);
                    p_450887_.accept(Items.WAXED_WEATHERED_COPPER_TRAPDOOR);
                    p_450887_.accept(Items.WAXED_WEATHERED_COPPER_BULB);
                    p_450887_.accept(Items.COPPER_CHAIN.waxedWeathered());
                    p_450887_.accept(Items.WAXED_OXIDIZED_COPPER);
                    p_450887_.accept(Items.WAXED_OXIDIZED_CHISELED_COPPER);
                    p_450887_.accept(Items.WAXED_OXIDIZED_COPPER_GRATE);
                    p_450887_.accept(Items.WAXED_OXIDIZED_CUT_COPPER);
                    p_450887_.accept(Items.WAXED_OXIDIZED_CUT_COPPER_STAIRS);
                    p_450887_.accept(Items.WAXED_OXIDIZED_CUT_COPPER_SLAB);
                    p_450887_.accept(Items.COPPER_BARS.waxedOxidized());
                    p_450887_.accept(Items.WAXED_OXIDIZED_COPPER_DOOR);
                    p_450887_.accept(Items.WAXED_OXIDIZED_COPPER_TRAPDOOR);
                    p_450887_.accept(Items.WAXED_OXIDIZED_COPPER_BULB);
                    p_450887_.accept(Items.COPPER_CHAIN.waxedOxidized());
                })
                .build()
        );
        Registry.register(
            registry,
            COLORED_BLOCKS,
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
                .title(Component.translatable("itemGroup.coloredBlocks"))
                .icon(() -> new ItemStack(Blocks.CYAN_WOOL))
                .displayItems((p_271005_, p_259465_) -> {
                    p_259465_.accept(Items.WHITE_WOOL);
                    p_259465_.accept(Items.LIGHT_GRAY_WOOL);
                    p_259465_.accept(Items.GRAY_WOOL);
                    p_259465_.accept(Items.BLACK_WOOL);
                    p_259465_.accept(Items.BROWN_WOOL);
                    p_259465_.accept(Items.RED_WOOL);
                    p_259465_.accept(Items.ORANGE_WOOL);
                    p_259465_.accept(Items.YELLOW_WOOL);
                    p_259465_.accept(Items.LIME_WOOL);
                    p_259465_.accept(Items.GREEN_WOOL);
                    p_259465_.accept(Items.CYAN_WOOL);
                    p_259465_.accept(Items.LIGHT_BLUE_WOOL);
                    p_259465_.accept(Items.BLUE_WOOL);
                    p_259465_.accept(Items.PURPLE_WOOL);
                    p_259465_.accept(Items.MAGENTA_WOOL);
                    p_259465_.accept(Items.PINK_WOOL);
                    p_259465_.accept(Items.WHITE_CARPET);
                    p_259465_.accept(Items.LIGHT_GRAY_CARPET);
                    p_259465_.accept(Items.GRAY_CARPET);
                    p_259465_.accept(Items.BLACK_CARPET);
                    p_259465_.accept(Items.BROWN_CARPET);
                    p_259465_.accept(Items.RED_CARPET);
                    p_259465_.accept(Items.ORANGE_CARPET);
                    p_259465_.accept(Items.YELLOW_CARPET);
                    p_259465_.accept(Items.LIME_CARPET);
                    p_259465_.accept(Items.GREEN_CARPET);
                    p_259465_.accept(Items.CYAN_CARPET);
                    p_259465_.accept(Items.LIGHT_BLUE_CARPET);
                    p_259465_.accept(Items.BLUE_CARPET);
                    p_259465_.accept(Items.PURPLE_CARPET);
                    p_259465_.accept(Items.MAGENTA_CARPET);
                    p_259465_.accept(Items.PINK_CARPET);
                    p_259465_.accept(Items.TERRACOTTA);
                    p_259465_.accept(Items.WHITE_TERRACOTTA);
                    p_259465_.accept(Items.LIGHT_GRAY_TERRACOTTA);
                    p_259465_.accept(Items.GRAY_TERRACOTTA);
                    p_259465_.accept(Items.BLACK_TERRACOTTA);
                    p_259465_.accept(Items.BROWN_TERRACOTTA);
                    p_259465_.accept(Items.RED_TERRACOTTA);
                    p_259465_.accept(Items.ORANGE_TERRACOTTA);
                    p_259465_.accept(Items.YELLOW_TERRACOTTA);
                    p_259465_.accept(Items.LIME_TERRACOTTA);
                    p_259465_.accept(Items.GREEN_TERRACOTTA);
                    p_259465_.accept(Items.CYAN_TERRACOTTA);
                    p_259465_.accept(Items.LIGHT_BLUE_TERRACOTTA);
                    p_259465_.accept(Items.BLUE_TERRACOTTA);
                    p_259465_.accept(Items.PURPLE_TERRACOTTA);
                    p_259465_.accept(Items.MAGENTA_TERRACOTTA);
                    p_259465_.accept(Items.PINK_TERRACOTTA);
                    p_259465_.accept(Items.WHITE_CONCRETE);
                    p_259465_.accept(Items.LIGHT_GRAY_CONCRETE);
                    p_259465_.accept(Items.GRAY_CONCRETE);
                    p_259465_.accept(Items.BLACK_CONCRETE);
                    p_259465_.accept(Items.BROWN_CONCRETE);
                    p_259465_.accept(Items.RED_CONCRETE);
                    p_259465_.accept(Items.ORANGE_CONCRETE);
                    p_259465_.accept(Items.YELLOW_CONCRETE);
                    p_259465_.accept(Items.LIME_CONCRETE);
                    p_259465_.accept(Items.GREEN_CONCRETE);
                    p_259465_.accept(Items.CYAN_CONCRETE);
                    p_259465_.accept(Items.LIGHT_BLUE_CONCRETE);
                    p_259465_.accept(Items.BLUE_CONCRETE);
                    p_259465_.accept(Items.PURPLE_CONCRETE);
                    p_259465_.accept(Items.MAGENTA_CONCRETE);
                    p_259465_.accept(Items.PINK_CONCRETE);
                    p_259465_.accept(Items.WHITE_CONCRETE_POWDER);
                    p_259465_.accept(Items.LIGHT_GRAY_CONCRETE_POWDER);
                    p_259465_.accept(Items.GRAY_CONCRETE_POWDER);
                    p_259465_.accept(Items.BLACK_CONCRETE_POWDER);
                    p_259465_.accept(Items.BROWN_CONCRETE_POWDER);
                    p_259465_.accept(Items.RED_CONCRETE_POWDER);
                    p_259465_.accept(Items.ORANGE_CONCRETE_POWDER);
                    p_259465_.accept(Items.YELLOW_CONCRETE_POWDER);
                    p_259465_.accept(Items.LIME_CONCRETE_POWDER);
                    p_259465_.accept(Items.GREEN_CONCRETE_POWDER);
                    p_259465_.accept(Items.CYAN_CONCRETE_POWDER);
                    p_259465_.accept(Items.LIGHT_BLUE_CONCRETE_POWDER);
                    p_259465_.accept(Items.BLUE_CONCRETE_POWDER);
                    p_259465_.accept(Items.PURPLE_CONCRETE_POWDER);
                    p_259465_.accept(Items.MAGENTA_CONCRETE_POWDER);
                    p_259465_.accept(Items.PINK_CONCRETE_POWDER);
                    p_259465_.accept(Items.WHITE_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.GRAY_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.BLACK_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.BROWN_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.RED_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.ORANGE_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.YELLOW_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.LIME_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.GREEN_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.CYAN_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.LIGHT_BLUE_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.BLUE_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.PURPLE_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.MAGENTA_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.PINK_GLAZED_TERRACOTTA);
                    p_259465_.accept(Items.GLASS);
                    p_259465_.accept(Items.TINTED_GLASS);
                    p_259465_.accept(Items.WHITE_STAINED_GLASS);
                    p_259465_.accept(Items.LIGHT_GRAY_STAINED_GLASS);
                    p_259465_.accept(Items.GRAY_STAINED_GLASS);
                    p_259465_.accept(Items.BLACK_STAINED_GLASS);
                    p_259465_.accept(Items.BROWN_STAINED_GLASS);
                    p_259465_.accept(Items.RED_STAINED_GLASS);
                    p_259465_.accept(Items.ORANGE_STAINED_GLASS);
                    p_259465_.accept(Items.YELLOW_STAINED_GLASS);
                    p_259465_.accept(Items.LIME_STAINED_GLASS);
                    p_259465_.accept(Items.GREEN_STAINED_GLASS);
                    p_259465_.accept(Items.CYAN_STAINED_GLASS);
                    p_259465_.accept(Items.LIGHT_BLUE_STAINED_GLASS);
                    p_259465_.accept(Items.BLUE_STAINED_GLASS);
                    p_259465_.accept(Items.PURPLE_STAINED_GLASS);
                    p_259465_.accept(Items.MAGENTA_STAINED_GLASS);
                    p_259465_.accept(Items.PINK_STAINED_GLASS);
                    p_259465_.accept(Items.GLASS_PANE);
                    p_259465_.accept(Items.WHITE_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.GRAY_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.BLACK_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.BROWN_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.RED_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.ORANGE_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.YELLOW_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.LIME_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.GREEN_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.CYAN_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.LIGHT_BLUE_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.BLUE_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.PURPLE_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.MAGENTA_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.PINK_STAINED_GLASS_PANE);
                    p_259465_.accept(Items.SHULKER_BOX);
                    p_259465_.accept(Items.WHITE_SHULKER_BOX);
                    p_259465_.accept(Items.LIGHT_GRAY_SHULKER_BOX);
                    p_259465_.accept(Items.GRAY_SHULKER_BOX);
                    p_259465_.accept(Items.BLACK_SHULKER_BOX);
                    p_259465_.accept(Items.BROWN_SHULKER_BOX);
                    p_259465_.accept(Items.RED_SHULKER_BOX);
                    p_259465_.accept(Items.ORANGE_SHULKER_BOX);
                    p_259465_.accept(Items.YELLOW_SHULKER_BOX);
                    p_259465_.accept(Items.LIME_SHULKER_BOX);
                    p_259465_.accept(Items.GREEN_SHULKER_BOX);
                    p_259465_.accept(Items.CYAN_SHULKER_BOX);
                    p_259465_.accept(Items.LIGHT_BLUE_SHULKER_BOX);
                    p_259465_.accept(Items.BLUE_SHULKER_BOX);
                    p_259465_.accept(Items.PURPLE_SHULKER_BOX);
                    p_259465_.accept(Items.MAGENTA_SHULKER_BOX);
                    p_259465_.accept(Items.PINK_SHULKER_BOX);
                    p_259465_.accept(Items.WHITE_BED);
                    p_259465_.accept(Items.LIGHT_GRAY_BED);
                    p_259465_.accept(Items.GRAY_BED);
                    p_259465_.accept(Items.BLACK_BED);
                    p_259465_.accept(Items.BROWN_BED);
                    p_259465_.accept(Items.RED_BED);
                    p_259465_.accept(Items.ORANGE_BED);
                    p_259465_.accept(Items.YELLOW_BED);
                    p_259465_.accept(Items.LIME_BED);
                    p_259465_.accept(Items.GREEN_BED);
                    p_259465_.accept(Items.CYAN_BED);
                    p_259465_.accept(Items.LIGHT_BLUE_BED);
                    p_259465_.accept(Items.BLUE_BED);
                    p_259465_.accept(Items.PURPLE_BED);
                    p_259465_.accept(Items.MAGENTA_BED);
                    p_259465_.accept(Items.PINK_BED);
                    p_259465_.accept(Items.CANDLE);
                    p_259465_.accept(Items.WHITE_CANDLE);
                    p_259465_.accept(Items.LIGHT_GRAY_CANDLE);
                    p_259465_.accept(Items.GRAY_CANDLE);
                    p_259465_.accept(Items.BLACK_CANDLE);
                    p_259465_.accept(Items.BROWN_CANDLE);
                    p_259465_.accept(Items.RED_CANDLE);
                    p_259465_.accept(Items.ORANGE_CANDLE);
                    p_259465_.accept(Items.YELLOW_CANDLE);
                    p_259465_.accept(Items.LIME_CANDLE);
                    p_259465_.accept(Items.GREEN_CANDLE);
                    p_259465_.accept(Items.CYAN_CANDLE);
                    p_259465_.accept(Items.LIGHT_BLUE_CANDLE);
                    p_259465_.accept(Items.BLUE_CANDLE);
                    p_259465_.accept(Items.PURPLE_CANDLE);
                    p_259465_.accept(Items.MAGENTA_CANDLE);
                    p_259465_.accept(Items.PINK_CANDLE);
                    p_259465_.accept(Items.WHITE_BANNER);
                    p_259465_.accept(Items.LIGHT_GRAY_BANNER);
                    p_259465_.accept(Items.GRAY_BANNER);
                    p_259465_.accept(Items.BLACK_BANNER);
                    p_259465_.accept(Items.BROWN_BANNER);
                    p_259465_.accept(Items.RED_BANNER);
                    p_259465_.accept(Items.ORANGE_BANNER);
                    p_259465_.accept(Items.YELLOW_BANNER);
                    p_259465_.accept(Items.LIME_BANNER);
                    p_259465_.accept(Items.GREEN_BANNER);
                    p_259465_.accept(Items.CYAN_BANNER);
                    p_259465_.accept(Items.LIGHT_BLUE_BANNER);
                    p_259465_.accept(Items.BLUE_BANNER);
                    p_259465_.accept(Items.PURPLE_BANNER);
                    p_259465_.accept(Items.MAGENTA_BANNER);
                    p_259465_.accept(Items.PINK_BANNER);
                })
                .build()
        );
        Registry.register(
            registry,
            NATURAL_BLOCKS,
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 2)
                .title(Component.translatable("itemGroup.natural"))
                .icon(() -> new ItemStack(Blocks.GRASS_BLOCK))
                .displayItems((p_415384_, p_415385_) -> {
                    p_415385_.accept(Items.GRASS_BLOCK);
                    p_415385_.accept(Items.PODZOL);
                    p_415385_.accept(Items.MYCELIUM);
                    p_415385_.accept(Items.DIRT_PATH);
                    p_415385_.accept(Items.DIRT);
                    p_415385_.accept(Items.COARSE_DIRT);
                    p_415385_.accept(Items.ROOTED_DIRT);
                    p_415385_.accept(Items.FARMLAND);
                    p_415385_.accept(Items.MUD);
                    p_415385_.accept(Items.CLAY);
                    p_415385_.accept(Items.GRAVEL);
                    p_415385_.accept(Items.SAND);
                    p_415385_.accept(Items.SANDSTONE);
                    p_415385_.accept(Items.RED_SAND);
                    p_415385_.accept(Items.RED_SANDSTONE);
                    p_415385_.accept(Items.ICE);
                    p_415385_.accept(Items.PACKED_ICE);
                    p_415385_.accept(Items.BLUE_ICE);
                    p_415385_.accept(Items.SNOW_BLOCK);
                    p_415385_.accept(Items.SNOW);
                    p_415385_.accept(Items.MOSS_BLOCK);
                    p_415385_.accept(Items.MOSS_CARPET);
                    p_415385_.accept(Items.PALE_MOSS_BLOCK);
                    p_415385_.accept(Items.PALE_MOSS_CARPET);
                    p_415385_.accept(Items.PALE_HANGING_MOSS);
                    p_415385_.accept(Items.STONE);
                    p_415385_.accept(Items.DEEPSLATE);
                    p_415385_.accept(Items.GRANITE);
                    p_415385_.accept(Items.DIORITE);
                    p_415385_.accept(Items.ANDESITE);
                    p_415385_.accept(Items.CALCITE);
                    p_415385_.accept(Items.TUFF);
                    p_415385_.accept(Items.DRIPSTONE_BLOCK);
                    p_415385_.accept(Items.POINTED_DRIPSTONE);
                    p_415385_.accept(Items.PRISMARINE);
                    p_415385_.accept(Items.MAGMA_BLOCK);
                    p_415385_.accept(Items.OBSIDIAN);
                    p_415385_.accept(Items.CRYING_OBSIDIAN);
                    p_415385_.accept(Items.NETHERRACK);
                    p_415385_.accept(Items.CRIMSON_NYLIUM);
                    p_415385_.accept(Items.WARPED_NYLIUM);
                    p_415385_.accept(Items.SOUL_SAND);
                    p_415385_.accept(Items.SOUL_SOIL);
                    p_415385_.accept(Items.BONE_BLOCK);
                    p_415385_.accept(Items.BLACKSTONE);
                    p_415385_.accept(Items.BASALT);
                    p_415385_.accept(Items.SMOOTH_BASALT);
                    p_415385_.accept(Items.END_STONE);
                    p_415385_.accept(Items.COAL_ORE);
                    p_415385_.accept(Items.DEEPSLATE_COAL_ORE);
                    p_415385_.accept(Items.IRON_ORE);
                    p_415385_.accept(Items.DEEPSLATE_IRON_ORE);
                    p_415385_.accept(Items.COPPER_ORE);
                    p_415385_.accept(Items.DEEPSLATE_COPPER_ORE);
                    p_415385_.accept(Items.GOLD_ORE);
                    p_415385_.accept(Items.DEEPSLATE_GOLD_ORE);
                    p_415385_.accept(Items.REDSTONE_ORE);
                    p_415385_.accept(Items.DEEPSLATE_REDSTONE_ORE);
                    p_415385_.accept(Items.EMERALD_ORE);
                    p_415385_.accept(Items.DEEPSLATE_EMERALD_ORE);
                    p_415385_.accept(Items.LAPIS_ORE);
                    p_415385_.accept(Items.DEEPSLATE_LAPIS_ORE);
                    p_415385_.accept(Items.DIAMOND_ORE);
                    p_415385_.accept(Items.DEEPSLATE_DIAMOND_ORE);
                    p_415385_.accept(Items.NETHER_GOLD_ORE);
                    p_415385_.accept(Items.NETHER_QUARTZ_ORE);
                    p_415385_.accept(Items.ANCIENT_DEBRIS);
                    p_415385_.accept(Items.RAW_IRON_BLOCK);
                    p_415385_.accept(Items.RAW_COPPER_BLOCK);
                    p_415385_.accept(Items.RAW_GOLD_BLOCK);
                    p_415385_.accept(Items.GLOWSTONE);
                    p_415385_.accept(Items.AMETHYST_BLOCK);
                    p_415385_.accept(Items.BUDDING_AMETHYST);
                    p_415385_.accept(Items.SMALL_AMETHYST_BUD);
                    p_415385_.accept(Items.MEDIUM_AMETHYST_BUD);
                    p_415385_.accept(Items.LARGE_AMETHYST_BUD);
                    p_415385_.accept(Items.AMETHYST_CLUSTER);
                    p_415385_.accept(Items.OAK_LOG);
                    p_415385_.accept(Items.SPRUCE_LOG);
                    p_415385_.accept(Items.BIRCH_LOG);
                    p_415385_.accept(Items.JUNGLE_LOG);
                    p_415385_.accept(Items.ACACIA_LOG);
                    p_415385_.accept(Items.DARK_OAK_LOG);
                    p_415385_.accept(Items.MANGROVE_LOG);
                    p_415385_.accept(Items.MANGROVE_ROOTS);
                    p_415385_.accept(Items.MUDDY_MANGROVE_ROOTS);
                    p_415385_.accept(Items.CHERRY_LOG);
                    p_415385_.accept(Items.PALE_OAK_LOG);
                    p_415385_.accept(Items.MUSHROOM_STEM);
                    p_415385_.accept(Items.CRIMSON_STEM);
                    p_415385_.accept(Items.WARPED_STEM);
                    p_415385_.accept(Items.OAK_LEAVES);
                    p_415385_.accept(Items.SPRUCE_LEAVES);
                    p_415385_.accept(Items.BIRCH_LEAVES);
                    p_415385_.accept(Items.JUNGLE_LEAVES);
                    p_415385_.accept(Items.ACACIA_LEAVES);
                    p_415385_.accept(Items.DARK_OAK_LEAVES);
                    p_415385_.accept(Items.MANGROVE_LEAVES);
                    p_415385_.accept(Items.CHERRY_LEAVES);
                    p_415385_.accept(Items.PALE_OAK_LEAVES);
                    p_415385_.accept(Items.AZALEA_LEAVES);
                    p_415385_.accept(Items.FLOWERING_AZALEA_LEAVES);
                    p_415385_.accept(Items.BROWN_MUSHROOM_BLOCK);
                    p_415385_.accept(Items.RED_MUSHROOM_BLOCK);
                    p_415385_.accept(Items.NETHER_WART_BLOCK);
                    p_415385_.accept(Items.WARPED_WART_BLOCK);
                    p_415385_.accept(Items.SHROOMLIGHT);
                    p_415385_.accept(Items.OAK_SAPLING);
                    p_415385_.accept(Items.SPRUCE_SAPLING);
                    p_415385_.accept(Items.BIRCH_SAPLING);
                    p_415385_.accept(Items.JUNGLE_SAPLING);
                    p_415385_.accept(Items.ACACIA_SAPLING);
                    p_415385_.accept(Items.DARK_OAK_SAPLING);
                    p_415385_.accept(Items.MANGROVE_PROPAGULE);
                    p_415385_.accept(Items.CHERRY_SAPLING);
                    p_415385_.accept(Items.PALE_OAK_SAPLING);
                    p_415385_.accept(Items.AZALEA);
                    p_415385_.accept(Items.FLOWERING_AZALEA);
                    p_415385_.accept(Items.BROWN_MUSHROOM);
                    p_415385_.accept(Items.RED_MUSHROOM);
                    p_415385_.accept(Items.CRIMSON_FUNGUS);
                    p_415385_.accept(Items.WARPED_FUNGUS);
                    p_415385_.accept(Items.SHORT_GRASS);
                    p_415385_.accept(Items.FERN);
                    p_415385_.accept(Items.DRY_SHORT_GRASS);
                    p_415385_.accept(Items.BUSH);
                    p_415385_.accept(Items.DEAD_BUSH);
                    p_415385_.accept(Items.DANDELION);
                    p_415385_.accept(Items.POPPY);
                    p_415385_.accept(Items.BLUE_ORCHID);
                    p_415385_.accept(Items.ALLIUM);
                    p_415385_.accept(Items.AZURE_BLUET);
                    p_415385_.accept(Items.RED_TULIP);
                    p_415385_.accept(Items.ORANGE_TULIP);
                    p_415385_.accept(Items.WHITE_TULIP);
                    p_415385_.accept(Items.PINK_TULIP);
                    p_415385_.accept(Items.OXEYE_DAISY);
                    p_415385_.accept(Items.CORNFLOWER);
                    p_415385_.accept(Items.LILY_OF_THE_VALLEY);
                    p_415385_.accept(Items.TORCHFLOWER);
                    p_415385_.accept(Items.CACTUS_FLOWER);
                    p_415385_.accept(Items.CLOSED_EYEBLOSSOM);
                    p_415385_.accept(Items.OPEN_EYEBLOSSOM);
                    p_415385_.accept(Items.WITHER_ROSE);
                    p_415385_.accept(Items.PINK_PETALS);
                    p_415385_.accept(Items.WILDFLOWERS);
                    p_415385_.accept(Items.LEAF_LITTER);
                    p_415385_.accept(Items.SPORE_BLOSSOM);
                    p_415385_.accept(Items.FIREFLY_BUSH);
                    p_415385_.accept(Items.BAMBOO);
                    p_415385_.accept(Items.SUGAR_CANE);
                    p_415385_.accept(Items.CACTUS);
                    p_415385_.accept(Items.CRIMSON_ROOTS);
                    p_415385_.accept(Items.WARPED_ROOTS);
                    p_415385_.accept(Items.NETHER_SPROUTS);
                    p_415385_.accept(Items.WEEPING_VINES);
                    p_415385_.accept(Items.TWISTING_VINES);
                    p_415385_.accept(Items.VINE);
                    p_415385_.accept(Items.TALL_GRASS);
                    p_415385_.accept(Items.LARGE_FERN);
                    p_415385_.accept(Items.DRY_TALL_GRASS);
                    p_415385_.accept(Items.SUNFLOWER);
                    p_415385_.accept(Items.LILAC);
                    p_415385_.accept(Items.ROSE_BUSH);
                    p_415385_.accept(Items.PEONY);
                    p_415385_.accept(Items.PITCHER_PLANT);
                    p_415385_.accept(Items.BIG_DRIPLEAF);
                    p_415385_.accept(Items.SMALL_DRIPLEAF);
                    p_415385_.accept(Items.CHORUS_PLANT);
                    p_415385_.accept(Items.CHORUS_FLOWER);
                    p_415385_.accept(Items.GLOW_LICHEN);
                    p_415385_.accept(Items.HANGING_ROOTS);
                    p_415385_.accept(Items.FROGSPAWN);
                    p_415385_.accept(Items.TURTLE_EGG);
                    p_415385_.accept(Items.SNIFFER_EGG);
                    p_415385_.accept(Items.DRIED_GHAST);
                    p_415385_.accept(Items.WHEAT_SEEDS);
                    p_415385_.accept(Items.COCOA_BEANS);
                    p_415385_.accept(Items.PUMPKIN_SEEDS);
                    p_415385_.accept(Items.MELON_SEEDS);
                    p_415385_.accept(Items.BEETROOT_SEEDS);
                    p_415385_.accept(Items.TORCHFLOWER_SEEDS);
                    p_415385_.accept(Items.PITCHER_POD);
                    p_415385_.accept(Items.GLOW_BERRIES);
                    p_415385_.accept(Items.SWEET_BERRIES);
                    p_415385_.accept(Items.NETHER_WART);
                    p_415385_.accept(Items.LILY_PAD);
                    p_415385_.accept(Items.SEAGRASS);
                    p_415385_.accept(Items.SEA_PICKLE);
                    p_415385_.accept(Items.KELP);
                    p_415385_.accept(Items.DRIED_KELP_BLOCK);
                    p_415385_.accept(Items.TUBE_CORAL_BLOCK);
                    p_415385_.accept(Items.BRAIN_CORAL_BLOCK);
                    p_415385_.accept(Items.BUBBLE_CORAL_BLOCK);
                    p_415385_.accept(Items.FIRE_CORAL_BLOCK);
                    p_415385_.accept(Items.HORN_CORAL_BLOCK);
                    p_415385_.accept(Items.DEAD_TUBE_CORAL_BLOCK);
                    p_415385_.accept(Items.DEAD_BRAIN_CORAL_BLOCK);
                    p_415385_.accept(Items.DEAD_BUBBLE_CORAL_BLOCK);
                    p_415385_.accept(Items.DEAD_FIRE_CORAL_BLOCK);
                    p_415385_.accept(Items.DEAD_HORN_CORAL_BLOCK);
                    p_415385_.accept(Items.TUBE_CORAL);
                    p_415385_.accept(Items.BRAIN_CORAL);
                    p_415385_.accept(Items.BUBBLE_CORAL);
                    p_415385_.accept(Items.FIRE_CORAL);
                    p_415385_.accept(Items.HORN_CORAL);
                    p_415385_.accept(Items.DEAD_TUBE_CORAL);
                    p_415385_.accept(Items.DEAD_BRAIN_CORAL);
                    p_415385_.accept(Items.DEAD_BUBBLE_CORAL);
                    p_415385_.accept(Items.DEAD_FIRE_CORAL);
                    p_415385_.accept(Items.DEAD_HORN_CORAL);
                    p_415385_.accept(Items.TUBE_CORAL_FAN);
                    p_415385_.accept(Items.BRAIN_CORAL_FAN);
                    p_415385_.accept(Items.BUBBLE_CORAL_FAN);
                    p_415385_.accept(Items.FIRE_CORAL_FAN);
                    p_415385_.accept(Items.HORN_CORAL_FAN);
                    p_415385_.accept(Items.DEAD_TUBE_CORAL_FAN);
                    p_415385_.accept(Items.DEAD_BRAIN_CORAL_FAN);
                    p_415385_.accept(Items.DEAD_BUBBLE_CORAL_FAN);
                    p_415385_.accept(Items.DEAD_FIRE_CORAL_FAN);
                    p_415385_.accept(Items.DEAD_HORN_CORAL_FAN);
                    p_415385_.accept(Items.SPONGE);
                    p_415385_.accept(Items.WET_SPONGE);
                    p_415385_.accept(Items.MELON);
                    p_415385_.accept(Items.PUMPKIN);
                    p_415385_.accept(Items.CARVED_PUMPKIN);
                    p_415385_.accept(Items.JACK_O_LANTERN);
                    p_415385_.accept(Items.HAY_BLOCK);
                    p_415385_.accept(Items.BEE_NEST);
                    p_415385_.accept(Items.HONEYCOMB_BLOCK);
                    p_415385_.accept(Items.SLIME_BLOCK);
                    p_415385_.accept(Items.HONEY_BLOCK);
                    p_415385_.accept(Items.RESIN_BLOCK);
                    p_415385_.accept(Items.OCHRE_FROGLIGHT);
                    p_415385_.accept(Items.VERDANT_FROGLIGHT);
                    p_415385_.accept(Items.PEARLESCENT_FROGLIGHT);
                    p_415385_.accept(Items.SCULK);
                    p_415385_.accept(Items.SCULK_VEIN);
                    p_415385_.accept(Items.SCULK_CATALYST);
                    p_415385_.accept(Items.SCULK_SHRIEKER);
                    p_415385_.accept(Items.SCULK_SENSOR);
                    p_415385_.accept(Items.COBWEB);
                    p_415385_.accept(Items.BEDROCK);
                })
                .build()
        );
        Registry.register(
            registry,
            FUNCTIONAL_BLOCKS,
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 3)
                .title(Component.translatable("itemGroup.functional"))
                .icon(() -> new ItemStack(Items.OAK_SIGN))
                .displayItems(
                    (p_442467_, p_442468_) -> {
                        p_442468_.accept(Items.TORCH);
                        p_442468_.accept(Items.SOUL_TORCH);
                        p_442468_.accept(Items.COPPER_TORCH);
                        p_442468_.accept(Items.REDSTONE_TORCH);
                        p_442468_.accept(Items.LANTERN);
                        p_442468_.accept(Items.SOUL_LANTERN);
                        Items.COPPER_LANTERN.forEach(p_442468_::accept);
                        p_442468_.accept(Items.IRON_CHAIN);
                        Items.COPPER_CHAIN.forEach(p_442468_::accept);
                        p_442468_.accept(Items.END_ROD);
                        p_442468_.accept(Items.SEA_LANTERN);
                        p_442468_.accept(Items.REDSTONE_LAMP);
                        p_442468_.accept(Items.COPPER_BULB);
                        p_442468_.accept(Items.EXPOSED_COPPER_BULB);
                        p_442468_.accept(Items.WEATHERED_COPPER_BULB);
                        p_442468_.accept(Items.OXIDIZED_COPPER_BULB);
                        p_442468_.accept(Items.WAXED_COPPER_BULB);
                        p_442468_.accept(Items.WAXED_EXPOSED_COPPER_BULB);
                        p_442468_.accept(Items.WAXED_WEATHERED_COPPER_BULB);
                        p_442468_.accept(Items.WAXED_OXIDIZED_COPPER_BULB);
                        p_442468_.accept(Items.GLOWSTONE);
                        p_442468_.accept(Items.SHROOMLIGHT);
                        p_442468_.accept(Items.OCHRE_FROGLIGHT);
                        p_442468_.accept(Items.VERDANT_FROGLIGHT);
                        p_442468_.accept(Items.PEARLESCENT_FROGLIGHT);
                        p_442468_.accept(Items.CRYING_OBSIDIAN);
                        p_442468_.accept(Items.GLOW_LICHEN);
                        p_442468_.accept(Items.MAGMA_BLOCK);
                        p_442468_.accept(Items.CRAFTING_TABLE);
                        p_442468_.accept(Items.STONECUTTER);
                        p_442468_.accept(Items.CARTOGRAPHY_TABLE);
                        p_442468_.accept(Items.FLETCHING_TABLE);
                        p_442468_.accept(Items.SMITHING_TABLE);
                        p_442468_.accept(Items.GRINDSTONE);
                        p_442468_.accept(Items.LOOM);
                        p_442468_.accept(Items.FURNACE);
                        p_442468_.accept(Items.SMOKER);
                        p_442468_.accept(Items.BLAST_FURNACE);
                        p_442468_.accept(Items.CAMPFIRE);
                        p_442468_.accept(Items.SOUL_CAMPFIRE);
                        p_442468_.accept(Items.ANVIL);
                        p_442468_.accept(Items.CHIPPED_ANVIL);
                        p_442468_.accept(Items.DAMAGED_ANVIL);
                        p_442468_.accept(Items.COMPOSTER);
                        p_442468_.accept(Items.NOTE_BLOCK);
                        p_442468_.accept(Items.JUKEBOX);
                        p_442468_.accept(Items.ENCHANTING_TABLE);
                        p_442468_.accept(Items.END_CRYSTAL);
                        p_442468_.accept(Items.BREWING_STAND);
                        p_442468_.accept(Items.CAULDRON);
                        p_442468_.accept(Items.BELL);
                        p_442468_.accept(Items.BEACON);
                        p_442468_.accept(Items.CONDUIT);
                        p_442468_.accept(Items.LODESTONE);
                        p_442468_.accept(Items.LADDER);
                        p_442468_.accept(Items.SCAFFOLDING);
                        p_442468_.accept(Items.BEE_NEST);
                        p_442468_.accept(Items.BEEHIVE);
                        p_442468_.accept(Items.SUSPICIOUS_SAND);
                        p_442468_.accept(Items.SUSPICIOUS_GRAVEL);
                        p_442468_.accept(Items.LIGHTNING_ROD);
                        p_442468_.accept(Items.EXPOSED_LIGHTNING_ROD);
                        p_442468_.accept(Items.WEATHERED_LIGHTNING_ROD);
                        p_442468_.accept(Items.OXIDIZED_LIGHTNING_ROD);
                        p_442468_.accept(Items.WAXED_LIGHTNING_ROD);
                        p_442468_.accept(Items.WAXED_EXPOSED_LIGHTNING_ROD);
                        p_442468_.accept(Items.WAXED_WEATHERED_LIGHTNING_ROD);
                        p_442468_.accept(Items.WAXED_OXIDIZED_LIGHTNING_ROD);
                        p_442468_.accept(Items.FLOWER_POT);
                        p_442468_.accept(Items.DECORATED_POT);
                        p_442468_.accept(Items.ARMOR_STAND);
                        p_442468_.accept(Items.ITEM_FRAME);
                        p_442468_.accept(Items.GLOW_ITEM_FRAME);
                        p_442468_.accept(Items.PAINTING);
                        p_442467_.holders()
                            .lookup(Registries.PAINTING_VARIANT)
                            .ifPresent(
                                p_344388_ -> generatePresetPaintings(
                                    p_442468_,
                                    p_442467_.holders(),
                                    (HolderLookup.RegistryLookup<PaintingVariant>)p_344388_,
                                    p_270037_ -> p_270037_.is(PaintingVariantTags.PLACEABLE),
                                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                                )
                            );
                        p_442468_.accept(Items.BOOKSHELF);
                        p_442468_.accept(Items.CHISELED_BOOKSHELF);
                        p_442468_.accept(Items.OAK_SHELF);
                        p_442468_.accept(Items.SPRUCE_SHELF);
                        p_442468_.accept(Items.BIRCH_SHELF);
                        p_442468_.accept(Items.JUNGLE_SHELF);
                        p_442468_.accept(Items.ACACIA_SHELF);
                        p_442468_.accept(Items.DARK_OAK_SHELF);
                        p_442468_.accept(Items.MANGROVE_SHELF);
                        p_442468_.accept(Items.CHERRY_SHELF);
                        p_442468_.accept(Items.PALE_OAK_SHELF);
                        p_442468_.accept(Items.BAMBOO_SHELF);
                        p_442468_.accept(Items.CRIMSON_SHELF);
                        p_442468_.accept(Items.WARPED_SHELF);
                        p_442468_.accept(Items.LECTERN);
                        p_442468_.accept(Items.TINTED_GLASS);
                        p_442468_.accept(Items.OAK_SIGN);
                        p_442468_.accept(Items.OAK_HANGING_SIGN);
                        p_442468_.accept(Items.SPRUCE_SIGN);
                        p_442468_.accept(Items.SPRUCE_HANGING_SIGN);
                        p_442468_.accept(Items.BIRCH_SIGN);
                        p_442468_.accept(Items.BIRCH_HANGING_SIGN);
                        p_442468_.accept(Items.JUNGLE_SIGN);
                        p_442468_.accept(Items.JUNGLE_HANGING_SIGN);
                        p_442468_.accept(Items.ACACIA_SIGN);
                        p_442468_.accept(Items.ACACIA_HANGING_SIGN);
                        p_442468_.accept(Items.DARK_OAK_SIGN);
                        p_442468_.accept(Items.DARK_OAK_HANGING_SIGN);
                        p_442468_.accept(Items.MANGROVE_SIGN);
                        p_442468_.accept(Items.MANGROVE_HANGING_SIGN);
                        p_442468_.accept(Items.CHERRY_SIGN);
                        p_442468_.accept(Items.CHERRY_HANGING_SIGN);
                        p_442468_.accept(Items.PALE_OAK_SIGN);
                        p_442468_.accept(Items.PALE_OAK_HANGING_SIGN);
                        p_442468_.accept(Items.BAMBOO_SIGN);
                        p_442468_.accept(Items.BAMBOO_HANGING_SIGN);
                        p_442468_.accept(Items.CRIMSON_SIGN);
                        p_442468_.accept(Items.CRIMSON_HANGING_SIGN);
                        p_442468_.accept(Items.WARPED_SIGN);
                        p_442468_.accept(Items.WARPED_HANGING_SIGN);
                        p_442468_.accept(Items.CHEST);
                        p_442468_.accept(Items.COPPER_CHEST);
                        p_442468_.accept(Items.EXPOSED_COPPER_CHEST);
                        p_442468_.accept(Items.WEATHERED_COPPER_CHEST);
                        p_442468_.accept(Items.OXIDIZED_COPPER_CHEST);
                        p_442468_.accept(Items.WAXED_COPPER_CHEST);
                        p_442468_.accept(Items.WAXED_EXPOSED_COPPER_CHEST);
                        p_442468_.accept(Items.WAXED_WEATHERED_COPPER_CHEST);
                        p_442468_.accept(Items.WAXED_OXIDIZED_COPPER_CHEST);
                        p_442468_.accept(Items.BARREL);
                        p_442468_.accept(Items.ENDER_CHEST);
                        p_442468_.accept(Items.SHULKER_BOX);
                        p_442468_.accept(Items.WHITE_SHULKER_BOX);
                        p_442468_.accept(Items.LIGHT_GRAY_SHULKER_BOX);
                        p_442468_.accept(Items.GRAY_SHULKER_BOX);
                        p_442468_.accept(Items.BLACK_SHULKER_BOX);
                        p_442468_.accept(Items.BROWN_SHULKER_BOX);
                        p_442468_.accept(Items.RED_SHULKER_BOX);
                        p_442468_.accept(Items.ORANGE_SHULKER_BOX);
                        p_442468_.accept(Items.YELLOW_SHULKER_BOX);
                        p_442468_.accept(Items.LIME_SHULKER_BOX);
                        p_442468_.accept(Items.GREEN_SHULKER_BOX);
                        p_442468_.accept(Items.CYAN_SHULKER_BOX);
                        p_442468_.accept(Items.LIGHT_BLUE_SHULKER_BOX);
                        p_442468_.accept(Items.BLUE_SHULKER_BOX);
                        p_442468_.accept(Items.PURPLE_SHULKER_BOX);
                        p_442468_.accept(Items.MAGENTA_SHULKER_BOX);
                        p_442468_.accept(Items.PINK_SHULKER_BOX);
                        p_442468_.accept(Items.RESPAWN_ANCHOR);
                        p_442468_.accept(Items.WHITE_BED);
                        p_442468_.accept(Items.LIGHT_GRAY_BED);
                        p_442468_.accept(Items.GRAY_BED);
                        p_442468_.accept(Items.BLACK_BED);
                        p_442468_.accept(Items.BROWN_BED);
                        p_442468_.accept(Items.RED_BED);
                        p_442468_.accept(Items.ORANGE_BED);
                        p_442468_.accept(Items.YELLOW_BED);
                        p_442468_.accept(Items.LIME_BED);
                        p_442468_.accept(Items.GREEN_BED);
                        p_442468_.accept(Items.CYAN_BED);
                        p_442468_.accept(Items.LIGHT_BLUE_BED);
                        p_442468_.accept(Items.BLUE_BED);
                        p_442468_.accept(Items.PURPLE_BED);
                        p_442468_.accept(Items.MAGENTA_BED);
                        p_442468_.accept(Items.PINK_BED);
                        p_442468_.accept(Items.CANDLE);
                        p_442468_.accept(Items.WHITE_CANDLE);
                        p_442468_.accept(Items.LIGHT_GRAY_CANDLE);
                        p_442468_.accept(Items.GRAY_CANDLE);
                        p_442468_.accept(Items.BLACK_CANDLE);
                        p_442468_.accept(Items.BROWN_CANDLE);
                        p_442468_.accept(Items.RED_CANDLE);
                        p_442468_.accept(Items.ORANGE_CANDLE);
                        p_442468_.accept(Items.YELLOW_CANDLE);
                        p_442468_.accept(Items.LIME_CANDLE);
                        p_442468_.accept(Items.GREEN_CANDLE);
                        p_442468_.accept(Items.CYAN_CANDLE);
                        p_442468_.accept(Items.LIGHT_BLUE_CANDLE);
                        p_442468_.accept(Items.BLUE_CANDLE);
                        p_442468_.accept(Items.PURPLE_CANDLE);
                        p_442468_.accept(Items.MAGENTA_CANDLE);
                        p_442468_.accept(Items.PINK_CANDLE);
                        p_442468_.accept(Items.WHITE_BANNER);
                        p_442468_.accept(Items.LIGHT_GRAY_BANNER);
                        p_442468_.accept(Items.GRAY_BANNER);
                        p_442468_.accept(Items.BLACK_BANNER);
                        p_442468_.accept(Items.BROWN_BANNER);
                        p_442468_.accept(Items.RED_BANNER);
                        p_442468_.accept(Items.ORANGE_BANNER);
                        p_442468_.accept(Items.YELLOW_BANNER);
                        p_442468_.accept(Items.LIME_BANNER);
                        p_442468_.accept(Items.GREEN_BANNER);
                        p_442468_.accept(Items.CYAN_BANNER);
                        p_442468_.accept(Items.LIGHT_BLUE_BANNER);
                        p_442468_.accept(Items.BLUE_BANNER);
                        p_442468_.accept(Items.PURPLE_BANNER);
                        p_442468_.accept(Items.MAGENTA_BANNER);
                        p_442468_.accept(Items.PINK_BANNER);
                        p_442468_.accept(Raid.getOminousBannerInstance(p_442467_.holders().lookupOrThrow(Registries.BANNER_PATTERN)));
                        p_442468_.accept(Items.SKELETON_SKULL);
                        p_442468_.accept(Items.WITHER_SKELETON_SKULL);
                        p_442468_.accept(Items.PLAYER_HEAD);
                        p_442468_.accept(Items.ZOMBIE_HEAD);
                        p_442468_.accept(Items.CREEPER_HEAD);
                        p_442468_.accept(Items.PIGLIN_HEAD);
                        p_442468_.accept(Items.DRAGON_HEAD);
                        p_442468_.accept(Items.DRAGON_EGG);
                        p_442468_.accept(Items.END_PORTAL_FRAME);
                        p_442468_.accept(Items.ENDER_EYE);
                        p_442468_.accept(Items.VAULT);
                        p_442468_.accept(Items.COPPER_GOLEM_STATUE);
                        p_442468_.accept(Items.EXPOSED_COPPER_GOLEM_STATUE);
                        p_442468_.accept(Items.WEATHERED_COPPER_GOLEM_STATUE);
                        p_442468_.accept(Items.OXIDIZED_COPPER_GOLEM_STATUE);
                        p_442468_.accept(Items.WAXED_COPPER_GOLEM_STATUE);
                        p_442468_.accept(Items.WAXED_EXPOSED_COPPER_GOLEM_STATUE);
                        p_442468_.accept(Items.WAXED_WEATHERED_COPPER_GOLEM_STATUE);
                        p_442468_.accept(Items.WAXED_OXIDIZED_COPPER_GOLEM_STATUE);
                        p_442468_.accept(Items.INFESTED_STONE);
                        p_442468_.accept(Items.INFESTED_COBBLESTONE);
                        p_442468_.accept(Items.INFESTED_STONE_BRICKS);
                        p_442468_.accept(Items.INFESTED_MOSSY_STONE_BRICKS);
                        p_442468_.accept(Items.INFESTED_CRACKED_STONE_BRICKS);
                        p_442468_.accept(Items.INFESTED_CHISELED_STONE_BRICKS);
                        p_442468_.accept(Items.INFESTED_DEEPSLATE);
                    }
                )
                .build()
        );
        Registry.register(
            registry,
            REDSTONE_BLOCKS,
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 4)
                .title(Component.translatable("itemGroup.redstone"))
                .icon(() -> new ItemStack(Items.REDSTONE))
                .displayItems((p_442469_, p_442470_) -> {
                    p_442470_.accept(Items.REDSTONE);
                    p_442470_.accept(Items.REDSTONE_TORCH);
                    p_442470_.accept(Items.REDSTONE_BLOCK);
                    p_442470_.accept(Items.REPEATER);
                    p_442470_.accept(Items.COMPARATOR);
                    p_442470_.accept(Items.TARGET);
                    p_442470_.accept(Items.WAXED_COPPER_BULB);
                    p_442470_.accept(Items.WAXED_EXPOSED_COPPER_BULB);
                    p_442470_.accept(Items.WAXED_WEATHERED_COPPER_BULB);
                    p_442470_.accept(Items.WAXED_OXIDIZED_COPPER_BULB);
                    p_442470_.accept(Items.LEVER);
                    p_442470_.accept(Items.OAK_BUTTON);
                    p_442470_.accept(Items.STONE_BUTTON);
                    p_442470_.accept(Items.OAK_PRESSURE_PLATE);
                    p_442470_.accept(Items.STONE_PRESSURE_PLATE);
                    p_442470_.accept(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
                    p_442470_.accept(Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
                    p_442470_.accept(Items.SCULK_SENSOR);
                    p_442470_.accept(Items.CALIBRATED_SCULK_SENSOR);
                    p_442470_.accept(Items.SCULK_SHRIEKER);
                    p_442470_.accept(Items.AMETHYST_BLOCK);
                    p_442470_.accept(Items.WHITE_WOOL);
                    p_442470_.accept(Items.TRIPWIRE_HOOK);
                    p_442470_.accept(Items.STRING);
                    p_442470_.accept(Items.LECTERN);
                    p_442470_.accept(Items.DAYLIGHT_DETECTOR);
                    p_442470_.accept(Items.WAXED_LIGHTNING_ROD);
                    p_442470_.accept(Items.PISTON);
                    p_442470_.accept(Items.STICKY_PISTON);
                    p_442470_.accept(Items.SLIME_BLOCK);
                    p_442470_.accept(Items.HONEY_BLOCK);
                    p_442470_.accept(Items.DISPENSER);
                    p_442470_.accept(Items.DROPPER);
                    p_442470_.accept(Items.CRAFTER);
                    p_442470_.accept(Items.HOPPER);
                    p_442470_.accept(Items.CHEST);
                    p_442470_.accept(Items.WAXED_COPPER_CHEST);
                    p_442470_.accept(Items.BARREL);
                    p_442470_.accept(Items.CHISELED_BOOKSHELF);
                    p_442470_.accept(Items.OAK_SHELF);
                    p_442470_.accept(Items.FURNACE);
                    p_442470_.accept(Items.TRAPPED_CHEST);
                    p_442470_.accept(Items.JUKEBOX);
                    p_442470_.accept(Items.DECORATED_POT);
                    p_442470_.accept(Items.OBSERVER);
                    p_442470_.accept(Items.NOTE_BLOCK);
                    p_442470_.accept(Items.COMPOSTER);
                    p_442470_.accept(Items.CAULDRON);
                    p_442470_.accept(Items.RAIL);
                    p_442470_.accept(Items.POWERED_RAIL);
                    p_442470_.accept(Items.DETECTOR_RAIL);
                    p_442470_.accept(Items.ACTIVATOR_RAIL);
                    p_442470_.accept(Items.MINECART);
                    p_442470_.accept(Items.HOPPER_MINECART);
                    p_442470_.accept(Items.CHEST_MINECART);
                    p_442470_.accept(Items.FURNACE_MINECART);
                    p_442470_.accept(Items.TNT_MINECART);
                    p_442470_.accept(Items.OAK_CHEST_BOAT);
                    p_442470_.accept(Items.BAMBOO_CHEST_RAFT);
                    p_442470_.accept(Items.OAK_DOOR);
                    p_442470_.accept(Items.IRON_DOOR);
                    p_442470_.accept(Items.OAK_FENCE_GATE);
                    p_442470_.accept(Items.OAK_TRAPDOOR);
                    p_442470_.accept(Items.IRON_TRAPDOOR);
                    p_442470_.accept(Items.TNT);
                    p_442470_.accept(Items.REDSTONE_LAMP);
                    p_442470_.accept(Items.BELL);
                    p_442470_.accept(Items.BIG_DRIPLEAF);
                    p_442470_.accept(Items.ARMOR_STAND);
                    p_442470_.accept(Items.REDSTONE_ORE);
                })
                .build()
        );
        Registry.register(
            registry,
            HOTBAR,
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 5)
                .title(Component.translatable("itemGroup.hotbar"))
                .icon(() -> new ItemStack(Blocks.BOOKSHELF))
                .alignedRight()
                .type(CreativeModeTab.Type.HOTBAR)
                .build()
        );
        Registry.register(
            registry,
            SEARCH,
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 6)
                .title(Component.translatable("itemGroup.search"))
                .icon(() -> new ItemStack(Items.COMPASS))
                .displayItems((p_330080_, p_330081_) -> {
                    Set<ItemStack> set = ItemStackLinkedSet.createTypeAndComponentsSet();

                    for (CreativeModeTab creativemodetab : registry) {
                        if (creativemodetab.getType() != CreativeModeTab.Type.SEARCH) {
                            set.addAll(creativemodetab.getSearchTabDisplayItems());
                        }
                    }

                    p_330081_.acceptAll(set);
                })
                .backgroundTexture(SEARCH_BACKGROUND)
                .alignedRight()
                .type(CreativeModeTab.Type.SEARCH)
                .build()
        );
        Registry.register(
            registry,
            TOOLS_AND_UTILITIES,
            CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 0)
                .title(Component.translatable("itemGroup.tools"))
                .icon(() -> new ItemStack(Items.DIAMOND_PICKAXE))
                .displayItems(
                    (p_432569_, p_432570_) -> {
                        p_432570_.accept(Items.WOODEN_SHOVEL);
                        p_432570_.accept(Items.WOODEN_PICKAXE);
                        p_432570_.accept(Items.WOODEN_AXE);
                        p_432570_.accept(Items.WOODEN_HOE);
                        p_432570_.accept(Items.STONE_SHOVEL);
                        p_432570_.accept(Items.STONE_PICKAXE);
                        p_432570_.accept(Items.STONE_AXE);
                        p_432570_.accept(Items.STONE_HOE);
                        p_432570_.accept(Items.COPPER_SHOVEL);
                        p_432570_.accept(Items.COPPER_PICKAXE);
                        p_432570_.accept(Items.COPPER_AXE);
                        p_432570_.accept(Items.COPPER_HOE);
                        p_432570_.accept(Items.IRON_SHOVEL);
                        p_432570_.accept(Items.IRON_PICKAXE);
                        p_432570_.accept(Items.IRON_AXE);
                        p_432570_.accept(Items.IRON_HOE);
                        p_432570_.accept(Items.GOLDEN_SHOVEL);
                        p_432570_.accept(Items.GOLDEN_PICKAXE);
                        p_432570_.accept(Items.GOLDEN_AXE);
                        p_432570_.accept(Items.GOLDEN_HOE);
                        p_432570_.accept(Items.DIAMOND_SHOVEL);
                        p_432570_.accept(Items.DIAMOND_PICKAXE);
                        p_432570_.accept(Items.DIAMOND_AXE);
                        p_432570_.accept(Items.DIAMOND_HOE);
                        p_432570_.accept(Items.NETHERITE_SHOVEL);
                        p_432570_.accept(Items.NETHERITE_PICKAXE);
                        p_432570_.accept(Items.NETHERITE_AXE);
                        p_432570_.accept(Items.NETHERITE_HOE);
                        p_432570_.accept(Items.BUCKET);
                        p_432570_.accept(Items.WATER_BUCKET);
                        p_432570_.accept(Items.COD_BUCKET);
                        p_432570_.accept(Items.SALMON_BUCKET);
                        p_432570_.accept(Items.TROPICAL_FISH_BUCKET);
                        p_432570_.accept(Items.PUFFERFISH_BUCKET);
                        p_432570_.accept(Items.AXOLOTL_BUCKET);
                        p_432570_.accept(Items.TADPOLE_BUCKET);
                        p_432570_.accept(Items.LAVA_BUCKET);
                        p_432570_.accept(Items.POWDER_SNOW_BUCKET);
                        p_432570_.accept(Items.MILK_BUCKET);
                        p_432570_.accept(Items.FISHING_ROD);
                        p_432570_.accept(Items.FLINT_AND_STEEL);
                        p_432570_.accept(Items.FIRE_CHARGE);
                        p_432570_.accept(Items.BONE_MEAL);
                        p_432570_.accept(Items.SHEARS);
                        p_432570_.accept(Items.BRUSH);
                        p_432570_.accept(Items.NAME_TAG);
                        p_432570_.accept(Items.LEAD);
                        p_432570_.accept(Items.BUNDLE);
                        p_432570_.accept(Items.WHITE_BUNDLE);
                        p_432570_.accept(Items.LIGHT_GRAY_BUNDLE);
                        p_432570_.accept(Items.GRAY_BUNDLE);
                        p_432570_.accept(Items.BLACK_BUNDLE);
                        p_432570_.accept(Items.BROWN_BUNDLE);
                        p_432570_.accept(Items.RED_BUNDLE);
                        p_432570_.accept(Items.ORANGE_BUNDLE);
                        p_432570_.accept(Items.YELLOW_BUNDLE);
                        p_432570_.accept(Items.LIME_BUNDLE);
                        p_432570_.accept(Items.GREEN_BUNDLE);
                        p_432570_.accept(Items.CYAN_BUNDLE);
                        p_432570_.accept(Items.LIGHT_BLUE_BUNDLE);
                        p_432570_.accept(Items.BLUE_BUNDLE);
                        p_432570_.accept(Items.PURPLE_BUNDLE);
                        p_432570_.accept(Items.MAGENTA_BUNDLE);
                        p_432570_.accept(Items.PINK_BUNDLE);
                        p_432570_.accept(Items.COMPASS);
                        p_432570_.accept(Items.RECOVERY_COMPASS);
                        p_432570_.accept(Items.CLOCK);
                        p_432570_.accept(Items.SPYGLASS);
                        p_432570_.accept(Items.MAP);
                        p_432570_.accept(Items.WRITABLE_BOOK);
                        p_432570_.accept(Items.WIND_CHARGE);
                        p_432570_.accept(Items.ENDER_PEARL);
                        p_432570_.accept(Items.ENDER_EYE);
                        p_432570_.accept(Items.ELYTRA);
                        generateFireworksAllDurations(p_432570_, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                        p_432570_.accept(Items.SADDLE);
                        p_432570_.accept(Items.WHITE_HARNESS);
                        p_432570_.accept(Items.LIGHT_GRAY_HARNESS);
                        p_432570_.accept(Items.GRAY_HARNESS);
                        p_432570_.accept(Items.BLACK_HARNESS);
                        p_432570_.accept(Items.BROWN_HARNESS);
                        p_432570_.accept(Items.RED_HARNESS);
                        p_432570_.accept(Items.ORANGE_HARNESS);
                        p_432570_.accept(Items.YELLOW_HARNESS);
                        p_432570_.accept(Items.LIME_HARNESS);
                        p_432570_.accept(Items.GREEN_HARNESS);
                        p_432570_.accept(Items.CYAN_HARNESS);
                        p_432570_.accept(Items.LIGHT_BLUE_HARNESS);
                        p_432570_.accept(Items.BLUE_HARNESS);
                        p_432570_.accept(Items.PURPLE_HARNESS);
                        p_432570_.accept(Items.MAGENTA_HARNESS);
                        p_432570_.accept(Items.PINK_HARNESS);
                        p_432570_.accept(Items.CARROT_ON_A_STICK);
                        p_432570_.accept(Items.WARPED_FUNGUS_ON_A_STICK);
                        p_432570_.accept(Items.OAK_BOAT);
                        p_432570_.accept(Items.OAK_CHEST_BOAT);
                        p_432570_.accept(Items.SPRUCE_BOAT);
                        p_432570_.accept(Items.SPRUCE_CHEST_BOAT);
                        p_432570_.accept(Items.BIRCH_BOAT);
                        p_432570_.accept(Items.BIRCH_CHEST_BOAT);
                        p_432570_.accept(Items.JUNGLE_BOAT);
                        p_432570_.accept(Items.JUNGLE_CHEST_BOAT);
                        p_432570_.accept(Items.ACACIA_BOAT);
                        p_432570_.accept(Items.ACACIA_CHEST_BOAT);
                        p_432570_.accept(Items.DARK_OAK_BOAT);
                        p_432570_.accept(Items.DARK_OAK_CHEST_BOAT);
                        p_432570_.accept(Items.MANGROVE_BOAT);
                        p_432570_.accept(Items.MANGROVE_CHEST_BOAT);
                        p_432570_.accept(Items.CHERRY_BOAT);
                        p_432570_.accept(Items.CHERRY_CHEST_BOAT);
                        p_432570_.accept(Items.PALE_OAK_BOAT);
                        p_432570_.accept(Items.PALE_OAK_CHEST_BOAT);
                        p_432570_.accept(Items.BAMBOO_RAFT);
                        p_432570_.accept(Items.BAMBOO_CHEST_RAFT);
                        p_432570_.accept(Items.RAIL);
                        p_432570_.accept(Items.POWERED_RAIL);
                        p_432570_.accept(Items.DETECTOR_RAIL);
                        p_432570_.accept(Items.ACTIVATOR_RAIL);
                        p_432570_.accept(Items.MINECART);
                        p_432570_.accept(Items.HOPPER_MINECART);
                        p_432570_.accept(Items.CHEST_MINECART);
                        p_432570_.accept(Items.FURNACE_MINECART);
                        p_432570_.accept(Items.TNT_MINECART);
                        p_432569_.holders()
                            .lookup(Registries.INSTRUMENT)
                            .ifPresent(
                                p_270036_ -> generateInstrumentTypes(
                                    p_432570_, p_270036_, Items.GOAT_HORN, InstrumentTags.GOAT_HORNS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                                )
                            );
                        p_432570_.accept(Items.MUSIC_DISC_13);
                        p_432570_.accept(Items.MUSIC_DISC_CAT);
                        p_432570_.accept(Items.MUSIC_DISC_BLOCKS);
                        p_432570_.accept(Items.MUSIC_DISC_CHIRP);
                        p_432570_.accept(Items.MUSIC_DISC_FAR);
                        p_432570_.accept(Items.MUSIC_DISC_MALL);
                        p_432570_.accept(Items.MUSIC_DISC_MELLOHI);
                        p_432570_.accept(Items.MUSIC_DISC_STAL);
                        p_432570_.accept(Items.MUSIC_DISC_STRAD);
                        p_432570_.accept(Items.MUSIC_DISC_WARD);
                        p_432570_.accept(Items.MUSIC_DISC_11);
                        p_432570_.accept(Items.MUSIC_DISC_CREATOR_MUSIC_BOX);
                        p_432570_.accept(Items.MUSIC_DISC_WAIT);
                        p_432570_.accept(Items.MUSIC_DISC_CREATOR);
                        p_432570_.accept(Items.MUSIC_DISC_PRECIPICE);
                        p_432570_.accept(Items.MUSIC_DISC_OTHERSIDE);
                        p_432570_.accept(Items.MUSIC_DISC_RELIC);
                        p_432570_.accept(Items.MUSIC_DISC_5);
                        p_432570_.accept(Items.MUSIC_DISC_PIGSTEP);
                        p_432570_.accept(Items.MUSIC_DISC_TEARS);
                        p_432570_.accept(Items.MUSIC_DISC_LAVA_CHICKEN);
                    }
                )
                .build()
        );
        Registry.register(
            registry,
            COMBAT,
            CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 1)
                .title(Component.translatable("itemGroup.combat"))
                .icon(() -> new ItemStack(Items.NETHERITE_SWORD))
                .displayItems(
                    (p_432565_, p_432566_) -> {
                        p_432566_.accept(Items.WOODEN_SWORD);
                        p_432566_.accept(Items.STONE_SWORD);
                        p_432566_.accept(Items.COPPER_SWORD);
                        p_432566_.accept(Items.IRON_SWORD);
                        p_432566_.accept(Items.GOLDEN_SWORD);
                        p_432566_.accept(Items.DIAMOND_SWORD);
                        p_432566_.accept(Items.NETHERITE_SWORD);
                        p_432566_.accept(Items.WOODEN_AXE);
                        p_432566_.accept(Items.STONE_AXE);
                        p_432566_.accept(Items.COPPER_AXE);
                        p_432566_.accept(Items.IRON_AXE);
                        p_432566_.accept(Items.GOLDEN_AXE);
                        p_432566_.accept(Items.DIAMOND_AXE);
                        p_432566_.accept(Items.NETHERITE_AXE);
                        p_432566_.accept(Items.TRIDENT);
                        p_432566_.accept(Items.MACE);
                        p_432566_.accept(Items.SHIELD);
                        p_432566_.accept(Items.LEATHER_HELMET);
                        p_432566_.accept(Items.LEATHER_CHESTPLATE);
                        p_432566_.accept(Items.LEATHER_LEGGINGS);
                        p_432566_.accept(Items.LEATHER_BOOTS);
                        p_432566_.accept(Items.COPPER_HELMET);
                        p_432566_.accept(Items.COPPER_CHESTPLATE);
                        p_432566_.accept(Items.COPPER_LEGGINGS);
                        p_432566_.accept(Items.COPPER_BOOTS);
                        p_432566_.accept(Items.CHAINMAIL_HELMET);
                        p_432566_.accept(Items.CHAINMAIL_CHESTPLATE);
                        p_432566_.accept(Items.CHAINMAIL_LEGGINGS);
                        p_432566_.accept(Items.CHAINMAIL_BOOTS);
                        p_432566_.accept(Items.IRON_HELMET);
                        p_432566_.accept(Items.IRON_CHESTPLATE);
                        p_432566_.accept(Items.IRON_LEGGINGS);
                        p_432566_.accept(Items.IRON_BOOTS);
                        p_432566_.accept(Items.GOLDEN_HELMET);
                        p_432566_.accept(Items.GOLDEN_CHESTPLATE);
                        p_432566_.accept(Items.GOLDEN_LEGGINGS);
                        p_432566_.accept(Items.GOLDEN_BOOTS);
                        p_432566_.accept(Items.DIAMOND_HELMET);
                        p_432566_.accept(Items.DIAMOND_CHESTPLATE);
                        p_432566_.accept(Items.DIAMOND_LEGGINGS);
                        p_432566_.accept(Items.DIAMOND_BOOTS);
                        p_432566_.accept(Items.NETHERITE_HELMET);
                        p_432566_.accept(Items.NETHERITE_CHESTPLATE);
                        p_432566_.accept(Items.NETHERITE_LEGGINGS);
                        p_432566_.accept(Items.NETHERITE_BOOTS);
                        p_432566_.accept(Items.TURTLE_HELMET);
                        p_432566_.accept(Items.LEATHER_HORSE_ARMOR);
                        p_432566_.accept(Items.COPPER_HORSE_ARMOR);
                        p_432566_.accept(Items.IRON_HORSE_ARMOR);
                        p_432566_.accept(Items.GOLDEN_HORSE_ARMOR);
                        p_432566_.accept(Items.DIAMOND_HORSE_ARMOR);
                        p_432566_.accept(Items.WOLF_ARMOR);
                        p_432566_.accept(Items.TOTEM_OF_UNDYING);
                        p_432566_.accept(Items.TNT);
                        p_432566_.accept(Items.END_CRYSTAL);
                        p_432566_.accept(Items.SNOWBALL);
                        p_432566_.accept(Items.EGG);
                        p_432566_.accept(Items.BROWN_EGG);
                        p_432566_.accept(Items.BLUE_EGG);
                        p_432566_.accept(Items.WIND_CHARGE);
                        p_432566_.accept(Items.BOW);
                        p_432566_.accept(Items.CROSSBOW);
                        generateFireworksAllDurations(p_432566_, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                        p_432566_.accept(Items.ARROW);
                        p_432566_.accept(Items.SPECTRAL_ARROW);
                        p_432565_.holders()
                            .lookup(Registries.POTION)
                            .ifPresent(
                                p_337917_ -> generatePotionEffectTypes(
                                    p_432566_, p_337917_, Items.TIPPED_ARROW, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS, p_432565_.enabledFeatures()
                                )
                            );
                    }
                )
                .build()
        );
        Registry.register(
            registry,
            FOOD_AND_DRINKS,
            CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 2)
                .title(Component.translatable("itemGroup.foodAndDrink"))
                .icon(() -> new ItemStack(Items.GOLDEN_APPLE))
                .displayItems(
                    (p_337921_, p_337922_) -> {
                        p_337922_.accept(Items.APPLE);
                        p_337922_.accept(Items.GOLDEN_APPLE);
                        p_337922_.accept(Items.ENCHANTED_GOLDEN_APPLE);
                        p_337922_.accept(Items.MELON_SLICE);
                        p_337922_.accept(Items.SWEET_BERRIES);
                        p_337922_.accept(Items.GLOW_BERRIES);
                        p_337922_.accept(Items.CHORUS_FRUIT);
                        p_337922_.accept(Items.CARROT);
                        p_337922_.accept(Items.GOLDEN_CARROT);
                        p_337922_.accept(Items.POTATO);
                        p_337922_.accept(Items.BAKED_POTATO);
                        p_337922_.accept(Items.POISONOUS_POTATO);
                        p_337922_.accept(Items.BEETROOT);
                        p_337922_.accept(Items.DRIED_KELP);
                        p_337922_.accept(Items.BEEF);
                        p_337922_.accept(Items.COOKED_BEEF);
                        p_337922_.accept(Items.PORKCHOP);
                        p_337922_.accept(Items.COOKED_PORKCHOP);
                        p_337922_.accept(Items.MUTTON);
                        p_337922_.accept(Items.COOKED_MUTTON);
                        p_337922_.accept(Items.CHICKEN);
                        p_337922_.accept(Items.COOKED_CHICKEN);
                        p_337922_.accept(Items.RABBIT);
                        p_337922_.accept(Items.COOKED_RABBIT);
                        p_337922_.accept(Items.COD);
                        p_337922_.accept(Items.COOKED_COD);
                        p_337922_.accept(Items.SALMON);
                        p_337922_.accept(Items.COOKED_SALMON);
                        p_337922_.accept(Items.TROPICAL_FISH);
                        p_337922_.accept(Items.PUFFERFISH);
                        p_337922_.accept(Items.BREAD);
                        p_337922_.accept(Items.COOKIE);
                        p_337922_.accept(Items.CAKE);
                        p_337922_.accept(Items.PUMPKIN_PIE);
                        p_337922_.accept(Items.ROTTEN_FLESH);
                        p_337922_.accept(Items.SPIDER_EYE);
                        p_337922_.accept(Items.MUSHROOM_STEW);
                        p_337922_.accept(Items.BEETROOT_SOUP);
                        p_337922_.accept(Items.RABBIT_STEW);
                        generateSuspiciousStews(p_337922_, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                        p_337922_.accept(Items.MILK_BUCKET);
                        p_337922_.accept(Items.HONEY_BOTTLE);
                        generateOminousBottles(p_337922_, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                        p_337921_.holders()
                            .lookup(Registries.POTION)
                            .ifPresent(
                                p_337908_ -> {
                                    generatePotionEffectTypes(
                                        p_337922_, p_337908_, Items.POTION, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS, p_337921_.enabledFeatures()
                                    );
                                    generatePotionEffectTypes(
                                        p_337922_,
                                        p_337908_,
                                        Items.SPLASH_POTION,
                                        CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS,
                                        p_337921_.enabledFeatures()
                                    );
                                    generatePotionEffectTypes(
                                        p_337922_,
                                        p_337908_,
                                        Items.LINGERING_POTION,
                                        CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS,
                                        p_337921_.enabledFeatures()
                                    );
                                }
                            );
                    }
                )
                .build()
        );
        Registry.register(
            registry,
            INGREDIENTS,
            CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 3)
                .title(Component.translatable("itemGroup.ingredients"))
                .icon(() -> new ItemStack(Items.IRON_INGOT))
                .displayItems((p_442473_, p_442474_) -> {
                    p_442474_.accept(Items.COAL);
                    p_442474_.accept(Items.CHARCOAL);
                    p_442474_.accept(Items.RAW_COPPER);
                    p_442474_.accept(Items.RAW_IRON);
                    p_442474_.accept(Items.RAW_GOLD);
                    p_442474_.accept(Items.EMERALD);
                    p_442474_.accept(Items.LAPIS_LAZULI);
                    p_442474_.accept(Items.DIAMOND);
                    p_442474_.accept(Items.ANCIENT_DEBRIS);
                    p_442474_.accept(Items.QUARTZ);
                    p_442474_.accept(Items.AMETHYST_SHARD);
                    p_442474_.accept(Items.COPPER_NUGGET);
                    p_442474_.accept(Items.IRON_NUGGET);
                    p_442474_.accept(Items.GOLD_NUGGET);
                    p_442474_.accept(Items.COPPER_INGOT);
                    p_442474_.accept(Items.IRON_INGOT);
                    p_442474_.accept(Items.GOLD_INGOT);
                    p_442474_.accept(Items.NETHERITE_SCRAP);
                    p_442474_.accept(Items.NETHERITE_INGOT);
                    p_442474_.accept(Items.STICK);
                    p_442474_.accept(Items.FLINT);
                    p_442474_.accept(Items.WHEAT);
                    p_442474_.accept(Items.BONE);
                    p_442474_.accept(Items.BONE_MEAL);
                    p_442474_.accept(Items.STRING);
                    p_442474_.accept(Items.FEATHER);
                    p_442474_.accept(Items.SNOWBALL);
                    p_442474_.accept(Items.EGG);
                    p_442474_.accept(Items.BROWN_EGG);
                    p_442474_.accept(Items.BLUE_EGG);
                    p_442474_.accept(Items.LEATHER);
                    p_442474_.accept(Items.RABBIT_HIDE);
                    p_442474_.accept(Items.HONEYCOMB);
                    p_442474_.accept(Items.RESIN_CLUMP);
                    p_442474_.accept(Items.INK_SAC);
                    p_442474_.accept(Items.GLOW_INK_SAC);
                    p_442474_.accept(Items.TURTLE_SCUTE);
                    p_442474_.accept(Items.ARMADILLO_SCUTE);
                    p_442474_.accept(Items.SLIME_BALL);
                    p_442474_.accept(Items.CLAY_BALL);
                    p_442474_.accept(Items.PRISMARINE_SHARD);
                    p_442474_.accept(Items.PRISMARINE_CRYSTALS);
                    p_442474_.accept(Items.NAUTILUS_SHELL);
                    p_442474_.accept(Items.HEART_OF_THE_SEA);
                    p_442474_.accept(Items.FIRE_CHARGE);
                    p_442474_.accept(Items.BLAZE_ROD);
                    p_442474_.accept(Items.BREEZE_ROD);
                    p_442474_.accept(Items.HEAVY_CORE);
                    p_442474_.accept(Items.NETHER_STAR);
                    p_442474_.accept(Items.ENDER_PEARL);
                    p_442474_.accept(Items.ENDER_EYE);
                    p_442474_.accept(Items.SHULKER_SHELL);
                    p_442474_.accept(Items.POPPED_CHORUS_FRUIT);
                    p_442474_.accept(Items.ECHO_SHARD);
                    p_442474_.accept(Items.DISC_FRAGMENT_5);
                    p_442474_.accept(Items.WHITE_DYE);
                    p_442474_.accept(Items.LIGHT_GRAY_DYE);
                    p_442474_.accept(Items.GRAY_DYE);
                    p_442474_.accept(Items.BLACK_DYE);
                    p_442474_.accept(Items.BROWN_DYE);
                    p_442474_.accept(Items.RED_DYE);
                    p_442474_.accept(Items.ORANGE_DYE);
                    p_442474_.accept(Items.YELLOW_DYE);
                    p_442474_.accept(Items.LIME_DYE);
                    p_442474_.accept(Items.GREEN_DYE);
                    p_442474_.accept(Items.CYAN_DYE);
                    p_442474_.accept(Items.LIGHT_BLUE_DYE);
                    p_442474_.accept(Items.BLUE_DYE);
                    p_442474_.accept(Items.PURPLE_DYE);
                    p_442474_.accept(Items.MAGENTA_DYE);
                    p_442474_.accept(Items.PINK_DYE);
                    p_442474_.accept(Items.BOWL);
                    p_442474_.accept(Items.BRICK);
                    p_442474_.accept(Items.NETHER_BRICK);
                    p_442474_.accept(Items.RESIN_BRICK);
                    p_442474_.accept(Items.PAPER);
                    p_442474_.accept(Items.BOOK);
                    p_442474_.accept(Items.FIREWORK_STAR);
                    p_442474_.accept(Items.GLASS_BOTTLE);
                    p_442474_.accept(Items.NETHER_WART);
                    p_442474_.accept(Items.REDSTONE);
                    p_442474_.accept(Items.GLOWSTONE_DUST);
                    p_442474_.accept(Items.GUNPOWDER);
                    p_442474_.accept(Items.DRAGON_BREATH);
                    p_442474_.accept(Items.FERMENTED_SPIDER_EYE);
                    p_442474_.accept(Items.BLAZE_POWDER);
                    p_442474_.accept(Items.SUGAR);
                    p_442474_.accept(Items.RABBIT_FOOT);
                    p_442474_.accept(Items.GLISTERING_MELON_SLICE);
                    p_442474_.accept(Items.SPIDER_EYE);
                    p_442474_.accept(Items.PUFFERFISH);
                    p_442474_.accept(Items.MAGMA_CREAM);
                    p_442474_.accept(Items.GOLDEN_CARROT);
                    p_442474_.accept(Items.GHAST_TEAR);
                    p_442474_.accept(Items.TURTLE_HELMET);
                    p_442474_.accept(Items.PHANTOM_MEMBRANE);
                    p_442474_.accept(Items.FIELD_MASONED_BANNER_PATTERN);
                    p_442474_.accept(Items.BORDURE_INDENTED_BANNER_PATTERN);
                    p_442474_.accept(Items.FLOWER_BANNER_PATTERN);
                    p_442474_.accept(Items.CREEPER_BANNER_PATTERN);
                    p_442474_.accept(Items.SKULL_BANNER_PATTERN);
                    p_442474_.accept(Items.MOJANG_BANNER_PATTERN);
                    p_442474_.accept(Items.GLOBE_BANNER_PATTERN);
                    p_442474_.accept(Items.PIGLIN_BANNER_PATTERN);
                    p_442474_.accept(Items.FLOW_BANNER_PATTERN);
                    p_442474_.accept(Items.GUSTER_BANNER_PATTERN);
                    p_442474_.accept(Items.ANGLER_POTTERY_SHERD);
                    p_442474_.accept(Items.ARCHER_POTTERY_SHERD);
                    p_442474_.accept(Items.ARMS_UP_POTTERY_SHERD);
                    p_442474_.accept(Items.BLADE_POTTERY_SHERD);
                    p_442474_.accept(Items.BREWER_POTTERY_SHERD);
                    p_442474_.accept(Items.BURN_POTTERY_SHERD);
                    p_442474_.accept(Items.DANGER_POTTERY_SHERD);
                    p_442474_.accept(Items.EXPLORER_POTTERY_SHERD);
                    p_442474_.accept(Items.FLOW_POTTERY_SHERD);
                    p_442474_.accept(Items.FRIEND_POTTERY_SHERD);
                    p_442474_.accept(Items.GUSTER_POTTERY_SHERD);
                    p_442474_.accept(Items.HEART_POTTERY_SHERD);
                    p_442474_.accept(Items.HEARTBREAK_POTTERY_SHERD);
                    p_442474_.accept(Items.HOWL_POTTERY_SHERD);
                    p_442474_.accept(Items.MINER_POTTERY_SHERD);
                    p_442474_.accept(Items.MOURNER_POTTERY_SHERD);
                    p_442474_.accept(Items.PLENTY_POTTERY_SHERD);
                    p_442474_.accept(Items.PRIZE_POTTERY_SHERD);
                    p_442474_.accept(Items.SCRAPE_POTTERY_SHERD);
                    p_442474_.accept(Items.SHEAF_POTTERY_SHERD);
                    p_442474_.accept(Items.SHELTER_POTTERY_SHERD);
                    p_442474_.accept(Items.SKULL_POTTERY_SHERD);
                    p_442474_.accept(Items.SNORT_POTTERY_SHERD);
                    p_442474_.accept(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
                    p_442474_.accept(Items.EXPERIENCE_BOTTLE);
                    p_442474_.accept(Items.TRIAL_KEY);
                    p_442474_.accept(Items.OMINOUS_TRIAL_KEY);
                    p_442473_.holders().lookup(Registries.ENCHANTMENT).ifPresent(p_350153_ -> {
                        generateEnchantmentBookTypesOnlyMaxLevel(p_442474_, p_350153_, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                        generateEnchantmentBookTypesAllLevels(p_442474_, p_350153_, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
                    });
                })
                .build()
        );
        Registry.register(
            registry,
            SPAWN_EGGS,
            CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 4)
                .title(Component.translatable("itemGroup.spawnEggs"))
                .icon(() -> new ItemStack(Items.CREEPER_SPAWN_EGG))
                .displayItems((p_438852_, p_438853_) -> {
                    p_438853_.accept(Items.SPAWNER);
                    p_438853_.accept(Items.TRIAL_SPAWNER);
                    p_438853_.accept(Items.CREAKING_HEART);
                    p_438853_.accept(Items.ALLAY_SPAWN_EGG);
                    p_438853_.accept(Items.ARMADILLO_SPAWN_EGG);
                    p_438853_.accept(Items.AXOLOTL_SPAWN_EGG);
                    p_438853_.accept(Items.BAT_SPAWN_EGG);
                    p_438853_.accept(Items.BEE_SPAWN_EGG);
                    p_438853_.accept(Items.BLAZE_SPAWN_EGG);
                    p_438853_.accept(Items.BOGGED_SPAWN_EGG);
                    p_438853_.accept(Items.BREEZE_SPAWN_EGG);
                    p_438853_.accept(Items.CAMEL_SPAWN_EGG);
                    p_438853_.accept(Items.CAT_SPAWN_EGG);
                    p_438853_.accept(Items.CAVE_SPIDER_SPAWN_EGG);
                    p_438853_.accept(Items.CHICKEN_SPAWN_EGG);
                    p_438853_.accept(Items.COD_SPAWN_EGG);
                    p_438853_.accept(Items.COPPER_GOLEM_SPAWN_EGG);
                    p_438853_.accept(Items.COW_SPAWN_EGG);
                    p_438853_.accept(Items.CREAKING_SPAWN_EGG);
                    p_438853_.accept(Items.CREEPER_SPAWN_EGG);
                    p_438853_.accept(Items.DOLPHIN_SPAWN_EGG);
                    p_438853_.accept(Items.DONKEY_SPAWN_EGG);
                    p_438853_.accept(Items.DROWNED_SPAWN_EGG);
                    p_438853_.accept(Items.ELDER_GUARDIAN_SPAWN_EGG);
                    p_438853_.accept(Items.ENDERMAN_SPAWN_EGG);
                    p_438853_.accept(Items.ENDERMITE_SPAWN_EGG);
                    p_438853_.accept(Items.EVOKER_SPAWN_EGG);
                    p_438853_.accept(Items.FOX_SPAWN_EGG);
                    p_438853_.accept(Items.FROG_SPAWN_EGG);
                    p_438853_.accept(Items.GHAST_SPAWN_EGG);
                    p_438853_.accept(Items.GLOW_SQUID_SPAWN_EGG);
                    p_438853_.accept(Items.GOAT_SPAWN_EGG);
                    p_438853_.accept(Items.GUARDIAN_SPAWN_EGG);
                    p_438853_.accept(Items.HAPPY_GHAST_SPAWN_EGG);
                    p_438853_.accept(Items.HOGLIN_SPAWN_EGG);
                    p_438853_.accept(Items.HORSE_SPAWN_EGG);
                    p_438853_.accept(Items.HUSK_SPAWN_EGG);
                    p_438853_.accept(Items.IRON_GOLEM_SPAWN_EGG);
                    p_438853_.accept(Items.LLAMA_SPAWN_EGG);
                    p_438853_.accept(Items.MAGMA_CUBE_SPAWN_EGG);
                    p_438853_.accept(Items.MOOSHROOM_SPAWN_EGG);
                    p_438853_.accept(Items.MULE_SPAWN_EGG);
                    p_438853_.accept(Items.OCELOT_SPAWN_EGG);
                    p_438853_.accept(Items.PANDA_SPAWN_EGG);
                    p_438853_.accept(Items.PARROT_SPAWN_EGG);
                    p_438853_.accept(Items.PHANTOM_SPAWN_EGG);
                    p_438853_.accept(Items.PIG_SPAWN_EGG);
                    p_438853_.accept(Items.PIGLIN_SPAWN_EGG);
                    p_438853_.accept(Items.PIGLIN_BRUTE_SPAWN_EGG);
                    p_438853_.accept(Items.PILLAGER_SPAWN_EGG);
                    p_438853_.accept(Items.POLAR_BEAR_SPAWN_EGG);
                    p_438853_.accept(Items.PUFFERFISH_SPAWN_EGG);
                    p_438853_.accept(Items.RABBIT_SPAWN_EGG);
                    p_438853_.accept(Items.RAVAGER_SPAWN_EGG);
                    p_438853_.accept(Items.SALMON_SPAWN_EGG);
                    p_438853_.accept(Items.SHEEP_SPAWN_EGG);
                    p_438853_.accept(Items.SHULKER_SPAWN_EGG);
                    p_438853_.accept(Items.SILVERFISH_SPAWN_EGG);
                    p_438853_.accept(Items.SKELETON_SPAWN_EGG);
                    p_438853_.accept(Items.SKELETON_HORSE_SPAWN_EGG);
                    p_438853_.accept(Items.SLIME_SPAWN_EGG);
                    p_438853_.accept(Items.SNIFFER_SPAWN_EGG);
                    p_438853_.accept(Items.SNOW_GOLEM_SPAWN_EGG);
                    p_438853_.accept(Items.SPIDER_SPAWN_EGG);
                    p_438853_.accept(Items.SQUID_SPAWN_EGG);
                    p_438853_.accept(Items.STRAY_SPAWN_EGG);
                    p_438853_.accept(Items.STRIDER_SPAWN_EGG);
                    p_438853_.accept(Items.TADPOLE_SPAWN_EGG);
                    p_438853_.accept(Items.TRADER_LLAMA_SPAWN_EGG);
                    p_438853_.accept(Items.TROPICAL_FISH_SPAWN_EGG);
                    p_438853_.accept(Items.TURTLE_SPAWN_EGG);
                    p_438853_.accept(Items.VEX_SPAWN_EGG);
                    p_438853_.accept(Items.VILLAGER_SPAWN_EGG);
                    p_438853_.accept(Items.VINDICATOR_SPAWN_EGG);
                    p_438853_.accept(Items.WANDERING_TRADER_SPAWN_EGG);
                    p_438853_.accept(Items.WARDEN_SPAWN_EGG);
                    p_438853_.accept(Items.WITCH_SPAWN_EGG);
                    p_438853_.accept(Items.WITHER_SKELETON_SPAWN_EGG);
                    p_438853_.accept(Items.WOLF_SPAWN_EGG);
                    p_438853_.accept(Items.ZOGLIN_SPAWN_EGG);
                    p_438853_.accept(Items.ZOMBIE_SPAWN_EGG);
                    p_438853_.accept(Items.ZOMBIE_HORSE_SPAWN_EGG);
                    p_438853_.accept(Items.ZOMBIE_VILLAGER_SPAWN_EGG);
                    p_438853_.accept(Items.ZOMBIFIED_PIGLIN_SPAWN_EGG);
                })
                .build()
        );
        Registry.register(
            registry,
            OP_BLOCKS,
            CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 5)
                .title(Component.translatable("itemGroup.op"))
                .icon(() -> new ItemStack(Items.COMMAND_BLOCK))
                .alignedRight()
                .displayItems(
                    (p_396887_, p_396888_) -> {
                        if (p_396887_.hasPermissions()) {
                            p_396888_.accept(Items.COMMAND_BLOCK);
                            p_396888_.accept(Items.CHAIN_COMMAND_BLOCK);
                            p_396888_.accept(Items.REPEATING_COMMAND_BLOCK);
                            p_396888_.accept(Items.COMMAND_BLOCK_MINECART);
                            p_396888_.accept(Items.JIGSAW);
                            p_396888_.accept(Items.STRUCTURE_BLOCK);
                            p_396888_.accept(Items.STRUCTURE_VOID);
                            p_396888_.accept(Items.BARRIER);
                            p_396888_.accept(Items.DEBUG_STICK);
                            p_396888_.accept(Items.TEST_INSTANCE_BLOCK);

                            for (TestBlockMode testblockmode : TestBlockMode.values()) {
                                p_396888_.accept(TestBlock.setModeOnStack(new ItemStack(Items.TEST_BLOCK), testblockmode));
                            }

                            for (int i = 15; i >= 0; i--) {
                                p_396888_.accept(LightBlock.setLightOnStack(new ItemStack(Items.LIGHT), i));
                            }

                            p_396887_.holders()
                                .lookup(Registries.PAINTING_VARIANT)
                                .ifPresent(
                                    p_344383_ -> generatePresetPaintings(
                                        p_396888_,
                                        p_396887_.holders(),
                                        (HolderLookup.RegistryLookup<PaintingVariant>)p_344383_,
                                        p_270003_ -> !p_270003_.is(PaintingVariantTags.PLACEABLE),
                                        CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                                    )
                                );
                        }
                    }
                )
                .build()
        );
        return Registry.register(
            registry,
            INVENTORY,
            CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 6)
                .title(Component.translatable("itemGroup.inventory"))
                .icon(() -> new ItemStack(Blocks.CHEST))
                .backgroundTexture(INVENTORY_BACKGROUND)
                .hideTitle()
                .alignedRight()
                .type(CreativeModeTab.Type.INVENTORY)
                .noScrollBar()
                .build()
        );
    }

    public static void validate() {
        Map<Pair<CreativeModeTab.Row, Integer>, String> map = new HashMap<>();

        for (ResourceKey<CreativeModeTab> resourcekey : BuiltInRegistries.CREATIVE_MODE_TAB.registryKeySet()) {
            CreativeModeTab creativemodetab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(resourcekey);
            String s = creativemodetab.getDisplayName().getString();
            String s1 = map.put(Pair.of(creativemodetab.row(), creativemodetab.column()), s);
            if (s1 != null) {
                throw new IllegalArgumentException("Duplicate position: " + s + " vs. " + s1);
            }
        }
    }

    public static CreativeModeTab getDefaultTab() {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(BUILDING_BLOCKS);
    }

    private static void generatePotionEffectTypes(
        CreativeModeTab.Output output, HolderLookup<Potion> potions, Item item, CreativeModeTab.TabVisibility tabVisibility, FeatureFlagSet requiredFeatures
    ) {
        potions.listElements()
            .filter(p_337926_ -> p_337926_.value().isEnabled(requiredFeatures))
            .map(p_330083_ -> PotionContents.createItemStack(item, p_330083_))
            .forEach(p_270000_ -> output.accept(p_270000_, tabVisibility));
    }

    private static void generateEnchantmentBookTypesOnlyMaxLevel(
        CreativeModeTab.Output output, HolderLookup<Enchantment> enchantments, CreativeModeTab.TabVisibility tabVisibility
    ) {
        enchantments.listElements()
            .map(p_360016_ -> EnchantmentHelper.createBook(new EnchantmentInstance(p_360016_, p_360016_.value().getMaxLevel())))
            .forEach(p_269989_ -> output.accept(p_269989_, tabVisibility));
    }

    private static void generateEnchantmentBookTypesAllLevels(
        CreativeModeTab.Output output, HolderLookup<Enchantment> enchantments, CreativeModeTab.TabVisibility tabVisibility
    ) {
        enchantments.listElements()
            .flatMap(
                p_344377_ -> IntStream.rangeClosed(p_344377_.value().getMinLevel(), p_344377_.value().getMaxLevel())
                    .mapToObj(p_360015_ -> EnchantmentHelper.createBook(new EnchantmentInstance(p_344377_, p_360015_)))
            )
            .forEach(p_270017_ -> output.accept(p_270017_, tabVisibility));
    }

    private static void generateInstrumentTypes(
        CreativeModeTab.Output output,
        HolderLookup<Instrument> instruments,
        Item item,
        TagKey<Instrument> instrument,
        CreativeModeTab.TabVisibility tabVisibility
    ) {
        instruments.get(instrument)
            .ifPresent(
                p_270021_ -> p_270021_.stream()
                    .map(p_269995_ -> InstrumentItem.create(item, (Holder<Instrument>)p_269995_))
                    .forEach(p_270011_ -> output.accept(p_270011_, tabVisibility))
            );
    }

    private static void generateSuspiciousStews(CreativeModeTab.Output output, CreativeModeTab.TabVisibility tabVisibility) {
        List<SuspiciousEffectHolder> list = SuspiciousEffectHolder.getAllEffectHolders();
        Set<ItemStack> set = ItemStackLinkedSet.createTypeAndComponentsSet();

        for (SuspiciousEffectHolder suspiciouseffectholder : list) {
            ItemStack itemstack = new ItemStack(Items.SUSPICIOUS_STEW);
            itemstack.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, suspiciouseffectholder.getSuspiciousEffects());
            set.add(itemstack);
        }

        output.acceptAll(set, tabVisibility);
    }

    private static void generateOminousBottles(CreativeModeTab.Output output, CreativeModeTab.TabVisibility tabVisibility) {
        for (int i = 0; i <= 4; i++) {
            ItemStack itemstack = new ItemStack(Items.OMINOUS_BOTTLE);
            itemstack.set(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(i));
            output.accept(itemstack, tabVisibility);
        }
    }

    private static void generateFireworksAllDurations(CreativeModeTab.Output output, CreativeModeTab.TabVisibility tabVisibility) {
        for (byte b0 : FireworkRocketItem.CRAFTABLE_DURATIONS) {
            ItemStack itemstack = new ItemStack(Items.FIREWORK_ROCKET);
            itemstack.set(DataComponents.FIREWORKS, new Fireworks(b0, List.of()));
            output.accept(itemstack, tabVisibility);
        }
    }

    private static void generatePresetPaintings(
        CreativeModeTab.Output output,
        HolderLookup.Provider registries,
        HolderLookup.RegistryLookup<PaintingVariant> paintingVariants,
        Predicate<Holder<PaintingVariant>> predicate,
        CreativeModeTab.TabVisibility tabVisibility
    ) {
        RegistryOps<Tag> registryops = registries.createSerializationContext(NbtOps.INSTANCE);
        paintingVariants.listElements().filter(predicate).sorted(PAINTING_COMPARATOR).forEach(p_396886_ -> {
            ItemStack itemstack = new ItemStack(Items.PAINTING);
            itemstack.set(DataComponents.PAINTING_VARIANT, p_396886_);
            output.accept(itemstack, tabVisibility);
        });
    }

    public static List<CreativeModeTab> tabs() {
        return streamAllTabs().filter(CreativeModeTab::shouldDisplay).toList();
    }

    public static List<CreativeModeTab> allTabs() {
        return streamAllTabs().toList();
    }

    private static Stream<CreativeModeTab> streamAllTabs() {
        return BuiltInRegistries.CREATIVE_MODE_TAB.stream();
    }

    public static CreativeModeTab searchTab() {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(SEARCH);
    }

    private static void buildAllTabContents(CreativeModeTab.ItemDisplayParameters parameters) {
        streamAllTabs().filter(p_259647_ -> p_259647_.getType() == CreativeModeTab.Type.CATEGORY).forEach(p_269997_ -> p_269997_.buildContents(parameters));
        streamAllTabs().filter(p_260124_ -> p_260124_.getType() != CreativeModeTab.Type.CATEGORY).forEach(p_270002_ -> p_270002_.buildContents(parameters));
    }

    public static boolean tryRebuildTabContents(FeatureFlagSet enabledFeatures, boolean hasPermissions, HolderLookup.Provider registries) {
        if (CACHED_PARAMETERS != null && !CACHED_PARAMETERS.needsUpdate(enabledFeatures, hasPermissions, registries)) {
            return false;
        } else {
            CACHED_PARAMETERS = new CreativeModeTab.ItemDisplayParameters(enabledFeatures, hasPermissions, registries);
            buildAllTabContents(CACHED_PARAMETERS);
            return true;
        }
    }
}
