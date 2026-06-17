// file: src/main/java/net/goui/cosmicdungeon/block/ModBlocks.java
package net.goui.cosmicdungeon.block;
// add this import near the top:
import net.minecraft.sounds.SoundEvents;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.amethyst.ColoredAmethystBlock;
import net.goui.cosmicdungeon.block.amethyst.ColoredBudBlock;
import net.goui.cosmicdungeon.block.amethyst.ColoredBuddingAmethystBlock;
import net.goui.cosmicdungeon.block.amethyst.ColoredClusterBlock;
import net.goui.cosmicdungeon.block.amethyst.LitColoredAmethystBlock;
import net.goui.cosmicdungeon.block.custom.BarrierBlock;
import net.goui.cosmicdungeon.block.custom.ChickenBlock;
import net.goui.cosmicdungeon.block.custom.CavernResidueBlock;
import net.goui.cosmicdungeon.block.custom.ClassSelectorBlock;
import net.goui.cosmicdungeon.block.custom.CosmicMobSpawnerBlock;
import net.goui.cosmicdungeon.block.custom.CosmicRiftPlacerBlock;
import net.goui.cosmicdungeon.block.custom.CosmicRiftTileBlock;
import net.goui.cosmicdungeon.block.custom.InfiniteDispenserBlock;
import net.goui.cosmicdungeon.block.custom.LesserBloomBlock;

