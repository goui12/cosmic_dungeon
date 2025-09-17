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
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.stream.Stream;

public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, CosmicDungeonMod.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // Shorthand helpers
        java.util.function.Consumer<net.minecraft.world.item.Item> FLAT = i -> itemModels.generateFlatItem(i, ModelTemplates.FLAT_ITEM);
        java.util.function.Consumer<net.minecraft.world.item.Item> MACE = i -> itemModels.generateFlatItem(i, ModelTemplates.FLAT_HANDHELD_MACE_ITEM);
        java.util.function.Consumer<net.minecraft.world.item.Item> ROD  = i -> itemModels.generateFlatItem(i, ModelTemplates.FLAT_HANDHELD_ROD_ITEM);

        // ===== Flat/simple items =====
        FLAT.accept(ModItems.BISMUTH.get());
        FLAT.accept(ModItems.RAW_BISMUTH.get());
        FLAT.accept(ModItems.RADISH.get());
        FLAT.accept(ModItems.FROSTFIRE_ICE.get());
        FLAT.accept(ModItems.STARLIGHT_ASHES.get());
        FLAT.accept(ModItems.BARNACLED_PEARL.get());
        FLAT.accept(ModItems.SHATTERED_REALITY_OF_SHUDDE_MELL.get());
        FLAT.accept(ModItems.SEISMIC_CORE_FRAGMENT.get());
        FLAT.accept(ModItems.BROODING_FORK.get());
        FLAT.accept(ModItems.CHISEL.get());
        FLAT.accept(ModItems.AEGIS_OF_ABSOLUTION.get());
        FLAT.accept(ModItems.AEGIS_OF_CHAOS.get());
        FLAT.accept(ModItems.AZATHOTS_HAMMER_OF_FINAL_VERDICT.get());
        FLAT.accept(ModItems.FORGE_CALLERS_MAUL.get());
        FLAT.accept(ModItems.HIGH_VELOCITY_ARROW.get());
        FLAT.accept(ModItems.TOTEM_OF_DOG_WHISPERING.get());
        FLAT.accept(ModItems.DOOR_KEY.get());

        // ===== Judicator — D1 T3 (Diamond) =====
        MACE.accept(ModItems.EDICT_OF_SILENCE.get());

        // Vowkeeper uses your hand-authored custom-geometry JSONs.
        registerExternalItem(itemModels, ModItems.VOWKEEPER.get(), rlMod("item/vowkeeper"));
        // DO NOT generate any model here.

        FLAT.accept(ModItems.VIELPIERCER.get());
        FLAT.accept(ModItems.SCINTILLA_VITALIS.get());
        FLAT.accept(ModItems.GUSTING_BOLT.get());
        FLAT.accept(ModItems.SHARD_OF_THE_MAD_STRIDER.get());
        FLAT.accept(ModItems.GONFALON_OF_JUDIFICATION.get());

        // ===== Judicator — D1 T4 (Netherite) =====
        MACE.accept(ModItems.EDICT_OF_RUIN.get());
        registerExternalItem(itemModels, ModItems.AEGIS_OF_TRUTH.get(), rlMod("item/aegis_of_truth"));
        FLAT.accept(ModItems.LUX_VITALIS.get());
        FLAT.accept(ModItems.EBONSIGHT.get());
        FLAT.accept(ModItems.FANG_OF_THE_BRUTE.get());
        FLAT.accept(ModItems.ENSIGN_OF_THE_JUDICATOR.get());

        // ===== Judicator — D2 T1 (Chainmail) =====
        FLAT.accept(ModItems.VISOR_OF_THE_RESOLUTE.get());
        FLAT.accept(ModItems.CUIRASS_OF_PURPOSE.get());
        FLAT.accept(ModItems.CHAUSSES_OF_THE_PLEDGE.get());
        FLAT.accept(ModItems.SABATONS_OF_THE_UNHEARD_OATH.get());
        MACE.accept(ModItems.EMPOWERED_HAMMER.get());
        registerExternalItem(itemModels, ModItems.WORKED_PLANK.get(), rlMod("item/worked_plank"));

        FLAT.accept(ModItems.ARROW_OF_WRIT.get());
        FLAT.accept(ModItems.STANDARD_OF_THE_INITIATE_JUDGE.get());

        // ===== Judicator — D2 T2 (Iron) =====
        FLAT.accept(ModItems.OATHBOUND_VISOR.get());
        FLAT.accept(ModItems.CUIRASS_OF_RESOLUTION.get());
        FLAT.accept(ModItems.GREAVES_OF_BINDING.get());
        FLAT.accept(ModItems.SABATONS_OF_THE_PACT.get());
        MACE.accept(ModItems.REINFORCED_HAMMER.get());
        registerExternalItem(itemModels, ModItems.REINFORCED_IRON_SLAB.get(), rlMod("item/reinforced_iron_slab"));
        FLAT.accept(ModItems.STANDARD_OF_THE_NASCENT_JUDGE.get());

        // ===== Judicator — D3 T3 (Diamond) =====
        FLAT.accept(ModItems.VISOR_OF_THE_COVENANT.get());
        FLAT.accept(ModItems.CUIRASS_OF_DEVOTION.get());
        FLAT.accept(ModItems.GREAVES_OF_THE_TRIBUNAL_PATH.get());
        FLAT.accept(ModItems.SABATONS_OF_PURSUIT.get());
        MACE.accept(ModItems.TIDAL_MACE.get());
        registerExternalItem(itemModels, ModItems.SHIELD_OF_TIDAL_FORCE.get(), rlMod("item/shield_of_tidal_force"));
        FLAT.accept(ModItems.STANDARD_OF_THE_ABYSSAL_JUDGE.get());

        // ===== Judicator — D3 T4 (Netherite) =====
        FLAT.accept(ModItems.VISOR_OF_IMMUTABLE_WILL.get());
        FLAT.accept(ModItems.CUIRASS_OF_CONVICTION.get());
        FLAT.accept(ModItems.GREAVES_OF_THE_ETERNAL_MARCH.get());
        FLAT.accept(ModItems.SABATONS_OF_BOUNDLESS_STEPS.get());
        MACE.accept(ModItems.ABYSSAL_MACE.get());
        registerExternalItem(itemModels, ModItems.SHIELD_OF_THE_DEEP.get(), rlMod("item/shield_of_the_deep"));


        // ===== Simple cubes =====
        blockModels.createTrivialCube(ModBlocks.BISMUTH_ORE.get());
        blockModels.createTrivialCube(ModBlocks.BISMUTH_DEEPSLATE_ORE.get());
        blockModels.createTrivialCube(ModBlocks.MAGIC_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.BISMUTH_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.CHICKEN_BLOCK.get());

        // ===== Pile of Books =====
        {
            var b = ModBlocks.PILE_OF_BOOKS.get();
            var blockModel = rlMod("block/pile_of_books");
            blockModels.blockStateOutput.accept(
                    MultiVariantGenerator.dispatch(b, new MultiVariant(WeightedList.of(new Variant(blockModel))))
            );
            FLAT.accept(b.asItem());
        }

        // ===== Colored amethyst sets =====
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
            if (litHolder != null) blockModels.createTrivialCube(litHolder.get());

            crossFacing(blockModels, itemModels, v.budSmall().get());
            crossFacing(blockModels, itemModels, v.budMedium().get());
            crossFacing(blockModels, itemModels, v.budLarge().get());
            crossFacing(blockModels, itemModels, v.cluster().get());
        }
    }

    // points <item> to an existing model at <model>, without generating that model file
    private static void registerExternalItem(ItemModelGenerators itemModels, Item item, ResourceLocation model) {
        itemModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(model));
    }

    // === Shield item definitions for vanilla-style shields ===
    private static void registerShield(ItemModelGenerators itemModels, Item item) {
        itemModels.generateShield(item);
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

    // === IMPORTANT: Exclude VOWKEEPER from auto item model generation ===
    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return ModItems.ITEMS.getEntries().stream()
                .filter(x -> !x.is(ModItems.VOWKEEPER))
                .filter(x -> !x.is(ModItems.AEGIS_OF_TRUTH))
                .filter(x -> !x.is(ModItems.REINFORCED_IRON_SLAB))
                .filter(x -> !x.is(ModItems.SHIELD_OF_THE_DEEP))
                .filter(x -> !x.is(ModItems.SHIELD_OF_TIDAL_FORCE))
                .filter(x -> !x.is(ModItems.WORKED_PLANK));
    }

}
