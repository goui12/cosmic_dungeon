package net.minecraft.server.waypoints;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.collect.Sets.SetView;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.waypoints.WaypointManager;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class ServerWaypointManager implements WaypointManager<WaypointTransmitter> {
    private final Set<WaypointTransmitter> waypoints = new HashSet<>();
    private final Set<ServerPlayer> players = new HashSet<>();
    private final Table<ServerPlayer, WaypointTransmitter, WaypointTransmitter.Connection> connections = HashBasedTable.create();

    public void trackWaypoint(WaypointTransmitter waypoint) {
        this.waypoints.add(waypoint);

        for (ServerPlayer serverplayer : this.players) {
            this.createConnection(serverplayer, waypoint);
        }
    }

    public void updateWaypoint(WaypointTransmitter waypoint) {
        if (this.waypoints.contains(waypoint)) {
            Map<ServerPlayer, WaypointTransmitter.Connection> map = Tables.transpose(this.connections).row(waypoint);
            SetView<ServerPlayer> setview = Sets.difference(this.players, map.keySet());

            for (Entry<ServerPlayer, WaypointTransmitter.Connection> entry : ImmutableSet.copyOf(map.entrySet())) {
                this.updateConnection(entry.getKey(), waypoint, entry.getValue());
            }

            for (ServerPlayer serverplayer : setview) {
                this.createConnection(serverplayer, waypoint);
            }
        }
    }

    public void untrackWaypoint(WaypointTransmitter waypoint) {
        this.connections.column(waypoint).forEach((p_415970_, p_416453_) -> p_416453_.disconnect());
        Tables.transpose(this.connections).row(waypoint).clear();
        this.waypoints.remove(waypoint);
    }

    public void addPlayer(ServerPlayer player) {
        this.players.add(player);

        for (WaypointTransmitter waypointtransmitter : this.waypoints) {
            this.createConnection(player, waypointtransmitter);
        }

        if (player.isTransmittingWaypoint()) {
            this.trackWaypoint((WaypointTransmitter)player);
        }
    }

    public void updatePlayer(ServerPlayer player) {
        Map<WaypointTransmitter, WaypointTransmitter.Connection> map = this.connections.row(player);
        SetView<WaypointTransmitter> setview = Sets.difference(this.waypoints, map.keySet());

        for (Entry<WaypointTransmitter, WaypointTransmitter.Connection> entry : ImmutableSet.copyOf(map.entrySet())) {
            this.updateConnection(player, entry.getKey(), entry.getValue());
        }

        for (WaypointTransmitter waypointtransmitter : setview) {
            this.createConnection(player, waypointtransmitter);
        }
    }

    public void removePlayer(ServerPlayer player) {
        this.connections.row(player).values().removeIf(p_415833_ -> {
            p_415833_.disconnect();
            return true;
        });
        this.untrackWaypoint((WaypointTransmitter)player);
        this.players.remove(player);
    }

    public void breakAllConnections() {
        this.connections.values().forEach(WaypointTransmitter.Connection::disconnect);
        this.connections.clear();
    }

    public void remakeConnections(WaypointTransmitter waypoint) {
        for (ServerPlayer serverplayer : this.players) {
            this.createConnection(serverplayer, waypoint);
        }
    }

    public Set<WaypointTransmitter> transmitters() {
        return this.waypoints;
    }

    private static boolean isLocatorBarEnabledFor(ServerPlayer player) {
        return player.level().getServer().getGameRules().getBoolean(GameRules.RULE_LOCATOR_BAR);
    }

    private void createConnection(ServerPlayer player, WaypointTransmitter waypoint) {
        if (player != waypoint) {
            if (isLocatorBarEnabledFor(player)) {
                waypoint.makeWaypointConnectionWith(player).ifPresentOrElse(p_416381_ -> {
                    this.connections.put(player, waypoint, p_416381_);
                    p_416381_.connect();
                }, () -> {
                    WaypointTransmitter.Connection waypointtransmitter$connection = this.connections.remove(player, waypoint);
                    if (waypointtransmitter$connection != null) {
                        waypointtransmitter$connection.disconnect();
                    }
                });
            }
        }
    }

    private void updateConnection(ServerPlayer player, WaypointTransmitter waypoint, WaypointTransmitter.Connection connection) {
        if (player != waypoint) {
            if (isLocatorBarEnabledFor(player)) {
                if (!connection.isBroken()) {
                    connection.update();
                } else {
                    waypoint.makeWaypointConnectionWith(player).ifPresentOrElse(p_416199_ -> {
                        p_416199_.connect();
                        this.connections.put(player, waypoint, p_416199_);
                    }, () -> {
                        connection.disconnect();
                        this.connections.remove(player, waypoint);
                    });
                }
            }
        }
    }
}
