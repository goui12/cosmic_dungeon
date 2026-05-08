package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardSaveData;

public class ServerScoreboard extends Scoreboard {
    public static final SavedDataType<ScoreboardSaveData> TYPE = new SavedDataType<>(
        "scoreboard", p_401139_ -> p_401139_.levelOrThrow().getScoreboard().createData(), p_401245_ -> {
            ServerScoreboard serverscoreboard = p_401245_.levelOrThrow().getScoreboard();
            return ScoreboardSaveData.Packed.CODEC.xmap(serverscoreboard::createData, ScoreboardSaveData::pack);
        }, DataFixTypes.SAVED_DATA_SCOREBOARD
    );
    private final MinecraftServer server;
    private final Set<Objective> trackedObjectives = Sets.newHashSet();
    private final List<Runnable> dirtyListeners = Lists.newArrayList();

    public ServerScoreboard(MinecraftServer server) {
        this.server = server;
    }

    @Override
    protected void onScoreChanged(ScoreHolder scoreHolder, Objective objective, Score score) {
        super.onScoreChanged(scoreHolder, objective, score);
        if (this.trackedObjectives.contains(objective)) {
            this.server
                .getPlayerList()
                .broadcastAll(
                    new ClientboundSetScorePacket(
                        scoreHolder.getScoreboardName(),
                        objective.getName(),
                        score.value(),
                        Optional.ofNullable(score.display()),
                        Optional.ofNullable(score.numberFormat())
                    )
                );
        }

        this.setDirty();
    }

    @Override
    protected void onScoreLockChanged(ScoreHolder scoreHolder, Objective objective) {
        super.onScoreLockChanged(scoreHolder, objective);
        this.setDirty();
    }

