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
                            })
                            .build()
            );

    public static final Supplier<CreativeModeTab> DUNGEON_ITEM_TAB = CREATIVE_MODE_TAB.register("dungeon_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.BARNACLED_PEARL.get()))
                    .title(Component.translatable("creativetab.cosmicdungeon.dungeon_items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.BARNACLED_PEARL);
                        output.accept(ModItems.SEISMIC_CORE_FRAGMENT);
                        output.accept(ModItems.SHATTERED_REALITY_OF_SHUDDE_MELL);
                        output.accept(ModItems.BROODING_FORK);
                        output.accept(ModBlocks.CHICKEN_BLOCK);

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