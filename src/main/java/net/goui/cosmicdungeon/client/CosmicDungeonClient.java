package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.block.entity.ModBlockEntities;
import net.goui.cosmicdungeon.client.model.*;
import net.goui.cosmicdungeon.client.render.*;
import net.goui.cosmicdungeon.client.render.blockentity.CosmicSpawnerRenderer;
import net.goui.cosmicdungeon.client.rift.RiftAmbienceClient;
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
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;


public final class CosmicDungeonClient {

    private CosmicDungeonClient() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(CosmicDungeonClient::registerLayers);
        modEventBus.addListener(CosmicDungeonClient::registerRenderers);
        modEventBus.addListener(CosmicDungeonClient::onClientSetup);
        modEventBus.addListener(CosmicDungeonClient::registerClientPayloads);

        // Metalmancer extra inventory screen
        modEventBus.addListener((RegisterMenuScreensEvent e) ->
                e.register(ModMenus.METALMANCER_INVENTORY.get(), ExtraInventoryScreen::new)
        );

        // Existing overlay
        NeoForge.EVENT_BUS.register(CosmicSpawnerHoverOverlay.class);

        // Rift ambience (client tick)
        NeoForge.EVENT_BUS.register(new RiftAmbienceClient());
    }

    private static void onClientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.COSMIC_MOB_SPAWNER.get(), ChunkSectionLayer.TRANSLUCENT);
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
    private static void registerClientPayloads(RegisterPayloadHandlersEvent e) {
        var reg = e.registrar(net.goui.cosmicdungeon.CosmicDungeonMod.MOD_ID);

        reg.playToClient(
                net.goui.cosmicdungeon.playerclass.api.ClassNet.S2C_ClassSync.TYPE,
                net.goui.cosmicdungeon.playerclass.api.ClassNet.S2C_ClassSync.STREAM_CODEC,
                (pkt, ctx) -> {
                    var mc = net.minecraft.client.Minecraft.getInstance();
                    var pl = mc.player;
                    if (pl == null) return;

                    // update class id
                    net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil.setClassId(pl, pkt.classId());

                    // update extra inventory PD
                    var pd = pl.getPersistentData();
                    var root = pd.getCompoundOrEmpty(net.goui.cosmicdungeon.playerclass.api.ClassData.ROOT_TAG).copy();
                    root.put(net.goui.cosmicdungeon.playerclass.api.ClassData.KEY_EXTRA, pkt.extraNbt());
                    pd.put(net.goui.cosmicdungeon.playerclass.api.ClassData.ROOT_TAG, root);
                }
        );
    }

}