    @Override
    public void onPlayerRemoved(ScoreHolder scoreHolder) {
        super.onPlayerRemoved(scoreHolder);
        this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(scoreHolder.getScoreboardName(), null));
        this.setDirty();
    }

    @Override
    public void onPlayerScoreRemoved(ScoreHolder scoreHolder, Objective objective) {
        super.onPlayerScoreRemoved(scoreHolder, objective);
        if (this.trackedObjectives.contains(objective)) {
            this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(scoreHolder.getScoreboardName(), objective.getName()));
        }

        this.setDirty();
    }

    @Override
    public void setDisplayObjective(DisplaySlot slot, @Nullable Objective p_objective) {
        Objective objective = this.getDisplayObjective(slot);
        super.setDisplayObjective(slot, p_objective);
        if (objective != p_objective && objective != null) {
            if (this.getObjectiveDisplaySlotCount(objective) > 0) {
                this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(slot, p_objective));
            } else {
                this.stopTrackingObjective(objective);
            }
        }

        if (p_objective != null) {
            if (this.trackedObjectives.contains(p_objective)) {
                this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(slot, p_objective));
            } else {
                this.startTrackingObjective(p_objective);
            }
        }

        this.setDirty();
    }

    @Override
    public boolean addPlayerToTeam(String playerName, PlayerTeam team) {
        if (super.addPlayerToTeam(playerName, team)) {
            this.server
                .getPlayerList()
                .broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(team, playerName, ClientboundSetPlayerTeamPacket.Action.ADD));
            this.updatePlayerWaypoint(playerName);
            this.setDirty();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes the given username from the given ScorePlayerTeam. If the player is not on the team then an IllegalStateException is thrown.
     */
    @Override
    public void removePlayerFromTeam(String username, PlayerTeam playerTeam) {
        super.removePlayerFromTeam(username, playerTeam);
        this.server
            .getPlayerList()
            .broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(playerTeam, username, ClientboundSetPlayerTeamPacket.Action.REMOVE));
        this.updatePlayerWaypoint(username);
        this.setDirty();
    }

    @Override
    public void onObjectiveAdded(Objective objective) {
        super.onObjectiveAdded(objective);
        this.setDirty();
    }

    @Override
    public void onObjectiveChanged(Objective objective) {
        super.onObjectiveChanged(objective);
        if (this.trackedObjectives.contains(objective)) {
            this.server.getPlayerList().broadcastAll(new ClientboundSetObjectivePacket(objective, 2));
        }

        this.setDirty();
    }

    @Override
    public void onObjectiveRemoved(Objective objective) {
        super.onObjectiveRemoved(objective);
        if (this.trackedObjectives.contains(objective)) {
            this.stopTrackingObjective(objective);
        }

        this.setDirty();
    }

    @Override
    public void onTeamAdded(PlayerTeam playerTeam) {
        super.onTeamAdded(playerTeam);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam, true));
        this.setDirty();
    }

    @Override
    public void onTeamChanged(PlayerTeam playerTeam) {
        super.onTeamChanged(playerTeam);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam, false));
        this.updateTeamWaypoints(playerTeam);
        this.setDirty();
    }

    @Override
    public void onTeamRemoved(PlayerTeam playerTeam) {
        super.onTeamRemoved(playerTeam);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createRemovePacket(playerTeam));
        this.updateTeamWaypoints(playerTeam);
        this.setDirty();
    }

    public void addDirtyListener(Runnable runnable) {
        this.dirtyListeners.add(runnable);
    }

    protected void setDirty() {
        for (Runnable runnable : this.dirtyListeners) {
            runnable.run();
        }
    }

    public List<Packet<?>> getStartTrackingPackets(Objective objective) {
        List<Packet<?>> list = Lists.newArrayList();
        list.add(new ClientboundSetObjectivePacket(objective, 0));

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            if (this.getDisplayObjective(displayslot) == objective) {
                list.add(new ClientboundSetDisplayObjectivePacket(displayslot, objective));
            }
        }

        for (PlayerScoreEntry playerscoreentry : this.listPlayerScores(objective)) {
            list.add(
                new ClientboundSetScorePacket(
                    playerscoreentry.owner(),
                    objective.getName(),
                    playerscoreentry.value(),
                    Optional.ofNullable(playerscoreentry.display()),
                    Optional.ofNullable(playerscoreentry.numberFormatOverride())
                )
            );
        }

        return list;
    }

    public void startTrackingObjective(Objective objective) {
        List<Packet<?>> list = this.getStartTrackingPackets(objective);

        for (ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
            for (Packet<?> packet : list) {
                serverplayer.connection.send(packet);
            }
        }

        this.trackedObjectives.add(objective);
    }

    public List<Packet<?>> getStopTrackingPackets(Objective objective) {
        List<Packet<?>> list = Lists.newArrayList();
        list.add(new ClientboundSetObjectivePacket(objective, 1));

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            if (this.getDisplayObjective(displayslot) == objective) {
                list.add(new ClientboundSetDisplayObjectivePacket(displayslot, objective));
            }
        }

        return list;
    }

    public void stopTrackingObjective(Objective objective) {
        List<Packet<?>> list = this.getStopTrackingPackets(objective);

        for (ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
            for (Packet<?> packet : list) {
                serverplayer.connection.send(packet);
            }
        }

        this.trackedObjectives.remove(objective);
    }

    public int getObjectiveDisplaySlotCount(Objective objective) {
        int i = 0;

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            if (this.getDisplayObjective(displayslot) == objective) {
                i++;
            }
        }

        return i;
    }

    private ScoreboardSaveData createData() {
        ScoreboardSaveData scoreboardsavedata = new ScoreboardSaveData(this);
        this.addDirtyListener(scoreboardsavedata::setDirty);
        return scoreboardsavedata;
    }

    private ScoreboardSaveData createData(ScoreboardSaveData.Packed data) {
        ScoreboardSaveData scoreboardsavedata = this.createData();
        scoreboardsavedata.loadFrom(data);
        return scoreboardsavedata;
    }

    private void updatePlayerWaypoint(String playerName) {
        ServerPlayer serverplayer = this.server.getPlayerList().getPlayerByName(playerName);
        if (serverplayer != null) {
            ServerLevel serverlevel = serverplayer.level();
            if (serverlevel instanceof ServerLevel) {
                serverlevel.getWaypointManager().remakeConnections(serverplayer);
            }
        }
    }

    private void updateTeamWaypoints(PlayerTeam team) {
        for (ServerLevel serverlevel : this.server.getAllLevels()) {
            team.getPlayers()
                .stream()
                .map(p_415011_ -> this.server.getPlayerList().getPlayerByName(p_415011_))
                .filter(Objects::nonNull)
                .forEach(p_415010_ -> serverlevel.getWaypointManager().remakeConnections(p_415010_));
        }
    }
}
