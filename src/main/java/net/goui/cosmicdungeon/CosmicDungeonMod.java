package net.goui.cosmicdungeon;

import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerIntrinsicDropEvents;
import net.goui.cosmicdungeon.block.entity.ModBlockEntities;
import net.goui.cosmicdungeon.client.CosmicDungeonClient;
import net.goui.cosmicdungeon.command.*;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.entity.*;
import net.goui.cosmicdungeon.item.ModCreativeModeTabs;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.menu.ModMenus;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.playerclass.metalmancer.MetalmancerCommand;
import net.goui.cosmicdungeon.playerclass.metalmancer.MetalmancerItems;
import net.goui.cosmicdungeon.redstone.rf.ModRfBlockEntities;
import net.goui.cosmicdungeon.region.RegionWandEvents;
import net.goui.cosmicdungeon.sound.ModSounds;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Spawner;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(CosmicDungeonMod.MOD_ID)
public class CosmicDungeonMod {
    public static final String MOD_ID = "cosmicdungeon";

    private static final Logger LOGGER = LogUtils.getLogger();

    public CosmicDungeonMod(IEventBus modEventBus, ModContainer modContainer) {
        // lifecycle
        modEventBus.addListener(this::commonSetup);

        // networking payloads
        modEventBus.addListener(ModNetwork::registerPayloadHandlers);


        // forge bus listeners (commands, etc.)
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        // Region wand selection (LeftClickBlock handler)
        NeoForge.EVENT_BUS.register(new RegionWandEvents());
        NeoForge.EVENT_BUS.register(CosmicSpawnerIntrinsicDropEvents.class);


        // registries

        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);

        ModMenus.register(modEventBus);
        ModEntities.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModSounds.registerSounds(modEventBus);
        ModBlockEntities.register(modEventBus);

        // client-only init — call this ONCE
        if (FMLLoader.getCurrent().getDist() == Dist.CLIENT) {
            CosmicDungeonClient.init(modEventBus);
        }

        // entity attributes
        modEventBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(ModEntities.MAGMA_GLOB.get(),       MagmaGlobEntity.createAttributes().build());
            e.put(ModEntities.STONE_WARDEN.get(),     StoneWardenEntity.createAttributes().build());
            e.put(ModEntities.GOBLIN_AMBUSHER.get(),  GoblinAmbusherEntity.createAttributes().build());
            e.put(ModEntities.METALMANCER_GOLEM.get(), MetalmancerGolemEntity.createAttributes().build());
            e.put(ModEntities.CRYSTAL_CREEPER.get(),   CrystalCreeperEntity.createAttributes().build());
            e.put(ModEntities.CTHONIAN_GNAWLING.get(), CthonianGnawlingEntity.createAttributes().build());
        });

        // config
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        WorldCommand.register(event.getDispatcher());
        CreativeCommand.register(event.getDispatcher());
        SurvivalCommand.register(event.getDispatcher());
        MetalmancerCommand.register(event.getDispatcher());
        MoreCommand.register(event.getDispatcher());
        DoorCountCommand.register(event.getDispatcher());
        DoorPassLimitCommand.register(event.getDispatcher());
        DoorResetCountCommand.register(event.getDispatcher());
        DoorLockCommand.register(event.getDispatcher());
        DoorInfoCommand.register(event.getDispatcher());
        DoorKeyInfoCommand.register(event.getDispatcher());
        DoorKeyDuplicateCommand.register(event.getDispatcher());
        HealCommand.register(event.getDispatcher());
        FlyCommand.register(event.getDispatcher());
        FlySpeedCommand.register(event.getDispatcher());
        FullBrightCommand.register(event.getDispatcher());
        DayCommand.register(event.getDispatcher());
        NightCommand.register(event.getDispatcher());
        RiftDestinationCommand.register(event.getDispatcher());
        ShakeCommand.register(event.getDispatcher());
        SpawnerCommand.register(event.getDispatcher());
        RiftCommand.register(event.getDispatcher());
        RegionCommand.register(event.getDispatcher());
        RankCommand.register(event.getDispatcher());
        DeveloperCommand.register(event.getDispatcher());
        DungeoneerCommand.register(event.getDispatcher());
        ClassSelectorDestinationCommand.register(event.getDispatcher());
        CurrencyCommand.register(event.getDispatcher());
        FactionCommand.register(event.getDispatcher());

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModBlocks::registerFlowerPots);

    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // server starting hook if needed
    }
}
