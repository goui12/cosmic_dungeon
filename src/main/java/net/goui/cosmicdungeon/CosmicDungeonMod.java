package net.goui.cosmicdungeon;

import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.client.CosmicDungeonClient;
import net.goui.cosmicdungeon.command.*;
import net.goui.cosmicdungeon.entity.GoblinAmbusherEntity;
import net.goui.cosmicdungeon.entity.MagmaGlobEntity;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.entity.StoneWardenEntity;
import net.goui.cosmicdungeon.item.ModCreativeModeTabs;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.sound.ModSounds;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.goui.cosmicdungeon.block.entity.ModBlockEntities;
import net.goui.cosmicdungeon.component.ModDataComponents;

@Mod(CosmicDungeonMod.MOD_ID)
public class CosmicDungeonMod {
    public static final String MOD_ID = "cosmicdungeon";



    private static final Logger LOGGER = LogUtils.getLogger();

    public CosmicDungeonMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);

        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        // >>> THIS LINE makes the door use CUTOUT on the client <<<
        if (FMLLoader.getCurrent().getDist() == Dist.CLIENT) {
            net.goui.cosmicdungeon.client.CosmicDungeonClient.init(modEventBus);
        }

        //Entities
        modEventBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e) -> {
            e.put(ModEntities.MAGMA_GLOB.get(), MagmaGlobEntity.createAttributes().build());
            e.put(ModEntities.STONE_WARDEN.get(), StoneWardenEntity.createAttributes().build());
            e.put(ModEntities.GOBLIN_AMBUSHER.get(), GoblinAmbusherEntity.createAttributes().build()); // fixed
        });


        ModEntities.register(modEventBus);
        if (FMLLoader.getCurrent().getDist().isClient()) {
            CosmicDungeonClient.init(modEventBus);
        }
        //ModBlocks
        ModBlocks.register(modEventBus);

        //Sounds
        ModSounds.registerSounds(modEventBus);

        //commands
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        WorldCommand.register(event.getDispatcher());
        CreativeCommand.register(event.getDispatcher());
        SurvivalCommand.register(event.getDispatcher());
        ClassCommand.register(event.getDispatcher());
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
    }


    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.BISMUTH);
            event.accept(ModItems.RAW_BISMUTH);
        }

        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.BISMUTH_BLOCK);
            event.accept(ModBlocks.BISMUTH_ORE);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }
}

