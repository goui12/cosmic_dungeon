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

    public record RecoveryRecord(
            UUID playerId,
            long runId,
            String dungeonId,
            String reason,
            CompoundTag inventoryNbt
    ) {
        // Keep this codec local: the public record may initialize before its enclosing SavedData.
        private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
        public static final Codec<RecoveryRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUID_CODEC.fieldOf("player_id").forGetter(RecoveryRecord::playerId),
                Codec.LONG.fieldOf("run_id").forGetter(RecoveryRecord::runId),
                Codec.STRING.fieldOf("dungeon_id").forGetter(RecoveryRecord::dungeonId),
                Codec.STRING.fieldOf("reason").forGetter(RecoveryRecord::reason),
                CompoundTag.CODEC.fieldOf("inventory_nbt").forGetter(RecoveryRecord::inventoryNbt)
        ).apply(i, RecoveryRecord::new));
    }

    private record Persisted(List<RecoveryRecord> entries, Map<String, InventoryHandoffPlan> handoffs, Map<String, Long> completed) {
        private static final Codec<Persisted> CODEC = RecordCodecBuilder.create(i -> i.group(
                RecoveryRecord.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(Persisted::entries),
                Codec.unboundedMap(Codec.STRING, InventoryHandoffPlan.CODEC).optionalFieldOf("handoffs", Map.of()).forGetter(Persisted::handoffs),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("completed_runs", Map.of()).forGetter(Persisted::completed)
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
        net.goui.cosmicdungeon.transaction.SavedDataProof.validate(server, SAVE_ID, CODEC);
        var data = overworld.getDataStorage().computeIfAbsent(TYPE); data.server = server; return data;
    }

    private final Map<UUID, RecoveryRecord> byPlayer = new HashMap<>();

    private final Map<String, InventoryHandoffPlan> handoffs = new HashMap<>();
    private final Map<String, Long> completed = new HashMap<>();
    private MinecraftServer server;
    private PendingDungeonRecoveryData() {}
    public boolean flushVerified() { return net.goui.cosmicdungeon.transaction.SavedDataProof.save(server, SAVE_ID, CODEC, this); }
    public InventoryHandoffPlan handoff(UUID owner) { return handoffs.get(owner.toString()); }
    public long completed(UUID owner) { return completed.getOrDefault(owner.toString(), 0L); }
    public void begin(InventoryHandoffPlan plan) {
        if (plan.worldReady()) throw new IllegalStateException("New handoff cannot skip world verification");
        if (byPlayer.containsKey(plan.owner()) || handoff(plan.owner()) != null)
            throw new IllegalStateException("Inventory recovery already pending; preserve existing decision");
        if (plan.kind().equals("cleanup") && completed(plan.owner()) >= plan.run())
            throw new IllegalStateException("Cleanup already acknowledged");
        handoffs.put(plan.owner().toString(), plan); setDirty();
    }
    public void worldReady(InventoryHandoffPlan plan) {
        require(plan); handoffs.put(plan.owner().toString(), plan.ready()); setDirty();
    }
    public void acknowledge(InventoryHandoffPlan plan) {
        require(plan);
        if (!handoff(plan.owner()).worldReady()) throw new IllegalStateException("World handoff not verified");
        if (plan.kind().equals("cleanup")) completed.merge(plan.owner().toString(), plan.run(), Math::max);
        handoffs.remove(plan.owner().toString()); setDirty();
    }
    private void require(InventoryHandoffPlan plan) {
        var current = handoff(plan.owner());
        if (current == null || !current.ready().image().equals(plan.ready().image()))
            throw new IllegalStateException("Different inventory handoff");
    }
    public boolean durable(UUID owner, long run) {
        var plan = handoff(owner);
        return completed(owner) >= run || plan != null && plan.kind().equals("cleanup") && plan.run() == run && plan.worldReady();
    }


    private static PendingDungeonRecoveryData fromPersisted(Persisted p) {
        PendingDungeonRecoveryData d = new PendingDungeonRecoveryData();
        for (RecoveryRecord rec : p.entries()) {
            if (rec.runId() <= 0 || d.byPlayer.put(rec.playerId(), rec) != null) throw new IllegalArgumentException("Invalid or duplicate legacy recovery");
        }
        p.handoffs().forEach((key, plan) -> {
            if (!UUID.fromString(key).equals(plan.owner()) || d.byPlayer.containsKey(plan.owner()))
                throw new IllegalArgumentException("Conflicting recovery owner");
            d.handoffs.put(key, plan);
        });
        p.completed().forEach((key, run) -> { UUID.fromString(key); if (run <= 0) throw new IllegalArgumentException("Invalid cleanup receipt"); d.completed.put(key, run); });
        for (var plan : d.handoffs.values())
            if (plan.kind().equals("cleanup") && d.completed(plan.owner()) >= plan.run())
                throw new IllegalArgumentException("Cleanup journal conflicts with its completed receipt");
        return d;
    }

    private Persisted toPersisted() {
        List<RecoveryRecord> out = new ArrayList<>(byPlayer.values());
        out.sort(Comparator.comparing(RecoveryRecord::playerId, Comparator.comparing(UUID::toString)));
        return new Persisted(out, Map.copyOf(handoffs), Map.copyOf(completed));
    }

    public void put(RecoveryRecord rec) {
        if (rec == null || rec.playerId() == null) return;
        if (byPlayer.containsKey(rec.playerId()) || handoff(rec.playerId()) != null) throw new IllegalStateException("Recovery already pending");
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