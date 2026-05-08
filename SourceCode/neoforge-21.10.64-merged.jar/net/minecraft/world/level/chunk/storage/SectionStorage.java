package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import org.slf4j.Logger;

public class SectionStorage<R, P> implements AutoCloseable {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final String SECTIONS_TAG = "Sections";
    private final SimpleRegionStorage simpleRegionStorage;
    private final Long2ObjectMap<Optional<R>> storage = new Long2ObjectOpenHashMap<>();
    private final LongLinkedOpenHashSet dirtyChunks = new LongLinkedOpenHashSet();
    private final Codec<P> codec;
    private final Function<R, P> packer;
    private final BiFunction<P, Runnable, R> unpacker;
    private final Function<Runnable, R> factory;
    private final RegistryAccess registryAccess;
    private final ChunkIOErrorReporter errorReporter;
    protected final LevelHeightAccessor levelHeightAccessor;
    private final LongSet loadedChunks = new LongOpenHashSet();
    private final Long2ObjectMap<CompletableFuture<Optional<SectionStorage.PackedChunk<P>>>> pendingLoads = new Long2ObjectOpenHashMap<>();
    private final Object loadLock = new Object();

    public SectionStorage(
        SimpleRegionStorage simpleRegionStorage,
        Codec<P> codec,
        Function<R, P> packer,
        BiFunction<P, Runnable, R> unpacker,
        Function<Runnable, R> factory,
        RegistryAccess registryAccess,
        ChunkIOErrorReporter errorReporter,
        LevelHeightAccessor levelHeightAccessor
    ) {
        this.simpleRegionStorage = simpleRegionStorage;
        this.codec = codec;
        this.packer = packer;
        this.unpacker = unpacker;
        this.factory = factory;
        this.registryAccess = registryAccess;
        this.errorReporter = errorReporter;
        this.levelHeightAccessor = levelHeightAccessor;
    }

    protected void tick(BooleanSupplier aheadOfTime) {
        LongIterator longiterator = this.dirtyChunks.iterator();

        while (longiterator.hasNext() && aheadOfTime.getAsBoolean()) {
            ChunkPos chunkpos = new ChunkPos(longiterator.nextLong());
            longiterator.remove();
            this.writeChunk(chunkpos);
        }

        this.unpackPendingLoads();
    }

    private void unpackPendingLoads() {
        synchronized (this.loadLock) {
            Iterator<Entry<CompletableFuture<Optional<SectionStorage.PackedChunk<P>>>>> iterator = Long2ObjectMaps.fastIterator(this.pendingLoads);

            while (iterator.hasNext()) {
                Entry<CompletableFuture<Optional<SectionStorage.PackedChunk<P>>>> entry = iterator.next();
                Optional<SectionStorage.PackedChunk<P>> optional = entry.getValue().getNow(null);
                if (optional != null) {
                    long i = entry.getLongKey();
                    this.unpackChunk(new ChunkPos(i), optional.orElse(null));
                    iterator.remove();
                    this.loadedChunks.add(i);
                }
            }
        }
    }

    public void flushAll() {
        if (!this.dirtyChunks.isEmpty()) {
            this.dirtyChunks.forEach(p_360211_ -> this.writeChunk(new ChunkPos(p_360211_)));
            this.dirtyChunks.clear();
        }
    }

    public boolean hasWork() {
        return !this.dirtyChunks.isEmpty();
    }

    @Nullable
    protected Optional<R> get(long sectionKey) {
        return this.storage.get(sectionKey);
    }

    protected Optional<R> getOrLoad(long sectionKey) {
        if (this.outsideStoredRange(sectionKey)) {
            return Optional.empty();
        } else {
            Optional<R> optional = this.get(sectionKey);
            if (optional != null) {
                return optional;
            } else {
                this.unpackChunk(SectionPos.of(sectionKey).chunk());
                optional = this.get(sectionKey);
                if (optional == null) {
                    throw (IllegalStateException)Util.pauseInIde(new IllegalStateException());
                } else {
                    return optional;
                }
            }
        }
    }

