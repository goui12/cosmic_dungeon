package net.goui.cosmicdungeon.redstone.rf;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Global bus manager for RF redstone frequencies.
 * Defers any world mutations (like block updates) to the next server tick
 * to avoid freezing the integrated server during world save/unload.
 */
final class RfBusManager extends SavedData {
    private static final String SAVE_ID = "cosmicdungeon_rf_bus";

    static final SavedDataType<RfBusManager> TYPE =
            new SavedDataType<>(SAVE_ID, RfBusManager::new, Codec.unit(new RfBusManager()));

    private final Int2ObjectOpenHashMap<Long2IntOpenHashMap> transmittersByHz = new Int2ObjectOpenHashMap<>();
    private final Int2IntOpenHashMap maxSignalByHz = new Int2IntOpenHashMap();

    private RfBusManager() {}

    static RfBusManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    int getSignal(int hz) {
        return maxSignalByHz.getOrDefault(hz, 0);
    }

    void addActive(ServerLevel level, BlockPos transmitterPos, int hz, int signal) {
        updateActive(level, transmitterPos, hz, signal);
    }

    void removeActive(ServerLevel level, BlockPos transmitterPos, int hz, int signal) {
        updateActive(level, transmitterPos, hz, 0);
    }

    void updateActive(ServerLevel level, BlockPos transmitterPos, int hz, int signal) {
        Long2IntOpenHashMap txMap = transmittersByHz.computeIfAbsent(hz, k -> new Long2IntOpenHashMap());
        long key = transmitterPos.asLong();
        int clamped = Math.max(0, Math.min(signal, 15));

        if (clamped <= 0) txMap.remove(key);
        else txMap.put(key, clamped);

        if (txMap.isEmpty()) {
            transmittersByHz.remove(hz);
        }

        int previous = maxSignalByHz.getOrDefault(hz, 0);
        int updated = 0;
        if (!txMap.isEmpty()) {
            for (int value : txMap.values()) {
                if (value > updated) updated = value;
            }
        }
        if (updated == 0) maxSignalByHz.remove(hz);
        else maxSignalByHz.put(hz, updated);
        setDirty();

        if (previous != updated && !isServerStopping(level)) {
            level.getServer().execute(() -> {
                if (!isServerStopping(level)) {
                    notifyReceivers(level, hz, updated > 0);
                }
            });
        }
    }

    private static boolean isServerStopping(ServerLevel level) {
        return level.getServer().isStopped();
    }

    private void notifyReceivers(ServerLevel level, int hz, boolean active) {
        ReceiverIndex idx = ReceiverIndex.get(level);
        Set<BlockPos> positions = idx.positionsFor(hz);
        if (positions.isEmpty()) return;
        Set<BlockPos> stale = null;

        for (BlockPos pos : positions) {
            BlockState st = level.getBlockState(pos);
            if (st.getBlock() instanceof RedstoneReceiverBlock block) {
                block.setPowered(level, pos, st, active);
            } else {
                if (stale == null) stale = new HashSet<>();
                stale.add(pos);
            }
        }

        if (stale != null) {
            for (BlockPos pos : stale) {
                idx.remove(hz, pos);
            }
        }
    }

    /* ----------------------------------------------------- */

    static final class ReceiverIndex extends SavedData {
        private static final String SAVE_ID = "cosmicdungeon_rf_receivers";

        static final SavedDataType<ReceiverIndex> TYPE =
                new SavedDataType<>(SAVE_ID, ReceiverIndex::new, Codec.unit(new ReceiverIndex()));

        private final Int2ObjectOpenHashMap<ObjectOpenHashSet<BlockPos>> byHz =
                new Int2ObjectOpenHashMap<>();

        private ReceiverIndex() {}

        static ReceiverIndex get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(TYPE);
        }

        void add(int hz, BlockPos pos) {
            byHz.computeIfAbsent(hz, k -> new ObjectOpenHashSet<>()).add(pos.immutable());
            setDirty();
        }

        void remove(int hz, BlockPos pos) {
            ObjectOpenHashSet<BlockPos> set = byHz.get(hz);
            if (set == null) return;
            set.remove(pos);
            if (set.isEmpty()) byHz.remove(hz);
            setDirty();
        }

        Set<BlockPos> positionsFor(int hz) {
            ObjectOpenHashSet<BlockPos> set = byHz.get(hz);
            return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
        }
    }
}
