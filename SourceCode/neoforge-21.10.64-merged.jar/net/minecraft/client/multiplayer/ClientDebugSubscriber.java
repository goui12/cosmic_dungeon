package net.minecraft.client.multiplayer;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundDebugSubscriptionRequestPacket;
import net.minecraft.util.debug.DebugSubscription;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.util.debugchart.RemoteDebugSampleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientDebugSubscriber {
    private final ClientPacketListener connection;
    private final DebugScreenOverlay debugScreenOverlay;
    private Set<DebugSubscription<?>> remoteSubscriptions = Set.of();
    private final Map<DebugSubscription<?>, ClientDebugSubscriber.ValueMaps<?>> valuesBySubscription = new HashMap<>();

    public ClientDebugSubscriber(ClientPacketListener connection, DebugScreenOverlay debugScreenOverlay) {
        this.debugScreenOverlay = debugScreenOverlay;
        this.connection = connection;
    }

    private static void addFlag(Set<DebugSubscription<?>> subscriptions, DebugSubscription<?> subscription, boolean enabled) {
        if (enabled) {
            subscriptions.add(subscription);
        }
    }

    private Set<DebugSubscription<?>> requestedSubscriptions() {
        Set<DebugSubscription<?>> set = new ReferenceOpenHashSet<>();
        addFlag(set, RemoteDebugSampleType.TICK_TIME.subscription(), this.debugScreenOverlay.showFpsCharts());
        if (SharedConstants.DEBUG_ENABLED) {
            addFlag(set, DebugSubscriptions.BEES, SharedConstants.DEBUG_BEES);
            addFlag(set, DebugSubscriptions.BEE_HIVES, SharedConstants.DEBUG_BEES);
            addFlag(set, DebugSubscriptions.BRAINS, SharedConstants.DEBUG_BRAIN);
            addFlag(set, DebugSubscriptions.BREEZES, SharedConstants.DEBUG_BREEZE_MOB);
            addFlag(set, DebugSubscriptions.ENTITY_BLOCK_INTERSECTIONS, SharedConstants.DEBUG_ENTITY_BLOCK_INTERSECTION);
            addFlag(set, DebugSubscriptions.ENTITY_PATHS, SharedConstants.DEBUG_PATHFINDING);
            addFlag(set, DebugSubscriptions.GAME_EVENTS, SharedConstants.DEBUG_GAME_EVENT_LISTENERS);
            addFlag(set, DebugSubscriptions.GAME_EVENT_LISTENERS, SharedConstants.DEBUG_GAME_EVENT_LISTENERS);
            addFlag(set, DebugSubscriptions.GOAL_SELECTORS, SharedConstants.DEBUG_GOAL_SELECTOR || SharedConstants.DEBUG_BEES);
            addFlag(set, DebugSubscriptions.NEIGHBOR_UPDATES, SharedConstants.DEBUG_NEIGHBORSUPDATE);
            addFlag(set, DebugSubscriptions.POIS, SharedConstants.DEBUG_POI);
            addFlag(set, DebugSubscriptions.RAIDS, SharedConstants.DEBUG_RAIDS);
            addFlag(set, DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS, SharedConstants.DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER);
            addFlag(set, DebugSubscriptions.STRUCTURES, SharedConstants.DEBUG_STRUCTURES);
            addFlag(set, DebugSubscriptions.VILLAGE_SECTIONS, SharedConstants.DEBUG_VILLAGE_SECTIONS);
        }

        return set;
    }

    public void clear() {
        this.remoteSubscriptions = Set.of();
        this.dropLevel();
    }

    public void tick(long gameTime) {
        Set<DebugSubscription<?>> set = this.requestedSubscriptions();
        if (!set.equals(this.remoteSubscriptions)) {
            this.remoteSubscriptions = set;
            this.onSubscriptionsChanged(set);
        }

        this.valuesBySubscription.forEach((p_449415_, p_449542_) -> {
            if (p_449415_.expireAfterTicks() != 0) {
                p_449542_.purgeExpired(gameTime);
            }
        });
    }

    private void onSubscriptionsChanged(Set<DebugSubscription<?>> subscriptions) {
        this.valuesBySubscription.keySet().retainAll(subscriptions);

        for (DebugSubscription<?> debugsubscription : subscriptions) {
            this.valuesBySubscription.computeIfAbsent(debugsubscription, p_449463_ -> new ClientDebugSubscriber.ValueMaps());
        }

        this.connection.send(new ServerboundDebugSubscriptionRequestPacket(subscriptions));
    }

    @Nullable
    <V> ClientDebugSubscriber.ValueMaps<V> getValueMaps(DebugSubscription<V> subscription) {
        return (ClientDebugSubscriber.ValueMaps<V>)this.valuesBySubscription.get(subscription);
    }

    @Nullable
    private <K, V> ClientDebugSubscriber.ValueMap<K, V> getValueMap(DebugSubscription<V> subscription, ClientDebugSubscriber.ValueMapType<K, V> type) {
        ClientDebugSubscriber.ValueMaps<V> valuemaps = this.getValueMaps(subscription);
        return valuemaps != null ? type.get(valuemaps) : null;
    }

    @Nullable
    <K, V> V getValue(DebugSubscription<V> subscription, K key, ClientDebugSubscriber.ValueMapType<K, V> type) {
        ClientDebugSubscriber.ValueMap<K, V> valuemap = this.getValueMap(subscription, type);
        return valuemap != null ? valuemap.getValue(key) : null;
    }

    public DebugValueAccess createDebugValueAccess(final Level level) {
        return new DebugValueAccess() {
            @Override
            public <T> void forEachChunk(DebugSubscription<T> p_449630_, BiConsumer<ChunkPos, T> p_449130_) {
                ClientDebugSubscriber.this.forEachValue(p_449630_, ClientDebugSubscriber.chunks(), p_449130_);
            }

            @Nullable
            @Override
            public <T> T getChunkValue(DebugSubscription<T> p_449512_, ChunkPos p_449631_) {
                return ClientDebugSubscriber.this.getValue(p_449512_, p_449631_, ClientDebugSubscriber.chunks());
            }

            @Override
            public <T> void forEachBlock(DebugSubscription<T> p_449957_, BiConsumer<BlockPos, T> p_449504_) {
                ClientDebugSubscriber.this.forEachValue(p_449957_, ClientDebugSubscriber.blocks(), p_449504_);
            }

            @Nullable
            @Override
            public <T> T getBlockValue(DebugSubscription<T> p_449946_, BlockPos p_449548_) {
                return ClientDebugSubscriber.this.getValue(p_449946_, p_449548_, ClientDebugSubscriber.blocks());
            }

            @Override
            public <T> void forEachEntity(DebugSubscription<T> p_449134_, BiConsumer<Entity, T> p_449817_) {
                ClientDebugSubscriber.this.forEachValue(p_449134_, ClientDebugSubscriber.entities(), (p_449106_, p_449916_) -> {
                    Entity entity = level.getEntity(p_449106_);
                    if (entity != null) {
                        p_449817_.accept(entity, p_449916_);
                    }
                });
            }

            @Nullable
            @Override
            public <T> T getEntityValue(DebugSubscription<T> p_449411_, Entity p_449503_) {
                return ClientDebugSubscriber.this.getValue(p_449411_, p_449503_.getUUID(), ClientDebugSubscriber.entities());
            }

            @Override
            public <T> void forEachEvent(DebugSubscription<T> p_449826_, DebugValueAccess.EventVisitor<T> p_449240_) {
                ClientDebugSubscriber.ValueMaps<T> valuemaps = ClientDebugSubscriber.this.getValueMaps(p_449826_);
                if (valuemaps != null) {
                    long i = level.getGameTime();

                    for (ClientDebugSubscriber.ValueWrapper<T> valuewrapper : valuemaps.events) {
                        int j = (int)(valuewrapper.expiresAfterTime() - i);
                        int k = p_449826_.expireAfterTicks();
                        p_449240_.accept(valuewrapper.value(), j, k);
                    }
                }
            }
        };
    }

    public <T> void updateChunk(long gameTime, ChunkPos chunkPos, DebugSubscription.Update<T> update) {
        this.updateMap(gameTime, chunkPos, update, chunks());
    }

    public <T> void updateBlock(long gameTime, BlockPos pos, DebugSubscription.Update<T> update) {
        this.updateMap(gameTime, pos, update, blocks());
    }

    public <T> void updateEntity(long gameTime, Entity entity, DebugSubscription.Update<T> update) {
        this.updateMap(gameTime, entity.getUUID(), update, entities());
    }

    public <T> void pushEvent(long gameTime, DebugSubscription.Event<T> event) {
        ClientDebugSubscriber.ValueMaps<T> valuemaps = this.getValueMaps(event.subscription());
        if (valuemaps != null) {
            valuemaps.events.add(new ClientDebugSubscriber.ValueWrapper<>(event.value(), gameTime + event.subscription().expireAfterTicks()));
        }
    }

    private <K, V> void updateMap(long gameTime, K key, DebugSubscription.Update<V> update, ClientDebugSubscriber.ValueMapType<K, V> type) {
        ClientDebugSubscriber.ValueMap<K, V> valuemap = this.getValueMap(update.subscription(), type);
        if (valuemap != null) {
            valuemap.apply(gameTime, key, update);
        }
    }

    <K, V> void forEachValue(DebugSubscription<V> subscription, ClientDebugSubscriber.ValueMapType<K, V> type, BiConsumer<K, V> action) {
        ClientDebugSubscriber.ValueMap<K, V> valuemap = this.getValueMap(subscription, type);
        if (valuemap != null) {
            valuemap.forEach(action);
        }
    }

    public void dropLevel() {
        this.valuesBySubscription.clear();
    }

    public void dropChunk(ChunkPos chunkPos) {
        if (!this.valuesBySubscription.isEmpty()) {
            for (ClientDebugSubscriber.ValueMaps<?> valuemaps : this.valuesBySubscription.values()) {
                valuemaps.dropChunkAndBlocks(chunkPos);
            }
        }
    }

    public void dropEntity(Entity entity) {
        if (!this.valuesBySubscription.isEmpty()) {
            for (ClientDebugSubscriber.ValueMaps<?> valuemaps : this.valuesBySubscription.values()) {
                valuemaps.entityValues.removeKey(entity.getUUID());
            }
        }
    }

    static <T> ClientDebugSubscriber.ValueMapType<UUID, T> entities() {
        return p_449714_ -> p_449714_.entityValues;
    }

    static <T> ClientDebugSubscriber.ValueMapType<BlockPos, T> blocks() {
        return p_449060_ -> p_449060_.blockValues;
    }

    static <T> ClientDebugSubscriber.ValueMapType<ChunkPos, T> chunks() {
        return p_449235_ -> p_449235_.chunkValues;
    }

    @OnlyIn(Dist.CLIENT)
    static class ValueMap<K, V> {
        private final Map<K, ClientDebugSubscriber.ValueWrapper<V>> values = new HashMap<>();

        public void removeValues(Predicate<ClientDebugSubscriber.ValueWrapper<V>> predicate) {
            this.values.values().removeIf(predicate);
        }

        public void removeKey(K key) {
            this.values.remove(key);
        }

        public void removeKeys(Predicate<K> predicate) {
            this.values.keySet().removeIf(predicate);
        }

        @Nullable
        public V getValue(K key) {
            ClientDebugSubscriber.ValueWrapper<V> valuewrapper = this.values.get(key);
            return valuewrapper != null ? valuewrapper.value() : null;
        }

        public void apply(long gameTime, K key, DebugSubscription.Update<V> update) {
            if (update.value().isPresent()) {
                this.values
                    .put(key, new ClientDebugSubscriber.ValueWrapper<>(update.value().get(), gameTime + update.subscription().expireAfterTicks()));
            } else {
                this.values.remove(key);
            }
        }

        public void forEach(BiConsumer<K, V> action) {
            this.values.forEach((p_449133_, p_449153_) -> action.accept((K)p_449133_, p_449153_.value()));
        }
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    interface ValueMapType<K, V> {
        ClientDebugSubscriber.ValueMap<K, V> get(ClientDebugSubscriber.ValueMaps<V> maps);
    }

    @OnlyIn(Dist.CLIENT)
    static class ValueMaps<V> {
        final ClientDebugSubscriber.ValueMap<ChunkPos, V> chunkValues = new ClientDebugSubscriber.ValueMap<>();
        final ClientDebugSubscriber.ValueMap<BlockPos, V> blockValues = new ClientDebugSubscriber.ValueMap<>();
        final ClientDebugSubscriber.ValueMap<UUID, V> entityValues = new ClientDebugSubscriber.ValueMap<>();
        final List<ClientDebugSubscriber.ValueWrapper<V>> events = new ArrayList<>();

        public void purgeExpired(long gameTime) {
            Predicate<ClientDebugSubscriber.ValueWrapper<V>> predicate = p_449834_ -> p_449834_.hasExpired(gameTime);
            this.chunkValues.removeValues(predicate);
            this.blockValues.removeValues(predicate);
            this.entityValues.removeValues(predicate);
            this.events.removeIf(predicate);
        }

        public void dropChunkAndBlocks(ChunkPos chunkPos) {
            this.chunkValues.removeKey(chunkPos);
            this.blockValues.removeKeys(chunkPos::contains);
        }
    }

    @OnlyIn(Dist.CLIENT)
    record ValueWrapper<T>(T value, long expiresAfterTime) {
        private static final long NO_EXPIRY = -1L;

        public boolean hasExpired(long gameTime) {
            return this.expiresAfterTime == -1L ? false : gameTime >= this.expiresAfterTime;
        }
    }
}
