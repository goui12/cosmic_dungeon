package net.goui.cosmicdungeon.door;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class DoorPassageData extends SavedData {
    private static final String STORAGE_NAME = "cosmicdungeon_door_passages";

    // Lifetime counts & optional limits per (dimension,pos)
    final Map<GlobalPos, Integer> counts = new HashMap<>();
    final Map<GlobalPos, Integer> limits = new HashMap<>();

    public DoorPassageData() {}

    /* ================== CODEC ================== */
    // If your mappings include GlobalPos.CODEC, use this:
    private static final Codec<Map<GlobalPos, Integer>> GP_INT_MAP =
            Codec.unboundedMap(GlobalPos.CODEC, Codec.INT);

    // --- If GlobalPos.CODEC does NOT exist, comment the line above and UNCOMMENT this fallback block:
    /*
    private static final Codec<GlobalPos> GLOBAL_POS_CODEC =
        RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("dimension")
                .forGetter(gp -> gp.dimension().location()),
            BlockPos.CODEC.fieldOf("pos")
                .forGetter(GlobalPos::pos)
        ).apply(inst, (dimLoc, pos) ->
            GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimLoc), pos)));

    private static final Codec<Map<GlobalPos, Integer>> GP_INT_MAP =
        Codec.unboundedMap(GLOBAL_POS_CODEC, Codec.INT);
    */

    public static final Codec<DoorPassageData> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    GP_INT_MAP.fieldOf("entries").forGetter(d -> d.counts),
                    GP_INT_MAP.optionalFieldOf("limits", Collections.emptyMap()).forGetter(d -> d.limits)
            ).apply(inst, (countsIn, limitsIn) -> {
                DoorPassageData d = new DoorPassageData();
                d.counts.putAll(countsIn);
                d.limits.putAll(limitsIn);
                return d;
            }));

    /* ============ SavedDataType binding ============ */
    // Your SavedDataType has a constructor (id, Supplier<T>, Codec<T>)
    private static final SavedDataType<DoorPassageData> TYPE =
            new SavedDataType<>(STORAGE_NAME, DoorPassageData::new, CODEC);

    /* ============ Accessor ============ */
    public static DoorPassageData get(Level level) {
        if (!(level instanceof ServerLevel sl)) {
            throw new IllegalStateException("DoorPassageData accessed on client or non-server level");
        }
        // 1-arg form exactly as in your decompile
        return sl.getDataStorage().computeIfAbsent(TYPE);
    }

    /* ============ Helpers ============ */
    private static GlobalPos key(Level level, BlockPos pos) {
        return GlobalPos.of(level.dimension(), pos.immutable());
    }

    /* ============ Mutators / Queries ============ */
    public void increment(Level level, BlockPos lowerDoorPos) {
        counts.merge(key(level, lowerDoorPos), 1, Integer::sum);
        setDirty();
    }

    public int get(Level level, BlockPos lowerDoorPos) {
        return counts.getOrDefault(key(level, lowerDoorPos), 0);
    }

    public void remove(Level level, BlockPos lowerDoorPos) {
        GlobalPos k = key(level, lowerDoorPos);
        boolean dirty = false;
        if (counts.remove(k) != null) dirty = true;
        if (limits.remove(k) != null) dirty = true; // wipe limit when door disappears
        if (dirty) setDirty();
    }

    /** Set a limit; use 0 to store but "disable" behaviorally (command clears instead). */
    public void setLimit(Level level, BlockPos lowerDoorPos, int limit) {
        limits.put(key(level, lowerDoorPos), Math.max(0, limit));
        setDirty();
    }

    public void clearLimit(Level level, BlockPos lowerDoorPos) {
        if (limits.remove(key(level, lowerDoorPos)) != null) {
            setDirty();
        }
    }

    public Integer getLimit(BlockPos lowerDoorPos, Level level) {
        return limits.get(key(level, lowerDoorPos));
    }

    // Convenience when you already have the GlobalPos key’d by base pos
    public Integer getLimit(BlockPos lowerDoorPos) { return null; } // avoid accidental misuse
    public Integer getLimit(GlobalPos gp) { return limits.get(gp); }

    // Simple getter for tracker usage (without level param)
    public Integer getLimit(Level level, BlockPos lowerDoorPos) {
        return limits.get(key(level, lowerDoorPos));
    }
    public void resetCount(Level level, BlockPos lowerDoorPos) {
        if (counts.remove(key(level, lowerDoorPos)) != null) {
            setDirty();
        }
    }

}
