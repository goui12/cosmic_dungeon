package net.goui.cosmicdungeon.dungeon;

import java.util.Optional;
import net.goui.cosmicdungeon.mixin.DungeonEquipmentAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Health decisions at a fresh D1 entry and its one-time inventory handoff. */
final class DungeonRunHealth {
    static final String ORIGINAL = "original_health";

    private DungeonRunHealth() {}

    static Optional<Float> originalHealth(CompoundTag image) {
        if (!image.contains(ORIGINAL)) return Optional.empty();
        return Optional.of(image.getFloat(ORIGINAL)
                .filter(hp -> Float.isFinite(hp) && hp > 0)
                .orElseThrow(() -> new IllegalArgumentException("Invalid pre-entry health")));
    }

    static void fill(ServerPlayer player) {
        refreshEquipment(player);
        player.setHealth(player.getMaxHealth());
    }

    static void afterCleanup(ServerPlayer player, String reason, Optional<Float> originalHealth) {
        if (reason.equals("ABANDONED")) {
            fill(player);
        } else if (reason.equals("STARTUP_ABORT") && originalHealth.isPresent()) {
            refreshEquipment(player);
            player.setHealth(originalHealth.get());
        }
    }

    private static void refreshEquipment(ServerPlayer player) {
        // Inventory.setItem changes the stacks. Vanilla normally applies their modifiers next tick.
        ((DungeonEquipmentAccess) player).cosmicdungeon$detectEquipmentUpdates();
    }
}
