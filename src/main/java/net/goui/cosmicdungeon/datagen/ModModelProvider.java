package net.goui.cosmicdungeon.datagen;

import com.mojang.math.Quadrant;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.common.color.AmethystColor;
import net.goui.cosmicdungeon.item.ModItems;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, CosmicDungeonMod.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        /* ===== Flat items ===== */
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
        itemModels.generateFlatItem(ModItems.AEGIS_OF_ABSOLUTION.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.AEGIS_OF_CHAOS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.AZATHOTS_HAMMER_OF_FINAL_VERDICT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.FORGE_CALLERS_MAUL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.HIGH_VELOCITY_ARROW.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.TOTEM_OF_DOG_WHISPERING.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.DOOR_KEY.get(), ModelTemplates.FLAT_ITEM);


        /* ===== Simple cubes ===== */
        blockModels.createTrivialCube(ModBlocks.BISMUTH_ORE.get());
        blockModels.createTrivialCube(ModBlocks.BISMUTH_DEEPSLATE_ORE.get());
        blockModels.createTrivialCube(ModBlocks.MAGIC_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.BISMUTH_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.CHICKEN_BLOCK.get());

        /* ===== Pile of Books ===== */
        {
            var b = ModBlocks.PILE_OF_BOOKS.get();
            var blockModel = rlMod("block/pile_of_books");
            blockModels.blockStateOutput.accept(
                    MultiVariantGenerator.dispatch(
                            b,
                            new MultiVariant(WeightedList.of(new Variant(blockModel)))
                    )
            );
            itemModels.generateFlatItem(b.asItem(), ModelTemplates.FLAT_ITEM);
        }

        /* ===== Colored amethyst sets ===== */
        for (AmethystColor ac : AmethystColor.values()) {
            DyeColor dye = switch (ac) {
                case WHITE -> DyeColor.WHITE;
                case ORANGE -> DyeColor.ORANGE;
                case MAGENTA -> DyeColor.MAGENTA;
                case LIGHT_BLUE -> DyeColor.LIGHT_BLUE;
                case YELLOW -> DyeColor.YELLOW;
                case LIME -> DyeColor.LIME;
                case PINK -> DyeColor.PINK;
                case GRAY -> DyeColor.GRAY;
                case LIGHT_GRAY -> DyeColor.LIGHT_GRAY;
                case CYAN -> DyeColor.CYAN;
                case PURPLE -> DyeColor.PURPLE;
                case BLUE -> DyeColor.BLUE;
                case BROWN -> DyeColor.BROWN;
                case GREEN -> DyeColor.GREEN;
                case RED -> DyeColor.RED;
                case BLACK -> DyeColor.BLACK;
            };

            var v = ModBlocks.AMETHYST.get(dye);
            if (v == null) continue;

            blockModels.createTrivialCube(v.block().get());
            blockModels.createTrivialCube(v.budding().get());

            var litHolder = ModBlocks.LIT_AMETHYST_BLOCKS.get(dye);
            if (litHolder != null) {
                blockModels.createTrivialCube(litHolder.get());
            }

            crossFacing(blockModels, itemModels, v.budSmall().get());
            crossFacing(blockModels, itemModels, v.budMedium().get());
            crossFacing(blockModels, itemModels, v.budLarge().get());
            crossFacing(blockModels, itemModels, v.cluster().get());
        }
    }

    // === Helpers ===
    private static ResourceLocation rlMod(String path) {
        return ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path);
    }

    private void crossFacing(BlockModelGenerators blockModels, ItemModelGenerators itemModels, Block b) {
        String id = b.builtInRegistryHolder().key().location().getPath();
        ResourceLocation modelLoc = rlMod("block/" + id);
        ResourceLocation tex = rlMod("block/" + id);

        blockModels.modelOutput.accept(modelLoc, () -> {
            var root = new com.google.gson.JsonObject();
            root.addProperty("parent", "block/cross");
            var texObj = new com.google.gson.JsonObject();
            texObj.addProperty("cross", tex.toString());
            texObj.addProperty("particle", tex.toString());
            root.add("textures", texObj);
            root.addProperty("render_type", "cutout");
            return root;
        });

        PropertyDispatch<MultiVariant> pd =
                PropertyDispatch.initial(BlockStateProperties.FACING)
                        .select(Direction.UP,    new MultiVariant(WeightedList.of(new Variant(modelLoc))))
                        .select(Direction.DOWN,  new MultiVariant(WeightedList.of(new Variant(modelLoc).withXRot(Quadrant.R180))))
                        .select(Direction.NORTH, new MultiVariant(WeightedList.of(new Variant(modelLoc).withXRot(Quadrant.R90))))
                        .select(Direction.SOUTH, new MultiVariant(WeightedList.of(new Variant(modelLoc).withXRot(Quadrant.R90).withYRot(Quadrant.R180))))
                        .select(Direction.WEST,  new MultiVariant(WeightedList.of(new Variant(modelLoc).withXRot(Quadrant.R90).withYRot(Quadrant.R270))))
                        .select(Direction.EAST,  new MultiVariant(WeightedList.of(new Variant(modelLoc).withXRot(Quadrant.R90).withYRot(Quadrant.R90))));

        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(b).with(pd));

        ResourceLocation itemLoc = rlMod("item/" + id);
        itemModels.modelOutput.accept(itemLoc, () -> {
            var root = new com.google.gson.JsonObject();
            root.addProperty("parent", modelLoc.toString());
            return root;
        });
    }
}
