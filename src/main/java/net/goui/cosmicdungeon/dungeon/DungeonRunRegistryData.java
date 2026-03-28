package net.goui.cosmicdungeon.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;
import java.util.stream.Collectors;

public final class DungeonRunRegistryData extends SavedData {
    private static final String SAVE_ID = "cosmicdungeon_dungeon_runs";

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public record RunRecord(
            long runId,
            String selectorDimensionId,
            long selectorPosLong,
            String dungeonDimensionId,
            List<UUID> orderedPlayers,
            List<UUID> exitedPlayers
    ) {
        public static final Codec<RunRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("run_id").forGetter(RunRecord::runId),
                Codec.STRING.fieldOf("selector_dimension").forGetter(RunRecord::selectorDimensionId),
                Codec.LONG.fieldOf("selector_pos").forGetter(RunRecord::selectorPosLong),
                Codec.STRING.fieldOf("dungeon_dimension").forGetter(RunRecord::dungeonDimensionId),
                UUID_CODEC.listOf().fieldOf("ordered_players").forGetter(RunRecord::orderedPlayers),
                UUID_CODEC.listOf().fieldOf("exited_players").forGetter(RunRecord::exitedPlayers)
        ).apply(i, RunRecord::new));

        public RunRecord withExited(UUID playerId) {
            if (playerId == null) return this;
            if (exitedPlayers.contains(playerId)) return this;

            List<UUID> updated = new ArrayList<>(exitedPlayers);
            updated.add(playerId);
            return new RunRecord(runId, selectorDimensionId, selectorPosLong, dungeonDimensionId, orderedPlayers, updated);
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

    private DungeonRunRegistryData() {
    }

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

    public long registerRun(ResourceKey<Level> selectorDimension,
                            long selectorPosLong,
                            ResourceKey<Level> dungeonDimension,
                            Collection<UUID> orderedPlayers) {
        if (selectorDimension == null || dungeonDimension == null || orderedPlayers == null || orderedPlayers.isEmpty()) {
            return -1L;
        }

        String selectorDimId = selectorDimension.location().toString();
        String dungeonDimId = dungeonDimension.location().toString();

        // Remove older runs for this selector block.
        runsById.entrySet().removeIf(e ->
                e.getValue().selectorDimensionId().equals(selectorDimId)
                        && e.getValue().selectorPosLong() == selectorPosLong
        );

        // Remove these players from any other active runs so one player cannot belong to two runs.
        Set<UUID> incoming = new HashSet<>(orderedPlayers);
        List<Map.Entry<Long, RunRecord>> updates = new ArrayList<>();

        for (var e : runsById.entrySet()) {
            RunRecord old = e.getValue();
            List<UUID> filteredPlayers = old.orderedPlayers().stream()
                    .filter(id -> !incoming.contains(id))
                    .collect(Collectors.toCollection(ArrayList::new));

            List<UUID> filteredExited = old.exitedPlayers().stream()
                    .filter(filteredPlayers::contains)
                    .collect(Collectors.toCollection(ArrayList::new));

            if (filteredPlayers.size() != old.orderedPlayers().size()) {
                if (filteredPlayers.isEmpty()) {
                    updates.add(Map.entry(e.getKey(), null));
                } else {
                    updates.add(Map.entry(e.getKey(), new RunRecord(
                            old.runId(),
                            old.selectorDimensionId(),
                            old.selectorPosLong(),
                            old.dungeonDimensionId(),
                            filteredPlayers,
                            filteredExited
                    )));
                }
            }
        }

        for (var up : updates) {
            if (up.getValue() == null) runsById.remove(up.getKey());
            else runsById.put(up.getKey(), up.getValue());
        }

        long runId = nextRunId++;
        RunRecord rec = new RunRecord(
                runId,
                selectorDimId,
                selectorPosLong,
                dungeonDimId,
                new ArrayList<>(orderedPlayers),
                new ArrayList<>()
        );

        runsById.put(runId, rec);
        setDirty();
        return runId;
    }

    public boolean markPlayerExitedAndShouldReset(MinecraftServer server, ResourceKey<Level> dungeonDimension, UUID playerId) {
        if (server == null || dungeonDimension == null || playerId == null) return false;

        String dungeonId = dungeonDimension.location().toString();
        boolean changed = false;

        List<Long> relevantRunIds = new ArrayList<>();
        for (var e : runsById.entrySet()) {
            if (e.getValue().dungeonDimensionId().equals(dungeonId)) {
                relevantRunIds.add(e.getKey());
            }
        }

        if (relevantRunIds.isEmpty()) return false;

        for (Long runId : relevantRunIds) {
            RunRecord old = runsById.get(runId);
            if (old == null) continue;
            if (!old.orderedPlayers().contains(playerId)) continue;

            RunRecord updated = old.withExited(playerId);
            if (updated != old) {
                runsById.put(runId, updated);
                changed = true;
            }
        }

        if (changed) setDirty();

        // Reset only when ALL currently-online tracked players for this dungeon dimension are already exited.
        int onlineTracked = 0;
        int onlineStillInside = 0;

        for (RunRecord run : runsById.values()) {
            if (!run.dungeonDimensionId().equals(dungeonId)) continue;

            Set<UUID> exited = new HashSet<>(run.exitedPlayers());

            for (UUID uuid : run.orderedPlayers()) {
                var player = server.getPlayerList().getPlayer(uuid);
                if (player == null) continue; // offline players are ignored by design

                onlineTracked++;
                if (!exited.contains(uuid)) {
                    onlineStillInside++;
                }
            }
        }

        return onlineTracked > 0 && onlineStillInside == 0;
    }

    public void clearRunsForDimension(ResourceKey<Level> dungeonDimension) {
        if (dungeonDimension == null) return;
        String dungeonId = dungeonDimension.location().toString();
        boolean changed = runsById.entrySet().removeIf(e -> e.getValue().dungeonDimensionId().equals(dungeonId));
        if (changed) setDirty();
    }

    public List<RunRecord> listRunsForDimension(ResourceKey<Level> dungeonDimension) {
        if (dungeonDimension == null) return List.of();
        String dungeonId = dungeonDimension.location().toString();

        List<RunRecord> out = runsById.values().stream()
                .filter(r -> r.dungeonDimensionId().equals(dungeonId))
                .sorted(Comparator.comparingLong(RunRecord::runId))
                .collect(Collectors.toCollection(ArrayList::new));

        return Collections.unmodifiableList(out);
    }

    public boolean hasAnyRunsForDimension(ResourceKey<Level> dungeonDimension) {
        if (dungeonDimension == null) return false;
        String dungeonId = dungeonDimension.location().toString();
        return runsById.values().stream().anyMatch(r -> r.dungeonDimensionId().equals(dungeonId));
    }
}