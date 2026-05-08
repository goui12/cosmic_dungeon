package net.minecraft.server.level.progress;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public interface LevelLoadListener {
    static LevelLoadListener compose(final LevelLoadListener listener1, final LevelLoadListener listener2) {
        return new LevelLoadListener() {
            @Override
            public void start(LevelLoadListener.Stage p_433873_, int p_435617_) {
                listener1.start(p_433873_, p_435617_);
                listener2.start(p_433873_, p_435617_);
            }

            @Override
            public void update(LevelLoadListener.Stage p_433548_, int p_433120_, int p_433990_) {
                listener1.update(p_433548_, p_433120_, p_433990_);
                listener2.update(p_433548_, p_433120_, p_433990_);
            }

            @Override
            public void finish(LevelLoadListener.Stage p_433286_) {
                listener1.finish(p_433286_);
                listener2.finish(p_433286_);
            }

            @Override
            public void updateFocus(ResourceKey<Level> p_433836_, ChunkPos p_433195_) {
                listener1.updateFocus(p_433836_, p_433195_);
                listener2.updateFocus(p_433836_, p_433195_);
            }
        };
    }

    void start(LevelLoadListener.Stage stage, int totalChunks);

    void update(LevelLoadListener.Stage stage, int readyChunks, int totalChunks);

    void finish(LevelLoadListener.Stage stage);

    void updateFocus(ResourceKey<Level> dimension, ChunkPos chunkPos);

    public static enum Stage {
        START_SERVER,
        PREPARE_GLOBAL_SPAWN,
        LOAD_INITIAL_CHUNKS,
        LOAD_PLAYER_CHUNKS;
    }
}
