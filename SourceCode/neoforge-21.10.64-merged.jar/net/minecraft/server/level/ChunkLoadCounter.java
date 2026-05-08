package net.minecraft.server.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class ChunkLoadCounter {
    private final List<ChunkHolder> pendingChunks = new ArrayList<>();
    private int totalChunks;

    public void track(ServerLevel level, Runnable task) {
        ServerChunkCache serverchunkcache = level.getChunkSource();
        LongSet longset = new LongOpenHashSet();
        serverchunkcache.runDistanceManagerUpdates();
        serverchunkcache.chunkMap.allChunksWithAtLeastStatus(ChunkStatus.FULL).forEach(p_435432_ -> longset.add(p_435432_.getPos().toLong()));
        task.run();
        serverchunkcache.runDistanceManagerUpdates();
        serverchunkcache.chunkMap.allChunksWithAtLeastStatus(ChunkStatus.FULL).forEach(p_433853_ -> {
            if (!longset.contains(p_433853_.getPos().toLong())) {
                this.pendingChunks.add(p_433853_);
                this.totalChunks++;
            }
        });
    }

    public int readyChunks() {
        return this.totalChunks - this.pendingChunks();
    }

    public int pendingChunks() {
        this.pendingChunks.removeIf(p_433610_ -> p_433610_.getLatestStatus() == ChunkStatus.FULL);
        return this.pendingChunks.size();
    }

    public int totalChunks() {
        return this.totalChunks;
    }
}
