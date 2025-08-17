package net.goui.cosmicdungeon.block;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.amethyst.ColoredAmethystBlock;
import net.goui.cosmicdungeon.block.amethyst.ColoredBudBlock;
import net.goui.cosmicdungeon.block.amethyst.ColoredBuddingAmethystBlock;
import net.goui.cosmicdungeon.block.amethyst.ColoredClusterBlock;
import net.goui.cosmicdungeon.block.custom.ChickenBlock;
import net.goui.cosmicdungeon.block.custom.MagicBlock;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.sound.ModSoundTypes;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

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
                    props.strength(2f).noLootTable()
            )
    );

    public static final DeferredBlock<Block> CHICKEN_BLOCK = BLOCKS.registerBlock(
            "chicken_block",
            (BlockBehaviour.Properties props) -> new ChickenBlock(
                    props.strength(1.0f).noLootTable().sound(ModSoundTypes.CHICKEN)
            )
    );

    /* ---------- Colored Amethyst: 6 variants × 16 colors ---------- */

    public record AmethystSet(
            DeferredBlock<Block> block,
            DeferredBlock<Block> budding,
            DeferredBlock<Block> budSmall,
            DeferredBlock<Block> budMedium,
            DeferredBlock<Block> budLarge,
            DeferredBlock<Block> cluster
    ) {}

    // Blocks (used by datagen and elsewhere)
    public static final Map<DyeColor, AmethystSet> AMETHYST = new EnumMap<>(DyeColor.class);

    // Matching BlockItems (no item for budding)
    public record AmethystItemSet(
            DeferredItem<BlockItem> block,
            DeferredItem<BlockItem> budding,
            DeferredItem<BlockItem> budSmall,
            DeferredItem<BlockItem> budMedium,
            DeferredItem<BlockItem> budLarge,
            DeferredItem<BlockItem> cluster
    ) {}

    public static final Map<DyeColor, AmethystItemSet> AMETHYST_ITEMS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor dye : DyeColor.values()) {
            final String color = dye.getName();

            final String solidPath   = "amethyst_block_" + color;
            final String buddingPath = "budding_amethyst_" + color;
            final String smallPath   = "small_amethyst_bud_" + color;
            final String mediumPath  = "medium_amethyst_bud_" + color;
            final String largePath   = "large_amethyst_bud_" + color;
            final String clusterPath = "amethyst_cluster_" + color;

            // --- Blocks ---
            DeferredBlock<Block> block = BLOCKS.registerBlock(
                    solidPath,
                    (BlockBehaviour.Properties props) -> new ColoredAmethystBlock(
                            props.mapColor(dye.getMapColor())
                                    .strength(1.5F, 6.0F)
                                    .requiresCorrectToolForDrops()
                                    .sound(SoundType.AMETHYST)
                                    .noLootTable()
                    )
            );

// small
            DeferredBlock<Block> budSmall = BLOCKS.registerBlock(
                    smallPath,
                    props -> ColoredBudBlock.small(
                            props.noOcclusion().strength(1.0F).sound(SoundType.AMETHYST).noLootTable().lightLevel(s -> 1)
                    )
            );


// medium
            DeferredBlock<Block> budMedium = BLOCKS.registerBlock(
                    mediumPath,
                    props -> ColoredBudBlock.medium(
                            props.noOcclusion().strength(1.0F).sound(SoundType.AMETHYST).noLootTable().lightLevel(s -> 2)
                    )
            );

// large
            DeferredBlock<Block> budLarge = BLOCKS.registerBlock(
                    largePath,
                    props -> ColoredBudBlock.large(
                            props.noOcclusion().strength(1.0F).sound(SoundType.AMETHYST).noLootTable().lightLevel(s -> 4)
                    )
            );

// cluster
            DeferredBlock<Block> cluster = BLOCKS.registerBlock(
                    clusterPath,
                    props -> new ColoredClusterBlock(
                            props.noOcclusion().strength(1.0F).sound(SoundType.AMETHYST).noLootTable().lightLevel(s -> 5)
                    )
            );

            DeferredBlock<Block> budding = BLOCKS.registerBlock(
                    buddingPath,
                    (BlockBehaviour.Properties props) -> new ColoredBuddingAmethystBlock(
                            props.mapColor(dye.getMapColor())
                                    .randomTicks()
                                    .strength(1.5F)
                                    .sound(SoundType.AMETHYST)
                                    .noLootTable(),
                            budSmall::get, budMedium::get, budLarge::get, cluster::get,
                            5 // 1-in-5 growth chance
                    )
            );

            AMETHYST.put(dye, new AmethystSet(block, budding, budSmall, budMedium, budLarge, cluster));

            // --- BlockItems (NO item for budding) ---
            AMETHYST_ITEMS.put(dye, new AmethystItemSet(
                    ModItems.ITEMS.registerSimpleBlockItem(solidPath,   block),
                    ModItems.ITEMS.registerSimpleBlockItem(buddingPath, budding),   // <— add budding item
                    ModItems.ITEMS.registerSimpleBlockItem(smallPath,   budSmall),
                    ModItems.ITEMS.registerSimpleBlockItem(mediumPath,  budMedium),
                    ModItems.ITEMS.registerSimpleBlockItem(largePath,   budLarge),
                    ModItems.ITEMS.registerSimpleBlockItem(clusterPath, cluster)
            ));

        }
    }

    /* Stream helper (optional) */
    public static Stream<DeferredBlock<? extends Block>> streamAllAmethystBlocks() {
        return AMETHYST.values().stream().flatMap(v -> Stream.of(
                v.block(), v.budding(), v.budSmall(), v.budMedium(), v.budLarge(), v.cluster()
        ));
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
}
