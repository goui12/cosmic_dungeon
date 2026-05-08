package net.minecraft.util.debug;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundDebugBlockValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugChunkValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEntityValuePacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;

public abstract class TrackingDebugSynchronizer<T> {
    protected final DebugSubscription<T> subscription;
    private final Set<UUID> subscribedPlayers = new ObjectOpenHashSet<>();

    public TrackingDebugSynchronizer(DebugSubscription<T> subscription) {
        this.subscription = subscription;
    }

    public final void tick(ServerLevel level) {
        for (ServerPlayer serverplayer : level.players()) {
            boolean flag = this.subscribedPlayers.contains(serverplayer.getUUID());
            boolean flag1 = serverplayer.debugSubscriptions().contains(this.subscription);
            if (flag1 != flag) {
                if (flag1) {
                    this.addSubscriber(serverplayer);
                } else {
                    this.subscribedPlayers.remove(serverplayer.getUUID());
                }
            }
        }

        this.subscribedPlayers.removeIf(p_449288_ -> level.getPlayerByUUID(p_449288_) == null);
        if (!this.subscribedPlayers.isEmpty()) {
            this.pollAndSendUpdates(level);
        }
    }

    private void addSubscriber(ServerPlayer player) {
        this.subscribedPlayers.add(player.getUUID());
        player.getChunkTrackingView().forEach(p_449536_ -> {
            if (!player.connection.chunkSender.isPending(p_449536_.toLong())) {
                this.startTrackingChunk(player, p_449536_);
            }
        });
        player.level().getChunkSource().chunkMap.forEachEntityTrackedBy(player, p_449691_ -> this.startTrackingEntity(player, p_449691_));
    }

