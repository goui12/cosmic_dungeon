package net.goui.cosmicdungeon.playerclass.metalmancer;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which Metalmancer players currently have a golem
 * co-resonating (standing still in resonance range).
 *
 * This is used by SatchelIdleTicker to double ore income
 * while the golem is synced and still.
 */
public final class MetalmancerResonanceTracker {

    private MetalmancerResonanceTracker() {}

    // Owner UUIDs that currently have at least one resonating golem.
    // (Design assumption: 1 golem per Metalmancer, so a simple set is enough.)
    private static final Set<UUID> GOLEM_RESONATING_PLAYERS = new HashSet<>();

    public static void setGolemResonating(UUID ownerId, boolean resonating) {
        if (ownerId == null) return;
        if (resonating) {
            GOLEM_RESONATING_PLAYERS.add(ownerId);
        } else {
            GOLEM_RESONATING_PLAYERS.remove(ownerId);
        }
    }

    public static boolean isGolemResonatingNear(ServerPlayer sp) {
        return GOLEM_RESONATING_PLAYERS.contains(sp.getUUID());
    }

    /** Optional helper if you ever want to hard-clear on logout, etc. */
    public static void clearForPlayer(UUID ownerId) {
        GOLEM_RESONATING_PLAYERS.remove(ownerId);
    }
}
