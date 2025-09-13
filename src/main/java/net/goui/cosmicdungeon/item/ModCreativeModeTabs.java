package net.goui.cosmicdungeon.item;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.common.color.AmethystColor;
import net.goui.cosmicdungeon.component.ModDataComponents;
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

    public static final Supplier<CreativeModeTab> BISMUTH_ITEMS_TAB = CREATIVE_MODE_TAB.register("bismuth_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.BISMUTH.get()))
                    .title(Component.translatable("creativetab.cosmicdungeon.bismuth_items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.BISMUTH);
                        output.accept(ModItems.RAW_BISMUTH);

                        output.accept(ModItems.CHISEL);
                        output.accept(ModItems.RADISH);

                        output.accept(ModItems.FROSTFIRE_ICE);
                        output.accept(ModItems.STARLIGHT_ASHES);
                    }).build());

    public static final Supplier<CreativeModeTab> BISMUTH_BLOCK_TAB =
            CREATIVE_MODE_TAB.register("bismuth_blocks_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModBlocks.BISMUTH_BLOCK.get())) // <-- .get()
                            .withTabsBefore(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "bismuth_items_tab"))
                            .title(Component.translatable("creativetab.cosmicdungeon.bismuth_blocks"))
                            .displayItems((p, out) -> {
                                out.accept(ModBlocks.BISMUTH_BLOCK);
                                out.accept(ModBlocks.BISMUTH_ORE);
                                out.accept(ModBlocks.BISMUTH_DEEPSLATE_ORE);
                                out.accept(ModBlocks.MAGIC_BLOCK);
                            }).build());

    public static final Supplier<CreativeModeTab> DUNGEON_ITEM_TAB =
            CREATIVE_MODE_TAB.register("dungeon_items_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.BARNACLED_PEARL.get()))
                    .title(Component.translatable("creativetab.cosmicdungeon.dungeon_items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.BARNACLED_PEARL);
                        output.accept(ModItems.SEISMIC_CORE_FRAGMENT);
                        output.accept(ModItems.SHATTERED_REALITY_OF_SHUDDE_MELL);
                        output.accept(ModItems.BROODING_FORK);
                        output.accept(ModBlocks.CHICKEN_BLOCK);
                        output.accept(ModBlocks.PILE_OF_BOOKS);
                        output.accept(ModItems.AEGIS_OF_ABSOLUTION);
                        output.accept(ModItems.AEGIS_OF_CHAOS);
                        output.accept(ModItems.AZATHOTS_HAMMER_OF_FINAL_VERDICT);
                    }).build());

    //~~~~~~~~~~~~~~~ Judicator Item Tab ~~~~~~~~~~~~~~~
    public static final Supplier<CreativeModeTab> JUDICATOR_ITEM_TAB =
            CREATIVE_MODE_TAB.register("judicator_items_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.AZATHOTS_HAMMER_OF_FINAL_VERDICT.get()))
                            .title(Component.translatable("creativetab.cosmicdungeon.judicator_items"))
                            .displayItems((itemDisplayParameters, output) -> {
                                output.accept(ModItems.VISOR_OF_FIRST_OATH.get());
                                output.accept(ModItems.HAUBERK_OF_THE_SWORN_WORD.get());
                                output.accept(ModItems.CHAUSSES_OF_THE_FIRST_VOW.get());
                                output.accept(ModItems.SABATONS_OF_THE_SILENT_PROMISE.get());


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

    //~~~~~~~~~~~~~~~ Bogatur Item Tab ~~~~~~~~~~~~~~~
    public static final Supplier<CreativeModeTab> BOGATUR_ITEM_TAB =
            CREATIVE_MODE_TAB.register("bogatur_items_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.TOTEM_OF_DOG_WHISPERING.get()))
                            .title(Component.translatable("creativetab.cosmicdungeon.bogatur_items"))
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