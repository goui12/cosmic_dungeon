package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;

public abstract class ChunkSource implements LightChunkGetter, AutoCloseable {
    @Nullable
    public LevelChunk getChunk(int chunkX, int chunkZ, boolean load) {
        return (LevelChunk)this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, load);
    }

    @Nullable
    public LevelChunk getChunkNow(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, false);
    }

    @Nullable
    @Override
    public LightChunk getChunkForLighting(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
    }

    /**
     * @return {@code true} if a chunk is loaded at the provided position, without forcing a chunk load.
     */
    public boolean hasChunk(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
    }

    @Nullable
    public abstract ChunkAccess getChunk(int x, int z, ChunkStatus chunkStatus, boolean requireChunk);

    public abstract void tick(BooleanSupplier hasTimeLeft, boolean tickChunks);

    public void onSectionEmptinessChanged(int x, int y, int z, boolean isEmpty) {
    }

    public abstract String gatherStats();

    public abstract int getLoadedChunksCount();

    @Override
    public void close() throws IOException {
    }

    public abstract LevelLightEngine getLightEngine();

    public void setSpawnSettings(boolean spawnSettings) {
    }

    public boolean updateChunkForced(ChunkPos chunkPos, boolean add) {
        return false;
    }

    public LongSet getForceLoadedChunks() {
        return LongSet.of();
    }
}
