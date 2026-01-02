package net.goui.cosmicdungeon.redstone.rf;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Collections;
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

    private final Int2IntOpenHashMap activeCounts = new Int2IntOpenHashMap();

    private RfBusManager() {}

    static RfBusManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    boolean isActive(int hz) {
        return activeCounts.getOrDefault(hz, 0) > 0;
    }

    void addActive(ServerLevel level, int hz) {
        int c = activeCounts.getOrDefault(hz, 0) + 1;
        activeCounts.put(hz, c);
        setDirty();

        // Only activate receivers when the first transmitter appears
        if (c == 1 && !isServerStopping(level)) {
            level.getServer().execute(() -> {
                if (!isServerStopping(level)) {
                    notifyReceivers(level, hz, true);
                }
            });
        }
    }

    void removeActive(ServerLevel level, int hz) {
        int old = activeCounts.getOrDefault(hz, 0);
        if (old <= 0) return;

        int c = old - 1;
        if (c == 0) activeCounts.remove(hz);
        else activeCounts.put(hz, c);
        setDirty();

        // Only deactivate receivers when the last transmitter disappears
        if (c == 0 && !isServerStopping(level)) {
            level.getServer().execute(() -> {
                if (!isServerStopping(level)) {
                    notifyReceivers(level, hz, false);
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

        for (BlockPos pos : positions) {
            BlockState st = level.getBlockState(pos);
            if (st.getBlock() instanceof RedstoneReceiverBlock block) {
                block.setPowered(level, pos, st, active);
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
