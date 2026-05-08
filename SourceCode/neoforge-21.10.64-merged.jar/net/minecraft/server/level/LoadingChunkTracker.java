package net.minecraft.server.level;

import net.minecraft.world.level.TicketStorage;

class LoadingChunkTracker extends ChunkTracker {
    private static final int MAX_LEVEL = ChunkLevel.MAX_LEVEL + 1;
    private final DistanceManager distanceManager;
    private final TicketStorage ticketStorage;

    public LoadingChunkTracker(DistanceManager distanceManager, TicketStorage ticketStorage) {
        super(MAX_LEVEL + 1, 16, 256);
        this.distanceManager = distanceManager;
        this.ticketStorage = ticketStorage;
        ticketStorage.setLoadingChunkUpdatedListener(this::update);
    }

    @Override
    protected int getLevelFromSource(long pos) {
        return this.ticketStorage.getTicketLevelAt(pos, false);
    }

    @Override
    protected int getLevel(long chunkPos) {
        if (!this.distanceManager.isChunkToRemove(chunkPos)) {
            ChunkHolder chunkholder = this.distanceManager.getChunk(chunkPos);
            if (chunkholder != null) {
                return chunkholder.getTicketLevel();
            }
        }

        return MAX_LEVEL;
    }

    @Override
    protected void setLevel(long chunkPos, int level) {
        ChunkHolder chunkholder = this.distanceManager.getChunk(chunkPos);
        int i = chunkholder == null ? MAX_LEVEL : chunkholder.getTicketLevel();
        if (i != level) {
            chunkholder = this.distanceManager.updateChunkScheduling(chunkPos, level, chunkholder, i);
            if (chunkholder != null) {
                this.distanceManager.chunksToUpdateFutures.add(chunkholder);
            }
        }
    }

    public int runDistanceUpdates(int toUpdateCount) {
        return this.runUpdates(toUpdateCount);
    }
}
