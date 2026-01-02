// file: src/main/java/net/goui/cosmicdungeon/client/CosmicDungeonClient.java
package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.block.entity.ModBlockEntities;
import net.goui.cosmicdungeon.client.model.*;
import net.goui.cosmicdungeon.client.render.*;
import net.goui.cosmicdungeon.client.render.blockentity.CosmicSpawnerRenderer;
import net.goui.cosmicdungeon.client.rift.RiftAmbienceClient;
import net.goui.cosmicdungeon.client.screen.ClassSelectorScreen;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.menu.ModMenus;
import net.goui.cosmicdungeon.playerclass.api.ExtraInventoryScreen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class CosmicDungeonClient {

    private CosmicDungeonClient() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(CosmicDungeonClient::registerLayers);
        modEventBus.addListener(CosmicDungeonClient::registerRenderers);
        modEventBus.addListener(CosmicDungeonClient::onClientSetup);

        modEventBus.addListener((RegisterMenuScreensEvent e) -> {
            // Metalmancer extra inventory screen
            e.register(ModMenus.METALMANCER_INVENTORY.get(), ExtraInventoryScreen::new);

            // Class selector screen
            e.register(ModMenus.CLASS_SELECTOR.get(), ClassSelectorScreen::new);
        });

        // Existing overlay
        NeoForge.EVENT_BUS.register(CosmicSpawnerHoverOverlay.class);

        // Rift ambience (client tick)
        NeoForge.EVENT_BUS.register(new RiftAmbienceClient());
    }

    private static void onClientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            // Spawner visuals
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.COSMIC_MOB_SPAWNER.get(), ChunkSectionLayer.TRANSLUCENT);

            // Region look ghost glass
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.REGION_GHOST_GLASS.get(), ChunkSectionLayer.TRANSLUCENT);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CLASS_SELECTOR_BLOCK.get(), ChunkSectionLayer.CUTOUT);

        });
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        // Existing mobs
        e.registerLayerDefinition(Magma_Glob.LAYER_LOCATION, Magma_Glob::createBodyLayer);
        e.registerLayerDefinition(StoneWardenModel.LAYER_LOCATION, StoneWardenModel::createBodyLayer);
        e.registerLayerDefinition(GoblinAmbusherModel.LAYER_LOCATION, GoblinAmbusherModel::createBodyLayer);

        // Metalmancer Golem
        e.registerLayerDefinition(MetalmancerGolemModel.LAYER_LOCATION, MetalmancerGolemModel::createBodyLayer);
        e.registerLayerDefinition(CrystalCreeperModel.LAYER_LOCATION, CrystalCreeperModel::createBodyLayer);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        // Block entities
        e.registerBlockEntityRenderer(
                ModBlockEntities.COSMIC_SPAWNER.get(),
                CosmicSpawnerRenderer::new
        );

        // Existing mobs
        e.registerEntityRenderer(ModEntities.MAGMA_GLOB.get(), MagmaGlobRenderer::new);
        e.registerEntityRenderer(ModEntities.STONE_WARDEN.get(), StoneWardenRenderer::new);
        e.registerEntityRenderer(ModEntities.GOBLIN_AMBUSHER.get(), GoblinAmbusherRenderer::new);

        // Metalmancer Golem
        e.registerEntityRenderer(ModEntities.METALMANCER_GOLEM.get(), MetalmancerGolemRenderer::new);
        e.registerEntityRenderer(ModEntities.CRYSTAL_CREEPER.get(), CrystalCreeperRenderer::new);
    }
}
