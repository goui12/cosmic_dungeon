package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.level.progress.LevelLoadProgressTracker;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class LevelLoadTracker implements LevelLoadListener {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final long CLIENT_WAIT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30L);
    public static final long LEVEL_LOAD_CLOSE_DELAY_MS = 500L;
    private final LevelLoadProgressTracker serverProgressTracker = new LevelLoadProgressTracker(true);
    @Nullable
    private ChunkLoadStatusView serverChunkStatusView;
    @Nullable
    private volatile LevelLoadListener.Stage serverStage;
    @Nullable
    private LevelLoadTracker.ClientState clientState;
    private final long closeDelayMs;

    public LevelLoadTracker() {
        this(0L);
    }

    public LevelLoadTracker(long closeDelayMs) {
        this.closeDelayMs = closeDelayMs;
    }

    public void setServerChunkStatusView(ChunkLoadStatusView serverChunkStatusView) {
        this.serverChunkStatusView = serverChunkStatusView;
    }

    public void startClientLoad(LocalPlayer player, ClientLevel level, LevelRenderer renderer) {
        this.clientState = new LevelLoadTracker.WaitingForServer(player, level, renderer, Util.getMillis() + CLIENT_WAIT_TIMEOUT_MS);
    }

    public void tickClientLoad() {
        if (this.clientState != null) {
            this.clientState = this.clientState.tick();
        }
    }

    public boolean isLevelReady() {
        if (this.clientState instanceof LevelLoadTracker.ClientLevelReady(long j)) {
            long i = j;
            if (Util.getMillis() >= i + this.closeDelayMs) {
                return true;
            }
        }

        return false;
    }

    public void loadingPacketsReceived() {
        if (this.clientState != null) {
            this.clientState = this.clientState.loadingPacketsReceived();
        }
    }

    @Override
    public void start(LevelLoadListener.Stage stage, int totalChunks) {
        this.serverProgressTracker.start(stage, totalChunks);
        this.serverStage = stage;
    }

    @Override
    public void update(LevelLoadListener.Stage stage, int readyChunks, int totalChunks) {
        this.serverProgressTracker.update(stage, readyChunks, totalChunks);
    }

    @Override
    public void finish(LevelLoadListener.Stage stage) {
        this.serverProgressTracker.finish(stage);
    }

    @Override
    public void updateFocus(ResourceKey<Level> dimension, ChunkPos chunkPos) {
        if (this.serverChunkStatusView != null) {
            this.serverChunkStatusView.moveTo(dimension, chunkPos);
        }
    }

    @Nullable
    public ChunkLoadStatusView statusView() {
        return this.serverChunkStatusView;
    }

    public float serverProgress() {
        return this.serverProgressTracker.get();
    }

    public boolean hasProgress() {
        return this.serverStage != null;
    }

    @OnlyIn(Dist.CLIENT)
    record ClientLevelReady(long readyAt) implements LevelLoadTracker.ClientState {
    }

    @OnlyIn(Dist.CLIENT)
    sealed interface ClientState permits LevelLoadTracker.WaitingForServer, LevelLoadTracker.WaitingForPlayerChunk, LevelLoadTracker.ClientLevelReady {
        default LevelLoadTracker.ClientState tick() {
            return this;
        }

        default LevelLoadTracker.ClientState loadingPacketsReceived() {
            return this;
        }
    }

    @OnlyIn(Dist.CLIENT)
    record WaitingForPlayerChunk(LocalPlayer player, ClientLevel level, LevelRenderer levelRenderer, long timeoutAfter) implements LevelLoadTracker.ClientState {
        @Override
        public LevelLoadTracker.ClientState tick() {
            return (LevelLoadTracker.ClientState)(this.isReady() ? new LevelLoadTracker.ClientLevelReady(Util.getMillis()) : this);
        }

        private boolean isReady() {
            if (Util.getMillis() > this.timeoutAfter) {
                LevelLoadTracker.LOGGER.warn("Timed out while waiting for the client to load chunks, letting the player into the world anyway");
                return true;
            } else {
                BlockPos blockpos = this.player.blockPosition();
                return !this.level.isOutsideBuildHeight(blockpos.getY()) && !this.player.isSpectator() && this.player.isAlive()
                    ? this.levelRenderer.isSectionCompiled(blockpos)
                    : true;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    record WaitingForServer(LocalPlayer player, ClientLevel level, LevelRenderer levelRenderer, long timeoutAfter) implements LevelLoadTracker.ClientState {
        @Override
        public LevelLoadTracker.ClientState loadingPacketsReceived() {
            return new LevelLoadTracker.WaitingForPlayerChunk(this.player, this.level, this.levelRenderer, this.timeoutAfter);
        }
    }
}
