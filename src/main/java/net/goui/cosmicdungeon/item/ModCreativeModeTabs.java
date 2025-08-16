package net.goui.cosmicdungeon.item;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.common.color.AmethystColor;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
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

    public static final Supplier<CreativeModeTab> BISMUTH_BLOCK_TAB = CREATIVE_MODE_TAB.register("bismuth_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.BISMUTH_BLOCK))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "bismuth_items_tab"))
                    .title(Component.translatable("creativetab.cosmicdungeon.bismuth_blocks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.BISMUTH_BLOCK);
                        output.accept(ModBlocks.BISMUTH_ORE);
                        output.accept(ModBlocks.BISMUTH_DEEPSLATE_ORE);

                        output.accept(ModBlocks.MAGIC_BLOCK);

                    }).build());

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

    // Tab: all colored amethyst variants
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COLORED_AMETHYST_ITEM_TAB =
            CREATIVE_MODE_TAB.register("amethyst_items_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("creativetab.cosmicdungeon.dungeon_items"))
                            // Icon: purple colored amethyst block (tinted via item color)
                            .icon(() -> withColor(ModBlocks.COLORED_AMETHYST_BLOCK.get().asItem(), AmethystColor.PURPLE))
                            .displayItems((params, out) -> {
                                for (AmethystColor c : AmethystColor.values()) {
                                    // Blocks
                                    out.accept(withColor(ModBlocks.COLORED_AMETHYST_BLOCK.get().asItem(), c));
                                    out.accept(withColor(ModBlocks.COLORED_BUDDING_AMETHYST.get().asItem(), c));
                                    out.accept(withColor(ModBlocks.COLORED_AMETHYST_BUD_SMALL.get().asItem(), c));
                                    out.accept(withColor(ModBlocks.COLORED_AMETHYST_BUD_MEDIUM.get().asItem(), c));
                                    out.accept(withColor(ModBlocks.COLORED_AMETHYST_BUD_LARGE.get().asItem(), c));
                                    out.accept(withColor(ModBlocks.COLORED_AMETHYST_CLUSTER.get().asItem(), c));
                                }
                            })
                            .build()
            );

    // helper to imprint the color component on an ItemStack
    private static ItemStack withColor(ItemLike item, AmethystColor color) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.AMETHYST_COLOR.get(), color);
        return stack;
    }


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}