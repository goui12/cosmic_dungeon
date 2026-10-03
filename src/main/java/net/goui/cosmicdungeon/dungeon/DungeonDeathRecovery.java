package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.rift.SafeTeleportUtil;
import net.goui.cosmicdungeon.transaction.InventoryTransactionGuard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

/** Native respawn at the normal outside recovery point, before the existing inventory handoff. */
public final class DungeonDeathRecovery {
    private DungeonDeathRecovery() {}

    static boolean needsRespawn(DungeonRunState state, DungeonResetReason reason,
            boolean connected, boolean alive) {
        return reason == DungeonResetReason.ABANDONED
                && (state == DungeonRunState.RESETTING || state == DungeonRunState.FAILED)
                && connected && !alive;
    }

    public static boolean prepare(MinecraftServer server, DungeonRunRegistryData.RunRecord run,
            DungeonResetReason reason) {
        var dead = run.orderedPlayers().stream().map(server.getPlayerList()::getPlayer)
                .filter(player -> player != null && needsRespawn(run.stateEnum(), reason,
                        player.connection.isAcceptingMessages(), player.isAlive())).toList();
        if (dead.isEmpty()) return true;
        // Never revive from an uncommitted reset decision or unresolved item/currency custody.
        if (!DungeonRunRegistryData.get(server).flushVerified()) return false;
        for (var player : dead) if (!InventoryTransactionGuard.otherTransactionsReady(player)) return false;
        var outside = server.overworld();
        var spawn = outside.getLevelData().getRespawnData();
        var safe = SafeTeleportUtil.findSafeTeleportPos(outside, spawn.pos());
        if (safe == null) return false;
        for (var player : dead) {
            // Avoid even a transient visit to the hostile dungeon bed. This is the same
            // outside respawn configuration installed by successful inventory cleanup.
            DungeonLifecycleService.setPlayerRespawnTo(player, outside, safe, spawn.yaw(), spawn.pitch());
            var replacement = server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
            replacement.connection.player = replacement;
            if (!replacement.isAlive()) return false;
        }
        return true;
    }
}
