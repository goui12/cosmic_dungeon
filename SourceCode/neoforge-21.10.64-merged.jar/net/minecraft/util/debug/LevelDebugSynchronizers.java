package net.minecraft.util.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundDebugBlockValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEntityValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEventPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public class LevelDebugSynchronizers {
    private final ServerLevel level;
    private final List<TrackingDebugSynchronizer<?>> allSynchronizers = new ArrayList<>();
    private final Map<DebugSubscription<?>, TrackingDebugSynchronizer.SourceSynchronizer<?>> sourceSynchronizers = new HashMap<>();
    private final TrackingDebugSynchronizer.PoiSynchronizer poiSynchronizer = new TrackingDebugSynchronizer.PoiSynchronizer();
    private final TrackingDebugSynchronizer.VillageSectionSynchronizer villageSectionSynchronizer = new TrackingDebugSynchronizer.VillageSectionSynchronizer();
    private boolean sleeping = true;
    private Set<DebugSubscription<?>> enabledSubscriptions = Set.of();

    public LevelDebugSynchronizers(ServerLevel level) {
        this.level = level;

        for (DebugSubscription<?> debugsubscription : BuiltInRegistries.DEBUG_SUBSCRIPTION) {
            if (debugsubscription.valueStreamCodec() != null) {
                this.sourceSynchronizers.put(debugsubscription, new TrackingDebugSynchronizer.SourceSynchronizer<>(debugsubscription));
            }
        }

        this.allSynchronizers.addAll(this.sourceSynchronizers.values());
        this.allSynchronizers.add(this.poiSynchronizer);
        this.allSynchronizers.add(this.villageSectionSynchronizer);
    }

    public void tick(ServerDebugSubscribers subscribers) {
        this.enabledSubscriptions = subscribers.enabledSubscriptions();
        boolean flag = this.enabledSubscriptions.isEmpty();
        if (this.sleeping != flag) {
            if (flag) {
                for (TrackingDebugSynchronizer<?> trackingdebugsynchronizer : this.allSynchronizers) {
                    trackingdebugsynchronizer.clear();
                }
            } else {
                this.wakeUp();
            }

            this.sleeping = flag;
        }

        if (!this.sleeping) {
            for (TrackingDebugSynchronizer<?> trackingdebugsynchronizer1 : this.allSynchronizers) {
                trackingdebugsynchronizer1.tick(this.level);
            }
        }
    }

    private void wakeUp() {
        ChunkMap chunkmap = this.level.getChunkSource().chunkMap;
        chunkmap.forEachReadyToSendChunk(this::registerChunk);

        for (Entity entity : this.level.getAllEntities()) {
            if (chunkmap.isTrackedByAnyPlayer(entity)) {
                this.registerEntity(entity);
            }
        }
    }

    <T> TrackingDebugSynchronizer.SourceSynchronizer<T> getSourceSynchronizer(DebugSubscription<T> subscription) {
        return (TrackingDebugSynchronizer.SourceSynchronizer<T>)this.sourceSynchronizers.get(subscription);
    }

    public void registerChunk(final LevelChunk chunk) {
        if (!this.sleeping) {
            chunk.registerDebugValues(this.level, new DebugValueSource.Registration() {
                @Override
                public <T> void register(DebugSubscription<T> p_449505_, DebugValueSource.ValueGetter<T> p_449171_) {
                    LevelDebugSynchronizers.this.getSourceSynchronizer(p_449505_).registerChunk(chunk.getPos(), p_449171_);
                }
            });
            chunk.getBlockEntities().values().forEach(this::registerBlockEntity);
        }
    }

    public void dropChunk(ChunkPos chunkPos) {
        if (!this.sleeping) {
            for (TrackingDebugSynchronizer.SourceSynchronizer<?> sourcesynchronizer : this.sourceSynchronizers.values()) {
                sourcesynchronizer.dropChunk(chunkPos);
            }
        }
    }

    public void registerBlockEntity(final BlockEntity blockEntity) {
        if (!this.sleeping) {
            blockEntity.registerDebugValues(this.level, new DebugValueSource.Registration() {
                @Override
                public <T> void register(DebugSubscription<T> p_449499_, DebugValueSource.ValueGetter<T> p_449473_) {
                    LevelDebugSynchronizers.this.getSourceSynchronizer(p_449499_).registerBlockEntity(blockEntity.getBlockPos(), p_449473_);
                }
            });
        }
    }

    public void dropBlockEntity(BlockPos pos) {
        if (!this.sleeping) {
            for (TrackingDebugSynchronizer.SourceSynchronizer<?> sourcesynchronizer : this.sourceSynchronizers.values()) {
                sourcesynchronizer.dropBlockEntity(this.level, pos);
            }
        }
    }

    public void registerEntity(final Entity entity) {
        if (!this.sleeping) {
            entity.registerDebugValues(this.level, new DebugValueSource.Registration() {
                @Override
                public <T> void register(DebugSubscription<T> p_449475_, DebugValueSource.ValueGetter<T> p_449045_) {
                    LevelDebugSynchronizers.this.getSourceSynchronizer(p_449475_).registerEntity(entity.getUUID(), p_449045_);
                }
            });
        }
    }

    public void dropEntity(Entity entity) {
        if (!this.sleeping) {
            for (TrackingDebugSynchronizer.SourceSynchronizer<?> sourcesynchronizer : this.sourceSynchronizers.values()) {
                sourcesynchronizer.dropEntity(entity);
            }
        }
    }

    public void startTrackingChunk(ServerPlayer player, ChunkPos chunkPos) {
        if (!this.sleeping) {
            for (TrackingDebugSynchronizer<?> trackingdebugsynchronizer : this.allSynchronizers) {
                trackingdebugsynchronizer.startTrackingChunk(player, chunkPos);
            }
        }
    }

    public void startTrackingEntity(ServerPlayer player, Entity entity) {
        if (!this.sleeping) {
            for (TrackingDebugSynchronizer<?> trackingdebugsynchronizer : this.allSynchronizers) {
                trackingdebugsynchronizer.startTrackingEntity(player, entity);
            }
        }
    }

    public void registerPoi(PoiRecord poiRecord) {
        if (!this.sleeping) {
            this.poiSynchronizer.onPoiAdded(this.level, poiRecord);
            this.villageSectionSynchronizer.onPoiAdded(this.level, poiRecord);
        }
    }

    public void updatePoi(BlockPos pos) {
        if (!this.sleeping) {
            this.poiSynchronizer.onPoiTicketCountChanged(this.level, pos);
        }
    }

    public void dropPoi(BlockPos pos) {
        if (!this.sleeping) {
            this.poiSynchronizer.onPoiRemoved(this.level, pos);
            this.villageSectionSynchronizer.onPoiRemoved(this.level, pos);
        }
    }

    public boolean hasAnySubscriberFor(DebugSubscription<?> subscription) {
        return this.enabledSubscriptions.contains(subscription);
    }

    public <T> void sendBlockValue(BlockPos pos, DebugSubscription<T> subscription, T value) {
        if (this.hasAnySubscriberFor(subscription)) {
            this.broadcastToTracking(new ChunkPos(pos), subscription, new ClientboundDebugBlockValuePacket(pos, subscription.packUpdate(value)));
        }
    }

    public <T> void clearBlockValue(BlockPos pos, DebugSubscription<T> subscription) {
        if (this.hasAnySubscriberFor(subscription)) {
            this.broadcastToTracking(new ChunkPos(pos), subscription, new ClientboundDebugBlockValuePacket(pos, subscription.emptyUpdate()));
        }
    }

    public <T> void sendEntityValue(Entity entity, DebugSubscription<T> subscription, T value) {
        if (this.hasAnySubscriberFor(subscription)) {
            this.broadcastToTracking(entity, subscription, new ClientboundDebugEntityValuePacket(entity.getId(), subscription.packUpdate(value)));
        }
    }

    public <T> void clearEntityValue(Entity entity, DebugSubscription<T> subscription) {
        if (this.hasAnySubscriberFor(subscription)) {
            this.broadcastToTracking(entity, subscription, new ClientboundDebugEntityValuePacket(entity.getId(), subscription.emptyUpdate()));
        }
    }

    public <T> void broadcastEventToTracking(BlockPos pos, DebugSubscription<T> subscription, T value) {
        if (this.hasAnySubscriberFor(subscription)) {
            this.broadcastToTracking(new ChunkPos(pos), subscription, new ClientboundDebugEventPacket(subscription.packEvent(value)));
        }
    }

    private void broadcastToTracking(ChunkPos chunkPos, DebugSubscription<?> subscription, Packet<? super ClientGamePacketListener> packet) {
        ChunkMap chunkmap = this.level.getChunkSource().chunkMap;

        for (ServerPlayer serverplayer : chunkmap.getPlayers(chunkPos, false)) {
            if (serverplayer.debugSubscriptions().contains(subscription)) {
                serverplayer.connection.send(packet);
            }
        }
    }

    private void broadcastToTracking(Entity entity, DebugSubscription<?> subscription, Packet<? super ClientGamePacketListener> packet) {
        ChunkMap chunkmap = this.level.getChunkSource().chunkMap;
        chunkmap.sendToTrackingPlayersFiltered(entity, packet, p_449879_ -> p_449879_.debugSubscriptions().contains(subscription));
    }
}
