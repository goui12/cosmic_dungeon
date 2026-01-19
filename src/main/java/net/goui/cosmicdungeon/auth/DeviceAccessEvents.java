// file: src/main/java/net/goui/cosmicdungeon/auth/DeviceAccessEvents.java
package net.goui.cosmicdungeon.auth;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.block.custom.ClassSelectorReadyManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DeviceAccessEvents {
    private DeviceAccessEvents() {}

    /** Prevent breaking protected blocks unless Developer. Also clears selector ready state if selector is broken. */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer sp)) return;
        if (!(e.getLevel() instanceof ServerLevel sl)) return;

        Block b = e.getState().getBlock();

        // Always clear selector party state on any attempt to break selector.
        if (b == ModBlocks.CLASS_SELECTOR_BLOCK.get()) {
            ClassSelectorReadyManager.clearFor(sl, e.getPos());
        }

        if (!AccessPolicy.isBreakProtectedDevice(b)) return;

        if (AccessPolicy.canBreakProtectedDevices(sp)) return;

        e.setCanceled(true);
        AccessPolicy.deny(sp, "You do not have permission to break that.");
    }

    /** Prevent using protected devices unless Developer. */
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;

        Block b = e.getLevel().getBlockState(e.getPos()).getBlock();
        if (!AccessPolicy.isUseProtectedDevice(b)) return;

        if (AccessPolicy.canUseProtectedDevices(sp)) return;

        e.setCanceled(true);
        AccessPolicy.deny(sp, "You do not have permission to use that.");
    }
}
