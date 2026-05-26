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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PlantFlagData extends SavedData {
    private static final String SAVE_ID = "cosmicdungeon_plant_flags_v1";
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    private record Persisted(long activeRunId, List<UUID> planted, long lastDisconnectEpochMillis, boolean completed,
                             String regionDimensionId, long regionPos1Long, long regionPos2Long) {
        private static final Codec<Persisted> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.optionalFieldOf("active_run_id", -1L).forGetter(Persisted::activeRunId),
                UUID_CODEC.listOf().optionalFieldOf("planted", List.of()).forGetter(Persisted::planted),
                Codec.LONG.optionalFieldOf("last_disconnect_epoch_millis", 0L).forGetter(Persisted::lastDisconnectEpochMillis),
                Codec.BOOL.optionalFieldOf("completed", false).forGetter(Persisted::completed),
                Codec.STRING.optionalFieldOf("region_dimension_id", "").forGetter(Persisted::regionDimensionId),
                Codec.LONG.optionalFieldOf("region_pos1", Long.MIN_VALUE).forGetter(Persisted::regionPos1Long),
                Codec.LONG.optionalFieldOf("region_pos2", Long.MIN_VALUE).forGetter(Persisted::regionPos2Long)
        ).apply(i, Persisted::new));
    }

    private static final Codec<PlantFlagData> CODEC = Persisted.CODEC.xmap(PlantFlagData::fromPersisted, PlantFlagData::toPersisted);
    public static final SavedDataType<PlantFlagData> TYPE = new SavedDataType<>(SAVE_ID, PlantFlagData::new, CODEC);

    private long activeRunId = -1L;
    private final Set<UUID> planted = new HashSet<>();
    private long lastDisconnectEpochMillis = 0L;
    private boolean completed = false;
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
        d.activeRunId = p.activeRunId();
        d.planted.addAll(p.planted());
        d.lastDisconnectEpochMillis = p.lastDisconnectEpochMillis();
        d.completed = p.completed();
        d.regionDimensionId = p.regionDimensionId() == null ? "" : p.regionDimensionId();
        if (p.regionPos1Long() != Long.MIN_VALUE) d.regionPos1 = BlockPos.of(p.regionPos1Long());
        if (p.regionPos2Long() != Long.MIN_VALUE) d.regionPos2 = BlockPos.of(p.regionPos2Long());
        return d;
    }

    private Persisted toPersisted() {
        return new Persisted(activeRunId, new ArrayList<>(planted), lastDisconnectEpochMillis, completed,
                regionDimensionId, regionPos1 == null ? Long.MIN_VALUE : regionPos1.asLong(), regionPos2 == null ? Long.MIN_VALUE : regionPos2.asLong());
    }

    public long activeRunId() { return activeRunId; }
    public Set<UUID> planted() { return Set.copyOf(planted); }
    public long lastDisconnectEpochMillis() { return lastDisconnectEpochMillis; }
    public boolean completed() { return completed; }
    public String regionDimensionId() { return regionDimensionId; }
    public BlockPos regionPos1() { return regionPos1; }
    public BlockPos regionPos2() { return regionPos2; }

    public void setRun(long runId) { this.activeRunId = runId; setDirty(); }
    public void markPlanted(UUID id) { if (planted.add(id)) setDirty(); }
    public void clearPlanted() { planted.clear(); setDirty(); }
    public void markDisconnectNow() { lastDisconnectEpochMillis = System.currentTimeMillis(); setDirty(); }
    public void setCompleted(boolean value) { completed = value; setDirty(); }
    public void setRegion(String dimensionId, BlockPos pos1, BlockPos pos2) {
        this.regionDimensionId = dimensionId == null ? "" : dimensionId;
        this.regionPos1 = pos1 == null ? null : pos1.immutable();
        this.regionPos2 = pos2 == null ? null : pos2.immutable();
        setDirty();
    }
}
