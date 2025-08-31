package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.client.model.GoblinAmbusherModel;
import net.goui.cosmicdungeon.client.model.Magma_Glob;
import net.goui.cosmicdungeon.client.model.StoneWardenModel;
import net.goui.cosmicdungeon.client.render.GoblinAmbusherRenderer;
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
        e.registerLayerDefinition(Magma_Glob.LAYER_LOCATION, Magma_Glob::createBodyLayer);
        e.registerLayerDefinition(StoneWardenModel.LAYER_LOCATION, StoneWardenModel::createBodyLayer);
        e.registerLayerDefinition(GoblinAmbusherModel.LAYER_LOCATION, GoblinAmbusherModel::createBodyLayer);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.MAGMA_GLOB.get(), MagmaGlobRenderer::new);
        e.registerEntityRenderer(ModEntities.STONE_WARDEN.get(), StoneWardenRenderer::new);
        e.registerEntityRenderer(ModEntities.GOBLIN_AMBUSHER.get(), GoblinAmbusherRenderer::new);
    }
}
