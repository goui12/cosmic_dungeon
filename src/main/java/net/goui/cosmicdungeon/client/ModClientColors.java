package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.client.tint.AmethystTintSource;
import net.goui.cosmicdungeon.common.properties.ModProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(
        modid = CosmicDungeonMod.MOD_ID,
        value = Dist.CLIENT
)
public final class ModClientColors {
    private ModClientColors() {}

    // 1) Register your custom tint source for items
    @SubscribeEvent
    public static void registerItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(
                ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "amethyst_color"),
                AmethystTintSource.CODEC
        );
    }

    // 2) Register block colors (world rendering)
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
                    if (tintIndex != 0) return 0xFFFFFFFF;
                    if (state != null && state.hasProperty(ModProperties.COLOR)) {
                        return state.getValue(ModProperties.COLOR).argb;
                    }
                    return 0xFFFFFFFF;
                },
                ModBlocks.COLORED_AMETHYST_BLOCK.get(),
                ModBlocks.COLORED_BUDDING_AMETHYST.get(),
                ModBlocks.COLORED_AMETHYST_BUD_SMALL.get(),
                ModBlocks.COLORED_AMETHYST_BUD_MEDIUM.get(),
                ModBlocks.COLORED_AMETHYST_BUD_LARGE.get(),
                ModBlocks.COLORED_AMETHYST_CLUSTER.get()
        );
    }
}
