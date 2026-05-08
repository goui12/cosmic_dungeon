package net.minecraft.util.debug;

import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

public interface DebugValueAccess {
    <T> void forEachChunk(DebugSubscription<T> subscription, BiConsumer<ChunkPos, T> action);

    @Nullable
    <T> T getChunkValue(DebugSubscription<T> subscription, ChunkPos chunkPos);

    <T> void forEachBlock(DebugSubscription<T> subscription, BiConsumer<BlockPos, T> action);

    @Nullable
    <T> T getBlockValue(DebugSubscription<T> subscription, BlockPos pos);

    <T> void forEachEntity(DebugSubscription<T> subscription, BiConsumer<Entity, T> action);

    @Nullable
    <T> T getEntityValue(DebugSubscription<T> subscription, Entity entity);

    <T> void forEachEvent(DebugSubscription<T> subscription, DebugValueAccess.EventVisitor<T> action);

    @FunctionalInterface
    public interface EventVisitor<T> {
        void accept(T value, int expires, int subscriptionExpires);
    }
}
