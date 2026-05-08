package net.minecraft.world.level.chunk;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.BitStorage;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.util.ZeroBitStorage;

public class PalettedContainer<T> implements PaletteResize<T>, PalettedContainerRO<T> {
    private static final int MIN_PALETTE_BITS = 0;
    private volatile PalettedContainer.Data<T> data;
    private final Strategy<T> strategy;
    private final ThreadingDetector threadingDetector = new ThreadingDetector("PalettedContainer");

    public void acquire() {
        this.threadingDetector.checkAndLock();
    }

    public void release() {
        this.threadingDetector.checkAndUnlock();
    }

    public static <T> Codec<PalettedContainer<T>> codecRW(Codec<T> valueCodec, Strategy<T> strategy, T defaultValue) {
        PalettedContainerRO.Unpacker<T, PalettedContainer<T>> unpacker = PalettedContainer::unpack;
        return codec(valueCodec, strategy, defaultValue, unpacker);
    }

    public static <T> Codec<PalettedContainerRO<T>> codecRO(Codec<T> valueCodec, Strategy<T> strategy, T defaultValue) {
        PalettedContainerRO.Unpacker<T, PalettedContainerRO<T>> unpacker = (p_445355_, p_445356_) -> unpack(p_445355_, p_445356_)
            .map(p_238264_ -> (PalettedContainerRO<T>)p_238264_);
        return codec(valueCodec, strategy, defaultValue, unpacker);
    }

    private static <T, C extends PalettedContainerRO<T>> Codec<C> codec(
        Codec<T> valueCodec, Strategy<T> strategy, T defaultValue, PalettedContainerRO.Unpacker<T, C> unpacker
    ) {
        return RecordCodecBuilder.<PalettedContainerRO.PackedData<T>>create(
                p_338082_ -> p_338082_.group(
                        valueCodec.mapResult(ExtraCodecs.orElsePartial(defaultValue))
                            .listOf()
                            .fieldOf("palette")
                            .forGetter(PalettedContainerRO.PackedData::paletteEntries),
                        Codec.LONG_STREAM.lenientOptionalFieldOf("data").forGetter(PalettedContainerRO.PackedData::storage)
                    )
                    .apply(p_338082_, PalettedContainerRO.PackedData::new)
            )
            .comapFlatMap(p_445354_ -> unpacker.read(strategy, (PalettedContainerRO.PackedData<T>)p_445354_), p_445360_ -> p_445360_.pack(strategy));
    }

    private PalettedContainer(Strategy<T> strategy, Configuration configuration, BitStorage storage, Palette<T> palette) {
        this.strategy = strategy;
        this.data = new PalettedContainer.Data<>(configuration, storage, palette);
    }

    private PalettedContainer(PalettedContainer<T> other) {
        this.strategy = other.strategy;
        this.data = other.data.copy();
    }

    public PalettedContainer(T defaultValue, Strategy<T> strategy) {
        this.strategy = strategy;
        this.data = this.createOrReuseData(null, 0);
        this.data.palette.idFor(defaultValue, this);
    }

    private PalettedContainer.Data<T> createOrReuseData(@Nullable PalettedContainer.Data<T> data, int id) {
        Configuration configuration = this.strategy.getConfigurationForBitCount(id);
        if (data != null && configuration.equals(data.configuration())) {
            return data;
        } else {
            BitStorage bitstorage = (BitStorage)(configuration.bitsInMemory() == 0
                ? new ZeroBitStorage(this.strategy.entryCount())
                : new SimpleBitStorage(configuration.bitsInMemory(), this.strategy.entryCount()));
            Palette<T> palette = configuration.createPalette(this.strategy, List.of());
            return new PalettedContainer.Data<>(configuration, bitstorage, palette);
        }
    }

    /**
     * Called when the underlying palette needs to resize itself to support additional objects.
     * @return The new integer mapping for the object added.
     *
     * @param bits The new palette size, in bits.
     */
    @Override
    public int onResize(int bits, T objectAdded) {
        PalettedContainer.Data<T> data = this.data;
        PalettedContainer.Data<T> data1 = this.createOrReuseData(data, bits);
        data1.copyFrom(data.palette, data.storage);
        this.data = data1;
        return data1.palette.idFor(objectAdded, PaletteResize.noResizeExpected());
    }

