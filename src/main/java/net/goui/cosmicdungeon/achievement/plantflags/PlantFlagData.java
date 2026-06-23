package net.goui.cosmicdungeon.achievement.plantflags;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlantFlagData extends SavedData {
    private static final String SAVE_ID = "cosmicdungeon_plant_flags_v1";
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public record RunPlantState(long runId, List<UUID> planted, boolean completed) {
        private static final Codec<RunPlantState> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("run_id").forGetter(RunPlantState::runId),
                UUID_CODEC.listOf().optionalFieldOf("planted", List.of()).forGetter(RunPlantState::planted),
                Codec.BOOL.optionalFieldOf("completed", false).forGetter(RunPlantState::completed)
        ).apply(i, RunPlantState::new));
    }

    private record Persisted(long activeRunId, List<UUID> planted, long lastDisconnectEpochMillis, boolean completed,
                             String regionDimensionId, long regionPos1Long, long regionPos2Long,
                             List<RunPlantState> runs) {
        private static final Codec<Persisted> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.optionalFieldOf("active_run_id", -1L).forGetter(Persisted::activeRunId),
                UUID_CODEC.listOf().optionalFieldOf("planted", List.of()).forGetter(Persisted::planted),
                Codec.LONG.optionalFieldOf("last_disconnect_epoch_millis", 0L).forGetter(Persisted::lastDisconnectEpochMillis),
                Codec.BOOL.optionalFieldOf("completed", false).forGetter(Persisted::completed),
                Codec.STRING.optionalFieldOf("region_dimension_id", "").forGetter(Persisted::regionDimensionId),
                Codec.LONG.optionalFieldOf("region_pos1", Long.MIN_VALUE).forGetter(Persisted::regionPos1Long),
                Codec.LONG.optionalFieldOf("region_pos2", Long.MIN_VALUE).forGetter(Persisted::regionPos2Long),
                RunPlantState.CODEC.listOf().optionalFieldOf("runs", List.of()).forGetter(Persisted::runs)
        ).apply(i, Persisted::new));
    }

    private static final Codec<PlantFlagData> CODEC = Persisted.CODEC.xmap(PlantFlagData::fromPersisted, PlantFlagData::toPersisted);
    public static final SavedDataType<PlantFlagData> TYPE = new SavedDataType<>(SAVE_ID, PlantFlagData::new, CODEC);

    private final Map<Long, Set<UUID>> plantedByRun = new HashMap<>();
    private final Set<Long> completedRuns = new HashSet<>();
    private String regionDimensionId = "";
    private BlockPos regionPos1;
    private BlockPos regionPos2;

    public static PlantFlagData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld is not available; cannot load PlantFlagData.");
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    private static PlantFlagData fromPersisted(Persisted p) {
        PlantFlagData d = new PlantFlagData();
        d.regionDimensionId = p.regionDimensionId() == null ? "" : p.regionDimensionId();
        if (p.regionPos1Long() != Long.MIN_VALUE) d.regionPos1 = BlockPos.of(p.regionPos1Long());
        if (p.regionPos2Long() != Long.MIN_VALUE) d.regionPos2 = BlockPos.of(p.regionPos2Long());
        for (RunPlantState state : p.runs()) {
            if (state.runId() <= 0L) continue;
            d.plantedByRun.put(state.runId(), new HashSet<>(state.planted()));
            if (state.completed()) d.completedRuns.add(state.runId());
        }
        if (p.runs().isEmpty() && p.activeRunId() > 0L) {
            d.plantedByRun.put(p.activeRunId(), new HashSet<>(p.planted()));
            if (p.completed()) d.completedRuns.add(p.activeRunId());
        }
        return d;
    }

    private Persisted toPersisted() {
        List<RunPlantState> runs = new ArrayList<>();
        for (var entry : plantedByRun.entrySet()) {
            runs.add(new RunPlantState(entry.getKey(), new ArrayList<>(entry.getValue()), completedRuns.contains(entry.getKey())));
        }
        for (Long runId : completedRuns) {
            if (!plantedByRun.containsKey(runId)) runs.add(new RunPlantState(runId, List.of(), true));
        }
        runs.sort(Comparator.comparingLong(RunPlantState::runId));
        return new Persisted(-1L, List.of(), 0L, false, regionDimensionId,
                regionPos1 == null ? Long.MIN_VALUE : regionPos1.asLong(),
                regionPos2 == null ? Long.MIN_VALUE : regionPos2.asLong(), runs);
    }

    public Set<UUID> planted(long runId) { return Set.copyOf(plantedByRun.getOrDefault(runId, Set.of())); }
    public boolean completed(long runId) { return completedRuns.contains(runId); }
    public String regionDimensionId() { return regionDimensionId; }
    public BlockPos regionPos1() { return regionPos1; }
    public BlockPos regionPos2() { return regionPos2; }

    public void initializeRun(long runId) { if (runId > 0L) { plantedByRun.put(runId, new HashSet<>()); completedRuns.remove(runId); setDirty(); } }
    public void markPlanted(long runId, UUID id) { if (runId > 0L && id != null && plantedByRun.computeIfAbsent(runId, k -> new HashSet<>()).add(id)) setDirty(); }
    public void clearRun(long runId) { if (runId > 0L) { plantedByRun.remove(runId); completedRuns.remove(runId); setDirty(); } }
    public void clearAllRuns() { plantedByRun.clear(); completedRuns.clear(); setDirty(); }
    public void setCompleted(long runId, boolean value) { if (runId <= 0L) return; if (value) completedRuns.add(runId); else completedRuns.remove(runId); setDirty(); }
    public List<RunPlantState> runStates() { return plantedByRun.keySet().stream().sorted().map(id -> new RunPlantState(id, new ArrayList<>(plantedByRun.getOrDefault(id, Set.of())), completedRuns.contains(id))).toList(); }
    public void setRegion(String dimensionId, BlockPos pos1, BlockPos pos2) {
        this.regionDimensionId = dimensionId == null ? "" : dimensionId;
        this.regionPos1 = pos1 == null ? null : pos1.immutable();
        this.regionPos2 = pos2 == null ? null : pos2.immutable();
        setDirty();
    }
}
