// file: src/main/java/net/goui/cosmicdungeon/client/CosmicDungeonClient.java
package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.block.entity.ModBlockEntities;
import net.goui.cosmicdungeon.client.model.*;
import net.goui.cosmicdungeon.client.render.*;
import net.goui.cosmicdungeon.client.render.blockentity.ClassLockedChestRenderer;
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
        // Layer definitions (models)
        modEventBus.addListener(CosmicDungeonClient::registerLayers);

        // Renderers (BERs + entity renderers)
        modEventBus.addListener(CosmicDungeonClient::registerRenderers);

        // Client setup (render layers, etc.)
        modEventBus.addListener(CosmicDungeonClient::onClientSetup);

        // Screens
        modEventBus.addListener((RegisterMenuScreensEvent e) -> {
            e.register(ModMenus.METALMANCER_INVENTORY.get(), ExtraInventoryScreen::new);
            e.register(ModMenus.CLASS_SELECTOR.get(), ClassSelectorScreen::new);
        });

        /*
         * Spawner preset hotkey registration is a MOD BUS event.
         * Do not register SpawnerPresetKeybindClient.class on NeoForge.EVENT_BUS,
         * because that class also contains RegisterKeyMappingsEvent, which is not
         * allowed on the common/game event bus.
         */
        modEventBus.addListener(SpawnerPresetKeybindClient::registerKeyMappings);

        // Existing overlay
        NeoForge.EVENT_BUS.register(CosmicSpawnerHoverOverlay.class);

        // Rift ambience (client tick)
        NeoForge.EVENT_BUS.register(new RiftAmbienceClient());

        // Spawner preset hotkey ticking is a NeoForge/game/client tick event.
        NeoForge.EVENT_BUS.addListener(SpawnerPresetKeybindClient::onClientTick);
    }

    private static void onClientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            // Spawner visuals
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.COSMIC_MOB_SPAWNER.get(), ChunkSectionLayer.TRANSLUCENT);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BARRIER_BLOCK.get(), ChunkSectionLayer.TRANSLUCENT);

            // Region look ghost glass + selector block
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.REGION_GHOST_GLASS.get(), ChunkSectionLayer.TRANSLUCENT);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CLASS_SELECTOR_BLOCK.get(), ChunkSectionLayer.CUTOUT);
        });
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        // Existing mobs
        e.registerLayerDefinition(Magma_Glob.LAYER_LOCATION, Magma_Glob::createBodyLayer);
        e.registerLayerDefinition(StoneWardenModel.LAYER_LOCATION, StoneWardenModel::createBodyLayer);
        e.registerLayerDefinition(GoblinAmbusherModel.LAYER_LOCATION, GoblinAmbusherModel::createBodyLayer);
        e.registerLayerDefinition(CthonianGnawlingModel.LAYER_LOCATION, CthonianGnawlingModel::createBodyLayer);

        // Metalmancer Golem
        e.registerLayerDefinition(MetalmancerGolemModel.LAYER_LOCATION, MetalmancerGolemModel::createBodyLayer);
        e.registerLayerDefinition(CrystalCreeperModel.LAYER_LOCATION, CrystalCreeperModel::createBodyLayer);

        // === Class-locked chest model layer (custom, NOT vanilla chest atlas) ===
        e.registerLayerDefinition(ClassLockedChestModel.LAYER_LOCATION, ClassLockedChestModel::createBodyLayer);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        // Block entities
        e.registerBlockEntityRenderer(ModBlockEntities.COSMIC_SPAWNER.get(), CosmicSpawnerRenderer::new);

        // === Custom chest renderer (replaces ChestRenderer::new) ===
        e.registerBlockEntityRenderer(ModBlockEntities.CLASS_LOCKED_CHEST.get(), ClassLockedChestRenderer::new);

        // Existing mobs
        e.registerEntityRenderer(ModEntities.MAGMA_GLOB.get(), MagmaGlobRenderer::new);
        e.registerEntityRenderer(ModEntities.STONE_WARDEN.get(), StoneWardenRenderer::new);
        e.registerEntityRenderer(ModEntities.GOBLIN_AMBUSHER.get(), GoblinAmbusherRenderer::new);
        e.registerEntityRenderer(ModEntities.CTHONIAN_GNAWLING.get(), CthonianGnawlingRenderer::new);

        // Metalmancer Golem
        e.registerEntityRenderer(ModEntities.METALMANCER_GOLEM.get(), MetalmancerGolemRenderer::new);
        e.registerEntityRenderer(ModEntities.CRYSTAL_CREEPER.get(), CrystalCreeperRenderer::new);
    }
}