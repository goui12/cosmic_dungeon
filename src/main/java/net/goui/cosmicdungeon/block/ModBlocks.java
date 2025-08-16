package net.goui.cosmicdungeon.block;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.custom.*;
import net.goui.cosmicdungeon.block.custom.amethyst.ColoredAmethystBlock;
import net.goui.cosmicdungeon.block.custom.amethyst.ColoredBuddingAmethystBlock;
import net.goui.cosmicdungeon.block.custom.amethyst.ColoredClusterBlock;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.sound.ModSoundTypes;
import net.goui.cosmicdungeon.block.custom.amethyst.ColoredBudBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CosmicDungeonMod.MOD_ID);
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
    // Block registration
    public static final DeferredBlock<Block> BISMUTH_BLOCK = BLOCKS.registerBlock("bismuth_block",
            props -> new Block(props.strength(4f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));


    public static final DeferredBlock<Block> BISMUTH_ORE = BLOCKS.registerBlock("bismuth_ore",
            props -> new DropExperienceBlock(UniformInt.of(2, 4), props.strength(3f).requiresCorrectToolForDrops().sound(SoundType.STONE)
            ));

    public static final DeferredBlock<Block> BISMUTH_DEEPSLATE_ORE = BLOCKS.registerBlock("bismuth_deepslate_ore",
                    props -> new DropExperienceBlock(UniformInt.of(3, 6),
                            props.strength(4f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)
                    ));
    public static final DeferredBlock<Block> MAGIC_BLOCK = BLOCKS.registerBlock("magic_block",
            props -> new MagicBlock(props.strength(2f).noLootTable()));

    public static final DeferredBlock<Block> CHICKEN_BLOCK = BLOCKS.registerBlock(
            "chicken_block",
            (net.minecraft.world.level.block.state.BlockBehaviour.Properties props) ->
                    new ChickenBlock(props.strength(1.0f).noLootTable().sound(ModSoundTypes.CHICKEN))
    );

    //Amethyst Family Block Registration
    public static final DeferredBlock<Block> COLORED_AMETHYST_BLOCK =
            BLOCKS.registerBlock("colored_amethyst_block",
                    props -> new ColoredAmethystBlock(props
                            .strength(1.5F)
                            .sound(SoundType.AMETHYST)));

    public static final DeferredBlock<Block> COLORED_BUDDING_AMETHYST =
            BLOCKS.registerBlock("colored_budding_amethyst",
                    props -> new ColoredBuddingAmethystBlock(props
                            .randomTicks()
                            .strength(1.5F)
                            .sound(SoundType.AMETHYST)));

    public static final DeferredBlock<Block> COLORED_AMETHYST_BUD_SMALL =
            BLOCKS.registerBlock("colored_amethyst_bud_small",
                    props -> new ColoredBudBlock(props
                            .noOcclusion()
                            .strength(1.5F)
                            .sound(SoundType.AMETHYST)));

    public static final DeferredBlock<Block> COLORED_AMETHYST_BUD_MEDIUM =
            BLOCKS.registerBlock("colored_amethyst_bud_medium",
                    props -> new ColoredBudBlock(props
                            .noOcclusion()
                            .strength(1.5F)
                            .sound(SoundType.AMETHYST)));

    public static final DeferredBlock<Block> COLORED_AMETHYST_BUD_LARGE =
            BLOCKS.registerBlock("colored_amethyst_bud_large",
                    props -> new ColoredBudBlock(props
                            .noOcclusion()
                            .strength(1.5F)
                            .sound(SoundType.AMETHYST)));

    public static final DeferredBlock<Block> COLORED_AMETHYST_CLUSTER =
            BLOCKS.registerBlock("colored_amethyst_cluster",
                    props -> new ColoredClusterBlock(props
                            .noOcclusion()
                            .strength(1.5F)
                            .sound(SoundType.AMETHYST)));


    // Item registration
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

    //Amethyst family item registration

    public static final DeferredItem<BlockItem> COLORED_AMETHYST_BLOCK_ITEM =
            ModItems.ITEMS.register("colored_amethyst_block",
                    () -> new net.goui.cosmicdungeon.item.custom.ColoredBlockItem(
                            COLORED_AMETHYST_BLOCK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> COLORED_BUDDING_AMETHYST_ITEM =
            ModItems.ITEMS.register("colored_budding_amethyst",
                    () -> new net.goui.cosmicdungeon.item.custom.ColoredBlockItem(
                            COLORED_BUDDING_AMETHYST.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> COLORED_AMETHYST_BUD_SMALL_ITEM =
            ModItems.ITEMS.register("colored_amethyst_bud_small",
                    () -> new net.goui.cosmicdungeon.item.custom.ColoredBlockItem(
                            COLORED_AMETHYST_BUD_SMALL.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> COLORED_AMETHYST_BUD_MEDIUM_ITEM =
            ModItems.ITEMS.register("colored_amethyst_bud_medium",
                    () -> new net.goui.cosmicdungeon.item.custom.ColoredBlockItem(
                            COLORED_AMETHYST_BUD_MEDIUM.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> COLORED_AMETHYST_BUD_LARGE_ITEM =
            ModItems.ITEMS.register("colored_amethyst_bud_large",
                    () -> new net.goui.cosmicdungeon.item.custom.ColoredBlockItem(
                            COLORED_AMETHYST_BUD_LARGE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> COLORED_AMETHYST_CLUSTER_ITEM =
            ModItems.ITEMS.register("colored_amethyst_cluster",
                    () -> new net.goui.cosmicdungeon.item.custom.ColoredBlockItem(
                            COLORED_AMETHYST_CLUSTER.get(), new Item.Properties()));
}
