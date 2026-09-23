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

    public record RunRecord(
            long runId,
            String dungeonId,
            String selectorDimensionId,
            long selectorPosLong,
            List<String> dungeonDimensionIds,
            int instanceSlot,
            String state,
            String resetReason,
            long startedAtEpochMillis,
            List<UUID> orderedPlayers,
            List<UUID> completionExitedPlayers,
            List<DungeonPlayerRunSnapshot> playerSnapshots
    ) {
        // Keep this codec local: the public record may initialize before its enclosing SavedData.
        private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
        public static final Codec<RunRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("run_id").forGetter(RunRecord::runId),
                Codec.STRING.fieldOf("dungeon_id").forGetter(RunRecord::dungeonId),
                Codec.STRING.fieldOf("selector_dimension").forGetter(RunRecord::selectorDimensionId),
                Codec.LONG.fieldOf("selector_pos").forGetter(RunRecord::selectorPosLong),
                Codec.STRING.listOf().fieldOf("dungeon_dimension_ids").forGetter(RunRecord::dungeonDimensionIds),
                Codec.INT.optionalFieldOf("instance_slot", 0).forGetter(RunRecord::instanceSlot),
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

        public Optional<UUID> groupLeader() {
            return orderedPlayers.isEmpty() ? Optional.empty() : Optional.ofNullable(orderedPlayers.getFirst());
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
                    instanceSlot,
                    state,
                    resetReason,
                    startedAtEpochMillis,
                    orderedPlayers,
                    updated,
                    playerSnapshots
            );
        }

        public RunRecord withoutPlayer(UUID playerId) {
            if (playerId == null || !orderedPlayers.contains(playerId)) return this;

            List<UUID> updatedPlayers = new ArrayList<>(orderedPlayers);
            updatedPlayers.remove(playerId);

            List<UUID> updatedExited = new ArrayList<>(completionExitedPlayers);
            updatedExited.remove(playerId);

            List<DungeonPlayerRunSnapshot> updatedSnapshots = new ArrayList<>();
            for (DungeonPlayerRunSnapshot snap : playerSnapshots) {
                if (snap == null || !playerId.equals(snap.playerId())) {
                    updatedSnapshots.add(snap);
                }
            }

            return new RunRecord(
                    runId,
                    dungeonId,
                    selectorDimensionId,
                    selectorPosLong,
                    dungeonDimensionIds,
                    instanceSlot,
                    state,
                    resetReason,
                    startedAtEpochMillis,
                    updatedPlayers,
                    updatedExited,
                    updatedSnapshots
            );
        }

        public RunRecord withState(DungeonRunState newState, DungeonResetReason reason) {
            return new RunRecord(
                    runId,
                    dungeonId,
                    selectorDimensionId,
                    selectorPosLong,
                    dungeonDimensionIds,
                    instanceSlot,
                    newState.name(),
                    reason == null ? "" : reason.name(),
                    startedAtEpochMillis,
                    orderedPlayers,
                    completionExitedPlayers,
                    playerSnapshots
            );
        }

        public RunRecord withInstance(int slot, List<String> dimensions) {
            return new RunRecord(runId, dungeonId, selectorDimensionId, selectorPosLong,
                    List.copyOf(dimensions), slot, state, resetReason, startedAtEpochMillis,
                    orderedPlayers, completionExitedPlayers, playerSnapshots);
        }
    }

    private record Persisted(long nextRunId, List<RunRecord> runs, Map<String, net.minecraft.nbt.CompoundTag> startup) {
        private static final Codec<Persisted> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("next_run_id").forGetter(Persisted::nextRunId),
                RunRecord.CODEC.listOf().fieldOf("runs").forGetter(Persisted::runs),
                Codec.unboundedMap(Codec.STRING, net.minecraft.nbt.CompoundTag.CODEC).optionalFieldOf("startup", Map.of()).forGetter(Persisted::startup)
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
        net.goui.cosmicdungeon.transaction.SavedDataProof.validate(server, SAVE_ID, CODEC);
        var data = overworld.getDataStorage().computeIfAbsent(TYPE); data.server = server; return data;
    }

    private long nextRunId = 1L;
    private final Map<Long, RunRecord> runsById = new HashMap<>();

    private final Map<String, net.minecraft.nbt.CompoundTag> startup = new HashMap<>();
    private MinecraftServer server;
    private DungeonRunRegistryData() {}
    public boolean flushVerified() { return net.goui.cosmicdungeon.transaction.SavedDataProof.save(server, SAVE_ID, CODEC, this); }
    public boolean starting(long run) { return startup.containsKey(Long.toString(run)); }
    public net.minecraft.nbt.CompoundTag startupImage(long run, UUID owner) {
        var image = startup.get(Long.toString(run));
        return image == null ? new net.minecraft.nbt.CompoundTag() : image.getCompoundOrEmpty(owner.toString()).copy();
    }
    public void prepareStartup(long run, net.minecraft.nbt.CompoundTag owners) {
        var record = getRun(run).orElseThrow();
        validateStartup(record, owners);
        if (startup.putIfAbsent(Long.toString(run), owners.copy()) != null) throw new IllegalStateException("Startup already recorded");
        setDirty();
    }
    public boolean completeStartupVerified(long run) {
        var image = startup.remove(Long.toString(run)); setDirty();
        if (flushVerified()) return true;
        if (image != null) startup.put(Long.toString(run), image);
        setDirty(); return false;
    }
    public boolean retireVerified(long run) {
        var record = runsById.remove(run);
        var image = startup.remove(Long.toString(run)); setDirty();
        if (flushVerified()) return true;
        if (record != null) runsById.put(run, record);
        if (image != null) startup.put(Long.toString(run), image);
        setDirty(); return false;
    }
    private static void validateStartup(RunRecord run, net.minecraft.nbt.CompoundTag owners) {
        var roster = run.orderedPlayers().stream().map(UUID::toString).collect(java.util.stream.Collectors.toSet());
        if (!owners.keySet().equals(roster)) throw new IllegalArgumentException("Startup roster differs from run");
        for (String owner : roster) {
            var image = owners.getCompound(owner).orElseThrow();
            if (image.getCompound("inventory").isEmpty() || image.getCompound("ownership").isEmpty())
                throw new IllegalArgumentException("Missing pre-entry image");
            var ownership = image.getCompoundOrEmpty("ownership");
            if (!ownership.isEmpty()) ChopOwnershipData.Entry.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, ownership).getOrThrow();
        }
    }


    private static DungeonRunRegistryData fromPersisted(Persisted p) {
        DungeonRunRegistryData d = new DungeonRunRegistryData();
        d.nextRunId = Math.max(1L, p.nextRunId());
        for (RunRecord r : p.runs()) {
            if (r.runId() <= 0 || r.runId() >= p.nextRunId() || d.runsById.put(r.runId(), r) != null) throw new IllegalArgumentException("Invalid or duplicate dungeon run");
        }
        p.startup().forEach((key, image) -> {
            long run = Long.parseLong(key);
            if (!key.equals(Long.toString(run))) throw new IllegalArgumentException("Invalid startup key");
            validateStartup(d.getRun(run).orElseThrow(), image); d.startup.put(key, image.copy());
        });
        return d;
    }

    private Persisted toPersisted() {
        List<RunRecord> runs = new ArrayList<>(runsById.values());
        runs.sort(Comparator.comparingLong(RunRecord::runId));
        return new Persisted(nextRunId, runs, Map.copyOf(startup));
    }

    public long startRun(ResourceKey<Level> selectorDimension,
                         long selectorPosLong,
                         DungeonDefinition def,
                         int instanceSlot,
                         java.util.Collection<String> instanceDimensionIds,
                         java.util.Collection<UUID> orderedPlayers,
                         java.util.Collection<DungeonPlayerRunSnapshot> snapshots) {
        if (selectorDimension == null || def == null || orderedPlayers == null || orderedPlayers.isEmpty()) {
            return -1L;
        }

        if (instanceSlot < 1 || instanceSlot > DungeonInstanceSlots.SLOT_COUNT
                || instanceDimensionIds == null || instanceDimensionIds.isEmpty()
                || isSlotOccupied(instanceSlot)) {
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
                new ArrayList<>(instanceDimensionIds),
                instanceSlot,
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

    public boolean isSlotOccupied(int slot) {
        return runsById.values().stream().anyMatch(r -> r.instanceSlot() == slot
                && (r.stateEnum() == DungeonRunState.ACTIVE
                || r.stateEnum() == DungeonRunState.RESETTING
                || r.stateEnum() == DungeonRunState.FAILED));
    }

    public Optional<Integer> firstAvailableSlot() {
        for (int slot = 1; slot <= DungeonInstanceSlots.SLOT_COUNT; slot++) {
            if (!isSlotOccupied(slot)) return Optional.of(slot);
        }
        return Optional.empty();
    }

    public Optional<RunRecord> findRunForInstanceDimension(ResourceKey<Level> dimension) {
        if (dimension == null) return Optional.empty();
        return runsById.values().stream()
                .filter(r -> (r.stateEnum() == DungeonRunState.ACTIVE || r.stateEnum() == DungeonRunState.RESETTING)
                        && r.containsDimension(dimension))
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

    public boolean removePlayer(long runId, UUID playerId) {
        if (runId <= 0L || playerId == null) return false;

        RunRecord old = runsById.get(runId);
        if (old == null) return false;

        if (starting(runId)) throw new IllegalStateException("Finish startup rollback before changing its roster");
        RunRecord updated = old.withoutPlayer(playerId);
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

    public boolean assignInstance(long runId, int slot, List<String> dimensions) {
        RunRecord old = runsById.get(runId);
        if (old == null || slot < 1 || dimensions == null || dimensions.isEmpty() || isSlotOccupied(slot)) return false;
        runsById.put(runId, old.withInstance(slot, dimensions));
        setDirty();
        return true;
    }

    public boolean removeRun(long runId) {
        if (runId <= 0L) return false;
        boolean changed = runsById.remove(runId) != null;
        if (changed) startup.remove(Long.toString(runId));
        if (changed) setDirty();
        return changed;
    }
}
