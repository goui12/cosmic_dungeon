package net.goui.cosmicdungeon.datagen;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {

    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, CosmicDungeonMod.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // ===== Vanilla: Bismuth + your spawner =====
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                ModBlocks.BISMUTH_BLOCK.get(),
                ModBlocks.BISMUTH_ORE.get(),
                ModBlocks.BISMUTH_DEEPSLATE_ORE.get(),
                ModBlocks.COSMIC_MOB_SPAWNER.get()
        );

        this.tag(BlockTags.NEEDS_IRON_TOOL).add(
                ModBlocks.BISMUTH_DEEPSLATE_ORE.get(),
                ModBlocks.COSMIC_MOB_SPAWNER.get()
        );

        this.tag(BlockTags.MINEABLE_WITH_AXE).add(
                ModBlocks.PILE_OF_BOOKS.get()
        );

        // ===== Vanilla: Colored Amethyst (all colors, all stages) =====
        Block[] amethystAllStages = allAmethystBlocks();
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(amethystAllStages);
        this.tag(BlockTags.NEEDS_IRON_TOOL).add(amethystAllStages);

        // ===== Custom tags (your namespace) =====
        this.tag(ModTags.Blocks.AMETHYST_BLOCKS).add(
                ModBlocks.AMETHYST.values().stream()
                        .map(v -> v.block().get())
                        .toArray(Block[]::new)
        );

        this.tag(ModTags.Blocks.BUDDING_AMETHYST).add(
                ModBlocks.AMETHYST.values().stream()
                        .map(v -> v.budding().get())
                        .toArray(Block[]::new)
        );

        this.tag(ModTags.Blocks.AMETHYST_BUDS_SMALL).add(
                ModBlocks.AMETHYST.values().stream()
                        .map(v -> v.budSmall().get())
                        .toArray(Block[]::new)
        );

        this.tag(ModTags.Blocks.AMETHYST_BUDS_MEDIUM).add(
                ModBlocks.AMETHYST.values().stream()
                        .map(v -> v.budMedium().get())
                        .toArray(Block[]::new)
        );

        this.tag(ModTags.Blocks.AMETHYST_BUDS_LARGE).add(
                ModBlocks.AMETHYST.values().stream()
                        .map(v -> v.budLarge().get())
                        .toArray(Block[]::new)
        );

        this.tag(ModTags.Blocks.AMETHYST_CLUSTERS).add(
                ModBlocks.AMETHYST.values().stream()
                        .map(v -> v.cluster().get())
                        .toArray(Block[]::new)
        );
    }

    /** Helper: all colored amethyst blocks (any stage) as a flat array for tag().add(...) */
    private static Block[] allAmethystBlocks() {
        return ModBlocks.streamAllAmethystBlocks()
                .map(DeferredBlock::get)
                .toArray(Block[]::new);
    }

    @Override
    public String getName() {
        return "CosmicDungeon Block Tags";
    }
}