    public T getAndSet(int x, int y, int z, T state) {
        this.acquire();

        Object object;
        try {
            object = this.getAndSet(this.strategy.getIndex(x, y, z), state);
        } finally {
            this.release();
        }

        return (T)object;
    }

    public T getAndSetUnchecked(int x, int y, int z, T state) {
        return this.getAndSet(this.strategy.getIndex(x, y, z), state);
    }

    private T getAndSet(int index, T state) {
        int i = this.data.palette.idFor(state, this);
        int j = this.data.storage.getAndSet(index, i);
        return this.data.palette.valueFor(j);
    }

    public void set(int x, int y, int z, T state) {
        this.acquire();

        try {
            this.set(this.strategy.getIndex(x, y, z), state);
        } finally {
            this.release();
        }
    }

    private void set(int index, T state) {
        int i = this.data.palette.idFor(state, this);
        this.data.storage.set(index, i);
    }

    @Override
    public T get(int x, int y, int z) {
        return this.get(this.strategy.getIndex(x, y, z));
    }

    protected T get(int index) {
        PalettedContainer.Data<T> data = this.data;
        return data.palette.valueFor(data.storage.get(index));
    }

    @Override
    public void getAll(Consumer<T> consumer) {
        Palette<T> palette = this.data.palette();
        IntSet intset = new IntArraySet();
        this.data.storage.getAll(intset::add);
        intset.forEach(p_238274_ -> consumer.accept(palette.valueFor(p_238274_)));
    }

    public void read(FriendlyByteBuf buffer) {
        this.acquire();

        try {
            int i = buffer.readByte();
            PalettedContainer.Data<T> data = this.createOrReuseData(this.data, i);
            data.palette.read(buffer, this.strategy.globalMap());
            buffer.readFixedSizeLongArray(data.storage.getRaw());
            this.data = data;
        } finally {
            this.release();
        }
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        this.acquire();

        try {
            this.data.write(buffer, this.strategy.globalMap());
        } finally {
            this.release();
        }
    }

    @VisibleForTesting
    public static <T> DataResult<PalettedContainer<T>> unpack(Strategy<T> strategy, PalettedContainerRO.PackedData<T> packedData) {
        List<T> list = packedData.paletteEntries();
        int i = strategy.entryCount();
        Configuration configuration = strategy.getConfigurationForPaletteSize(list.size());
        int j = configuration.bitsInStorage();
        if (packedData.bitsPerEntry() != -1 && j != packedData.bitsPerEntry()) {
            return DataResult.error(() -> "Invalid bit count, calculated " + j + ", but container declared " + packedData.bitsPerEntry());
        } else {
            BitStorage bitstorage;
            Palette<T> palette;
            if (configuration.bitsInMemory() == 0) {
                palette = configuration.createPalette(strategy, list);
                bitstorage = new ZeroBitStorage(i);
            } else {
                Optional<LongStream> optional = packedData.storage();
                if (optional.isEmpty()) {
                    return DataResult.error(() -> "Missing values for non-zero storage");
                }

                long[] along = optional.get().toArray();

                try {
                    if (!configuration.alwaysRepack() && configuration.bitsInMemory() == j) {
                        palette = configuration.createPalette(strategy, list);
                        bitstorage = new SimpleBitStorage(configuration.bitsInMemory(), i, along);
                    } else {
                        Palette<T> palette1 = new HashMapPalette<>(j, list);
                        SimpleBitStorage simplebitstorage = new SimpleBitStorage(j, i, along);
                        Palette<T> palette2 = configuration.createPalette(strategy, list);
                        int[] aint = reencodeContents(simplebitstorage, palette1, palette2);
                        palette = palette2;
                        bitstorage = new SimpleBitStorage(configuration.bitsInMemory(), i, aint);
                    }
                } catch (SimpleBitStorage.InitializationException simplebitstorage$initializationexception) {
                    return DataResult.error(() -> "Failed to read PalettedContainer: " + simplebitstorage$initializationexception.getMessage());
                }
            }

            return DataResult.success(new PalettedContainer<>(strategy, configuration, bitstorage, palette));
        }
    }

