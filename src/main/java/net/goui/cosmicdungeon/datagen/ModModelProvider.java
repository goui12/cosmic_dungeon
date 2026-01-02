// file: src/main/java/net/goui/cosmicdungeon/datagen/ModModelProvider.java
package net.goui.cosmicdungeon.datagen;

import com.mojang.math.Quadrant;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.common.color.AmethystColor;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.playerclass.metalmancer.MetalmancerItems;

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
        java.util.function.Consumer<Item> FLAT = i -> itemModels.generateFlatItem(i, ModelTemplates.FLAT_ITEM);
        java.util.function.Consumer<Item> MACE = i -> itemModels.generateFlatItem(i, ModelTemplates.FLAT_HANDHELD_MACE_ITEM);
        java.util.function.Consumer<Item> ROD  = i -> itemModels.generateFlatItem(i, ModelTemplates.FLAT_HANDHELD_ROD_ITEM);

        // ===== Dungeon 1: Spectral Blooms (vanilla flower-style cross model) =====
        simpleCrossPlant(blockModels, itemModels, ModBlocks.BLOOM_OF_QUIET_ASSURANCE.get());
        simpleCrossPlant(blockModels, itemModels, ModBlocks.BLOOM_OF_GENTLE_LIES.get());
        simpleCrossPlant(blockModels, itemModels, ModBlocks.BLOOM_OF_WANING_MERCY.get());
        simpleCrossPlant(blockModels, itemModels, ModBlocks.BLOOM_OF_CONSTRICTING_BONDS.get());
        simpleCrossPlant(blockModels, itemModels, ModBlocks.BLOOM_OF_UNSPOKEN_RESIGNATION.get());
        simpleCrossPlant(blockModels, itemModels, ModBlocks.BLOOM_OF_ELEGY.get());

        // ===== Dungeon 1: Spectral Blooms (potted variants) =====
        pottedCrossPlant(blockModels, itemModels, ModBlocks.POTTED_BLOOM_OF_QUIET_ASSURANCE.get());
        pottedCrossPlant(blockModels, itemModels, ModBlocks.POTTED_BLOOM_OF_GENTLE_LIES.get());
        pottedCrossPlant(blockModels, itemModels, ModBlocks.POTTED_BLOOM_OF_WANING_MERCY.get());
        pottedCrossPlant(blockModels, itemModels, ModBlocks.POTTED_BLOOM_OF_CONSTRICTING_BONDS.get());
        pottedCrossPlant(blockModels, itemModels, ModBlocks.POTTED_BLOOM_OF_UNSPOKEN_RESIGNATION.get());
        pottedCrossPlant(blockModels, itemModels, ModBlocks.POTTED_BLOOM_OF_ELEGY.get());

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
        FLAT.accept(ModItems.REGION_WAND.get());
        FLAT.accept(ModItems.AEGIS_OF_ABSOLUTION.get());
        FLAT.accept(ModItems.AEGIS_OF_CHAOS.get());
        FLAT.accept(ModItems.AZATHOTS_HAMMER_OF_FINAL_VERDICT.get());
        FLAT.accept(ModItems.FORGE_CALLERS_MAUL.get());
        FLAT.accept(ModItems.HIGH_VELOCITY_ARROW.get());
        FLAT.accept(ModItems.TOTEM_OF_DOG_WHISPERING.get());
        FLAT.accept(ModItems.DOOR_KEY.get());

        // ===== Judicator — D1 T3 (Diamond) =====
        MACE.accept(ModItems.EDICT_OF_SILENCE.get());

        // Vowkeeper uses hand-authored custom-geometry JSONs (no generation here)
        registerExternalItem(itemModels, ModItems.VOWKEEPER.get(), rlMod("item/vowkeeper"));

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

        // ===== Metalmancer =====
        FLAT.accept(MetalmancerItems.SATCHEL_OF_SAMPLES.get());

        // New: Summoning staffs (rod models)
        ROD.accept(MetalmancerItems.BENT_ROD_OF_MELTED_SHAVINGS.get());
        ROD.accept(MetalmancerItems.ERZFUEHLER.get());

        // ===== Simple cubes =====
        blockModels.createTrivialCube(ModBlocks.BISMUTH_ORE.get());
        blockModels.createTrivialCube(ModBlocks.BISMUTH_DEEPSLATE_ORE.get());
        blockModels.createTrivialCube(ModBlocks.MAGIC_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.BISMUTH_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.CHICKEN_BLOCK.get());

        // ===== Ghost Block =====
        ghostCube(blockModels, itemModels, ModBlocks.REGION_GHOST_GLASS.get(), true);

        // ===== Cosmic Rift (placer) =====
        {
            var b = ModBlocks.COSMIC_RIFT.get();
            blockModels.createTrivialCube(b); // uses block/cosmic_rift texture
            // Ensure item points to block model
            registerExternalItem(itemModels, b.asItem(), rlMod("block/cosmic_rift"));
        }

        registerRiftTile_BlockbenchStyle(blockModels, ModBlocks.COSMIC_RIFT_TILE.get(), rlMod("block/rift/cosmic_rift_tile"));

        // ===== Pile of Books (points at external hand-authored model) =====
        {
            var b = ModBlocks.PILE_OF_BOOKS.get();
            var blockModel = rlMod("block/pile_of_books");
            blockModels.blockStateOutput.accept(
                    MultiVariantGenerator.dispatch(b, new MultiVariant(WeightedList.of(new Variant(blockModel))))
            );
            FLAT.accept(b.asItem());
        }

        // ===== Class Selector Block (points at external hand-authored Blockbench model) =====
        {
            var b = ModBlocks.CLASS_SELECTOR_BLOCK.get();
            var blockModel = rlMod("block/class_selector_block");

            blockModels.blockStateOutput.accept(
                    MultiVariantGenerator.dispatch(b, new MultiVariant(WeightedList.of(new Variant(blockModel))))
            );

            // Item should be the 3D block model (not flat) so it looks like the placed model.
            registerExternalItem(itemModels, b.asItem(), blockModel);
        }

        // ===== Infinite Dispenser (uses vanilla parents) =====
        {
            var b = ModBlocks.INFINITE_DISPENSER.get();

            ResourceLocation horiz = rlMod("block/infinite_dispenser");
            ResourceLocation vert  = rlMod("block/infinite_dispenser_vertical");

            blockModels.modelOutput.accept(horiz, () -> {
                var root = new com.google.gson.JsonObject();
                root.addProperty("parent", "minecraft:block/dispenser");
                return root;
            });
            blockModels.modelOutput.accept(vert, () -> {
                var root = new com.google.gson.JsonObject();
                root.addProperty("parent", "minecraft:block/dispenser_vertical");
                return root;
            });

            var pd = PropertyDispatch.initial(BlockStateProperties.FACING)
                    .select(Direction.NORTH, new MultiVariant(WeightedList.of(new Variant(horiz))))
                    .select(Direction.SOUTH, new MultiVariant(WeightedList.of(new Variant(horiz).withYRot(Quadrant.R180))))
                    .select(Direction.WEST,  new MultiVariant(WeightedList.of(new Variant(horiz).withYRot(Quadrant.R270))))
                    .select(Direction.EAST,  new MultiVariant(WeightedList.of(new Variant(horiz).withYRot(Quadrant.R90))))
                    .select(Direction.UP,    new MultiVariant(WeightedList.of(new Variant(vert))))
                    .select(Direction.DOWN,  new MultiVariant(WeightedList.of(new Variant(vert).withXRot(Quadrant.R180))));

            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(b).with(pd));

            registerExternalItem(itemModels, b.asItem(), horiz);
        }

        // ===== Cosmic Mob Spawner (fully custom model, no vanilla spawner parent) =====
        blockModels.createTrivialCube(ModBlocks.COSMIC_MOB_SPAWNER.get());

        // ===== Redstone: Transmitter & Receiver (point at external Blockbench JSONs) =====
        {
            // --- Transmitter: POWERED -> on/off
            {
                var b = ModBlocks.REDSTONE_TRANSMITTER.get();
                ResourceLocation modelOff = rlMod("block/redstone_transmitter_off");
                ResourceLocation modelOn  = rlMod("block/redstone_transmitter_on");

                var pd = PropertyDispatch.initial(BlockStateProperties.POWERED);
                pd.select(Boolean.TRUE,  new MultiVariant(WeightedList.of(new Variant(modelOn))));
                pd.select(Boolean.FALSE, new MultiVariant(WeightedList.of(new Variant(modelOff))));

                blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(b).with(pd));

                registerExternalItem(itemModels, b.asItem(), modelOff);
            }

            // --- Receiver: FACING x POWERED -> rotated on/off
            {
                var b = ModBlocks.REDSTONE_RECEIVER.get();
                ResourceLocation modelOff = rlMod("block/redstone_receiver_off");
                ResourceLocation modelOn  = rlMod("block/redstone_receiver_on");

                var pd = PropertyDispatch.initial(BlockStateProperties.FACING, BlockStateProperties.POWERED);

                pd.select(Direction.NORTH, Boolean.TRUE,  new MultiVariant(WeightedList.of(new Variant(modelOn))));
                pd.select(Direction.NORTH, Boolean.FALSE, new MultiVariant(WeightedList.of(new Variant(modelOff))));

                pd.select(Direction.SOUTH, Boolean.TRUE,  new MultiVariant(WeightedList.of(new Variant(modelOn ).withYRot(Quadrant.R180))));
                pd.select(Direction.SOUTH, Boolean.FALSE, new MultiVariant(WeightedList.of(new Variant(modelOff).withYRot(Quadrant.R180))));

                pd.select(Direction.WEST,  Boolean.TRUE,  new MultiVariant(WeightedList.of(new Variant(modelOn ).withYRot(Quadrant.R270))));
                pd.select(Direction.WEST,  Boolean.FALSE, new MultiVariant(WeightedList.of(new Variant(modelOff).withYRot(Quadrant.R270))));

                pd.select(Direction.EAST,  Boolean.TRUE,  new MultiVariant(WeightedList.of(new Variant(modelOn ).withYRot(Quadrant.R90))));
                pd.select(Direction.EAST,  Boolean.FALSE, new MultiVariant(WeightedList.of(new Variant(modelOff).withYRot(Quadrant.R90))));

                pd.select(Direction.UP,    Boolean.TRUE,  new MultiVariant(WeightedList.of(new Variant(modelOn ).withXRot(Quadrant.R270))));
                pd.select(Direction.UP,    Boolean.FALSE, new MultiVariant(WeightedList.of(new Variant(modelOff).withXRot(Quadrant.R270))));

                pd.select(Direction.DOWN,  Boolean.TRUE,  new MultiVariant(WeightedList.of(new Variant(modelOn ).withXRot(Quadrant.R90))));
                pd.select(Direction.DOWN,  Boolean.FALSE, new MultiVariant(WeightedList.of(new Variant(modelOff).withXRot(Quadrant.R90))));

                blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(b).with(pd));

                registerExternalItem(itemModels, b.asItem(), modelOff);
            }
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

    /**
     * Vanilla flower-style model:
     * - block model: parent minecraft:block/cross
     * - blockstate: single variant -> that model (no properties)
     * - item model: parent -> block model
     */
    private void simpleCrossPlant(BlockModelGenerators blockModels, ItemModelGenerators itemModels, Block b) {
        String id = b.builtInRegistryHolder().key().location().getPath();
        ResourceLocation modelLoc = rlMod("block/" + id);
        ResourceLocation tex = rlMod("block/" + id);

        // block/<id>.json
        blockModels.modelOutput.accept(modelLoc, () -> {
            var root = new com.google.gson.JsonObject();
            root.addProperty("parent", "minecraft:block/cross");

            var texObj = new com.google.gson.JsonObject();
            texObj.addProperty("cross", tex.toString());
            texObj.addProperty("particle", tex.toString());
            root.add("textures", texObj);

            root.addProperty("render_type", "cutout");
            return root;
        });

        // blockstates/<id>.json  (single variant)
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(b, new MultiVariant(WeightedList.of(new Variant(modelLoc))))
        );

        // item/<id>.json -> parent is the block model
        ResourceLocation itemLoc = rlMod("item/" + id);
        itemModels.modelOutput.accept(itemLoc, () -> {
            var root = new com.google.gson.JsonObject();
            root.addProperty("parent", modelLoc.toString());
            return root;
        });
    }

    // points <item> to an existing model at <model>, without generating that model file
    private static void registerExternalItem(ItemModelGenerators itemModels, Item item, ResourceLocation model) {
        itemModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(model));
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

    // === IMPORTANT: Exclude specific items from auto item model generation ===
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

    private void ghostCube(BlockModelGenerators blockModels, ItemModelGenerators itemModels, Block b, boolean translucent) {
        String id = b.builtInRegistryHolder().key().location().getPath();
        ResourceLocation modelLoc = rlMod("block/" + id);
        ResourceLocation tex = rlMod("block/" + id);

        blockModels.modelOutput.accept(modelLoc, () -> {
            var root = new com.google.gson.JsonObject();
            root.addProperty("parent", "block/cube_all");

            var textures = new com.google.gson.JsonObject();
            textures.addProperty("all", tex.toString());
            textures.addProperty("particle", tex.toString());
            root.add("textures", textures);

            root.addProperty("render_type", translucent ? "translucent" : "cutout");
            return root;
        });

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(b, new MultiVariant(WeightedList.of(new Variant(modelLoc))))
        );

        // Item points at the block model
        registerExternalItem(itemModels, b.asItem(), modelLoc);
    }

    private void registerRiftTile_BlockbenchStyle(BlockModelGenerators blockModels, Block b, ResourceLocation texture) {
        String id = b.builtInRegistryHolder().key().location().getPath();
        ResourceLocation modelLoc = rlMod("block/" + id);

        blockModels.modelOutput.accept(modelLoc, () -> {
            var root = new com.google.gson.JsonObject();

            root.addProperty("format_version", "1.21.6");
            root.addProperty("credit", "Made with Blockbench");

            var texSize = new com.google.gson.JsonArray();
            texSize.add(256);
            texSize.add(256);
            root.add("texture_size", texSize);

            var textures = new com.google.gson.JsonObject();
            textures.addProperty("1", texture.toString());
            textures.addProperty("particle", texture.toString());
            root.add("textures", textures);

            var elements = new com.google.gson.JsonArray();
            var el = new com.google.gson.JsonObject();

            var from = new com.google.gson.JsonArray();
            from.add(0); from.add(0); from.add(0);
            el.add("from", from);

            var to = new com.google.gson.JsonArray();
            to.add(16); to.add(1); to.add(16);
            el.add("to", to);

            var faces = new com.google.gson.JsonObject();
            faces.add("north", face(0, 0, 1, 0.0625f, "#1"));
            faces.add("east",  face(0.0625f, 0, 1, 0.0625f, "#1"));
            faces.add("south", face(0, 0, 1, 0.0625f, "#1"));
            faces.add("west",  face(0, 0, 0.9375f, 0.0625f, "#1"));
            faces.add("up",    face(0, 0, 16, 16, "#1"));
            faces.add("down",  face(2, 2.5625f, 3, 3.5f, "#1"));

            el.add("faces", faces);
            elements.add(el);
            root.add("elements", elements);

            return root;
        });

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(b, new MultiVariant(WeightedList.of(new Variant(modelLoc))))
        );
    }

    private static com.google.gson.JsonObject face(float u1, float v1, float u2, float v2, String texRef) {
        var o = new com.google.gson.JsonObject();
        var uv = new com.google.gson.JsonArray();
        uv.add(u1); uv.add(v1); uv.add(u2); uv.add(v2);
        o.add("uv", uv);
        o.addProperty("texture", texRef);
        return o;
    }

    private void pottedCrossPlant(BlockModelGenerators blockModels, ItemModelGenerators itemModels, Block pottedBlock) {
        String id = pottedBlock.builtInRegistryHolder().key().location().getPath();
        ResourceLocation modelLoc = rlMod("block/" + id);

        blockModels.modelOutput.accept(modelLoc, () -> {
            var root = new com.google.gson.JsonObject();
            root.addProperty("parent", "minecraft:block/flower_pot_cross");

            var texObj = new com.google.gson.JsonObject();

            String plantId = id.startsWith("potted_") ? id.substring("potted_".length()) : id;
            texObj.addProperty("plant", rlMod("block/" + plantId).toString());
            texObj.addProperty("dirt", ResourceLocation.withDefaultNamespace("block/dirt").toString());
            texObj.addProperty("particle", rlMod("block/" + plantId).toString());

            root.add("textures", texObj);
            root.addProperty("render_type", "cutout");
            return root;
        });

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(pottedBlock, new MultiVariant(WeightedList.of(new Variant(modelLoc))))
        );
    }
}