    protected boolean outsideStoredRange(long sectionKey) {
        int i = SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey));
        return this.levelHeightAccessor.isOutsideBuildHeight(i);
    }

    protected R getOrCreate(long sectionKey) {
        if (this.outsideStoredRange(sectionKey)) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("sectionPos out of bounds"));
        } else {
            Optional<R> optional = this.getOrLoad(sectionKey);
            if (optional.isPresent()) {
                return optional.get();
            } else {
                R r = this.factory.apply(() -> this.setDirty(sectionKey));
                this.storage.put(sectionKey, Optional.of(r));
                return r;
            }
        }
    }

    public CompletableFuture<?> prefetch(ChunkPos pos) {
        synchronized (this.loadLock) {
            long i = pos.toLong();
            return this.loadedChunks.contains(i)
                ? CompletableFuture.completedFuture(null)
                : this.pendingLoads.computeIfAbsent(i, p_360206_ -> this.tryRead(pos));
        }
    }

    private void unpackChunk(ChunkPos pos) {
        long i = pos.toLong();
        CompletableFuture<Optional<SectionStorage.PackedChunk<P>>> completablefuture;
        synchronized (this.loadLock) {
            if (!this.loadedChunks.add(i)) {
                return;
            }

            completablefuture = this.pendingLoads.computeIfAbsent(i, p_360213_ -> this.tryRead(pos));
        }

        this.unpackChunk(pos, completablefuture.join().orElse(null));
        synchronized (this.loadLock) {
            this.pendingLoads.remove(i);
        }
    }

    private CompletableFuture<Optional<SectionStorage.PackedChunk<P>>> tryRead(ChunkPos chunkPos) {
        RegistryOps<Tag> registryops = this.registryAccess.createSerializationContext(NbtOps.INSTANCE);
        return this.simpleRegionStorage
            .read(chunkPos)
            .thenApplyAsync(
                p_360208_ -> p_360208_.map(
                    p_360215_ -> SectionStorage.PackedChunk.parse(this.codec, registryops, p_360215_, this.simpleRegionStorage, this.levelHeightAccessor)
                ),
                Util.backgroundExecutor().forName("parseSection")
            )
            .exceptionally(p_382775_ -> {
                if (p_382775_ instanceof CompletionException) {
                    p_382775_ = p_382775_.getCause();
                }

                if (p_382775_ instanceof IOException ioexception) {
                    LOGGER.error("Error reading chunk {} data from disk", chunkPos, ioexception);
                    this.errorReporter.reportChunkLoadFailure(ioexception, this.simpleRegionStorage.storageInfo(), chunkPos);
                    return Optional.empty();
                } else {
                    throw new CompletionException(p_382775_);
                }
            });
    }

    private void unpackChunk(ChunkPos pos, @Nullable SectionStorage.PackedChunk<P> packedChunk) {
        if (packedChunk == null) {
            for (int i = this.levelHeightAccessor.getMinSectionY(); i <= this.levelHeightAccessor.getMaxSectionY(); i++) {
                this.storage.put(getKey(pos, i), Optional.empty());
            }
        } else {
            boolean flag = packedChunk.versionChanged();

            for (int j = this.levelHeightAccessor.getMinSectionY(); j <= this.levelHeightAccessor.getMaxSectionY(); j++) {
                long k = getKey(pos, j);
                Optional<R> optional = Optional.ofNullable(packedChunk.sectionsByY.get(j))
                    .map(p_360210_ -> this.unpacker.apply((P)p_360210_, () -> this.setDirty(k)));
                this.storage.put(k, optional);
                optional.ifPresent(p_223523_ -> {
                    this.onSectionLoad(k);
                    if (flag) {
                        this.setDirty(k);
                    }
                });
            }
        }
    }

    private void writeChunk(ChunkPos pos) {
        RegistryOps<Tag> registryops = this.registryAccess.createSerializationContext(NbtOps.INSTANCE);
        Dynamic<Tag> dynamic = this.writeChunk(pos, registryops);
        Tag tag = dynamic.getValue();
        if (tag instanceof CompoundTag) {
            this.simpleRegionStorage.write(pos, (CompoundTag)tag).exceptionally(p_351992_ -> {
                this.errorReporter.reportChunkSaveFailure(p_351992_, this.simpleRegionStorage.storageInfo(), pos);
                return null;
            });
        } else {
            LOGGER.error("Expected compound tag, got {}", tag);
        }
    }

    private <T> Dynamic<T> writeChunk(ChunkPos pos, DynamicOps<T> ops) {
        Map<T, T> map = Maps.newHashMap();

        for (int i = this.levelHeightAccessor.getMinSectionY(); i <= this.levelHeightAccessor.getMaxSectionY(); i++) {
            long j = getKey(pos, i);
            Optional<R> optional = this.storage.get(j);
            if (optional != null && !optional.isEmpty()) {
                DataResult<T> dataresult = this.codec.encodeStart(ops, this.packer.apply(optional.get()));
                String s = Integer.toString(i);
                dataresult.resultOrPartial(LOGGER::error).ifPresent(p_223531_ -> map.put(ops.createString(s), (T)p_223531_));
            }
        }

        return new Dynamic<>(
            ops,
            ops.createMap(
                ImmutableMap.of(
                    ops.createString("Sections"),
                    ops.createMap(map),
                    ops.createString("DataVersion"),
                    ops.createInt(SharedConstants.getCurrentVersion().dataVersion().version())
                )
            )
        );
    }

    private static long getKey(ChunkPos chunkPos, int sectionY) {
        return SectionPos.asLong(chunkPos.x, sectionY, chunkPos.z);
    }

    protected void onSectionLoad(long sectionKey) {
    }

    protected void setDirty(long sectionPos) {
        Optional<R> optional = this.storage.get(sectionPos);
        if (optional != null && !optional.isEmpty()) {
            this.dirtyChunks.add(ChunkPos.asLong(SectionPos.x(sectionPos), SectionPos.z(sectionPos)));
        } else {
            LOGGER.warn("No data for position: {}", SectionPos.of(sectionPos));
        }
    }

    public void flush(ChunkPos chunkPos) {
        if (this.dirtyChunks.remove(chunkPos.toLong())) {
            this.writeChunk(chunkPos);
        }
    }

    @Override
    public void close() throws IOException {
        this.simpleRegionStorage.close();
    }

    /**
     * Neo: Removes the data for the given chunk position.
     * See PR #937
     */
    public void remove(ChunkPos chunkPos) {
        synchronized (this.loadLock) {
            for (int y = this.levelHeightAccessor.getMinSectionY(); y <= this.levelHeightAccessor.getMaxSectionY(); y++) {
                this.storage.remove(getKey(chunkPos, y));
            }
            this.loadedChunks.remove(chunkPos.toLong());
        }
    }

    record PackedChunk<T>(Int2ObjectMap<T> sectionsByY, boolean versionChanged) {
        public static <T> SectionStorage.PackedChunk<T> parse(
            Codec<T> codec, DynamicOps<Tag> ops, Tag value, SimpleRegionStorage simpleRegionStorage, LevelHeightAccessor levelHeightAccessor
        ) {
            Dynamic<Tag> dynamic = new Dynamic<>(ops, value);
            Dynamic<Tag> dynamic1 = simpleRegionStorage.upgradeChunkTag(dynamic, 1945);
            boolean flag = dynamic != dynamic1;
            OptionalDynamic<Tag> optionaldynamic = dynamic1.get("Sections");
            Int2ObjectMap<T> int2objectmap = new Int2ObjectOpenHashMap<>();

            for (int i = levelHeightAccessor.getMinSectionY(); i <= levelHeightAccessor.getMaxSectionY(); i++) {
                Optional<T> optional = optionaldynamic.get(Integer.toString(i))
                    .result()
                    .flatMap(p_361362_ -> codec.parse((Dynamic<Tag>)p_361362_).resultOrPartial(SectionStorage.LOGGER::error));
                if (optional.isPresent()) {
                    int2objectmap.put(i, optional.get());
                }
            }

            return new SectionStorage.PackedChunk<>(int2objectmap, flag);
        }
    }
}