    protected final void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, Packet<? super ClientGamePacketListener> packet) {
        ChunkMap chunkmap = level.getChunkSource().chunkMap;

        for (UUID uuid : this.subscribedPlayers) {
            if (level.getPlayerByUUID(uuid) instanceof ServerPlayer serverplayer && chunkmap.isChunkTracked(serverplayer, chunkPos.x, chunkPos.z)) {
                serverplayer.connection.send(packet);
            }
        }
    }

    protected final void sendToPlayersTrackingEntity(ServerLevel level, Entity entity, Packet<? super ClientGamePacketListener> packet) {
        ChunkMap chunkmap = level.getChunkSource().chunkMap;
        chunkmap.sendToTrackingPlayersFiltered(entity, packet, p_449476_ -> this.subscribedPlayers.contains(p_449476_.getUUID()));
    }

    public final void startTrackingChunk(ServerPlayer player, ChunkPos chunkPos) {
        if (this.subscribedPlayers.contains(player.getUUID())) {
            this.sendInitialChunk(player, chunkPos);
        }
    }

    public final void startTrackingEntity(ServerPlayer player, Entity entity) {
        if (this.subscribedPlayers.contains(player.getUUID())) {
            this.sendInitialEntity(player, entity);
        }
    }

    protected void clear() {
    }

    protected void pollAndSendUpdates(ServerLevel level) {
    }

    protected void sendInitialChunk(ServerPlayer player, ChunkPos chunkPos) {
    }

    protected void sendInitialEntity(ServerPlayer player, Entity entity) {
    }

    public static class PoiSynchronizer extends TrackingDebugSynchronizer<DebugPoiInfo> {
        public PoiSynchronizer() {
            super(DebugSubscriptions.POIS);
        }

        @Override
        protected void sendInitialChunk(ServerPlayer player, ChunkPos chunkPos) {
            ServerLevel serverlevel = player.level();
            PoiManager poimanager = serverlevel.getPoiManager();
            poimanager.getInChunk(p_449938_ -> true, chunkPos, PoiManager.Occupancy.ANY)
                .forEach(
                    p_449585_ -> player.connection
                        .send(new ClientboundDebugBlockValuePacket(p_449585_.getPos(), this.subscription.packUpdate(new DebugPoiInfo(p_449585_))))
                );
        }

        public void onPoiAdded(ServerLevel level, PoiRecord poi) {
            this.sendToPlayersTrackingChunk(
                level,
                new ChunkPos(poi.getPos()),
                new ClientboundDebugBlockValuePacket(poi.getPos(), this.subscription.packUpdate(new DebugPoiInfo(poi)))
            );
        }

        public void onPoiRemoved(ServerLevel level, BlockPos pos) {
            this.sendToPlayersTrackingChunk(
                level, new ChunkPos(pos), new ClientboundDebugBlockValuePacket(pos, this.subscription.emptyUpdate())
            );
        }

        public void onPoiTicketCountChanged(ServerLevel level, BlockPos pos) {
            this.sendToPlayersTrackingChunk(
                level,
                new ChunkPos(pos),
                new ClientboundDebugBlockValuePacket(pos, this.subscription.packUpdate(level.getPoiManager().getDebugPoiInfo(pos)))
            );
        }
    }

    public static class SourceSynchronizer<T> extends TrackingDebugSynchronizer<T> {
        private final Map<ChunkPos, TrackingDebugSynchronizer.ValueSource<T>> chunkSources = new HashMap<>();
        private final Map<BlockPos, TrackingDebugSynchronizer.ValueSource<T>> blockEntitySources = new HashMap<>();
        private final Map<UUID, TrackingDebugSynchronizer.ValueSource<T>> entitySources = new HashMap<>();

        public SourceSynchronizer(DebugSubscription<T> subscription) {
            super(subscription);
        }

        @Override
        protected void clear() {
            this.chunkSources.clear();
            this.blockEntitySources.clear();
            this.entitySources.clear();
        }

        @Override
        protected void pollAndSendUpdates(ServerLevel level) {
            for (Entry<ChunkPos, TrackingDebugSynchronizer.ValueSource<T>> entry : this.chunkSources.entrySet()) {
                DebugSubscription.Update<T> update = entry.getValue().pollUpdate(this.subscription);
                if (update != null) {
                    ChunkPos chunkpos = entry.getKey();
                    this.sendToPlayersTrackingChunk(level, chunkpos, new ClientboundDebugChunkValuePacket(chunkpos, update));
                }
            }

            for (Entry<BlockPos, TrackingDebugSynchronizer.ValueSource<T>> entry1 : this.blockEntitySources.entrySet()) {
                DebugSubscription.Update<T> update1 = entry1.getValue().pollUpdate(this.subscription);
                if (update1 != null) {
                    BlockPos blockpos = entry1.getKey();
                    ChunkPos chunkpos1 = new ChunkPos(blockpos);
                    this.sendToPlayersTrackingChunk(level, chunkpos1, new ClientboundDebugBlockValuePacket(blockpos, update1));
                }
            }

            for (Entry<UUID, TrackingDebugSynchronizer.ValueSource<T>> entry2 : this.entitySources.entrySet()) {
                DebugSubscription.Update<T> update2 = entry2.getValue().pollUpdate(this.subscription);
                if (update2 != null) {
                    Entity entity = Objects.requireNonNull(level.getEntity(entry2.getKey()));
                    this.sendToPlayersTrackingEntity(level, entity, new ClientboundDebugEntityValuePacket(entity.getId(), update2));
                }
            }
        }

        public void registerChunk(ChunkPos chunkPos, DebugValueSource.ValueGetter<T> getter) {
            this.chunkSources.put(chunkPos, new TrackingDebugSynchronizer.ValueSource<>(getter));
        }

        public void registerBlockEntity(BlockPos pos, DebugValueSource.ValueGetter<T> getter) {
            this.blockEntitySources.put(pos, new TrackingDebugSynchronizer.ValueSource<>(getter));
        }

        public void registerEntity(UUID uuid, DebugValueSource.ValueGetter<T> getter) {
            this.entitySources.put(uuid, new TrackingDebugSynchronizer.ValueSource<>(getter));
        }

        public void dropChunk(ChunkPos chunkPos) {
            this.chunkSources.remove(chunkPos);
            this.blockEntitySources.keySet().removeIf(chunkPos::contains);
        }

        public void dropBlockEntity(ServerLevel level, BlockPos pos) {
            TrackingDebugSynchronizer.ValueSource<T> valuesource = this.blockEntitySources.remove(pos);
            if (valuesource != null) {
                ChunkPos chunkpos = new ChunkPos(pos);
                this.sendToPlayersTrackingChunk(level, chunkpos, new ClientboundDebugBlockValuePacket(pos, this.subscription.emptyUpdate()));
            }
        }

        public void dropEntity(Entity entity) {
            this.entitySources.remove(entity.getUUID());
        }

        @Override
        protected void sendInitialChunk(ServerPlayer player, ChunkPos chunkPos) {
            TrackingDebugSynchronizer.ValueSource<T> valuesource = this.chunkSources.get(chunkPos);
            if (valuesource != null && valuesource.lastSyncedValue != null) {
                player.connection.send(new ClientboundDebugChunkValuePacket(chunkPos, this.subscription.packUpdate(valuesource.lastSyncedValue)));
            }

            for (Entry<BlockPos, TrackingDebugSynchronizer.ValueSource<T>> entry : this.blockEntitySources.entrySet()) {
                T t = entry.getValue().lastSyncedValue;
                if (t != null) {
                    BlockPos blockpos = entry.getKey();
                    if (chunkPos.contains(blockpos)) {
                        player.connection.send(new ClientboundDebugBlockValuePacket(blockpos, this.subscription.packUpdate(t)));
                    }
                }
            }
        }

        @Override
        protected void sendInitialEntity(ServerPlayer player, Entity entity) {
            TrackingDebugSynchronizer.ValueSource<T> valuesource = this.entitySources.get(entity.getUUID());
            if (valuesource != null && valuesource.lastSyncedValue != null) {
                player.connection.send(new ClientboundDebugEntityValuePacket(entity.getId(), this.subscription.packUpdate(valuesource.lastSyncedValue)));
            }
        }
    }

    static class ValueSource<T> {
        private final DebugValueSource.ValueGetter<T> getter;
        @Nullable
        T lastSyncedValue;

        ValueSource(DebugValueSource.ValueGetter<T> getter) {
            this.getter = getter;
        }

        @Nullable
        public DebugSubscription.Update<T> pollUpdate(DebugSubscription<T> subscription) {
            T t = this.getter.get();
            if (!Objects.equals(t, this.lastSyncedValue)) {
                this.lastSyncedValue = t;
                return subscription.packUpdate(t);
            } else {
                return null;
            }
        }
    }

    public static class VillageSectionSynchronizer extends TrackingDebugSynchronizer<Unit> {
        public VillageSectionSynchronizer() {
            super(DebugSubscriptions.VILLAGE_SECTIONS);
        }

        @Override
        protected void sendInitialChunk(ServerPlayer player, ChunkPos chunkPos) {
            ServerLevel serverlevel = player.level();
            PoiManager poimanager = serverlevel.getPoiManager();
            poimanager.getInChunk(p_449632_ -> true, chunkPos, PoiManager.Occupancy.ANY).forEach(p_449398_ -> {
                SectionPos sectionpos = SectionPos.of(p_449398_.getPos());
                forEachVillageSectionUpdate(serverlevel, sectionpos, (p_449508_, p_449216_) -> {
                    BlockPos blockpos = p_449508_.center();
                    player.connection.send(new ClientboundDebugBlockValuePacket(blockpos, this.subscription.packUpdate(p_449216_ ? Unit.INSTANCE : null)));
                });
            });
        }

        public void onPoiAdded(ServerLevel level, PoiRecord po) {
            this.sendVillageSectionsPacket(level, po.getPos());
        }

        public void onPoiRemoved(ServerLevel level, BlockPos pos) {
            this.sendVillageSectionsPacket(level, pos);
        }

        private void sendVillageSectionsPacket(ServerLevel level, BlockPos pos) {
            forEachVillageSectionUpdate(
                level,
                SectionPos.of(pos),
                (p_449805_, p_449681_) -> {
                    BlockPos blockpos = p_449805_.center();
                    if (p_449681_) {
                        this.sendToPlayersTrackingChunk(
                            level, new ChunkPos(blockpos), new ClientboundDebugBlockValuePacket(blockpos, this.subscription.packUpdate(Unit.INSTANCE))
                        );
                    } else {
                        this.sendToPlayersTrackingChunk(
                            level, new ChunkPos(blockpos), new ClientboundDebugBlockValuePacket(blockpos, this.subscription.emptyUpdate())
                        );
                    }
                }
            );
        }

        private static void forEachVillageSectionUpdate(ServerLevel level, SectionPos sectionPos, BiConsumer<SectionPos, Boolean> action) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    for (int k = -1; k <= 1; k++) {
                        SectionPos sectionpos = sectionPos.offset(j, k, i);
                        if (level.isVillage(sectionpos.center())) {
                            action.accept(sectionpos, true);
                        } else {
                            action.accept(sectionpos, false);
                        }
                    }
                }
            }
        }
    }
}
