package net.goui.cosmicdungeon.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class DungeonRunRegistryData extends SavedData {
    private static final String SAVE_ID = "cosmicdungeon_dungeon_runs";
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public record RunRecord(
            long runId,
            String dungeonId,
            String selectorDimensionId,
            long selectorPosLong,
            List<String> dungeonDimensionIds,
            String state,
            String resetReason,
            long startedAtEpochMillis,
            List<UUID> orderedPlayers,
            List<UUID> completionExitedPlayers,
            List<DungeonPlayerRunSnapshot> playerSnapshots
    ) {
        public static final Codec<RunRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("run_id").forGetter(RunRecord::runId),
                Codec.STRING.fieldOf("dungeon_id").forGetter(RunRecord::dungeonId),
                Codec.STRING.fieldOf("selector_dimension").forGetter(RunRecord::selectorDimensionId),
                Codec.LONG.fieldOf("selector_pos").forGetter(RunRecord::selectorPosLong),
                Codec.STRING.listOf().fieldOf("dungeon_dimension_ids").forGetter(RunRecord::dungeonDimensionIds),
                Codec.STRING.fieldOf("state").forGetter(RunRecord::state),
                Codec.STRING.optionalFieldOf("reset_reason", "").forGetter(RunRecord::resetReason),
                Codec.LONG.optionalFieldOf("started_at", 0L).forGetter(RunRecord::startedAtEpochMillis),
                UUID_CODEC.listOf().fieldOf("ordered_players").forGetter(RunRecord::orderedPlayers),
                UUID_CODEC.listOf().optionalFieldOf("completion_exited_players", List.of()).forGetter(RunRecord::completionExitedPlayers),
                DungeonPlayerRunSnapshot.CODEC.listOf().optionalFieldOf("player_snapshots", List.of()).forGetter(RunRecord::playerSnapshots)
        ).apply(i, RunRecord::new));

        public DungeonRunState stateEnum() {
            try {
                return DungeonRunState.valueOf(state.toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {
                return DungeonRunState.ACTIVE;
            }
        }

        public boolean containsDimension(ResourceKey<Level> dim) {
            return dim != null && dungeonDimensionIds.contains(dim.location().toString());
        }

        public boolean containsPlayer(UUID playerId) {
            return playerId != null && orderedPlayers.contains(playerId);
        }

        public boolean isCompletionExited(UUID playerId) {
            return playerId != null && completionExitedPlayers.contains(playerId);
        }

        public Optional<DungeonPlayerRunSnapshot> snapshotFor(UUID playerId) {
            if (playerId == null) return Optional.empty();
            for (DungeonPlayerRunSnapshot snap : playerSnapshots) {
                if (playerId.equals(snap.playerId())) {
                    return Optional.of(snap);
                }
            }
            return Optional.empty();
        }

        public RunRecord withCompletionExited(UUID playerId) {
            if (playerId == null || completionExitedPlayers.contains(playerId)) return this;

            List<UUID> updated = new ArrayList<>(completionExitedPlayers);
            updated.add(playerId);

            return new RunRecord(
                    runId,
                    dungeonId,
                    selectorDimensionId,
                    selectorPosLong,
                    dungeonDimensionIds,
                    state,
                    resetReason,
                    startedAtEpochMillis,
                    orderedPlayers,
                    updated,
                    playerSnapshots
            );
        }

        public RunRecord withState(DungeonRunState newState, DungeonResetReason reason) {
            return new RunRecord(
                    runId,
                    dungeonId,
                    selectorDimensionId,
                    selectorPosLong,
                    dungeonDimensionIds,
                    newState.name(),
                    reason == null ? "" : reason.name(),
                    startedAtEpochMillis,
                    orderedPlayers,
                    completionExitedPlayers,
                    playerSnapshots
            );
        }
    }

    private record Persisted(long nextRunId, List<RunRecord> runs) {
        private static final Codec<Persisted> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("next_run_id").forGetter(Persisted::nextRunId),
                RunRecord.CODEC.listOf().fieldOf("runs").forGetter(Persisted::runs)
        ).apply(i, Persisted::new));
    }

    private static final Codec<DungeonRunRegistryData> CODEC = Persisted.CODEC.xmap(
            DungeonRunRegistryData::fromPersisted,
            DungeonRunRegistryData::toPersisted
    );

    public static final SavedDataType<DungeonRunRegistryData> TYPE =
            new SavedDataType<>(SAVE_ID, DungeonRunRegistryData::new, CODEC);

    public static DungeonRunRegistryData get(ServerLevel anyLevel) {
        return get(anyLevel.getServer());
    }

    public static DungeonRunRegistryData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not available; cannot load DungeonRunRegistryData.");
        }
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    private long nextRunId = 1L;
    private final Map<Long, RunRecord> runsById = new HashMap<>();

    private DungeonRunRegistryData() {}

    private static DungeonRunRegistryData fromPersisted(Persisted p) {
        DungeonRunRegistryData d = new DungeonRunRegistryData();
        d.nextRunId = Math.max(1L, p.nextRunId());
        for (RunRecord r : p.runs()) {
            d.runsById.put(r.runId(), r);
        }
        return d;
    }

    private Persisted toPersisted() {
        List<RunRecord> runs = new ArrayList<>(runsById.values());
        runs.sort(Comparator.comparingLong(RunRecord::runId));
        return new Persisted(nextRunId, runs);
    }

    public long startRun(ResourceKey<Level> selectorDimension,
                         long selectorPosLong,
                         DungeonDefinition def,
                         java.util.Collection<UUID> orderedPlayers,
                         java.util.Collection<DungeonPlayerRunSnapshot> snapshots) {
        if (selectorDimension == null || def == null || orderedPlayers == null || orderedPlayers.isEmpty()) {
            return -1L;
        }

        Optional<RunRecord> existing = findActiveOrResettingRun(def.id());
        if (existing.isPresent()) {
            return -1L;
        }

        Set<UUID> incoming = Set.copyOf(orderedPlayers);
        for (RunRecord old : runsById.values()) {
            if (old.stateEnum() != DungeonRunState.ACTIVE && old.stateEnum() != DungeonRunState.RESETTING) continue;
            for (UUID id : old.orderedPlayers()) {
                if (incoming.contains(id)) {
                    return -1L;
                }
            }
        }

        long runId = nextRunId++;
        RunRecord rec = new RunRecord(
                runId,
                def.id(),
                selectorDimension.location().toString(),
                selectorPosLong,
                def.dimensionIds(),
                DungeonRunState.ACTIVE.name(),
                "",
                System.currentTimeMillis(),
                new ArrayList<>(orderedPlayers),
                new ArrayList<>(),
                snapshots == null ? List.of() : new ArrayList<>(snapshots)
        );

        runsById.put(runId, rec);
        setDirty();
        return runId;
    }

    public Optional<RunRecord> getRun(long runId) {
        return Optional.ofNullable(runsById.get(runId));
    }

    public Optional<RunRecord> findRunForPlayer(UUID playerId) {
        if (playerId == null) return Optional.empty();

        return runsById.values().stream()
                .filter(r -> (r.stateEnum() == DungeonRunState.ACTIVE || r.stateEnum() == DungeonRunState.RESETTING)
                        && r.containsPlayer(playerId))
                .sorted(Comparator.comparingLong(RunRecord::runId))
                .findFirst();
    }

    public Optional<RunRecord> findActiveOrResettingRun(String dungeonId) {
        if (dungeonId == null || dungeonId.isBlank()) return Optional.empty();

        return runsById.values().stream()
                .filter(r -> r.dungeonId().equalsIgnoreCase(dungeonId)
                        && (r.stateEnum() == DungeonRunState.ACTIVE || r.stateEnum() == DungeonRunState.RESETTING))
                .sorted(Comparator.comparingLong(RunRecord::runId))
                .findFirst();
    }

    public List<RunRecord> listRunsForDungeon(String dungeonId) {
        if (dungeonId == null || dungeonId.isBlank()) return List.of();

        List<RunRecord> out = runsById.values().stream()
                .filter(r -> r.dungeonId().equalsIgnoreCase(dungeonId))
                .sorted(Comparator.comparingLong(RunRecord::runId))
                .toList();

        return List.copyOf(out);
    }

    public List<RunRecord> listAllRuns() {
        List<RunRecord> out = runsById.values().stream()
                .sorted(Comparator.comparingLong(RunRecord::runId))
                .toList();
        return List.copyOf(out);
    }

    public boolean hasActiveOrResettingRun(String dungeonId) {
        return findActiveOrResettingRun(dungeonId).isPresent();
    }

    public boolean markCompletionExited(long runId, UUID playerId) {
        if (runId <= 0L || playerId == null) return false;

        RunRecord old = runsById.get(runId);
        if (old == null) return false;

        RunRecord updated = old.withCompletionExited(playerId);
        if (updated == old) return false;

        runsById.put(runId, updated);
        setDirty();
        return true;
    }

    public boolean setState(long runId, DungeonRunState state, DungeonResetReason reason) {
        if (runId <= 0L || state == null) return false;

        RunRecord old = runsById.get(runId);
        if (old == null) return false;

        RunRecord updated = old.withState(state, reason);
        runsById.put(runId, updated);
        setDirty();
        return true;
    }

    public boolean removeRun(long runId) {
        if (runId <= 0L) return false;
        boolean changed = runsById.remove(runId) != null;
        if (changed) setDirty();
        return changed;
    }
}