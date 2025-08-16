package net.goui.cosmicdungeon.datagen;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, CosmicDungeonMod.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                ModBlocks.BISMUTH_BLOCK.get(),
                ModBlocks.BISMUTH_ORE.get(),
                ModBlocks.BISMUTH_DEEPSLATE_ORE.get(),
                ModBlocks.COLORED_AMETHYST_BLOCK.get(),
                ModBlocks.COLORED_BUDDING_AMETHYST.get(),
                ModBlocks.COLORED_AMETHYST_BUD_SMALL.get(),
                ModBlocks.COLORED_AMETHYST_BUD_MEDIUM.get(),
                ModBlocks.COLORED_AMETHYST_BUD_LARGE.get(),
                ModBlocks.COLORED_AMETHYST_CLUSTER.get());

        tag(BlockTags.NEEDS_IRON_TOOL).add(
                ModBlocks.BISMUTH_DEEPSLATE_ORE.get(),
                ModBlocks.COLORED_AMETHYST_BLOCK.get(),
                ModBlocks.COLORED_BUDDING_AMETHYST.get(),
                ModBlocks.COLORED_AMETHYST_CLUSTER.get());

        tag(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL).add(
                ModBlocks.COLORED_AMETHYST_BLOCK.get(),
                ModBlocks.COLORED_BUDDING_AMETHYST.get(),
                ModBlocks.COLORED_AMETHYST_CLUSTER.get()
        );
        // ---- Your custom grouping tags
        tag(net.goui.cosmicdungeon.util.ModTags.Blocks.AMETHYST_BLOCKS)
                .add(ModBlocks.COLORED_AMETHYST_BLOCK.get());

        tag(net.goui.cosmicdungeon.util.ModTags.Blocks.BUDDING_AMETHYST)
                .add(ModBlocks.COLORED_BUDDING_AMETHYST.get());

        tag(net.goui.cosmicdungeon.util.ModTags.Blocks.AMETHYST_BUDS_SMALL)
                .add(ModBlocks.COLORED_AMETHYST_BUD_SMALL.get());

        tag(net.goui.cosmicdungeon.util.ModTags.Blocks.AMETHYST_BUDS_MEDIUM)
                .add(ModBlocks.COLORED_AMETHYST_BUD_MEDIUM.get());

        tag(net.goui.cosmicdungeon.util.ModTags.Blocks.AMETHYST_BUDS_LARGE)
                .add(ModBlocks.COLORED_AMETHYST_BUD_LARGE.get());

        tag(net.goui.cosmicdungeon.util.ModTags.Blocks.AMETHYST_CLUSTERS)
                .add(ModBlocks.COLORED_AMETHYST_CLUSTER.get());
    }
}