package net.goui.cosmicdungeon.door;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

import static net.goui.cosmicdungeon.component.ModDataComponents.UUID_STRING_CODEC;

/**
 * Persists lock info per-door (vanilla doors only).
 * Stored per-dimension in that dimension's data storage so snapshot resets restore
 * lock state exactly as it existed when the snapshot was taken.
 * Keys are normalized to the LOWER-half blockpos of the door.
 */
public final class DoorLockData extends SavedData {
    private static final String STORAGE_NAME = "cosmicdungeon_door_locks_v1";

    /** Map key: dimension + lower-half pos */
    private static final class Key {
        final ResourceLocation dim;
        final BlockPos pos;
        Key(ResourceLocation dim, BlockPos pos) {
            this.dim = dim;
            this.pos = pos.immutable();
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return dim.equals(k.dim) && pos.equals(k.pos);
        }
        @Override public int hashCode() { return Objects.hash(dim, pos); }
    }

    /** Public data returned for a lock */
    public static final class LockInfo {
        public final UUID lockId;
        public final UUID owner;
        public final long createdAtEpochMillis;
        private LockInfo(UUID lockId, UUID owner, long createdAtEpochMillis) {
            this.lockId = lockId;
            this.owner = owner;
            this.createdAtEpochMillis = createdAtEpochMillis;
        }
    }

    private final Map<Key, LockInfo> locks = new HashMap<>();
    private final Map<UUID, Key> byId = new HashMap<>();

    public DoorLockData() {}

    /* -------------------- CODEC -------------------- */

    private static final Codec<LockEntry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(e -> e.dim),
            Codec.INT.fieldOf("x").forGetter(e -> e.x),
            Codec.INT.fieldOf("y").forGetter(e -> e.y),
            Codec.INT.fieldOf("z").forGetter(e -> e.z),
            UUID_STRING_CODEC.fieldOf("lock_id").forGetter(e -> e.lockId),  // <-- changed
            UUID_STRING_CODEC.fieldOf("owner").forGetter(e -> e.owner),     // <-- changed
            Codec.LONG.fieldOf("created_at").forGetter(e -> e.createdAt)
    ).apply(i, LockEntry::new));

    public static final Codec<DoorLockData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ENTRY_CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(DoorLockData::entriesView)
    ).apply(i, DoorLockData::fromEntries));

    private record LockEntry(ResourceLocation dim, int x, int y, int z, UUID lockId, UUID owner, long createdAt) {
        BlockPos pos() { return new BlockPos(x, y, z); }
        static LockEntry of(ResourceLocation dim, BlockPos pos, LockInfo info) {
            return new LockEntry(dim, pos.getX(), pos.getY(), pos.getZ(), info.lockId, info.owner, info.createdAtEpochMillis);
        }
    }

    private List<LockEntry> entriesView() {
        List<LockEntry> out = new ArrayList<>(locks.size());
        for (Map.Entry<Key, LockInfo> e : locks.entrySet()) {
            out.add(LockEntry.of(e.getKey().dim, e.getKey().pos, e.getValue()));
        }
        out.sort(Comparator.comparing((LockEntry e) -> e.dim.toString())
                .thenComparingInt(e -> e.x).thenComparingInt(e -> e.y).thenComparingInt(e -> e.z));
        return out;
    }

    private static DoorLockData fromEntries(List<LockEntry> list) {
        DoorLockData d = new DoorLockData();
        for (LockEntry e : list) {
            Key k = new Key(e.dim, e.pos());
            LockInfo info = new LockInfo(e.lockId, e.owner, e.createdAt);
            d.locks.put(k, info);
            d.byId.put(info.lockId, k);
        }
        return d;
    }

    /* -------------------- SavedDataType -------------------- */

    private static final SavedDataType<DoorLockData> TYPE =
            new SavedDataType<>(STORAGE_NAME, DoorLockData::new, CODEC);

    public static DoorLockData get(Level level) {
        if (!(level instanceof ServerLevel sl)) {
            throw new IllegalStateException("DoorLockData accessed on client or non-server level");
        }
        return sl.getDataStorage().computeIfAbsent(TYPE);
    }

    /* -------------------- API -------------------- */

    public boolean isLocked(Level level, BlockPos lowerDoorPos) {
        return locks.containsKey(key(level, lowerDoorPos));
    }

    public LockInfo getLock(Level level, BlockPos lowerDoorPos) {
        return locks.get(key(level, lowerDoorPos));
    }

    public LockInfo lock(Level level, BlockPos lowerDoorPos, UUID owner) {
        Key k = key(level, lowerDoorPos);
        if (locks.containsKey(k)) return locks.get(k);
        LockInfo info = new LockInfo(UUID.randomUUID(), owner, System.currentTimeMillis());
        locks.put(k, info);
        byId.put(info.lockId, k);
        setDirty();
        return info;
    }

    public boolean unlock(Level level, BlockPos lowerDoorPos) {
        Key k = key(level, lowerDoorPos);
        LockInfo removed = locks.remove(k);
        if (removed != null) {
            byId.remove(removed.lockId);
            setDirty();
            return true;
        }
        return false;
    }

    public Optional<DoorRef> findByLockId(UUID id) {
        Key k = byId.get(id);
        if (k == null) return Optional.empty();
        return Optional.of(new DoorRef(k.dim, k.pos, locks.get(k)));
    }

    public record DoorRef(ResourceLocation dimension, BlockPos lowerPos, LockInfo info) {}

    private static Key key(Level level, BlockPos lowerDoorPos) {
        return new Key(level.dimension().location(), lowerDoorPos.immutable());
    }
}
