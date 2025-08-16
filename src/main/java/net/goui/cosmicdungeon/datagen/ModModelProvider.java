package net.goui.cosmicdungeon.datagen;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.item.ModItems;


import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, CosmicDungeonMod.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        /* ITEMS */
        itemModels.generateFlatItem(ModItems.BISMUTH.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.RAW_BISMUTH.get(), ModelTemplates.FLAT_ITEM);

        itemModels.generateFlatItem(ModItems.RADISH.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.FROSTFIRE_ICE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.STARLIGHT_ASHES.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.BARNACLED_PEARL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SHATTERED_REALITY_OF_SHUDDE_MELL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SEISMIC_CORE_FRAGMENT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.BROODING_FORK.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CHISEL.get(), ModelTemplates.FLAT_ITEM);



        // ---- Item models (generated; layer0 should be grayscale & tinted in ItemColor)
        itemModels.generateFlatItem(ModBlocks.COLORED_AMETHYST_BLOCK.get().asItem(), net.minecraft.client.data.models.model.ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModBlocks.COLORED_BUDDING_AMETHYST.get().asItem(), net.minecraft.client.data.models.model.ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModBlocks.COLORED_AMETHYST_BUD_SMALL.get().asItem(), net.minecraft.client.data.models.model.ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModBlocks.COLORED_AMETHYST_BUD_MEDIUM.get().asItem(), net.minecraft.client.data.models.model.ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModBlocks.COLORED_AMETHYST_BUD_LARGE.get().asItem(), net.minecraft.client.data.models.model.ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModBlocks.COLORED_AMETHYST_CLUSTER.get().asItem(), net.minecraft.client.data.models.model.ModelTemplates.FLAT_ITEM);
        blockModels.createTrivialCube(ModBlocks.BISMUTH_ORE.get());
        blockModels.createTrivialCube(ModBlocks.BISMUTH_DEEPSLATE_ORE.get());
        blockModels.createTrivialCube(ModBlocks.MAGIC_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.BISMUTH_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.CHICKEN_BLOCK.get());

    }
}