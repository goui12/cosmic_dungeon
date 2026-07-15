package net.goui.cosmicdungeon.item;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CosmicDungeonMod.MOD_ID);

    public static final Supplier<CreativeModeTab> DUNGEON_ITEM_TAB =
            CREATIVE_MODE_TAB.register("dungeon_items_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.BARNACLED_PEARL.get()))
                            .title(Component.translatable("creativetab.cosmicdungeon.dungeon_items"))
                            .displayItems((itemDisplayParameters, output) -> {
                                // Dungeon items
                                output.accept(ModItems.BARNACLED_PEARL);
                                output.accept(ModItems.SEISMIC_CORE_FRAGMENT);
                                output.accept(ModItems.SHATTERED_REALITY_OF_SHUDDE_MELL);
                                output.accept(ModItems.LEATHER_PATCH);
                                output.accept(ModItems.CHAIN_LINK);
                                output.accept(ModItems.NETHERITE_REPAIR_FRAGMENT);
                                output.accept(ModItems.BROODING_FORK);
                                output.accept(ModItems.REGION_WAND);
                                output.accept(ModItems.POTION_OF_COMPANIONSHIP);
                                output.accept(ModItems.RAW_FARROWS_CHOP);
                                output.accept(ModItems.FARROWS_CHOP);
                                output.accept(ModBlocks.CLASS_SELECTOR_BLOCK);
                                // Dungeon blocks
                                output.accept(ModBlocks.BARRIER_BLOCK);
                                output.accept(ModBlocks.CHICKEN_BLOCK);
                                output.accept(ModBlocks.PILE_OF_BOOKS);
                                output.accept(ModBlocks.BEATRIX_CAMPFIRE_ITEM.get());
                                output.accept(ModBlocks.CAVERN_RESIDUE_ITEM.get());
                                output.accept(ModBlocks.INFINITE_DISPENSER);
                                output.accept(ModBlocks.COSMIC_MOB_SPAWNER);
                               output.accept(ModBlocks.COSMIC_RIFT);
                                // Class locked chests
                                output.accept(ModBlocks.BOGATYR_CHEST);
                                output.accept(ModBlocks.DEADEYE_CHEST);
                                output.accept(ModBlocks.DRAGOON_CHEST);
                                output.accept(ModBlocks.JUDICATOR_CHEST);
                                output.accept(ModBlocks.METALMANCER_CHEST);
                                output.accept(ModBlocks.PYROCLAST_CHEST);
                                output.accept(ModBlocks.THEURGIST_CHEST);
                                output.accept(ModBlocks.VENEFEX_CHEST);

                                // RF system
                                output.accept(ModBlocks.REDSTONE_TRANSMITTER);
                                output.accept(ModBlocks.REDSTONE_RECEIVER);
                                output.accept(ModBlocks.LESSER_BLOOM_ITEM.get());
                                output.accept(ModBlocks.BLOOM_OF_QUIET_ASSURANCE_ITEM.get());
                                output.accept(ModBlocks.BLOOM_OF_GENTLE_LIES_ITEM.get());
                                output.accept(ModBlocks.BLOOM_OF_WANING_MERCY_ITEM.get());
                                output.accept(ModBlocks.BLOOM_OF_CONSTRICTING_BONDS_ITEM.get());
                                output.accept(ModBlocks.BLOOM_OF_UNSPOKEN_RESIGNATION_ITEM.get());
                                output.accept(ModBlocks.BLOOM_OF_ELEGY_ITEM.get());

                            }).build());




    public static final Supplier<CreativeModeTab> DUNGEON_BUILDING_TAB =
            CREATIVE_MODE_TAB.register("dungeon_building_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModBlocks.DUNGEON_BUILDING_BLOCKS.get("grave_mold_bricks").get()))
                            .title(Component.translatable("creativetab.cosmicdungeon.dungeon_building"))
                            .displayItems((p, output) -> ModBlocks.DUNGEON_BUILDING_BLOCKS.values().forEach(output::accept))
                            .build());

    //~~~~~~~~~~~~~~~ Judicator Item Tab ~~~~~~~~~~~~~~~
    public static final Supplier<CreativeModeTab> JUDICATOR_ITEM_TAB =
            CREATIVE_MODE_TAB.register("judicator_items_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.AZATHOTS_HAMMER_OF_FINAL_VERDICT.get()))
                            .title(Component.translatable("creativetab.cosmicdungeon.judicator_items"))
                            .displayItems((itemDisplayParameters, output) -> {

                                /*
                                DUNGEON 1 Judicator items should be generic Minecraft items, and not custom items
                                // ===== Dungeon 1 — Tier 3 (Diamond) =====
                                output.accept(ModItems.EDICT_OF_SILENCE.get());
                                output.accept(ModItems.VOWKEEPER.get());
                                output.accept(ModItems.VIELPIERCER.get());
                                output.accept(ModItems.SCINTILLA_VITALIS.get());     // first and only listing
                                output.accept(ModItems.GUSTING_BOLT.get());
                                output.accept(ModItems.SHARD_OF_THE_MAD_STRIDER.get());
                                output.accept(ModItems.GONFALON_OF_JUDIFICATION.get());

                                // ===== Dungeon 1 — Tier 4 (Netherite) =====
                                output.accept(ModItems.EDICT_OF_RUIN.get());
                                output.accept(ModItems.AEGIS_OF_TRUTH.get());
                                output.accept(ModItems.LUX_VITALIS.get());            // first and only listing
                                output.accept(ModItems.EBONSIGHT.get());
                                output.accept(ModItems.FANG_OF_THE_BRUTE.get());
                                output.accept(ModItems.ENSIGN_OF_THE_JUDICATOR.get());

                                */
                                // ===== Dungeon 2 — Tier 1 (Chainmail) =====
                                output.accept(ModItems.VISOR_OF_THE_RESOLUTE.get()); // Head
                                output.accept(ModItems.CUIRASS_OF_PURPOSE.get());    // Body
                                output.accept(ModItems.CHAUSSES_OF_THE_PLEDGE.get());// Legs
                                output.accept(ModItems.SABATONS_OF_THE_UNHEARD_OATH.get()); // Feet
                                output.accept(ModItems.EMPOWERED_HAMMER.get());
                                output.accept(ModItems.WORKED_PLANK.get());
                                output.accept(ModItems.ARROW_OF_WRIT.get());          // first and only listing
                                output.accept(ModItems.STANDARD_OF_THE_INITIATE_JUDGE.get());

                                // ===== Dungeon 2 — Tier 2 (Iron) =====
                                output.accept(ModItems.OATHBOUND_VISOR.get());       // Head
                                output.accept(ModItems.CUIRASS_OF_RESOLUTION.get()); // Body
                                output.accept(ModItems.GREAVES_OF_BINDING.get());    // Legs
                                output.accept(ModItems.SABATONS_OF_THE_PACT.get());  // Feet
                                output.accept(ModItems.REINFORCED_HAMMER.get());
                                output.accept(ModItems.REINFORCED_IRON_SLAB.get());
                                // (SCINTILLA_VITALIS was already listed in D1–T3; don't add again)
                                output.accept(ModItems.STANDARD_OF_THE_NASCENT_JUDGE.get());

                                // ===== Dungeon 3 — Tier 3 (Diamond) =====
                                output.accept(ModItems.VISOR_OF_THE_COVENANT.get()); // Head
                                output.accept(ModItems.CUIRASS_OF_DEVOTION.get());   // Body
                                output.accept(ModItems.GREAVES_OF_THE_TRIBUNAL_PATH.get()); // Legs
                                output.accept(ModItems.SABATONS_OF_PURSUIT.get());   // Feet
                                output.accept(ModItems.TIDAL_MACE.get());
                                output.accept(ModItems.SHIELD_OF_TIDAL_FORCE.get());
                                // (ARROW_OF_WRIT & LUX_VITALIS already listed earlier; don't add again)
                                output.accept(ModItems.STANDARD_OF_THE_ABYSSAL_JUDGE.get()); // first and only listing

                                // ===== Dungeon 3 — Tier 4 (Netherite) =====
                                output.accept(ModItems.VISOR_OF_IMMUTABLE_WILL.get()); // Head
                                output.accept(ModItems.CUIRASS_OF_CONVICTION.get());   // Body
                                output.accept(ModItems.GREAVES_OF_THE_ETERNAL_MARCH.get()); // Legs
                                output.accept(ModItems.SABATONS_OF_BOUNDLESS_STEPS.get());  // Feet
                                output.accept(ModItems.ABYSSAL_MACE.get());
                                output.accept(ModItems.SHIELD_OF_THE_DEEP.get());
                                // (ARROW_OF_WRIT, LUX_VITALIS, and STANDARD_OF_THE_ABYSSAL_JUDGE already listed; don't add again)
                            }).build());


    //~~~~~~~~~~~~~~~ Sanctified Theurgist Item Tab ~~~~~~~~~~~~~~~
    public static final Supplier<CreativeModeTab> SANCTIFIED_THEURGIST_ITEM_TAB =
            CREATIVE_MODE_TAB.register("sanctified_theurgist_items_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.BARNACLED_PEARL.get()))
                            .title(Component.translatable("creativetab.cosmicdungeon.sanctified_theurgist_items"))
                            .displayItems((itemDisplayParameters, output) -> {
                                //                            output.accept(ModItems.BARNACLED_PEARL);
                                //                            output.accept(ModItems.SEISMIC_CORE_FRAGMENT);
                                //                            output.accept(ModItems.SHATTERED_REALITY_OF_SHUDDE_MELL);
                                //                            output.accept(ModItems.BROODING_FORK);
                                //                            output.accept(ModBlocks.CHICKEN_BLOCK);
                                //                            output.accept(ModBlocks.PILE_OF_BOOKS);

                            }).build());

    //~~~~~~~~~~~~~~~ Pyroclast Item Tab ~~~~~~~~~~~~~~~
    public static final Supplier<CreativeModeTab> PYROCLAST_ITEM_TAB =
            CREATIVE_MODE_TAB.register("pyroclast_items_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.BARNACLED_PEARL.get()))
                            .title(Component.translatable("creativetab.cosmicdungeon.pyroclast_items"))
                            .displayItems((itemDisplayParameters, output) -> {
                                //                            output.accept(ModItems.BARNACLED_PEARL);
                                //                            output.accept(ModItems.SEISMIC_CORE_FRAGMENT);
                                //                            output.accept(ModItems.SHATTERED_REALITY_OF_SHUDDE_MELL);
                                //                            output.accept(ModItems.BROODING_FORK);
                                //                            output.accept(ModBlocks.CHICKEN_BLOCK);
                                //                            output.accept(ModBlocks.PILE_OF_BOOKS);

                            }).build());

    //~~~~~~~~~~~~~~~ Venefex Item Tab ~~~~~~~~~~~~~~~
    public static final Supplier<CreativeModeTab> VENEFEX_ITEM_TAB =
            CREATIVE_MODE_TAB.register("venefex_items_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.BARNACLED_PEARL.get()))
                            .title(Component.translatable("creativetab.cosmicdungeon.venefex_items"))
                            .displayItems((itemDisplayParameters, output) -> {
                                //                            output.accept(ModItems.BARNACLED_PEARL);
                                //                            output.accept(ModItems.SEISMIC_CORE_FRAGMENT);
                                //                            output.accept(ModItems.SHATTERED_REALITY_OF_SHUDDE_MELL);
                                //                            output.accept(ModItems.BROODING_FORK);
                                //                            output.accept(ModBlocks.CHICKEN_BLOCK);
                                //                            output.accept(ModBlocks.PILE_OF_BOOKS);

                            }).build());

    //~~~~~~~~~~~~~~~ Bogatyr Item Tab ~~~~~~~~~~~~~~~
    public static final Supplier<CreativeModeTab> BOGATYR_ITEM_TAB =
            CREATIVE_MODE_TAB.register("bogatyr_items_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.TOTEM_OF_DOG_WHISPERING.get()))
                            .title(Component.translatable("creativetab.cosmicdungeon.bogatyr_items"))
                            .displayItems((itemDisplayParameters, output) -> {
                            output.accept(ModItems.TOTEM_OF_DOG_WHISPERING);
                                //                            output.accept(ModItems.SEISMIC_CORE_FRAGMENT);
                                //                            output.accept(ModItems.SHATTERED_REALITY_OF_SHUDDE_MELL);
                                //                            output.accept(ModItems.BROODING_FORK);
                                //                            output.accept(ModBlocks.CHICKEN_BLOCK);
                                //                            output.accept(ModBlocks.PILE_OF_BOOKS);

                            }).build());

    //~~~~~~~~~~~~~~~ Dragoon Item Tab ~~~~~~~~~~~~~~~
    public static final Supplier<CreativeModeTab> DRAGOON_ITEM_TAB =
            CREATIVE_MODE_TAB.register("dragoon_items_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.BARNACLED_PEARL.get()))
                            .title(Component.translatable("creativetab.cosmicdungeon.dragoon_items"))
                            .displayItems((itemDisplayParameters, output) -> {
                                output.accept(ModItems.LEATHER_PATCH);
                                output.accept(ModItems.CHAIN_LINK);
                                output.accept(ModItems.NETHERITE_REPAIR_FRAGMENT);
                                //                            output.accept(ModItems.BARNACLED_PEARL);
                                //                            output.accept(ModItems.SEISMIC_CORE_FRAGMENT);
                                //                            output.accept(ModItems.SHATTERED_REALITY_OF_SHUDDE_MELL);
                                //                            output.accept(ModItems.BROODING_FORK);
                                //                            output.accept(ModBlocks.CHICKEN_BLOCK);
                                //                            output.accept(ModBlocks.PILE_OF_BOOKS);

                            }).build());

    //~~~~~~~~~~~~~~~ Metalmancer Item Tab ~~~~~~~~~~~~~~~
    public static final Supplier<CreativeModeTab> METALMANCER_ITEM_TAB =
            CREATIVE_MODE_TAB.register("metalmancer_items_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.FORGE_CALLERS_MAUL.get()))
                            .title(Component.translatable("creativetab.cosmicdungeon.metalmancer_items"))
                            .displayItems((itemDisplayParameters, output) -> {
                            output.accept(ModItems.FORGE_CALLERS_MAUL);
                                //                            output.accept(ModItems.SEISMIC_CORE_FRAGMENT);
                                //                            output.accept(ModItems.SHATTERED_REALITY_OF_SHUDDE_MELL);
                                //                            output.accept(ModItems.BROODING_FORK);
                                //                            output.accept(ModBlocks.CHICKEN_BLOCK);
                                //                            output.accept(ModBlocks.PILE_OF_BOOKS);

                            }).build());

    //~~~~~~~~~~~~~~~ Deadeye Item Tab ~~~~~~~~~~~~~~~
    public static final Supplier<CreativeModeTab> DEADEYE_ITEM_TAB =
            CREATIVE_MODE_TAB.register("deadeye_items_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.HIGH_VELOCITY_ARROW.get()))
                            .title(Component.translatable("creativetab.cosmicdungeon.deadeye_items"))
                            .displayItems((itemDisplayParameters, output) -> {
                           output.accept(ModItems.HIGH_VELOCITY_ARROW);
                            //output.accept(ModItems.SEISMIC_CORE_FRAGMENT);
                                //                            output.accept(ModItems.SHATTERED_REALITY_OF_SHUDDE_MELL);
                                //                            output.accept(ModItems.BROODING_FORK);
                                //                            output.accept(ModBlocks.CHICKEN_BLOCK);
                                //                            output.accept(ModBlocks.PILE_OF_BOOKS);

                            }).build());




    private static void acceptIfHasItem(CreativeModeTab.Output out, ItemLike like) {
        if (like.asItem() != Items.AIR) out.accept(like);
    }
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COLORED_AMETHYST_ITEM_TAB =
            CREATIVE_MODE_TAB.register("amethyst_items_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("creativetab.cosmicdungeon.amethyst_items"))
                            .icon(() -> new ItemStack(ModBlocks.AMETHYST.get(DyeColor.PURPLE).block().get()))
                            .displayItems((params, out) -> {

                                // Per-color ordering: base block -> LIT block -> buds/cluster/budding
                                for (var color : DyeColor.values()) {
                                    var fam = ModBlocks.AMETHYST.get(color);
                                    if (fam == null) continue;

                                    // Base block
                                    out.accept(fam.block());

                                    // Lit block (if registered for this color)
                                    var litItem = ModBlocks.LIT_AMETHYST_ITEMS.get(color);
                                    if (litItem != null) out.accept(litItem);

                                    // Buds + cluster + budding for that color
                                    out.accept(fam.budSmall());
                                    out.accept(fam.budMedium());
                                    out.accept(fam.budLarge());
                                    out.accept(fam.cluster());
                                    out.accept(fam.budding());
                                }

                                // --- Vanilla Minecraft amethyst (guarded) ---
                                acceptIfHasItem(out, Blocks.AMETHYST_BLOCK);
                                acceptIfHasItem(out, Blocks.BUDDING_AMETHYST);
                                acceptIfHasItem(out, Blocks.SMALL_AMETHYST_BUD);
                                acceptIfHasItem(out, Blocks.MEDIUM_AMETHYST_BUD);
                                acceptIfHasItem(out, Blocks.LARGE_AMETHYST_BUD);
                                acceptIfHasItem(out, Blocks.AMETHYST_CLUSTER);
                            })
                            .build()
            );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}