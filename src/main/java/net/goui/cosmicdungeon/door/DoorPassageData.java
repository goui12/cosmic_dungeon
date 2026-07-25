package net.goui.cosmicdungeon.door;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.goui.cosmicdungeon.dungeon.DungeonInstanceSlots;

import java.util.*;

/**
 * v2: encodes as { entries: [ {dimension:"...", x:.., y:.., z:.., count:.., limit?:..}, ... ] }
 * No maps, no non-string keys — safe for NBT + DFU.
 */
public class DoorPassageData extends SavedData {
    private static final String STORAGE_NAME = "cosmicdungeon_door_passages_v2";

    private static final class Key {
        final ResourceLocation dim;
        final BlockPos pos;
        Key(ResourceLocation dim, BlockPos pos) { this.dim = dim; this.pos = pos.immutable(); }
        @Override public boolean equals(Object o){ if(this==o) return true; if(!(o instanceof Key k)) return false; return dim.equals(k.dim) && pos.equals(k.pos); }
        @Override public int hashCode(){ return Objects.hash(dim, pos); }
    }

    private static final class Entry {
        final ResourceLocation dim;
        final int x, y, z;
        final int count;
        final Integer limit; // nullable

        Entry(ResourceLocation dim, int x, int y, int z, int count, Integer limit) {
            this.dim = dim; this.x = x; this.y = y; this.z = z; this.count = count; this.limit = limit;
        }
        BlockPos pos() { return new BlockPos(x, y, z); }
    }

    private final Map<Key, Integer> counts = new HashMap<>();
    private final Map<Key, Integer> limits = new HashMap<>();

    public DoorPassageData() {}

    /* ---------- CODEC (flat primitives only) ---------- */

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(e -> e.dim),
            Codec.INT.fieldOf("x").forGetter(e -> e.x),
            Codec.INT.fieldOf("y").forGetter(e -> e.y),
            Codec.INT.fieldOf("z").forGetter(e -> e.z),
            Codec.INT.fieldOf("count").forGetter(e -> e.count),
            Codec.INT.optionalFieldOf("limit").forGetter(e -> Optional.ofNullable(e.limit))
    ).apply(i, (dim, x, y, z, count, optLimit) -> new Entry(dim, x, y, z, count, optLimit.orElse(null))));

    public static final Codec<DoorPassageData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ENTRY_CODEC.listOf().optionalFieldOf("entries", List.of())
                    .forGetter(DoorPassageData::entriesView)
    ).apply(i, DoorPassageData::fromEntries));

    private List<Entry> entriesView() {
        Set<Key> keys = new HashSet<>(counts.keySet());
        keys.addAll(limits.keySet());
        List<Entry> out = new ArrayList<>(keys.size());
        for (Key k : keys) {
            int c = counts.getOrDefault(k, 0);
            Integer lim = limits.get(k);
            out.add(new Entry(k.dim, k.pos.getX(), k.pos.getY(), k.pos.getZ(), c, lim));
        }
        return out;
    }

    private static DoorPassageData fromEntries(List<Entry> list) {
        DoorPassageData d = new DoorPassageData();
        for (Entry e : list) {
            Key k = new Key(e.dim, e.pos());
            d.counts.put(k, e.count);
            if (e.limit != null) d.limits.put(k, e.limit);
        }
        return d;
    }

    /* ---------- SavedDataType binding / accessor ---------- */

    private static final SavedDataType<DoorPassageData> TYPE =
            new SavedDataType<>(STORAGE_NAME, DoorPassageData::new, CODEC);

    public static DoorPassageData get(Level level) {
        if (!(level instanceof ServerLevel sl)) {
            throw new IllegalStateException("DoorPassageData accessed on client or non-server level");
        }
        return sl.getDataStorage().computeIfAbsent(TYPE);
    }

    /* ---------- API ---------- */

    private static Key key(Level level, BlockPos lowerDoorPos) {
        ResourceLocation dimension = level.dimension().location();
        if (level instanceof ServerLevel serverLevel) {
            dimension = DungeonInstanceSlots.templateDimensionForPhysical(serverLevel.getServer(), serverLevel.dimension()).location();
        }
        return new Key(dimension, lowerDoorPos);
    }

    public void increment(Level level, BlockPos lowerDoorPos) {
        Key k = key(level, lowerDoorPos);
        counts.merge(k, 1, Integer::sum);
        setDirty();
    }

    public int get(Level level, BlockPos lowerDoorPos) {
        return counts.getOrDefault(key(level, lowerDoorPos), 0);
    }

    public void resetCount(Level level, BlockPos lowerDoorPos) {
        if (counts.remove(key(level, lowerDoorPos)) != null) setDirty();
    }

    public void remove(Level level, BlockPos lowerDoorPos) {
        Key k = key(level, lowerDoorPos);
        boolean ch = false;
        if (counts.remove(k) != null) ch = true;
        if (limits.remove(k) != null) ch = true;
        if (ch) setDirty();
    }

    public void setLimit(Level level, BlockPos lowerDoorPos, int limit) {
        limits.put(key(level, lowerDoorPos), Math.max(0, limit));
        setDirty();
    }

    public Integer getLimit(Level level, BlockPos lowerDoorPos) {
        return limits.get(key(level, lowerDoorPos));
    }

    public void clearLimit(Level level, BlockPos lowerDoorPos) {
        if (limits.remove(key(level, lowerDoorPos)) != null) setDirty();
    }
}
