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
import net.goui.cosmicdungeon.block.custom.ChickenBlock;
import net.goui.cosmicdungeon.block.custom.ClassSelectorBlock;
import net.goui.cosmicdungeon.block.custom.CosmicMobSpawnerBlock;
import net.goui.cosmicdungeon.block.custom.CosmicRiftPlacerBlock;
import net.goui.cosmicdungeon.block.custom.CosmicRiftTileBlock;
import net.goui.cosmicdungeon.block.custom.InfiniteDispenserBlock;
import net.goui.cosmicdungeon.block.custom.MagicBlock;
import net.goui.cosmicdungeon.block.custom.SpectralBloomBlock;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.redstone.rf.RedstoneReceiverBlock;
import net.goui.cosmicdungeon.redstone.rf.RedstoneTransmitterBlock;
import net.goui.cosmicdungeon.sound.ModSoundTypes;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.goui.cosmicdungeon.block.custom.ClassLockedChestBlock;
import net.goui.cosmicdungeon.block.entity.ModBlockEntities;
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

    /* ---------- Regular blocks ---------- */

    public static final DeferredBlock<Block> BISMUTH_BLOCK = BLOCKS.registerBlock(
            "bismuth_block",
            (BlockBehaviour.Properties props) -> new Block(
                    props.strength(4f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
            )
    );

    public static final DeferredBlock<Block> BISMUTH_ORE = BLOCKS.registerBlock(
            "bismuth_ore",
            (BlockBehaviour.Properties props) -> new DropExperienceBlock(
                    UniformInt.of(2, 4),
                    props.strength(3f).requiresCorrectToolForDrops().sound(SoundType.STONE)
            )
    );

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

    public static final DeferredBlock<Block> BISMUTH_DEEPSLATE_ORE = BLOCKS.registerBlock(
            "bismuth_deepslate_ore",
            (BlockBehaviour.Properties props) -> new DropExperienceBlock(
                    UniformInt.of(3, 6),
                    props.strength(4f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)
            )
    );

    public static final DeferredBlock<Block> MAGIC_BLOCK = BLOCKS.registerBlock(
            "magic_block",
            (BlockBehaviour.Properties props) -> new MagicBlock(
                    props.strength(2f)
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

    // POTTED variants (no item). These MUST exist for flower pots.
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

        pot.addPlant(BLOOM_OF_QUIET_ASSURANCE.getId(), POTTED_BLOOM_OF_QUIET_ASSURANCE);
        pot.addPlant(BLOOM_OF_GENTLE_LIES.getId(), POTTED_BLOOM_OF_GENTLE_LIES);
        pot.addPlant(BLOOM_OF_WANING_MERCY.getId(), POTTED_BLOOM_OF_WANING_MERCY);
        pot.addPlant(BLOOM_OF_CONSTRICTING_BONDS.getId(), POTTED_BLOOM_OF_CONSTRICTING_BONDS);
        pot.addPlant(BLOOM_OF_UNSPOKEN_RESIGNATION.getId(), POTTED_BLOOM_OF_UNSPOKEN_RESIGNATION);
        pot.addPlant(BLOOM_OF_ELEGY.getId(), POTTED_BLOOM_OF_ELEGY);
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

    /* ---------- BlockItems for non-amethyst misc ---------- */
    public static final DeferredItem<BlockItem> BISMUTH_BLOCK_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("bismuth_block", BISMUTH_BLOCK);
    public static final DeferredItem<BlockItem> BISMUTH_ORE_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("bismuth_ore", BISMUTH_ORE);
    public static final DeferredItem<BlockItem> BISMUTH_DEEPSLATE_ORE_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("bismuth_deepslate_ore", BISMUTH_DEEPSLATE_ORE);
    public static final DeferredItem<BlockItem> MAGIC_BLOCK_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem("magic_block", MAGIC_BLOCK);
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
