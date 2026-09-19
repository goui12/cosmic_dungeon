package net.goui.cosmicdungeon.dungeon.d1;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.transaction.SavedDataProof;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.*;

/** Success retains both inventories. Each deliberate claim uses the shared owner handoff journal. */
public final class D1StoredInventoryData extends SavedData {
    private static final String SAVE_ID = "cosmicdungeon_d1_stored_inventory_v1";
    private static final Codec<D1StoredInventoryData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, CompoundTag.CODEC).optionalFieldOf("pending", Map.of())
                    .forGetter((D1StoredInventoryData d) -> d.pending),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("last_stashed_run", Map.of())
                    .forGetter((D1StoredInventoryData d) -> d.lastStashed)
    ).apply(i, D1StoredInventoryData::load));
    private static final SavedDataType<D1StoredInventoryData> TYPE =
            new SavedDataType<>(SAVE_ID, D1StoredInventoryData::new, CODEC);
    private final Map<String, CompoundTag> pending = new LinkedHashMap<>();
    private final Map<String, Long> lastStashed = new HashMap<>();
    private final Map<UUID, NavigableMap<Long, String>> byOwner = new HashMap<>();
    private MinecraftServer server;
    private D1StoredInventoryData() {}
    public static long run(String key) {
        int separator = key.indexOf('|');
        if (separator < 0) throw new IllegalArgumentException("Malformed stored inventory key");
        UUID.fromString(key.substring(0, separator));
        long run = Long.parseLong(key.substring(separator + 1));
        if (run <= 0) throw new IllegalArgumentException("Invalid stored inventory run");
        return run;
    }
    private void index(String key) {
        long run = run(key); UUID owner = UUID.fromString(key.substring(0, key.indexOf('|')));
        if (!key.equals(owner + "|" + run)) throw new IllegalArgumentException("Noncanonical stored inventory key");
        byOwner.computeIfAbsent(owner, ignored -> new TreeMap<>()).put(run, key);
    }
    private static D1StoredInventoryData load(Map<String, CompoundTag> pending, Map<String, Long> last) {
        var data = new D1StoredInventoryData();
        last.forEach((key, run) -> { UUID.fromString(key); if (run <= 0) throw new IllegalArgumentException("Invalid stash receipt"); data.lastStashed.put(key, run); });
        pending.forEach((key, image) -> { data.index(key); data.pending.put(key, image.copy()); });
        return data;
    }
    public static D1StoredInventoryData get(MinecraftServer server) {
        SavedDataProof.validate(server, SAVE_ID, CODEC);
        var data = server.overworld().getDataStorage().computeIfAbsent(TYPE); data.server = server; return data;
    }
    public boolean flushVerified() { return SavedDataProof.save(server, SAVE_ID, CODEC, this); }
    public void stash(long runId, UUID player, CompoundTag inventory) {
        if (runId <= 0) throw new IllegalArgumentException("Invalid stash run");
        String owner = player.toString(), key = owner + "|" + runId;
        var existing = pending.get(key);
        if (existing != null) {
            if (!existing.equals(inventory)) throw new IllegalStateException("Stored inventory differs from cleanup decision");
            return;
        }
        if (runId <= lastStashed.getOrDefault(owner, 0L))
            throw new IllegalStateException("Older stash has already been retired; retain cleanup for review");
        pending.put(key, inventory.copy()); index(key); lastStashed.put(owner, runId); setDirty();
    }
    public CompoundTag image(String key) { var image = pending.get(key); return image == null ? new CompoundTag() : image.copy(); }
    public String first(UUID player) { var entries = byOwner.get(player); return entries == null || entries.isEmpty() ? null : entries.firstEntry().getValue(); }
    public boolean hasPending(UUID player) { return first(player) != null; }
    public void applyClaim(String key, CompoundTag before, CompoundTag after) {
        run(key);
        var current = image(key);
        if (!current.equals(before) && !current.equals(after)) throw new IllegalStateException("Stored inventory changed during claim");
        if (after.isEmpty()) {
            pending.remove(key);
            UUID owner = UUID.fromString(key.substring(0, key.indexOf('|')));
            var entries = byOwner.get(owner);
            if (entries != null) { entries.remove(run(key)); if (entries.isEmpty()) byOwner.remove(owner); }
        } else { pending.put(key, after.copy()); index(key); }
        setDirty();
    }
    public int claim(ServerPlayer player) { return DungeonInventoryHandoffs.claim(player); }
}
