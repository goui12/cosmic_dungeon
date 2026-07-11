package net.goui.cosmicdungeon.command;

import net.goui.cosmicdungeon.auth.Authority;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.payload.SpawnerLabelPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-session-only state for each player's developer spawner-label preference.
 *
 * This is intentionally not saved data: labels are a local developer display preference,
 * and the effective client state is always gated by the current server-authoritative rank.
 */
public final class SpawnerLabelServerState {
    private static final Set<UUID> ENABLED_PLAYERS = ConcurrentHashMap.newKeySet();

    private SpawnerLabelServerState() {}

    public static boolean isRequested(ServerPlayer player) {
        return player != null && ENABLED_PLAYERS.contains(player.getUUID());
    }

    public static boolean isEffective(ServerPlayer player) {
        return player != null && Authority.isDeveloper(player) && isRequested(player);
    }

    public static boolean toggle(ServerPlayer player) {
        return setRequested(player, !isRequested(player));
    }

    public static boolean setRequested(ServerPlayer player, boolean enabled) {
        if (player == null) return false;
        UUID playerId = player.getUUID();
        if (enabled) {
            if (Authority.isDeveloper(player)) {
                ENABLED_PLAYERS.add(playerId);
            } else {
                ENABLED_PLAYERS.remove(playerId);
            }
        } else {
            ENABLED_PLAYERS.remove(playerId);
        }
        return isEffective(player);
    }

    public static void sync(ServerPlayer player) {
        if (player == null) return;
        if (!Authority.isDeveloper(player)) {
            ENABLED_PLAYERS.remove(player.getUUID());
            ModNetwork.sendTo(player, new SpawnerLabelPayload(false));
            return;
        }
        ModNetwork.sendTo(player, new SpawnerLabelPayload(isEffective(player)));
    }

    public static void revoke(ServerPlayer player) {
        if (player == null) return;
        ENABLED_PLAYERS.remove(player.getUUID());
        ModNetwork.sendTo(player, new SpawnerLabelPayload(false));
    }
}
