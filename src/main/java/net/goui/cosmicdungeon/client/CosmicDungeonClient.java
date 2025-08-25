package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.client.model.Magma_Glob;
import net.goui.cosmicdungeon.client.model.StoneWardenModel;
import net.goui.cosmicdungeon.client.render.MagmaGlobRenderer;
import net.goui.cosmicdungeon.client.render.StoneWardenRenderer;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class CosmicDungeonClient {
    private CosmicDungeonClient() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(CosmicDungeonClient::registerLayers);
        modEventBus.addListener(CosmicDungeonClient::registerRenderers);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        // Magma Glob model layer
        e.registerLayerDefinition(Magma_Glob.LAYER_LOCATION, Magma_Glob::createBodyLayer);
        // Stone Warden model layer
        e.registerLayerDefinition(StoneWardenModel.LAYER_LOCATION, StoneWardenModel::createBodyLayer);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        // Magma Glob renderer
        e.registerEntityRenderer(ModEntities.MAGMA_GLOB.get(), MagmaGlobRenderer::new);
        // Stone Warden renderer
        e.registerEntityRenderer(ModEntities.STONE_WARDEN.get(), StoneWardenRenderer::new);
    }
}