    @Override
    public PalettedContainerRO.PackedData<T> pack(Strategy<T> strategy) {
        this.acquire();

        PalettedContainerRO.PackedData palettedcontainerro$packeddata;
        try {
            BitStorage bitstorage = this.data.storage;
            Palette<T> palette = this.data.palette;
            HashMapPalette<T> hashmappalette = new HashMapPalette<>(bitstorage.getBits());
            int i = strategy.entryCount();
            int[] aint = reencodeContents(bitstorage, palette, hashmappalette);
            Configuration configuration = strategy.getConfigurationForPaletteSize(hashmappalette.getSize());
            int j = configuration.bitsInStorage();
            Optional<LongStream> optional;
            if (j != 0) {
                SimpleBitStorage simplebitstorage = new SimpleBitStorage(j, i, aint);
                optional = Optional.of(Arrays.stream(simplebitstorage.getRaw()));
            } else {
                optional = Optional.empty();
            }

            palettedcontainerro$packeddata = new PalettedContainerRO.PackedData<>(hashmappalette.getEntries(), optional, j);
        } finally {
            this.release();
        }

        return palettedcontainerro$packeddata;
    }

    private static <T> int[] reencodeContents(BitStorage storage, Palette<T> oldPalette, Palette<T> newPalette) {
        int[] aint = new int[storage.getSize()];
        storage.unpack(aint);
        PaletteResize<T> paletteresize = PaletteResize.noResizeExpected();
        int i = -1;
        int j = -1;

        for (int k = 0; k < aint.length; k++) {
            int l = aint[k];
            if (l != i) {
                i = l;
                j = newPalette.idFor(oldPalette.valueFor(l), paletteresize);
            }

            aint[k] = j;
        }

        return aint;
    }

    @Override
    public int getSerializedSize() {
        return this.data.getSerializedSize(this.strategy.globalMap());
    }

    @Override
    public int bitsPerEntry() {
        return this.data.storage().getBits();
    }

    @Override
    public boolean maybeHas(Predicate<T> predicate) {
        return this.data.palette.maybeHas(predicate);
    }

    @Override
    public PalettedContainer<T> copy() {
        return new PalettedContainer<>(this);
    }

    @Override
    public PalettedContainer<T> recreate() {
        return new PalettedContainer<>(this.data.palette.valueFor(0), this.strategy);
    }

    /**
     * Counts the number of instances of each state in the container.
     * The provided consumer is invoked for each state with the number of instances.
     */
    @Override
    public void count(PalettedContainer.CountConsumer<T> countConsumer) {
        if (this.data.palette.getSize() == 1) {
            countConsumer.accept(this.data.palette.valueFor(0), this.data.storage.getSize());
        } else {
            Int2IntOpenHashMap int2intopenhashmap = new Int2IntOpenHashMap();
            this.data.storage.getAll(p_238269_ -> int2intopenhashmap.addTo(p_238269_, 1));
            int2intopenhashmap.int2IntEntrySet()
                .forEach(p_238271_ -> countConsumer.accept(this.data.palette.valueFor(p_238271_.getIntKey()), p_238271_.getIntValue()));
        }
    }

    @FunctionalInterface
    public interface CountConsumer<T> {
        void accept(T state, int count);
    }

    record Data<T>(Configuration configuration, BitStorage storage, Palette<T> palette) {
        public void copyFrom(Palette<T> palette, BitStorage bitStorage) {
            PaletteResize<T> paletteresize = PaletteResize.noResizeExpected();

            for (int i = 0; i < bitStorage.getSize(); i++) {
                T t = palette.valueFor(bitStorage.get(i));
                this.storage.set(i, this.palette.idFor(t, paletteresize));
            }
        }

        public int getSerializedSize(IdMap<T> idMap) {
            return 1 + this.palette.getSerializedSize(idMap) + this.storage.getRaw().length * 8;
        }

        public void write(FriendlyByteBuf buffer, IdMap<T> idMap) {
            buffer.writeByte(this.storage.getBits());
            this.palette.write(buffer, idMap);
            buffer.writeFixedSizeLongArray(this.storage.getRaw());
        }

        public PalettedContainer.Data<T> copy() {
            return new PalettedContainer.Data<>(this.configuration, this.storage.copy(), this.palette.copy());
        }
    }
}
