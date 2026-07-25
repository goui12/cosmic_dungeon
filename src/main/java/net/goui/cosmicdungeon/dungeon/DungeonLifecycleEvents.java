package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.goui.cosmicdungeon.achievement.plantflags.PlantFlagService;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DungeonLifecycleEvents {
    private DungeonLifecycleEvents() {}

    private static volatile boolean reevaluateSoon = false;

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        DungeonLifecycleService.recoverInstancePoolOnServerStarted(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;

        DungeonAfkService.markActivity(sp);
        DungeonLifecycleService.performPendingRecoveryIfNeeded(sp);
        DungeonTravelRouter.evacuateUnauthorizedLocation(sp);
        reevaluateSoon = true;
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        FarrowsChopTravelService.syncOutsideInventory(sp);
        DungeonAfkService.onPlayerLoggedOut(sp);
        reevaluateSoon = true;
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        DungeonTravelRouter.evacuateUnauthorizedLocation(sp);
        reevaluateSoon = true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (!e.hasTime()) return;

        MinecraftServer server = e.getServer();
        if (server == null) return;

        boolean doPeriodic = false;
        var overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            doPeriodic = (overworld.getGameTime() % 20L) == 0L;
        }

        DungeonLifecycleService.processPendingResets(server);
        PlantFlagService.completeIfReady(server);
        DungeonAfkService.tick(server);
        if (doPeriodic) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                FarrowsChopTravelService.syncOutsideInventory(player);
                DungeonTravelRouter.evacuateUnauthorizedLocation(player);
            }
        }

        if (!reevaluateSoon && !doPeriodic) return;

        reevaluateSoon = false;
        DungeonLifecycleService.evaluateActiveRuns(server);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getEntity() instanceof ServerPlayer sp) DungeonAfkService.markActivity(sp);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (e.getEntity() instanceof ServerPlayer sp) DungeonAfkService.markActivity(sp);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e) {
        if (e.getEntity() instanceof ServerPlayer sp) DungeonAfkService.markActivity(sp);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getEntity() instanceof ServerPlayer sp) DungeonAfkService.markActivity(sp);
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent e) {
        DungeonAfkService.markActivity(e.getPlayer());
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent e) {
        if (e.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer sp) {
            DungeonAfkService.markActivity(sp);
        }
    }
}
