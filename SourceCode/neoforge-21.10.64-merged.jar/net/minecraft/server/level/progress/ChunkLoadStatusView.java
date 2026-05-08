package net.minecraft.server.level.progress;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public interface ChunkLoadStatusView {
    void moveTo(ResourceKey<Level> dimension, ChunkPos chunkPos);

    @Nullable
    ChunkStatus get(int x, int z);

    int radius();
}
