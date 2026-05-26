package net.goui.cosmicdungeon.achievement.plantflags;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BannerBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class PlantFlagEvents {
    private PlantFlagEvents() {}

    @SubscribeEvent
    public static void onBannerPlaced(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;
        if (!(e.getPlacedBlock().getBlock() instanceof BannerBlock)) return;
        PlantFlagService.recordFlagPlanted(sp, e.getPos());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        PlantFlagService.onPlayerDisconnected(sp);
    }
}
