package net.goui.cosmicdungeon.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PendingDungeonRecoveryData extends SavedData {
    private static final String SAVE_ID = "cosmicdungeon_pending_dungeon_recovery";
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public record RecoveryRecord(
            UUID playerId,
            long runId,
            String dungeonId,
            String reason,
            CompoundTag inventoryNbt
    ) {
        public static final Codec<RecoveryRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUID_CODEC.fieldOf("player_id").forGetter(RecoveryRecord::playerId),
                Codec.LONG.fieldOf("run_id").forGetter(RecoveryRecord::runId),
                Codec.STRING.fieldOf("dungeon_id").forGetter(RecoveryRecord::dungeonId),
                Codec.STRING.fieldOf("reason").forGetter(RecoveryRecord::reason),
                CompoundTag.CODEC.fieldOf("inventory_nbt").forGetter(RecoveryRecord::inventoryNbt)
        ).apply(i, RecoveryRecord::new));
    }

    private record Persisted(List<RecoveryRecord> entries) {
        private static final Codec<Persisted> CODEC = RecordCodecBuilder.create(i -> i.group(
                RecoveryRecord.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(Persisted::entries)
        ).apply(i, Persisted::new));
    }

    private static final Codec<PendingDungeonRecoveryData> CODEC = Persisted.CODEC.xmap(
            PendingDungeonRecoveryData::fromPersisted,
            PendingDungeonRecoveryData::toPersisted
    );

    public static final SavedDataType<PendingDungeonRecoveryData> TYPE =
            new SavedDataType<>(SAVE_ID, PendingDungeonRecoveryData::new, CODEC);

    public static PendingDungeonRecoveryData get(ServerLevel anyLevel) {
        return get(anyLevel.getServer());
    }

    public static PendingDungeonRecoveryData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not available; cannot load PendingDungeonRecoveryData.");
        }
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    private final Map<UUID, RecoveryRecord> byPlayer = new HashMap<>();

    private PendingDungeonRecoveryData() {}

    private static PendingDungeonRecoveryData fromPersisted(Persisted p) {
        PendingDungeonRecoveryData d = new PendingDungeonRecoveryData();
        for (RecoveryRecord rec : p.entries()) {
            d.byPlayer.put(rec.playerId(), rec);
        }
        return d;
    }

    private Persisted toPersisted() {
        List<RecoveryRecord> out = new ArrayList<>(byPlayer.values());
        out.sort(Comparator.comparing(RecoveryRecord::playerId, Comparator.comparing(UUID::toString)));
        return new Persisted(out);
    }

    public void put(RecoveryRecord rec) {
        if (rec == null || rec.playerId() == null) return;
        byPlayer.put(rec.playerId(), rec);
        setDirty();
    }

    public Optional<RecoveryRecord> get(UUID playerId) {
        if (playerId == null) return Optional.empty();
        return Optional.ofNullable(byPlayer.get(playerId));
    }

    public boolean remove(UUID playerId) {
        if (playerId == null) return false;
        boolean changed = byPlayer.remove(playerId) != null;
        if (changed) setDirty();
        return changed;
    }

    public List<RecoveryRecord> listAll() {
        List<RecoveryRecord> out = new ArrayList<>(byPlayer.values());
        out.sort(Comparator.comparing(RecoveryRecord::runId).thenComparing(r -> r.playerId().toString()));
        return List.copyOf(out);
    }
}