import net.goui.cosmicdungeon.block.custom.SpectralBloomBlock;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.redstone.rf.RedstoneReceiverBlock;
import net.goui.cosmicdungeon.redstone.rf.RedstoneTransmitterBlock;
import net.goui.cosmicdungeon.sound.ModSoundTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.goui.cosmicdungeon.block.custom.ClassLockedChestBlock;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CosmicDungeonMod.MOD_ID);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    // ===== Region Look: Ghost block for client-side rendering =====
    // No BlockItem on purpose: this is a visual-only helper block.
    public static final DeferredBlock<Block> REGION_GHOST_GLASS = BLOCKS.registerBlock(
            "region_ghost_glass",
            (BlockBehaviour.Properties props) -> new Block(
                    props
                            .strength(0.3F)
                            .sound(SoundType.GLASS)
                            .noCollision()
                            .isViewBlocking((state, level, pos) -> false)
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
            )
    );

    public static final DeferredBlock<Block> BARRIER_BLOCK = BLOCKS.registerBlock(
            "barrier_block",
            (BlockBehaviour.Properties props) -> new BarrierBlock(
                    props
                            .strength(-1.0F, 3_600_000.0F)
                            .sound(SoundType.STONE)
                            .mapColor(MapColor.NONE)
                            .noOcclusion()
                            .noLootTable()
            )
    );

    public static final DeferredBlock<Block> CHICKEN_BLOCK = BLOCKS.registerBlock(
            "chicken_block",
            (BlockBehaviour.Properties props) -> new ChickenBlock(
                    props.strength(1.0f).sound(ModSoundTypes.CHICKEN)
            )
    );

    public static final DeferredBlock<Block> PILE_OF_BOOKS = BLOCKS.registerBlock(
            "pile_of_books",
            (BlockBehaviour.Properties props) -> new Block(
                    props.strength(0.8F).sound(SoundType.WOOD).noOcclusion()
            )
    );

    public static final DeferredBlock<Block> CAVERN_RESIDUE = BLOCKS.registerBlock(
            "cavern_residue",
            (BlockBehaviour.Properties props) -> new CavernResidueBlock(
                    props.strength(0.4F, 0.4F).sound(SoundType.GRAVEL).noOcclusion()
            )
    );

    // ===== Class Selector Block (custom block opens GUI) =====
    public static final DeferredBlock<Block> CLASS_SELECTOR_BLOCK = BLOCKS.registerBlock(
            "class_selector_block",
            (BlockBehaviour.Properties props) -> new ClassSelectorBlock(
                    props.strength(0.8F).sound(SoundType.WOOD).noOcclusion()
            )
    );
    /* ---------- Class Locked Chests (8 class variants) ---------- */

    private static BlockBehaviour.Properties classChestProps(BlockBehaviour.Properties props) {
        return props
                .mapColor(MapColor.WOOD)
                .strength(2.5F)
                .sound(SoundType.WOOD)
                .noOcclusion();
    }

    public static final DeferredBlock<Block> BOGATYR_CHEST = BLOCKS.registerBlock(
            "bogatyr_chest",
            props -> new ClassLockedChestBlock(classChestProps(props), ClassKeys.CLASS_ID_BOGATYR, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE)
    );

    public static final DeferredBlock<Block> DEADEYE_CHEST = BLOCKS.registerBlock(
            "deadeye_chest",
            props -> new ClassLockedChestBlock(classChestProps(props), ClassKeys.CLASS_ID_DEADEYE, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE)
    );

    public static final DeferredBlock<Block> DRAGOON_CHEST = BLOCKS.registerBlock(
            "dragoon_chest",
            props -> new ClassLockedChestBlock(classChestProps(props), ClassKeys.CLASS_ID_DRAGOON, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE)
    );

    public static final DeferredBlock<Block> JUDICATOR_CHEST = BLOCKS.registerBlock(
            "judicator_chest",
            props -> new ClassLockedChestBlock(classChestProps(props), ClassKeys.CLASS_ID_JUDICATOR, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE)
    );

    public static final DeferredBlock<Block> METALMANCER_CHEST = BLOCKS.registerBlock(
            "metalmancer_chest",
            props -> new ClassLockedChestBlock(classChestProps(props), ClassKeys.CLASS_ID_METALMANCER, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE)
    );

    public static final DeferredBlock<Block> PYROCLAST_CHEST = BLOCKS.registerBlock(
            "pyroclast_chest",
            props -> new ClassLockedChestBlock(classChestProps(props), ClassKeys.CLASS_ID_PYROCLAST, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE)
    );

    public static final DeferredBlock<Block> THEURGIST_CHEST = BLOCKS.registerBlock(
            "theurgist_chest",
            props -> new ClassLockedChestBlock(classChestProps(props), ClassKeys.CLASS_ID_THEURGIST, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE)
    );

    public static final DeferredBlock<Block> VENEFEX_CHEST = BLOCKS.registerBlock(
            "venefex_chest",
            props -> new ClassLockedChestBlock(classChestProps(props), ClassKeys.CLASS_ID_VENEFEX, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE)
    );


    public static final DeferredBlock<Block> INFINITE_DISPENSER = BLOCKS.registerBlock(
            "infinite_dispenser",
            (BlockBehaviour.Properties props) -> new InfiniteDispenserBlock(
                    props
                            .mapColor(MapColor.STONE)
                            .strength(3.5F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)
            )
    );

    public static final DeferredBlock<Block> REDSTONE_TRANSMITTER = BLOCKS.registerBlock(
            "redstone_transmitter",
            (BlockBehaviour.Properties props) -> new RedstoneTransmitterBlock(
                    props
                            .mapColor(MapColor.STONE)
                            .strength(1.5F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            )
    );

    public static final DeferredBlock<Block> REDSTONE_RECEIVER = BLOCKS.registerBlock(
            "redstone_receiver",
            (BlockBehaviour.Properties props) -> new RedstoneReceiverBlock(
                    props
                            .mapColor(MapColor.METAL)
                            .strength(0.5F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            )
    );

    /* ---------- Cosmic Mob Spawner ---------- */

    public static final DeferredBlock<Block> COSMIC_MOB_SPAWNER = BLOCKS.registerBlock(
            "cosmic_mob_spawner",
            (BlockBehaviour.Properties props) -> new CosmicMobSpawnerBlock(
                    props
                            .strength(3.5F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.SPAWNER)
                            .lightLevel(state -> 8)
                            .noOcclusion()
            )
    );

    /* ---------- Cosmic Rift (placer + single tile) ---------- */

    public static final DeferredBlock<Block> COSMIC_RIFT = BLOCKS.registerBlock(
            "cosmic_rift",
            (BlockBehaviour.Properties props) -> new CosmicRiftPlacerBlock(
                    props.strength(1.0F).noOcclusion().sound(SoundType.BASALT)
            )
    );

    private static BlockBehaviour.Properties riftTileProps(BlockBehaviour.Properties props) {
        return props.strength(1.0F).noOcclusion().sound(SoundType.BASALT);
    }

    public static final DeferredBlock<Block> COSMIC_RIFT_TILE =
            BLOCKS.registerBlock("cosmic_rift_tile", p -> new CosmicRiftTileBlock(riftTileProps(p)));

    /* ---------- Dungeon 1: Spectral Blooms ---------- */

    private static BlockBehaviour.Properties spectralBloomProps(BlockBehaviour.Properties props) {
        return props
                .noCollision()
                .instabreak()
                .sound(SoundType.GRASS)
                .noOcclusion()
                .lightLevel(s -> 15); // legendary glow
    }

    public static final DeferredBlock<Block> BLOOM_OF_QUIET_ASSURANCE = BLOCKS.registerBlock(
            "bloom_of_quiet_assurance",
            props -> new SpectralBloomBlock(spectralBloomProps(props))
    );

    public static final DeferredBlock<Block> BLOOM_OF_GENTLE_LIES = BLOCKS.registerBlock(
            "bloom_of_gentle_lies",
            props -> new SpectralBloomBlock(spectralBloomProps(props))
    );

    public static final DeferredBlock<Block> BLOOM_OF_WANING_MERCY = BLOCKS.registerBlock(
            "bloom_of_waning_mercy",
            props -> new SpectralBloomBlock(spectralBloomProps(props))
    );

    public static final DeferredBlock<Block> BLOOM_OF_CONSTRICTING_BONDS = BLOCKS.registerBlock(
            "bloom_of_constricting_bonds",
            props -> new SpectralBloomBlock(spectralBloomProps(props))
    );

    public static final DeferredBlock<Block> BLOOM_OF_UNSPOKEN_RESIGNATION = BLOCKS.registerBlock(
            "bloom_of_unspoken_resignation",
            props -> new SpectralBloomBlock(spectralBloomProps(props))
    );

    public static final DeferredBlock<Block> BLOOM_OF_ELEGY = BLOCKS.registerBlock(
            "bloom_of_elegy",
            props -> new SpectralBloomBlock(spectralBloomProps(props))
    );

    public static final DeferredBlock<Block> LESSER_BLOOM = BLOCKS.registerBlock(
            "lesser_bloom",
            props -> new LesserBloomBlock(props.noCollision().instabreak().sound(SoundType.GRASS).noOcclusion())
    );

    // POTTED variants (no item). These MUST exist for flower pots.
    public static final DeferredBlock<Block> POTTED_LESSER_BLOOM = BLOCKS.registerBlock(
            "potted_lesser_bloom",
            props -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, LESSER_BLOOM, props)
    );

    public static final DeferredBlock<Block> POTTED_BLOOM_OF_QUIET_ASSURANCE = BLOCKS.registerBlock(
            "potted_bloom_of_quiet_assurance",
            props -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, BLOOM_OF_QUIET_ASSURANCE, props)
    );

    public static final DeferredBlock<Block> POTTED_BLOOM_OF_GENTLE_LIES = BLOCKS.registerBlock(
            "potted_bloom_of_gentle_lies",
            props -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, BLOOM_OF_GENTLE_LIES, props)
    );

    public static final DeferredBlock<Block> POTTED_BLOOM_OF_WANING_MERCY = BLOCKS.registerBlock(
            "potted_bloom_of_waning_mercy",
            props -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, BLOOM_OF_WANING_MERCY, props)
    );

    public static final DeferredBlock<Block> POTTED_BLOOM_OF_CONSTRICTING_BONDS = BLOCKS.registerBlock(
            "potted_bloom_of_constricting_bonds",
            props -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, BLOOM_OF_CONSTRICTING_BONDS, props)
    );

    public static final DeferredBlock<Block> POTTED_BLOOM_OF_UNSPOKEN_RESIGNATION = BLOCKS.registerBlock(
            "potted_bloom_of_unspoken_resignation",
            props -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, BLOOM_OF_UNSPOKEN_RESIGNATION, props)
    );

    public static final DeferredBlock<Block> POTTED_BLOOM_OF_ELEGY = BLOCKS.registerBlock(
            "potted_bloom_of_elegy",
            props -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, BLOOM_OF_ELEGY, props)
    );

    /**
     * Call this during common setup (enqueueWork) AFTER registries are ready.
     * This registers the plant -> potted mapping in the vanilla flower pot.
     */
    public static void registerFlowerPots() {
        FlowerPotBlock pot = (FlowerPotBlock) Blocks.FLOWER_POT;

        pot.addPlant(LESSER_BLOOM.getId(), POTTED_LESSER_BLOOM);
        pot.addPlant(BLOOM_OF_QUIET_ASSURANCE.getId(), POTTED_BLOOM_OF_QUIET_ASSURANCE);
        pot.addPlant(BLOOM_OF_GENTLE_LIES.getId(), POTTED_BLOOM_OF_GENTLE_LIES);
        pot.addPlant(BLOOM_OF_WANING_MERCY.getId(), POTTED_BLOOM_OF_WANING_MERCY);
        pot.addPlant(BLOOM_OF_CONSTRICTING_BONDS.getId(), POTTED_BLOOM_OF_CONSTRICTING_BONDS);
        pot.addPlant(BLOOM_OF_UNSPOKEN_RESIGNATION.getId(), POTTED_BLOOM_OF_UNSPOKEN_RESIGNATION);
        pot.addPlant(BLOOM_OF_ELEGY.getId(), POTTED_BLOOM_OF_ELEGY);
    }


    private static BlockBehaviour.Properties dungeonBuildingStoneProps(BlockBehaviour.Properties props) {
        return props.mapColor(MapColor.STONE).strength(1.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE);
    }

    public static final java.util.Map<String, DeferredBlock<Block>> DUNGEON_BUILDING_BLOCKS = new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, DeferredItem<BlockItem>> DUNGEON_BUILDING_ITEMS = new java.util.LinkedHashMap<>();

    static {
        DUNGEON_BUILDING_BLOCKS.put("ashen_bone_dust_blackstone_bricks", BLOCKS.registerBlock("ashen_bone_dust_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("ashen_bone_dust_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("ashen_bone_dust_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("ashen_bone_dust_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("ashen_bone_dust_blackstone_top", BLOCKS.registerBlock("ashen_bone_dust_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("ashen_bone_dust_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("ashen_bone_dust_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("ashen_bone_dust_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("ashen_bone_dust_cobbled_deepslate", BLOCKS.registerBlock("ashen_bone_dust_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("ashen_bone_dust_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("ashen_bone_dust_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("ashen_bone_dust_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("ashen_bone_dust_deepslate_bricks", BLOCKS.registerBlock("ashen_bone_dust_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("ashen_bone_dust_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("ashen_bone_dust_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("ashen_bone_dust_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("ashen_bone_dust_deepslate_tiles", BLOCKS.registerBlock("ashen_bone_dust_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("ashen_bone_dust_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("ashen_bone_dust_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("ashen_bone_dust_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("ashen_bone_dust_polished_blackstone", BLOCKS.registerBlock("ashen_bone_dust_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("ashen_bone_dust_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("ashen_bone_dust_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("ashen_bone_dust_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("ashen_bone_dust_polished_deepslate", BLOCKS.registerBlock("ashen_bone_dust_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("ashen_bone_dust_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("ashen_bone_dust_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("ashen_bone_dust_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("black_eye_knots_blackstone_bricks", BLOCKS.registerBlock("black_eye_knots_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("black_eye_knots_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("black_eye_knots_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("black_eye_knots_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("black_eye_knots_blackstone_top", BLOCKS.registerBlock("black_eye_knots_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("black_eye_knots_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("black_eye_knots_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("black_eye_knots_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("black_eye_knots_cobbled_deepslate", BLOCKS.registerBlock("black_eye_knots_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("black_eye_knots_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("black_eye_knots_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("black_eye_knots_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("black_eye_knots_deepslate_bricks", BLOCKS.registerBlock("black_eye_knots_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("black_eye_knots_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("black_eye_knots_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("black_eye_knots_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("black_eye_knots_deepslate_tiles", BLOCKS.registerBlock("black_eye_knots_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("black_eye_knots_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("black_eye_knots_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("black_eye_knots_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("black_eye_knots_polished_blackstone", BLOCKS.registerBlock("black_eye_knots_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("black_eye_knots_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("black_eye_knots_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("black_eye_knots_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("black_eye_knots_polished_deepslate", BLOCKS.registerBlock("black_eye_knots_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("black_eye_knots_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("black_eye_knots_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("black_eye_knots_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("black_ooze_ashmire_tile", BLOCKS.registerBlock("black_ooze_ashmire_tile", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("black_ooze_ashmire_tile", ModItems.ITEMS.registerSimpleBlockItem("black_ooze_ashmire_tile", DUNGEON_BUILDING_BLOCKS.get("black_ooze_ashmire_tile")));
        DUNGEON_BUILDING_BLOCKS.put("black_ooze_cryptbrick", BLOCKS.registerBlock("black_ooze_cryptbrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("black_ooze_cryptbrick", ModItems.ITEMS.registerSimpleBlockItem("black_ooze_cryptbrick", DUNGEON_BUILDING_BLOCKS.get("black_ooze_cryptbrick")));
        DUNGEON_BUILDING_BLOCKS.put("black_ooze_gutter_cobble", BLOCKS.registerBlock("black_ooze_gutter_cobble", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("black_ooze_gutter_cobble", ModItems.ITEMS.registerSimpleBlockItem("black_ooze_gutter_cobble", DUNGEON_BUILDING_BLOCKS.get("black_ooze_gutter_cobble")));
        DUNGEON_BUILDING_BLOCKS.put("black_ooze_ironwake_stone", BLOCKS.registerBlock("black_ooze_ironwake_stone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("black_ooze_ironwake_stone", ModItems.ITEMS.registerSimpleBlockItem("black_ooze_ironwake_stone", DUNGEON_BUILDING_BLOCKS.get("black_ooze_ironwake_stone")));
        DUNGEON_BUILDING_BLOCKS.put("black_ooze_rotwall_brick", BLOCKS.registerBlock("black_ooze_rotwall_brick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("black_ooze_rotwall_brick", ModItems.ITEMS.registerSimpleBlockItem("black_ooze_rotwall_brick", DUNGEON_BUILDING_BLOCKS.get("black_ooze_rotwall_brick")));
        DUNGEON_BUILDING_BLOCKS.put("black_ooze_shadebrick", BLOCKS.registerBlock("black_ooze_shadebrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("black_ooze_shadebrick", ModItems.ITEMS.registerSimpleBlockItem("black_ooze_shadebrick", DUNGEON_BUILDING_BLOCKS.get("black_ooze_shadebrick")));
        DUNGEON_BUILDING_BLOCKS.put("black_ooze_tomblime_block", BLOCKS.registerBlock("black_ooze_tomblime_block", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("black_ooze_tomblime_block", ModItems.ITEMS.registerSimpleBlockItem("black_ooze_tomblime_block", DUNGEON_BUILDING_BLOCKS.get("black_ooze_tomblime_block")));
        DUNGEON_BUILDING_BLOCKS.put("blood_spattered_ashmire_tile", BLOCKS.registerBlock("blood_spattered_ashmire_tile", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("blood_spattered_ashmire_tile", ModItems.ITEMS.registerSimpleBlockItem("blood_spattered_ashmire_tile", DUNGEON_BUILDING_BLOCKS.get("blood_spattered_ashmire_tile")));
        DUNGEON_BUILDING_BLOCKS.put("blood_spattered_cryptbrick", BLOCKS.registerBlock("blood_spattered_cryptbrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("blood_spattered_cryptbrick", ModItems.ITEMS.registerSimpleBlockItem("blood_spattered_cryptbrick", DUNGEON_BUILDING_BLOCKS.get("blood_spattered_cryptbrick")));
        DUNGEON_BUILDING_BLOCKS.put("blood_spattered_gutter_cobble", BLOCKS.registerBlock("blood_spattered_gutter_cobble", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("blood_spattered_gutter_cobble", ModItems.ITEMS.registerSimpleBlockItem("blood_spattered_gutter_cobble", DUNGEON_BUILDING_BLOCKS.get("blood_spattered_gutter_cobble")));
        DUNGEON_BUILDING_BLOCKS.put("blood_spattered_ironwake_stone", BLOCKS.registerBlock("blood_spattered_ironwake_stone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("blood_spattered_ironwake_stone", ModItems.ITEMS.registerSimpleBlockItem("blood_spattered_ironwake_stone", DUNGEON_BUILDING_BLOCKS.get("blood_spattered_ironwake_stone")));
        DUNGEON_BUILDING_BLOCKS.put("blood_spattered_rotwall_brick", BLOCKS.registerBlock("blood_spattered_rotwall_brick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("blood_spattered_rotwall_brick", ModItems.ITEMS.registerSimpleBlockItem("blood_spattered_rotwall_brick", DUNGEON_BUILDING_BLOCKS.get("blood_spattered_rotwall_brick")));
        DUNGEON_BUILDING_BLOCKS.put("blood_spattered_shadebrick", BLOCKS.registerBlock("blood_spattered_shadebrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("blood_spattered_shadebrick", ModItems.ITEMS.registerSimpleBlockItem("blood_spattered_shadebrick", DUNGEON_BUILDING_BLOCKS.get("blood_spattered_shadebrick")));
        DUNGEON_BUILDING_BLOCKS.put("blood_spattered_tomblime_block", BLOCKS.registerBlock("blood_spattered_tomblime_block", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("blood_spattered_tomblime_block", ModItems.ITEMS.registerSimpleBlockItem("blood_spattered_tomblime_block", DUNGEON_BUILDING_BLOCKS.get("blood_spattered_tomblime_block")));
        DUNGEON_BUILDING_BLOCKS.put("bone_shard_block", BLOCKS.registerBlock("bone_shard_block", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("bone_shard_block", ModItems.ITEMS.registerSimpleBlockItem("bone_shard_block", DUNGEON_BUILDING_BLOCKS.get("bone_shard_block")));
        DUNGEON_BUILDING_BLOCKS.put("bruise_rot_ashmire_tile", BLOCKS.registerBlock("bruise_rot_ashmire_tile", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("bruise_rot_ashmire_tile", ModItems.ITEMS.registerSimpleBlockItem("bruise_rot_ashmire_tile", DUNGEON_BUILDING_BLOCKS.get("bruise_rot_ashmire_tile")));
        DUNGEON_BUILDING_BLOCKS.put("bruise_rot_cryptbrick", BLOCKS.registerBlock("bruise_rot_cryptbrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("bruise_rot_cryptbrick", ModItems.ITEMS.registerSimpleBlockItem("bruise_rot_cryptbrick", DUNGEON_BUILDING_BLOCKS.get("bruise_rot_cryptbrick")));
        DUNGEON_BUILDING_BLOCKS.put("bruise_rot_gutter_cobble", BLOCKS.registerBlock("bruise_rot_gutter_cobble", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("bruise_rot_gutter_cobble", ModItems.ITEMS.registerSimpleBlockItem("bruise_rot_gutter_cobble", DUNGEON_BUILDING_BLOCKS.get("bruise_rot_gutter_cobble")));
        DUNGEON_BUILDING_BLOCKS.put("bruise_rot_ironwake_stone", BLOCKS.registerBlock("bruise_rot_ironwake_stone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("bruise_rot_ironwake_stone", ModItems.ITEMS.registerSimpleBlockItem("bruise_rot_ironwake_stone", DUNGEON_BUILDING_BLOCKS.get("bruise_rot_ironwake_stone")));
        DUNGEON_BUILDING_BLOCKS.put("bruise_rot_rotwall_brick", BLOCKS.registerBlock("bruise_rot_rotwall_brick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("bruise_rot_rotwall_brick", ModItems.ITEMS.registerSimpleBlockItem("bruise_rot_rotwall_brick", DUNGEON_BUILDING_BLOCKS.get("bruise_rot_rotwall_brick")));
        DUNGEON_BUILDING_BLOCKS.put("bruise_rot_shadebrick", BLOCKS.registerBlock("bruise_rot_shadebrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("bruise_rot_shadebrick", ModItems.ITEMS.registerSimpleBlockItem("bruise_rot_shadebrick", DUNGEON_BUILDING_BLOCKS.get("bruise_rot_shadebrick")));
        DUNGEON_BUILDING_BLOCKS.put("bruise_rot_tomblime_block", BLOCKS.registerBlock("bruise_rot_tomblime_block", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("bruise_rot_tomblime_block", ModItems.ITEMS.registerSimpleBlockItem("bruise_rot_tomblime_block", DUNGEON_BUILDING_BLOCKS.get("bruise_rot_tomblime_block")));
        DUNGEON_BUILDING_BLOCKS.put("cave_glyph_ashmire_tile", BLOCKS.registerBlock("cave_glyph_ashmire_tile", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cave_glyph_ashmire_tile", ModItems.ITEMS.registerSimpleBlockItem("cave_glyph_ashmire_tile", DUNGEON_BUILDING_BLOCKS.get("cave_glyph_ashmire_tile")));
        DUNGEON_BUILDING_BLOCKS.put("cave_glyph_cryptbrick", BLOCKS.registerBlock("cave_glyph_cryptbrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cave_glyph_cryptbrick", ModItems.ITEMS.registerSimpleBlockItem("cave_glyph_cryptbrick", DUNGEON_BUILDING_BLOCKS.get("cave_glyph_cryptbrick")));
        DUNGEON_BUILDING_BLOCKS.put("cave_glyph_gutter_cobble", BLOCKS.registerBlock("cave_glyph_gutter_cobble", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cave_glyph_gutter_cobble", ModItems.ITEMS.registerSimpleBlockItem("cave_glyph_gutter_cobble", DUNGEON_BUILDING_BLOCKS.get("cave_glyph_gutter_cobble")));
        DUNGEON_BUILDING_BLOCKS.put("cave_glyph_ironwake_stone", BLOCKS.registerBlock("cave_glyph_ironwake_stone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cave_glyph_ironwake_stone", ModItems.ITEMS.registerSimpleBlockItem("cave_glyph_ironwake_stone", DUNGEON_BUILDING_BLOCKS.get("cave_glyph_ironwake_stone")));
        DUNGEON_BUILDING_BLOCKS.put("cave_glyph_rotwall_brick", BLOCKS.registerBlock("cave_glyph_rotwall_brick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cave_glyph_rotwall_brick", ModItems.ITEMS.registerSimpleBlockItem("cave_glyph_rotwall_brick", DUNGEON_BUILDING_BLOCKS.get("cave_glyph_rotwall_brick")));
        DUNGEON_BUILDING_BLOCKS.put("cave_glyph_shadebrick", BLOCKS.registerBlock("cave_glyph_shadebrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cave_glyph_shadebrick", ModItems.ITEMS.registerSimpleBlockItem("cave_glyph_shadebrick", DUNGEON_BUILDING_BLOCKS.get("cave_glyph_shadebrick")));
        DUNGEON_BUILDING_BLOCKS.put("cave_glyph_tomblime_block", BLOCKS.registerBlock("cave_glyph_tomblime_block", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cave_glyph_tomblime_block", ModItems.ITEMS.registerSimpleBlockItem("cave_glyph_tomblime_block", DUNGEON_BUILDING_BLOCKS.get("cave_glyph_tomblime_block")));
        DUNGEON_BUILDING_BLOCKS.put("chitin_splintered_blackstone_bricks", BLOCKS.registerBlock("chitin_splintered_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("chitin_splintered_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("chitin_splintered_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("chitin_splintered_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("chitin_splintered_blackstone_top", BLOCKS.registerBlock("chitin_splintered_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("chitin_splintered_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("chitin_splintered_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("chitin_splintered_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("chitin_splintered_cobbled_deepslate", BLOCKS.registerBlock("chitin_splintered_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("chitin_splintered_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("chitin_splintered_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("chitin_splintered_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("chitin_splintered_deepslate_bricks", BLOCKS.registerBlock("chitin_splintered_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("chitin_splintered_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("chitin_splintered_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("chitin_splintered_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("chitin_splintered_deepslate_tiles", BLOCKS.registerBlock("chitin_splintered_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("chitin_splintered_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("chitin_splintered_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("chitin_splintered_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("chitin_splintered_polished_blackstone", BLOCKS.registerBlock("chitin_splintered_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("chitin_splintered_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("chitin_splintered_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("chitin_splintered_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("chitin_splintered_polished_deepslate", BLOCKS.registerBlock("chitin_splintered_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("chitin_splintered_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("chitin_splintered_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("chitin_splintered_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("cold_blue_curse_blackstone_bricks", BLOCKS.registerBlock("cold_blue_curse_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cold_blue_curse_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("cold_blue_curse_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("cold_blue_curse_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("cold_blue_curse_blackstone_top", BLOCKS.registerBlock("cold_blue_curse_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cold_blue_curse_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("cold_blue_curse_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("cold_blue_curse_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("cold_blue_curse_cobbled_deepslate", BLOCKS.registerBlock("cold_blue_curse_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cold_blue_curse_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("cold_blue_curse_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("cold_blue_curse_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("cold_blue_curse_deepslate_bricks", BLOCKS.registerBlock("cold_blue_curse_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cold_blue_curse_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("cold_blue_curse_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("cold_blue_curse_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("cold_blue_curse_deepslate_tiles", BLOCKS.registerBlock("cold_blue_curse_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cold_blue_curse_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("cold_blue_curse_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("cold_blue_curse_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("cold_blue_curse_polished_blackstone", BLOCKS.registerBlock("cold_blue_curse_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cold_blue_curse_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("cold_blue_curse_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("cold_blue_curse_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("cold_blue_curse_polished_deepslate", BLOCKS.registerBlock("cold_blue_curse_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cold_blue_curse_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("cold_blue_curse_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("cold_blue_curse_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("cursed_chitin_block", BLOCKS.registerBlock("cursed_chitin_block", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cursed_chitin_block", ModItems.ITEMS.registerSimpleBlockItem("cursed_chitin_block", DUNGEON_BUILDING_BLOCKS.get("cursed_chitin_block")));
        DUNGEON_BUILDING_BLOCKS.put("cursed_root_tangle", BLOCKS.registerBlock("cursed_root_tangle", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("cursed_root_tangle", ModItems.ITEMS.registerSimpleBlockItem("cursed_root_tangle", DUNGEON_BUILDING_BLOCKS.get("cursed_root_tangle")));
        DUNGEON_BUILDING_BLOCKS.put("dark_matter_rift_blackstone_bricks", BLOCKS.registerBlock("dark_matter_rift_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("dark_matter_rift_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("dark_matter_rift_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("dark_matter_rift_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("dark_matter_rift_blackstone_top", BLOCKS.registerBlock("dark_matter_rift_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("dark_matter_rift_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("dark_matter_rift_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("dark_matter_rift_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("dark_matter_rift_cobbled_deepslate", BLOCKS.registerBlock("dark_matter_rift_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("dark_matter_rift_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("dark_matter_rift_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("dark_matter_rift_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("dark_matter_rift_deepslate_bricks", BLOCKS.registerBlock("dark_matter_rift_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("dark_matter_rift_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("dark_matter_rift_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("dark_matter_rift_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("dark_matter_rift_deepslate_tiles", BLOCKS.registerBlock("dark_matter_rift_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("dark_matter_rift_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("dark_matter_rift_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("dark_matter_rift_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("dark_matter_rift_polished_blackstone", BLOCKS.registerBlock("dark_matter_rift_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("dark_matter_rift_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("dark_matter_rift_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("dark_matter_rift_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("dark_matter_rift_polished_deepslate", BLOCKS.registerBlock("dark_matter_rift_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("dark_matter_rift_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("dark_matter_rift_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("dark_matter_rift_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("fly_egg_cluster_blackstone_bricks", BLOCKS.registerBlock("fly_egg_cluster_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("fly_egg_cluster_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("fly_egg_cluster_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("fly_egg_cluster_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("fly_egg_cluster_blackstone_top", BLOCKS.registerBlock("fly_egg_cluster_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("fly_egg_cluster_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("fly_egg_cluster_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("fly_egg_cluster_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("fly_egg_cluster_cobbled_deepslate", BLOCKS.registerBlock("fly_egg_cluster_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("fly_egg_cluster_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("fly_egg_cluster_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("fly_egg_cluster_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("fly_egg_cluster_deepslate_bricks", BLOCKS.registerBlock("fly_egg_cluster_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("fly_egg_cluster_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("fly_egg_cluster_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("fly_egg_cluster_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("fly_egg_cluster_deepslate_tiles", BLOCKS.registerBlock("fly_egg_cluster_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("fly_egg_cluster_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("fly_egg_cluster_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("fly_egg_cluster_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("fly_egg_cluster_polished_blackstone", BLOCKS.registerBlock("fly_egg_cluster_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("fly_egg_cluster_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("fly_egg_cluster_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("fly_egg_cluster_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("fly_egg_cluster_polished_deepslate", BLOCKS.registerBlock("fly_egg_cluster_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("fly_egg_cluster_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("fly_egg_cluster_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("fly_egg_cluster_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("fresh_splatter_blackstone_bricks", BLOCKS.registerBlock("fresh_splatter_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("fresh_splatter_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("fresh_splatter_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("fresh_splatter_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("fresh_splatter_blackstone_top", BLOCKS.registerBlock("fresh_splatter_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("fresh_splatter_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("fresh_splatter_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("fresh_splatter_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("fresh_splatter_cobbled_deepslate", BLOCKS.registerBlock("fresh_splatter_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("fresh_splatter_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("fresh_splatter_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("fresh_splatter_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("fresh_splatter_deepslate_bricks", BLOCKS.registerBlock("fresh_splatter_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("fresh_splatter_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("fresh_splatter_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("fresh_splatter_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("fresh_splatter_deepslate_tiles", BLOCKS.registerBlock("fresh_splatter_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("fresh_splatter_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("fresh_splatter_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("fresh_splatter_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("fresh_splatter_polished_blackstone", BLOCKS.registerBlock("fresh_splatter_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("fresh_splatter_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("fresh_splatter_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("fresh_splatter_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("fresh_splatter_polished_deepslate", BLOCKS.registerBlock("fresh_splatter_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("fresh_splatter_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("fresh_splatter_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("fresh_splatter_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("grave_mold_bricks", BLOCKS.registerBlock("grave_mold_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("grave_mold_bricks", ModItems.ITEMS.registerSimpleBlockItem("grave_mold_bricks", DUNGEON_BUILDING_BLOCKS.get("grave_mold_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("grave_moss_ashmire_tile", BLOCKS.registerBlock("grave_moss_ashmire_tile", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("grave_moss_ashmire_tile", ModItems.ITEMS.registerSimpleBlockItem("grave_moss_ashmire_tile", DUNGEON_BUILDING_BLOCKS.get("grave_moss_ashmire_tile")));
        DUNGEON_BUILDING_BLOCKS.put("grave_moss_cryptbrick", BLOCKS.registerBlock("grave_moss_cryptbrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("grave_moss_cryptbrick", ModItems.ITEMS.registerSimpleBlockItem("grave_moss_cryptbrick", DUNGEON_BUILDING_BLOCKS.get("grave_moss_cryptbrick")));
        DUNGEON_BUILDING_BLOCKS.put("grave_moss_gutter_cobble", BLOCKS.registerBlock("grave_moss_gutter_cobble", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("grave_moss_gutter_cobble", ModItems.ITEMS.registerSimpleBlockItem("grave_moss_gutter_cobble", DUNGEON_BUILDING_BLOCKS.get("grave_moss_gutter_cobble")));
        DUNGEON_BUILDING_BLOCKS.put("grave_moss_ironwake_stone", BLOCKS.registerBlock("grave_moss_ironwake_stone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("grave_moss_ironwake_stone", ModItems.ITEMS.registerSimpleBlockItem("grave_moss_ironwake_stone", DUNGEON_BUILDING_BLOCKS.get("grave_moss_ironwake_stone")));
        DUNGEON_BUILDING_BLOCKS.put("grave_moss_rotwall_brick", BLOCKS.registerBlock("grave_moss_rotwall_brick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("grave_moss_rotwall_brick", ModItems.ITEMS.registerSimpleBlockItem("grave_moss_rotwall_brick", DUNGEON_BUILDING_BLOCKS.get("grave_moss_rotwall_brick")));
        DUNGEON_BUILDING_BLOCKS.put("grave_moss_shadebrick", BLOCKS.registerBlock("grave_moss_shadebrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("grave_moss_shadebrick", ModItems.ITEMS.registerSimpleBlockItem("grave_moss_shadebrick", DUNGEON_BUILDING_BLOCKS.get("grave_moss_shadebrick")));
        DUNGEON_BUILDING_BLOCKS.put("grave_moss_tomblime_block", BLOCKS.registerBlock("grave_moss_tomblime_block", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("grave_moss_tomblime_block", ModItems.ITEMS.registerSimpleBlockItem("grave_moss_tomblime_block", DUNGEON_BUILDING_BLOCKS.get("grave_moss_tomblime_block")));
        DUNGEON_BUILDING_BLOCKS.put("infected_waterline_blackstone_bricks", BLOCKS.registerBlock("infected_waterline_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("infected_waterline_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("infected_waterline_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("infected_waterline_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("infected_waterline_blackstone_top", BLOCKS.registerBlock("infected_waterline_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("infected_waterline_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("infected_waterline_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("infected_waterline_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("infected_waterline_cobbled_deepslate", BLOCKS.registerBlock("infected_waterline_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("infected_waterline_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("infected_waterline_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("infected_waterline_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("infected_waterline_deepslate_bricks", BLOCKS.registerBlock("infected_waterline_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("infected_waterline_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("infected_waterline_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("infected_waterline_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("infected_waterline_deepslate_tiles", BLOCKS.registerBlock("infected_waterline_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("infected_waterline_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("infected_waterline_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("infected_waterline_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("infected_waterline_polished_blackstone", BLOCKS.registerBlock("infected_waterline_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("infected_waterline_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("infected_waterline_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("infected_waterline_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("infected_waterline_polished_deepslate", BLOCKS.registerBlock("infected_waterline_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("infected_waterline_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("infected_waterline_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("infected_waterline_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("lichen_crawl_blackstone_bricks", BLOCKS.registerBlock("lichen_crawl_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("lichen_crawl_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("lichen_crawl_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("lichen_crawl_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("lichen_crawl_blackstone_top", BLOCKS.registerBlock("lichen_crawl_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("lichen_crawl_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("lichen_crawl_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("lichen_crawl_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("lichen_crawl_cobbled_deepslate", BLOCKS.registerBlock("lichen_crawl_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("lichen_crawl_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("lichen_crawl_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("lichen_crawl_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("lichen_crawl_deepslate_bricks", BLOCKS.registerBlock("lichen_crawl_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("lichen_crawl_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("lichen_crawl_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("lichen_crawl_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("lichen_crawl_deepslate_tiles", BLOCKS.registerBlock("lichen_crawl_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("lichen_crawl_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("lichen_crawl_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("lichen_crawl_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("lichen_crawl_polished_blackstone", BLOCKS.registerBlock("lichen_crawl_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("lichen_crawl_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("lichen_crawl_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("lichen_crawl_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("lichen_crawl_polished_deepslate", BLOCKS.registerBlock("lichen_crawl_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("lichen_crawl_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("lichen_crawl_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("lichen_crawl_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("maggot_waterline_blackstone_bricks", BLOCKS.registerBlock("maggot_waterline_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("maggot_waterline_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("maggot_waterline_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("maggot_waterline_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("maggot_waterline_blackstone_top", BLOCKS.registerBlock("maggot_waterline_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("maggot_waterline_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("maggot_waterline_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("maggot_waterline_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("maggot_waterline_cobbled_deepslate", BLOCKS.registerBlock("maggot_waterline_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("maggot_waterline_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("maggot_waterline_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("maggot_waterline_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("maggot_waterline_deepslate_bricks", BLOCKS.registerBlock("maggot_waterline_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("maggot_waterline_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("maggot_waterline_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("maggot_waterline_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("maggot_waterline_deepslate_tiles", BLOCKS.registerBlock("maggot_waterline_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("maggot_waterline_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("maggot_waterline_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("maggot_waterline_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("maggot_waterline_polished_blackstone", BLOCKS.registerBlock("maggot_waterline_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("maggot_waterline_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("maggot_waterline_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("maggot_waterline_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("maggot_waterline_polished_deepslate", BLOCKS.registerBlock("maggot_waterline_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("maggot_waterline_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("maggot_waterline_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("maggot_waterline_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("maw_teeth_block", BLOCKS.registerBlock("maw_teeth_block", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("maw_teeth_block", ModItems.ITEMS.registerSimpleBlockItem("maw_teeth_block", DUNGEON_BUILDING_BLOCKS.get("maw_teeth_block")));
        DUNGEON_BUILDING_BLOCKS.put("mildew_clot_cryptbrick", BLOCKS.registerBlock("mildew_clot_cryptbrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("mildew_clot_cryptbrick", ModItems.ITEMS.registerSimpleBlockItem("mildew_clot_cryptbrick", DUNGEON_BUILDING_BLOCKS.get("mildew_clot_cryptbrick")));
        DUNGEON_BUILDING_BLOCKS.put("mildew_clot_rotwall_brick", BLOCKS.registerBlock("mildew_clot_rotwall_brick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("mildew_clot_rotwall_brick", ModItems.ITEMS.registerSimpleBlockItem("mildew_clot_rotwall_brick", DUNGEON_BUILDING_BLOCKS.get("mildew_clot_rotwall_brick")));
        DUNGEON_BUILDING_BLOCKS.put("necrotic_edges_blackstone_bricks", BLOCKS.registerBlock("necrotic_edges_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("necrotic_edges_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("necrotic_edges_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("necrotic_edges_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("necrotic_edges_blackstone_top", BLOCKS.registerBlock("necrotic_edges_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("necrotic_edges_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("necrotic_edges_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("necrotic_edges_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("necrotic_edges_cobbled_deepslate", BLOCKS.registerBlock("necrotic_edges_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("necrotic_edges_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("necrotic_edges_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("necrotic_edges_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("necrotic_edges_deepslate_bricks", BLOCKS.registerBlock("necrotic_edges_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("necrotic_edges_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("necrotic_edges_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("necrotic_edges_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("necrotic_edges_deepslate_tiles", BLOCKS.registerBlock("necrotic_edges_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("necrotic_edges_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("necrotic_edges_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("necrotic_edges_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("necrotic_edges_polished_blackstone", BLOCKS.registerBlock("necrotic_edges_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("necrotic_edges_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("necrotic_edges_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("necrotic_edges_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("necrotic_edges_polished_deepslate", BLOCKS.registerBlock("necrotic_edges_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("necrotic_edges_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("necrotic_edges_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("necrotic_edges_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("old_blood_smear_blackstone_bricks", BLOCKS.registerBlock("old_blood_smear_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("old_blood_smear_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("old_blood_smear_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("old_blood_smear_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("old_blood_smear_blackstone_top", BLOCKS.registerBlock("old_blood_smear_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("old_blood_smear_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("old_blood_smear_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("old_blood_smear_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("old_blood_smear_cobbled_deepslate", BLOCKS.registerBlock("old_blood_smear_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("old_blood_smear_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("old_blood_smear_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("old_blood_smear_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("old_blood_smear_deepslate_bricks", BLOCKS.registerBlock("old_blood_smear_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("old_blood_smear_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("old_blood_smear_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("old_blood_smear_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("old_blood_smear_deepslate_tiles", BLOCKS.registerBlock("old_blood_smear_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("old_blood_smear_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("old_blood_smear_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("old_blood_smear_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("old_blood_smear_polished_blackstone", BLOCKS.registerBlock("old_blood_smear_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("old_blood_smear_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("old_blood_smear_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("old_blood_smear_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("old_blood_smear_polished_deepslate", BLOCKS.registerBlock("old_blood_smear_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("old_blood_smear_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("old_blood_smear_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("old_blood_smear_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("pale_handprint_blackstone_bricks", BLOCKS.registerBlock("pale_handprint_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("pale_handprint_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("pale_handprint_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("pale_handprint_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("pale_handprint_blackstone_top", BLOCKS.registerBlock("pale_handprint_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("pale_handprint_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("pale_handprint_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("pale_handprint_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("pale_handprint_cobbled_deepslate", BLOCKS.registerBlock("pale_handprint_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("pale_handprint_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("pale_handprint_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("pale_handprint_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("pale_handprint_deepslate_bricks", BLOCKS.registerBlock("pale_handprint_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("pale_handprint_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("pale_handprint_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("pale_handprint_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("pale_handprint_deepslate_tiles", BLOCKS.registerBlock("pale_handprint_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("pale_handprint_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("pale_handprint_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("pale_handprint_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("pale_handprint_polished_blackstone", BLOCKS.registerBlock("pale_handprint_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("pale_handprint_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("pale_handprint_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("pale_handprint_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("pale_handprint_polished_deepslate", BLOCKS.registerBlock("pale_handprint_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("pale_handprint_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("pale_handprint_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("pale_handprint_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("purple_bruise_bloom_blackstone_bricks", BLOCKS.registerBlock("purple_bruise_bloom_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("purple_bruise_bloom_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("purple_bruise_bloom_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("purple_bruise_bloom_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("purple_bruise_bloom_blackstone_top", BLOCKS.registerBlock("purple_bruise_bloom_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("purple_bruise_bloom_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("purple_bruise_bloom_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("purple_bruise_bloom_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("purple_bruise_bloom_cobbled_deepslate", BLOCKS.registerBlock("purple_bruise_bloom_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("purple_bruise_bloom_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("purple_bruise_bloom_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("purple_bruise_bloom_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("purple_bruise_bloom_deepslate_bricks", BLOCKS.registerBlock("purple_bruise_bloom_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("purple_bruise_bloom_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("purple_bruise_bloom_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("purple_bruise_bloom_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("purple_bruise_bloom_deepslate_tiles", BLOCKS.registerBlock("purple_bruise_bloom_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("purple_bruise_bloom_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("purple_bruise_bloom_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("purple_bruise_bloom_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("purple_bruise_bloom_polished_blackstone", BLOCKS.registerBlock("purple_bruise_bloom_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("purple_bruise_bloom_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("purple_bruise_bloom_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("purple_bruise_bloom_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("purple_bruise_bloom_polished_deepslate", BLOCKS.registerBlock("purple_bruise_bloom_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("purple_bruise_bloom_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("purple_bruise_bloom_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("purple_bruise_bloom_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("pus_bubbled_blackstone_bricks", BLOCKS.registerBlock("pus_bubbled_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("pus_bubbled_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("pus_bubbled_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("pus_bubbled_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("pus_bubbled_blackstone_top", BLOCKS.registerBlock("pus_bubbled_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("pus_bubbled_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("pus_bubbled_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("pus_bubbled_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("pus_bubbled_cobbled_deepslate", BLOCKS.registerBlock("pus_bubbled_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("pus_bubbled_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("pus_bubbled_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("pus_bubbled_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("pus_bubbled_deepslate_bricks", BLOCKS.registerBlock("pus_bubbled_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("pus_bubbled_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("pus_bubbled_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("pus_bubbled_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("pus_bubbled_deepslate_tiles", BLOCKS.registerBlock("pus_bubbled_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("pus_bubbled_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("pus_bubbled_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("pus_bubbled_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("pus_bubbled_polished_blackstone", BLOCKS.registerBlock("pus_bubbled_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("pus_bubbled_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("pus_bubbled_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("pus_bubbled_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("pus_bubbled_polished_deepslate", BLOCKS.registerBlock("pus_bubbled_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("pus_bubbled_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("pus_bubbled_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("pus_bubbled_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("pustule_moss_block", BLOCKS.registerBlock("pustule_moss_block", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("pustule_moss_block", ModItems.ITEMS.registerSimpleBlockItem("pustule_moss_block", DUNGEON_BUILDING_BLOCKS.get("pustule_moss_block")));
        DUNGEON_BUILDING_BLOCKS.put("rib_glyph_shadebrick", BLOCKS.registerBlock("rib_glyph_shadebrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("rib_glyph_shadebrick", ModItems.ITEMS.registerSimpleBlockItem("rib_glyph_shadebrick", DUNGEON_BUILDING_BLOCKS.get("rib_glyph_shadebrick")));
        DUNGEON_BUILDING_BLOCKS.put("rib_glyph_tomblime_block", BLOCKS.registerBlock("rib_glyph_tomblime_block", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("rib_glyph_tomblime_block", ModItems.ITEMS.registerSimpleBlockItem("rib_glyph_tomblime_block", DUNGEON_BUILDING_BLOCKS.get("rib_glyph_tomblime_block")));
        DUNGEON_BUILDING_BLOCKS.put("ritual_chalk_blackstone_bricks", BLOCKS.registerBlock("ritual_chalk_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("ritual_chalk_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("ritual_chalk_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("ritual_chalk_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("ritual_chalk_blackstone_top", BLOCKS.registerBlock("ritual_chalk_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("ritual_chalk_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("ritual_chalk_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("ritual_chalk_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("ritual_chalk_cobbled_deepslate", BLOCKS.registerBlock("ritual_chalk_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("ritual_chalk_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("ritual_chalk_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("ritual_chalk_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("ritual_chalk_deepslate_bricks", BLOCKS.registerBlock("ritual_chalk_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("ritual_chalk_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("ritual_chalk_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("ritual_chalk_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("ritual_chalk_deepslate_tiles", BLOCKS.registerBlock("ritual_chalk_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("ritual_chalk_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("ritual_chalk_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("ritual_chalk_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("ritual_chalk_polished_blackstone", BLOCKS.registerBlock("ritual_chalk_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("ritual_chalk_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("ritual_chalk_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("ritual_chalk_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("ritual_chalk_polished_deepslate", BLOCKS.registerBlock("ritual_chalk_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("ritual_chalk_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("ritual_chalk_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("ritual_chalk_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("rust_scabbed_blackstone_bricks", BLOCKS.registerBlock("rust_scabbed_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("rust_scabbed_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("rust_scabbed_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("rust_scabbed_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("rust_scabbed_blackstone_top", BLOCKS.registerBlock("rust_scabbed_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("rust_scabbed_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("rust_scabbed_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("rust_scabbed_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("rust_scabbed_cobbled_deepslate", BLOCKS.registerBlock("rust_scabbed_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("rust_scabbed_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("rust_scabbed_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("rust_scabbed_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("rust_scabbed_deepslate_bricks", BLOCKS.registerBlock("rust_scabbed_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("rust_scabbed_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("rust_scabbed_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("rust_scabbed_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("rust_scabbed_deepslate_tiles", BLOCKS.registerBlock("rust_scabbed_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("rust_scabbed_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("rust_scabbed_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("rust_scabbed_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("rust_scabbed_polished_blackstone", BLOCKS.registerBlock("rust_scabbed_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("rust_scabbed_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("rust_scabbed_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("rust_scabbed_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("rust_scabbed_polished_deepslate", BLOCKS.registerBlock("rust_scabbed_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("rust_scabbed_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("rust_scabbed_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("rust_scabbed_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("scratch_runes_blackstone_bricks", BLOCKS.registerBlock("scratch_runes_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("scratch_runes_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("scratch_runes_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("scratch_runes_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("scratch_runes_blackstone_top", BLOCKS.registerBlock("scratch_runes_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("scratch_runes_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("scratch_runes_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("scratch_runes_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("scratch_runes_cobbled_deepslate", BLOCKS.registerBlock("scratch_runes_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("scratch_runes_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("scratch_runes_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("scratch_runes_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("scratch_runes_deepslate_bricks", BLOCKS.registerBlock("scratch_runes_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("scratch_runes_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("scratch_runes_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("scratch_runes_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("scratch_runes_deepslate_tiles", BLOCKS.registerBlock("scratch_runes_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("scratch_runes_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("scratch_runes_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("scratch_runes_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("scratch_runes_polished_blackstone", BLOCKS.registerBlock("scratch_runes_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("scratch_runes_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("scratch_runes_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("scratch_runes_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("scratch_runes_polished_deepslate", BLOCKS.registerBlock("scratch_runes_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("scratch_runes_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("scratch_runes_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("scratch_runes_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("seeping_watcher_blackstone_bricks", BLOCKS.registerBlock("seeping_watcher_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("seeping_watcher_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("seeping_watcher_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("seeping_watcher_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("seeping_watcher_blackstone_top", BLOCKS.registerBlock("seeping_watcher_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("seeping_watcher_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("seeping_watcher_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("seeping_watcher_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("seeping_watcher_cobbled_deepslate", BLOCKS.registerBlock("seeping_watcher_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("seeping_watcher_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("seeping_watcher_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("seeping_watcher_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("seeping_watcher_deepslate_bricks", BLOCKS.registerBlock("seeping_watcher_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("seeping_watcher_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("seeping_watcher_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("seeping_watcher_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("seeping_watcher_deepslate_tiles", BLOCKS.registerBlock("seeping_watcher_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("seeping_watcher_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("seeping_watcher_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("seeping_watcher_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("seeping_watcher_polished_blackstone", BLOCKS.registerBlock("seeping_watcher_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("seeping_watcher_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("seeping_watcher_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("seeping_watcher_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("seeping_watcher_polished_deepslate", BLOCKS.registerBlock("seeping_watcher_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("seeping_watcher_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("seeping_watcher_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("seeping_watcher_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("sinew_flesh_block", BLOCKS.registerBlock("sinew_flesh_block", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("sinew_flesh_block", ModItems.ITEMS.registerSimpleBlockItem("sinew_flesh_block", DUNGEON_BUILDING_BLOCKS.get("sinew_flesh_block")));
        DUNGEON_BUILDING_BLOCKS.put("skull_stone_block", BLOCKS.registerBlock("skull_stone_block", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("skull_stone_block", ModItems.ITEMS.registerSimpleBlockItem("skull_stone_block", DUNGEON_BUILDING_BLOCKS.get("skull_stone_block")));
        DUNGEON_BUILDING_BLOCKS.put("slaughter_lichen_blackstone_bricks", BLOCKS.registerBlock("slaughter_lichen_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("slaughter_lichen_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("slaughter_lichen_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("slaughter_lichen_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("slaughter_lichen_blackstone_top", BLOCKS.registerBlock("slaughter_lichen_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("slaughter_lichen_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("slaughter_lichen_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("slaughter_lichen_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("slaughter_lichen_cobbled_deepslate", BLOCKS.registerBlock("slaughter_lichen_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("slaughter_lichen_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("slaughter_lichen_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("slaughter_lichen_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("slaughter_lichen_deepslate_bricks", BLOCKS.registerBlock("slaughter_lichen_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("slaughter_lichen_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("slaughter_lichen_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("slaughter_lichen_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("slaughter_lichen_deepslate_tiles", BLOCKS.registerBlock("slaughter_lichen_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("slaughter_lichen_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("slaughter_lichen_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("slaughter_lichen_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("slaughter_lichen_polished_blackstone", BLOCKS.registerBlock("slaughter_lichen_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("slaughter_lichen_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("slaughter_lichen_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("slaughter_lichen_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("slaughter_lichen_polished_deepslate", BLOCKS.registerBlock("slaughter_lichen_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("slaughter_lichen_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("slaughter_lichen_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("slaughter_lichen_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("starless_seep_gutter_cobble", BLOCKS.registerBlock("starless_seep_gutter_cobble", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("starless_seep_gutter_cobble", ModItems.ITEMS.registerSimpleBlockItem("starless_seep_gutter_cobble", DUNGEON_BUILDING_BLOCKS.get("starless_seep_gutter_cobble")));
        DUNGEON_BUILDING_BLOCKS.put("starless_seep_ironwake_stone", BLOCKS.registerBlock("starless_seep_ironwake_stone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("starless_seep_ironwake_stone", ModItems.ITEMS.registerSimpleBlockItem("starless_seep_ironwake_stone", DUNGEON_BUILDING_BLOCKS.get("starless_seep_ironwake_stone")));
        DUNGEON_BUILDING_BLOCKS.put("starrot_gore_blackstone_bricks", BLOCKS.registerBlock("starrot_gore_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("starrot_gore_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("starrot_gore_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("starrot_gore_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("starrot_gore_blackstone_top", BLOCKS.registerBlock("starrot_gore_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("starrot_gore_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("starrot_gore_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("starrot_gore_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("starrot_gore_cobbled_deepslate", BLOCKS.registerBlock("starrot_gore_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("starrot_gore_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("starrot_gore_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("starrot_gore_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("starrot_gore_deepslate_bricks", BLOCKS.registerBlock("starrot_gore_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("starrot_gore_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("starrot_gore_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("starrot_gore_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("starrot_gore_deepslate_tiles", BLOCKS.registerBlock("starrot_gore_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("starrot_gore_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("starrot_gore_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("starrot_gore_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("starrot_gore_polished_blackstone", BLOCKS.registerBlock("starrot_gore_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("starrot_gore_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("starrot_gore_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("starrot_gore_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("starrot_gore_polished_deepslate", BLOCKS.registerBlock("starrot_gore_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("starrot_gore_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("starrot_gore_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("starrot_gore_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("tar_seams_blackstone_bricks", BLOCKS.registerBlock("tar_seams_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("tar_seams_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("tar_seams_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("tar_seams_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("tar_seams_blackstone_top", BLOCKS.registerBlock("tar_seams_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("tar_seams_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("tar_seams_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("tar_seams_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("tar_seams_cobbled_deepslate", BLOCKS.registerBlock("tar_seams_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("tar_seams_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("tar_seams_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("tar_seams_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("tar_seams_deepslate_bricks", BLOCKS.registerBlock("tar_seams_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("tar_seams_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("tar_seams_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("tar_seams_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("tar_seams_deepslate_tiles", BLOCKS.registerBlock("tar_seams_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("tar_seams_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("tar_seams_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("tar_seams_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("tar_seams_polished_blackstone", BLOCKS.registerBlock("tar_seams_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("tar_seams_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("tar_seams_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("tar_seams_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("tar_seams_polished_deepslate", BLOCKS.registerBlock("tar_seams_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("tar_seams_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("tar_seams_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("tar_seams_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("tarred_bone_rites_blackstone_bricks", BLOCKS.registerBlock("tarred_bone_rites_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("tarred_bone_rites_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("tarred_bone_rites_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("tarred_bone_rites_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("tarred_bone_rites_blackstone_top", BLOCKS.registerBlock("tarred_bone_rites_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("tarred_bone_rites_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("tarred_bone_rites_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("tarred_bone_rites_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("tarred_bone_rites_cobbled_deepslate", BLOCKS.registerBlock("tarred_bone_rites_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("tarred_bone_rites_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("tarred_bone_rites_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("tarred_bone_rites_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("tarred_bone_rites_deepslate_bricks", BLOCKS.registerBlock("tarred_bone_rites_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("tarred_bone_rites_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("tarred_bone_rites_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("tarred_bone_rites_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("tarred_bone_rites_deepslate_tiles", BLOCKS.registerBlock("tarred_bone_rites_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("tarred_bone_rites_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("tarred_bone_rites_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("tarred_bone_rites_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("tarred_bone_rites_polished_blackstone", BLOCKS.registerBlock("tarred_bone_rites_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("tarred_bone_rites_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("tarred_bone_rites_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("tarred_bone_rites_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("tarred_bone_rites_polished_deepslate", BLOCKS.registerBlock("tarred_bone_rites_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("tarred_bone_rites_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("tarred_bone_rites_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("tarred_bone_rites_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("thread_sinew_blackstone_bricks", BLOCKS.registerBlock("thread_sinew_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("thread_sinew_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("thread_sinew_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("thread_sinew_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("thread_sinew_blackstone_top", BLOCKS.registerBlock("thread_sinew_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("thread_sinew_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("thread_sinew_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("thread_sinew_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("thread_sinew_cobbled_deepslate", BLOCKS.registerBlock("thread_sinew_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("thread_sinew_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("thread_sinew_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("thread_sinew_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("thread_sinew_deepslate_bricks", BLOCKS.registerBlock("thread_sinew_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("thread_sinew_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("thread_sinew_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("thread_sinew_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("thread_sinew_deepslate_tiles", BLOCKS.registerBlock("thread_sinew_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("thread_sinew_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("thread_sinew_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("thread_sinew_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("thread_sinew_polished_blackstone", BLOCKS.registerBlock("thread_sinew_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("thread_sinew_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("thread_sinew_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("thread_sinew_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("thread_sinew_polished_deepslate", BLOCKS.registerBlock("thread_sinew_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("thread_sinew_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("thread_sinew_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("thread_sinew_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("void_eye_tile", BLOCKS.registerBlock("void_eye_tile", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("void_eye_tile", ModItems.ITEMS.registerSimpleBlockItem("void_eye_tile", DUNGEON_BUILDING_BLOCKS.get("void_eye_tile")));
        DUNGEON_BUILDING_BLOCKS.put("void_stars_blackstone_bricks", BLOCKS.registerBlock("void_stars_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("void_stars_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("void_stars_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("void_stars_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("void_stars_blackstone_top", BLOCKS.registerBlock("void_stars_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("void_stars_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("void_stars_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("void_stars_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("void_stars_cobbled_deepslate", BLOCKS.registerBlock("void_stars_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("void_stars_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("void_stars_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("void_stars_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("void_stars_deepslate_bricks", BLOCKS.registerBlock("void_stars_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("void_stars_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("void_stars_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("void_stars_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("void_stars_deepslate_tiles", BLOCKS.registerBlock("void_stars_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("void_stars_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("void_stars_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("void_stars_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("void_stars_polished_blackstone", BLOCKS.registerBlock("void_stars_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("void_stars_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("void_stars_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("void_stars_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("void_stars_polished_deepslate", BLOCKS.registerBlock("void_stars_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("void_stars_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("void_stars_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("void_stars_polished_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("void_veined_ashmire_tile", BLOCKS.registerBlock("void_veined_ashmire_tile", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("void_veined_ashmire_tile", ModItems.ITEMS.registerSimpleBlockItem("void_veined_ashmire_tile", DUNGEON_BUILDING_BLOCKS.get("void_veined_ashmire_tile")));
        DUNGEON_BUILDING_BLOCKS.put("void_veined_cryptbrick", BLOCKS.registerBlock("void_veined_cryptbrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("void_veined_cryptbrick", ModItems.ITEMS.registerSimpleBlockItem("void_veined_cryptbrick", DUNGEON_BUILDING_BLOCKS.get("void_veined_cryptbrick")));
        DUNGEON_BUILDING_BLOCKS.put("void_veined_gutter_cobble", BLOCKS.registerBlock("void_veined_gutter_cobble", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("void_veined_gutter_cobble", ModItems.ITEMS.registerSimpleBlockItem("void_veined_gutter_cobble", DUNGEON_BUILDING_BLOCKS.get("void_veined_gutter_cobble")));
        DUNGEON_BUILDING_BLOCKS.put("void_veined_ironwake_stone", BLOCKS.registerBlock("void_veined_ironwake_stone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("void_veined_ironwake_stone", ModItems.ITEMS.registerSimpleBlockItem("void_veined_ironwake_stone", DUNGEON_BUILDING_BLOCKS.get("void_veined_ironwake_stone")));
        DUNGEON_BUILDING_BLOCKS.put("void_veined_rotwall_brick", BLOCKS.registerBlock("void_veined_rotwall_brick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("void_veined_rotwall_brick", ModItems.ITEMS.registerSimpleBlockItem("void_veined_rotwall_brick", DUNGEON_BUILDING_BLOCKS.get("void_veined_rotwall_brick")));
        DUNGEON_BUILDING_BLOCKS.put("void_veined_shadebrick", BLOCKS.registerBlock("void_veined_shadebrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("void_veined_shadebrick", ModItems.ITEMS.registerSimpleBlockItem("void_veined_shadebrick", DUNGEON_BUILDING_BLOCKS.get("void_veined_shadebrick")));
        DUNGEON_BUILDING_BLOCKS.put("void_veined_tomblime_block", BLOCKS.registerBlock("void_veined_tomblime_block", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("void_veined_tomblime_block", ModItems.ITEMS.registerSimpleBlockItem("void_veined_tomblime_block", DUNGEON_BUILDING_BLOCKS.get("void_veined_tomblime_block")));
        DUNGEON_BUILDING_BLOCKS.put("watcher_stain_ashmire_tile", BLOCKS.registerBlock("watcher_stain_ashmire_tile", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("watcher_stain_ashmire_tile", ModItems.ITEMS.registerSimpleBlockItem("watcher_stain_ashmire_tile", DUNGEON_BUILDING_BLOCKS.get("watcher_stain_ashmire_tile")));
        DUNGEON_BUILDING_BLOCKS.put("watcher_stain_cryptbrick", BLOCKS.registerBlock("watcher_stain_cryptbrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("watcher_stain_cryptbrick", ModItems.ITEMS.registerSimpleBlockItem("watcher_stain_cryptbrick", DUNGEON_BUILDING_BLOCKS.get("watcher_stain_cryptbrick")));
        DUNGEON_BUILDING_BLOCKS.put("watcher_stain_gutter_cobble", BLOCKS.registerBlock("watcher_stain_gutter_cobble", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("watcher_stain_gutter_cobble", ModItems.ITEMS.registerSimpleBlockItem("watcher_stain_gutter_cobble", DUNGEON_BUILDING_BLOCKS.get("watcher_stain_gutter_cobble")));
        DUNGEON_BUILDING_BLOCKS.put("watcher_stain_ironwake_stone", BLOCKS.registerBlock("watcher_stain_ironwake_stone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("watcher_stain_ironwake_stone", ModItems.ITEMS.registerSimpleBlockItem("watcher_stain_ironwake_stone", DUNGEON_BUILDING_BLOCKS.get("watcher_stain_ironwake_stone")));
        DUNGEON_BUILDING_BLOCKS.put("watcher_stain_rotwall_brick", BLOCKS.registerBlock("watcher_stain_rotwall_brick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("watcher_stain_rotwall_brick", ModItems.ITEMS.registerSimpleBlockItem("watcher_stain_rotwall_brick", DUNGEON_BUILDING_BLOCKS.get("watcher_stain_rotwall_brick")));
        DUNGEON_BUILDING_BLOCKS.put("watcher_stain_shadebrick", BLOCKS.registerBlock("watcher_stain_shadebrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("watcher_stain_shadebrick", ModItems.ITEMS.registerSimpleBlockItem("watcher_stain_shadebrick", DUNGEON_BUILDING_BLOCKS.get("watcher_stain_shadebrick")));
        DUNGEON_BUILDING_BLOCKS.put("watcher_stain_tomblime_block", BLOCKS.registerBlock("watcher_stain_tomblime_block", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("watcher_stain_tomblime_block", ModItems.ITEMS.registerSimpleBlockItem("watcher_stain_tomblime_block", DUNGEON_BUILDING_BLOCKS.get("watcher_stain_tomblime_block")));
        DUNGEON_BUILDING_BLOCKS.put("weeping_bloodstone", BLOCKS.registerBlock("weeping_bloodstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("weeping_bloodstone", ModItems.ITEMS.registerSimpleBlockItem("weeping_bloodstone", DUNGEON_BUILDING_BLOCKS.get("weeping_bloodstone")));
        DUNGEON_BUILDING_BLOCKS.put("whisper_pox_ashmire_tile", BLOCKS.registerBlock("whisper_pox_ashmire_tile", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("whisper_pox_ashmire_tile", ModItems.ITEMS.registerSimpleBlockItem("whisper_pox_ashmire_tile", DUNGEON_BUILDING_BLOCKS.get("whisper_pox_ashmire_tile")));
        DUNGEON_BUILDING_BLOCKS.put("worm_pocked_ashmire_tile", BLOCKS.registerBlock("worm_pocked_ashmire_tile", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("worm_pocked_ashmire_tile", ModItems.ITEMS.registerSimpleBlockItem("worm_pocked_ashmire_tile", DUNGEON_BUILDING_BLOCKS.get("worm_pocked_ashmire_tile")));
        DUNGEON_BUILDING_BLOCKS.put("worm_pocked_cryptbrick", BLOCKS.registerBlock("worm_pocked_cryptbrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("worm_pocked_cryptbrick", ModItems.ITEMS.registerSimpleBlockItem("worm_pocked_cryptbrick", DUNGEON_BUILDING_BLOCKS.get("worm_pocked_cryptbrick")));
        DUNGEON_BUILDING_BLOCKS.put("worm_pocked_gutter_cobble", BLOCKS.registerBlock("worm_pocked_gutter_cobble", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("worm_pocked_gutter_cobble", ModItems.ITEMS.registerSimpleBlockItem("worm_pocked_gutter_cobble", DUNGEON_BUILDING_BLOCKS.get("worm_pocked_gutter_cobble")));
        DUNGEON_BUILDING_BLOCKS.put("worm_pocked_ironwake_stone", BLOCKS.registerBlock("worm_pocked_ironwake_stone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("worm_pocked_ironwake_stone", ModItems.ITEMS.registerSimpleBlockItem("worm_pocked_ironwake_stone", DUNGEON_BUILDING_BLOCKS.get("worm_pocked_ironwake_stone")));
        DUNGEON_BUILDING_BLOCKS.put("worm_pocked_rotwall_brick", BLOCKS.registerBlock("worm_pocked_rotwall_brick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("worm_pocked_rotwall_brick", ModItems.ITEMS.registerSimpleBlockItem("worm_pocked_rotwall_brick", DUNGEON_BUILDING_BLOCKS.get("worm_pocked_rotwall_brick")));
        DUNGEON_BUILDING_BLOCKS.put("worm_pocked_shadebrick", BLOCKS.registerBlock("worm_pocked_shadebrick", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("worm_pocked_shadebrick", ModItems.ITEMS.registerSimpleBlockItem("worm_pocked_shadebrick", DUNGEON_BUILDING_BLOCKS.get("worm_pocked_shadebrick")));
        DUNGEON_BUILDING_BLOCKS.put("worm_pocked_tomblime_block", BLOCKS.registerBlock("worm_pocked_tomblime_block", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("worm_pocked_tomblime_block", ModItems.ITEMS.registerSimpleBlockItem("worm_pocked_tomblime_block", DUNGEON_BUILDING_BLOCKS.get("worm_pocked_tomblime_block")));
        DUNGEON_BUILDING_BLOCKS.put("yellow_mold_blackstone_bricks", BLOCKS.registerBlock("yellow_mold_blackstone_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("yellow_mold_blackstone_bricks", ModItems.ITEMS.registerSimpleBlockItem("yellow_mold_blackstone_bricks", DUNGEON_BUILDING_BLOCKS.get("yellow_mold_blackstone_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("yellow_mold_blackstone_top", BLOCKS.registerBlock("yellow_mold_blackstone_top", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("yellow_mold_blackstone_top", ModItems.ITEMS.registerSimpleBlockItem("yellow_mold_blackstone_top", DUNGEON_BUILDING_BLOCKS.get("yellow_mold_blackstone_top")));
        DUNGEON_BUILDING_BLOCKS.put("yellow_mold_cobbled_deepslate", BLOCKS.registerBlock("yellow_mold_cobbled_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("yellow_mold_cobbled_deepslate", ModItems.ITEMS.registerSimpleBlockItem("yellow_mold_cobbled_deepslate", DUNGEON_BUILDING_BLOCKS.get("yellow_mold_cobbled_deepslate")));
        DUNGEON_BUILDING_BLOCKS.put("yellow_mold_deepslate_bricks", BLOCKS.registerBlock("yellow_mold_deepslate_bricks", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("yellow_mold_deepslate_bricks", ModItems.ITEMS.registerSimpleBlockItem("yellow_mold_deepslate_bricks", DUNGEON_BUILDING_BLOCKS.get("yellow_mold_deepslate_bricks")));
        DUNGEON_BUILDING_BLOCKS.put("yellow_mold_deepslate_tiles", BLOCKS.registerBlock("yellow_mold_deepslate_tiles", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("yellow_mold_deepslate_tiles", ModItems.ITEMS.registerSimpleBlockItem("yellow_mold_deepslate_tiles", DUNGEON_BUILDING_BLOCKS.get("yellow_mold_deepslate_tiles")));
        DUNGEON_BUILDING_BLOCKS.put("yellow_mold_polished_blackstone", BLOCKS.registerBlock("yellow_mold_polished_blackstone", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("yellow_mold_polished_blackstone", ModItems.ITEMS.registerSimpleBlockItem("yellow_mold_polished_blackstone", DUNGEON_BUILDING_BLOCKS.get("yellow_mold_polished_blackstone")));
        DUNGEON_BUILDING_BLOCKS.put("yellow_mold_polished_deepslate", BLOCKS.registerBlock("yellow_mold_polished_deepslate", p -> new Block(dungeonBuildingStoneProps(p))));
        DUNGEON_BUILDING_ITEMS.put("yellow_mold_polished_deepslate", ModItems.ITEMS.registerSimpleBlockItem("yellow_mold_polished_deepslate", DUNGEON_BUILDING_BLOCKS.get("yellow_mold_polished_deepslate")));
    }

    /* ---------- Colored Amethyst: 6 variants × 16 colors ---------- */

    public record AmethystSet(
            DeferredBlock<Block> block,
            DeferredBlock<Block> budding,
            DeferredBlock<Block> budSmall,
            DeferredBlock<Block> budMedium,
            DeferredBlock<Block> budLarge,
            DeferredBlock<Block> cluster
    ) {}

    public static final Map<DyeColor, AmethystSet> AMETHYST = new EnumMap<>(DyeColor.class);

    public record AmethystItemSet(
            DeferredItem<BlockItem> block,
            DeferredItem<BlockItem> budding,
            DeferredItem<BlockItem> budSmall,
            DeferredItem<BlockItem> budMedium,
            DeferredItem<BlockItem> budLarge,
            DeferredItem<BlockItem> cluster
    ) {}

    public static final Map<DyeColor, AmethystItemSet> AMETHYST_ITEMS = new EnumMap<>(DyeColor.class);

    public static final Map<DyeColor, DeferredBlock<? extends Block>> LIT_AMETHYST_BLOCKS =
            new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, DeferredItem<BlockItem>> LIT_AMETHYST_ITEMS =
            new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor dye : DyeColor.values()) {
            final String color = dye.getName();

            final String solidPath   = "amethyst_block_" + color;
            final String buddingPath = "budding_amethyst_" + color;
            final String smallPath   = "small_amethyst_bud_" + color;
            final String mediumPath  = "medium_amethyst_bud_" + color;
            final String largePath   = "large_amethyst_bud_" + color;
            final String clusterPath = "amethyst_cluster_" + color;

            DeferredBlock<Block> block = BLOCKS.registerBlock(
                    solidPath,
                    (BlockBehaviour.Properties props) -> new ColoredAmethystBlock(
                            props.mapColor(dye.getMapColor())
                                    .strength(1.5F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .sound(SoundType.AMETHYST)

                    )
            );

            DeferredBlock<Block> budSmall = BLOCKS.registerBlock(
                    smallPath,
                    props -> ColoredBudBlock.small(
                            props.noOcclusion().strength(1.0F).sound(SoundType.AMETHYST).lightLevel(s -> 1)
                    )
            );

            DeferredBlock<Block> budMedium = BLOCKS.registerBlock(
                    mediumPath,
                    props -> ColoredBudBlock.medium(
                            props.noOcclusion().strength(1.0F).sound(SoundType.AMETHYST).lightLevel(s -> 2)
                    )
            );

            DeferredBlock<Block> budLarge = BLOCKS.registerBlock(
                    largePath,
                    props -> ColoredBudBlock.large(
                            props.noOcclusion().strength(1.0F).sound(SoundType.AMETHYST).lightLevel(s -> 4)
                    )
            );

            DeferredBlock<Block> cluster = BLOCKS.registerBlock(
                    clusterPath,
                    props -> new ColoredClusterBlock(
                            props.noOcclusion().strength(1.0F).sound(SoundType.AMETHYST).lightLevel(s -> 5)
                    )
            );

            DeferredBlock<Block> budding = BLOCKS.registerBlock(
                    buddingPath,
                    (BlockBehaviour.Properties props) -> new ColoredBuddingAmethystBlock(
                            props.mapColor(dye.getMapColor())
                                    .randomTicks()
                                    .strength(1.5F)
                                    .sound(SoundType.AMETHYST)
                            ,
                            budSmall::get, budMedium::get, budLarge::get, cluster::get,
                            5
                    )
            );

            AMETHYST.put(dye, new AmethystSet(block, budding, budSmall, budMedium, budLarge, cluster));

            AMETHYST_ITEMS.put(dye, new AmethystItemSet(
                    ModItems.ITEMS.registerSimpleBlockItem(solidPath,   block),
                    ModItems.ITEMS.registerSimpleBlockItem(buddingPath, budding),
                    ModItems.ITEMS.registerSimpleBlockItem(smallPath,   budSmall),
                    ModItems.ITEMS.registerSimpleBlockItem(mediumPath,  budMedium),
                    ModItems.ITEMS.registerSimpleBlockItem(largePath,   budLarge),
                    ModItems.ITEMS.registerSimpleBlockItem(clusterPath, cluster)
            ));

            String litPath = "lit_amethyst_block_" + color;
            DeferredBlock<Block> litBlock = BLOCKS.registerBlock(
                    litPath,
                    props -> new LitColoredAmethystBlock(
                            props
                                    .mapColor(dye.getMapColor())
                                    .strength(1.5F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .sound(SoundType.AMETHYST)

                                    .lightLevel(s -> 15)
                    )
            );

            LIT_AMETHYST_BLOCKS.put(dye, litBlock);

            DeferredItem<BlockItem> litItem =
                    ModItems.ITEMS.registerSimpleBlockItem(litPath, litBlock);
            LIT_AMETHYST_ITEMS.put(dye, litItem);
        }
    }

    /* Stream helpers */
    public static Stream<DeferredBlock<? extends Block>> streamAllAmethystBlocks() {
        return AMETHYST.values().stream().flatMap(v -> Stream.of(
                v.block(), v.budding(), v.budSmall(), v.budMedium(), v.budLarge(), v.cluster()
        ));
    }

    public static Stream<DeferredBlock<? extends Block>> streamAllLitAmethystBlocks() {
        return LIT_AMETHYST_BLOCKS.values().stream();
    }

    /* ---------- BlockItems---------- */
    public static final DeferredItem<BlockItem> BARRIER_BLOCK_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("barrier_block", BARRIER_BLOCK);
    public static final DeferredItem<BlockItem> CHICKEN_BLOCK_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("chicken_block", CHICKEN_BLOCK);
    public static final DeferredItem<BlockItem> INFINITE_DISPENSER_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("infinite_dispenser", INFINITE_DISPENSER);
    public static final DeferredItem<BlockItem> REDSTONE_TRANSMITTER_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("redstone_transmitter", REDSTONE_TRANSMITTER);
    public static final DeferredItem<BlockItem> REDSTONE_RECEIVER_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("redstone_receiver", REDSTONE_RECEIVER);
    public static final DeferredItem<BlockItem> COSMIC_MOB_SPAWNER_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("cosmic_mob_spawner", COSMIC_MOB_SPAWNER);
    public static final DeferredItem<BlockItem> COSMIC_RIFT_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("cosmic_rift", COSMIC_RIFT);

    // ===== BlockItem for Class Selector Block =====
    public static final DeferredItem<BlockItem> CLASS_SELECTOR_BLOCK_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("class_selector_block", CLASS_SELECTOR_BLOCK);

    // == BlockItem for Speical Chests ==
    public static final DeferredItem<BlockItem> BOGATYR_CHEST_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("bogatyr_chest", BOGATYR_CHEST);
    public static final DeferredItem<BlockItem> DEADEYE_CHEST_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("deadeye_chest", DEADEYE_CHEST);
    public static final DeferredItem<BlockItem> DRAGOON_CHEST_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("dragoon_chest", DRAGOON_CHEST);
    public static final DeferredItem<BlockItem> JUDICATOR_CHEST_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("judicator_chest", JUDICATOR_CHEST);
    public static final DeferredItem<BlockItem> METALMANCER_CHEST_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("metalmancer_chest", METALMANCER_CHEST);
    public static final DeferredItem<BlockItem> PYROCLAST_CHEST_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("pyroclast_chest", PYROCLAST_CHEST);
    public static final DeferredItem<BlockItem> THEURGIST_CHEST_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("theurgist_chest", THEURGIST_CHEST);
    public static final DeferredItem<BlockItem> VENEFEX_CHEST_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("venefex_chest", VENEFEX_CHEST);

    /* ---------- BlockItems for Spectral Blooms (for creative tab & pickup) ---------- */
    public static final DeferredItem<BlockItem> LESSER_BLOOM_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("lesser_bloom", LESSER_BLOOM);
    public static final DeferredItem<BlockItem> CAVERN_RESIDUE_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("cavern_residue", CAVERN_RESIDUE);

    public static final DeferredItem<BlockItem> BLOOM_OF_QUIET_ASSURANCE_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("bloom_of_quiet_assurance", BLOOM_OF_QUIET_ASSURANCE);
    public static final DeferredItem<BlockItem> BLOOM_OF_GENTLE_LIES_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("bloom_of_gentle_lies", BLOOM_OF_GENTLE_LIES);
    public static final DeferredItem<BlockItem> BLOOM_OF_WANING_MERCY_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("bloom_of_waning_mercy", BLOOM_OF_WANING_MERCY);
    public static final DeferredItem<BlockItem> BLOOM_OF_CONSTRICTING_BONDS_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("bloom_of_constricting_bonds", BLOOM_OF_CONSTRICTING_BONDS);
    public static final DeferredItem<BlockItem> BLOOM_OF_UNSPOKEN_RESIGNATION_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("bloom_of_unspoken_resignation", BLOOM_OF_UNSPOKEN_RESIGNATION);
    public static final DeferredItem<BlockItem> BLOOM_OF_ELEGY_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("bloom_of_elegy", BLOOM_OF_ELEGY);
}
