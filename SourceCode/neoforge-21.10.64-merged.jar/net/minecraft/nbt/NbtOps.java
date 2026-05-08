package net.minecraft.nbt;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractStringBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;

public class NbtOps implements DynamicOps<Tag> {
    public static final NbtOps INSTANCE = new NbtOps();

    private NbtOps() {
    }

    public Tag empty() {
        return EndTag.INSTANCE;
    }

    public <U> U convertTo(DynamicOps<U> ops, Tag tag) {
        return (U)(switch (tag) {
            case EndTag endtag -> (Object)ops.empty();
            case ByteTag(byte b0) -> (Object)ops.createByte(b0);
            case ShortTag(short short1) -> (Object)ops.createShort(short1);
            case IntTag(int i) -> (Object)ops.createInt(i);
            case LongTag(long j) -> (Object)ops.createLong(j);
            case FloatTag(float f) -> (Object)ops.createFloat(f);
            case DoubleTag(double d0) -> (Object)ops.createDouble(d0);
            case ByteArrayTag bytearraytag -> (Object)ops.createByteList(ByteBuffer.wrap(bytearraytag.getAsByteArray()));
            case StringTag(String s) -> (Object)ops.createString(s);
            case ListTag listtag -> (Object)this.convertList(ops, listtag);
            case CompoundTag compoundtag -> (Object)this.convertMap(ops, compoundtag);
            case IntArrayTag intarraytag -> (Object)ops.createIntList(Arrays.stream(intarraytag.getAsIntArray()));
            case LongArrayTag longarraytag -> (Object)ops.createLongList(Arrays.stream(longarraytag.getAsLongArray()));
            default -> throw new MatchException(null, null);
        });
    }

    public DataResult<Number> getNumberValue(Tag tag) {
        return tag.asNumber().map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Not a number"));
    }

    public Tag createNumeric(Number data) {
        return DoubleTag.valueOf(data.doubleValue());
    }

    public Tag createByte(byte data) {
        return ByteTag.valueOf(data);
    }

    public Tag createShort(short data) {
        return ShortTag.valueOf(data);
    }

    public Tag createInt(int data) {
        return IntTag.valueOf(data);
    }

    public Tag createLong(long data) {
        return LongTag.valueOf(data);
    }

    public Tag createFloat(float data) {
        return FloatTag.valueOf(data);
    }

    public Tag createDouble(double data) {
        return DoubleTag.valueOf(data);
    }

    public Tag createBoolean(boolean data) {
        return ByteTag.valueOf(data);
    }

    public DataResult<String> getStringValue(Tag tag) {
        return tag instanceof StringTag(String s) ? DataResult.success(s) : DataResult.error(() -> "Not a string");
    }

    public Tag createString(String data) {
        return StringTag.valueOf(data);
    }

    public DataResult<Tag> mergeToList(Tag list, Tag tag) {
        return createCollector(list)
            .map(p_248053_ -> DataResult.success(p_248053_.accept(tag).result()))
            .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + list, list));
    }

    public DataResult<Tag> mergeToList(Tag list, List<Tag> tags) {
        return createCollector(list)
            .map(p_248048_ -> DataResult.success(p_248048_.acceptAll(tags).result()))
            .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + list, list));
    }

    public DataResult<Tag> mergeToMap(Tag map, Tag key, Tag value) {
        if (!(map instanceof CompoundTag) && !(map instanceof EndTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
        } else if (key instanceof StringTag(String s1)) {
            String $$5 = s1;
            CompoundTag compoundtag = map instanceof CompoundTag compoundtag1 ? compoundtag1.shallowCopy() : new CompoundTag();
            compoundtag.put($$5, value);
            return DataResult.success(compoundtag);
        } else {
            return DataResult.error(() -> "key is not a string: " + key, map);
        }
    }

    public DataResult<Tag> mergeToMap(Tag map, MapLike<Tag> otherMap) {
        if (!(map instanceof CompoundTag) && !(map instanceof EndTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
        } else {
            CompoundTag compoundtag = map instanceof CompoundTag compoundtag1 ? compoundtag1.shallowCopy() : new CompoundTag();
            List<Tag> list = new ArrayList<>();
            otherMap.entries().forEach(p_409125_ -> {
                Tag tag = p_409125_.getFirst();
                if (tag instanceof StringTag(String s)) {
                    compoundtag.put(s, p_409125_.getSecond());
                } else {
                    list.add(tag);
                }
            });
            return !list.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + list, compoundtag) : DataResult.success(compoundtag);
        }
    }

    public DataResult<Tag> mergeToMap(Tag p_341945_, Map<Tag, Tag> p_341920_) {
        if (!(p_341945_ instanceof CompoundTag) && !(p_341945_ instanceof EndTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + p_341945_, p_341945_);
        } else {
            CompoundTag compoundtag = p_341945_ instanceof CompoundTag compoundtag1 ? compoundtag1.shallowCopy() : new CompoundTag();
            List<Tag> list = new ArrayList<>();

            for (Entry<Tag, Tag> entry : p_341920_.entrySet()) {
                Tag tag = entry.getKey();
                if (tag instanceof StringTag(String s)) {
                    compoundtag.put(s, entry.getValue());
                } else {
                    list.add(tag);
                }
            }

            return !list.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + list, compoundtag) : DataResult.success(compoundtag);
        }
    }

    public DataResult<Stream<Pair<Tag, Tag>>> getMapValues(Tag map) {
        return map instanceof CompoundTag compoundtag
            ? DataResult.success(compoundtag.entrySet().stream().map(p_341872_ -> Pair.of(this.createString(p_341872_.getKey()), p_341872_.getValue())))
            : DataResult.error(() -> "Not a map: " + map);
    }

    public DataResult<Consumer<BiConsumer<Tag, Tag>>> getMapEntries(Tag map) {
        return map instanceof CompoundTag compoundtag ? DataResult.success(p_341867_ -> {
            for (Entry<String, Tag> entry : compoundtag.entrySet()) {
                p_341867_.accept(this.createString(entry.getKey()), entry.getValue());
            }
        }) : DataResult.error(() -> "Not a map: " + map);
    }

    public DataResult<MapLike<Tag>> getMap(Tag map) {
        return map instanceof CompoundTag compoundtag ? DataResult.success(new MapLike<Tag>() {
            @Nullable
            public Tag get(Tag p_129174_) {
                if (p_129174_ instanceof StringTag(String s)) {
                    return compoundtag.get(s);
                } else {
                    throw new UnsupportedOperationException("Cannot get map entry with non-string key: " + p_129174_);
                }
            }

            @Nullable
            public Tag get(String p_129169_) {
                return compoundtag.get(p_129169_);
            }

            @Override
            public Stream<Pair<Tag, Tag>> entries() {
                return compoundtag.entrySet().stream().map(p_341873_ -> Pair.of(NbtOps.this.createString(p_341873_.getKey()), p_341873_.getValue()));
            }

            @Override
            public String toString() {
                return "MapLike[" + compoundtag + "]";
            }
        }) : DataResult.error(() -> "Not a map: " + map);
    }

    public Tag createMap(Stream<Pair<Tag, Tag>> data) {
        CompoundTag compoundtag = new CompoundTag();
        data.forEach(p_409127_ -> {
            Tag tag = p_409127_.getFirst();
            Tag tag1 = p_409127_.getSecond();
            if (tag instanceof StringTag(String s)) {
                compoundtag.put(s, tag1);
            } else {
                throw new UnsupportedOperationException("Cannot create map with non-string key: " + tag);
            }
        });
        return compoundtag;
    }

    public DataResult<Stream<Tag>> getStream(Tag tag) {
        return tag instanceof CollectionTag collectiontag ? DataResult.success(collectiontag.stream()) : DataResult.error(() -> "Not a list");
    }

    public DataResult<Consumer<Consumer<Tag>>> getList(Tag tag) {
        return tag instanceof CollectionTag collectiontag
            ? DataResult.success(collectiontag::forEach)
            : DataResult.error(() -> "Not a list: " + tag);
    }

    public DataResult<ByteBuffer> getByteBuffer(Tag tag) {
        return tag instanceof ByteArrayTag bytearraytag
            ? DataResult.success(ByteBuffer.wrap(bytearraytag.getAsByteArray()))
            : DynamicOps.super.getByteBuffer(tag);
    }

    public Tag createByteList(ByteBuffer data) {
        ByteBuffer bytebuffer = data.duplicate().clear();
        byte[] abyte = new byte[data.capacity()];
        bytebuffer.get(0, abyte, 0, abyte.length);
        return new ByteArrayTag(abyte);
    }

    public DataResult<IntStream> getIntStream(Tag tag) {
        return tag instanceof IntArrayTag intarraytag
            ? DataResult.success(Arrays.stream(intarraytag.getAsIntArray()))
            : DynamicOps.super.getIntStream(tag);
    }

    public Tag createIntList(IntStream data) {
        return new IntArrayTag(data.toArray());
    }

    public DataResult<LongStream> getLongStream(Tag tag) {
        return tag instanceof LongArrayTag longarraytag
            ? DataResult.success(Arrays.stream(longarraytag.getAsLongArray()))
            : DynamicOps.super.getLongStream(tag);
    }

    public Tag createLongList(LongStream data) {
        return new LongArrayTag(data.toArray());
    }

    public Tag createList(Stream<Tag> data) {
        return new ListTag(data.collect(Util.toMutableList()));
    }

    public Tag remove(Tag map, String removeKey) {
        if (map instanceof CompoundTag compoundtag) {
            CompoundTag compoundtag1 = compoundtag.shallowCopy();
            compoundtag1.remove(removeKey);
            return compoundtag1;
        } else {
            return map;
        }
    }

    @Override
    public String toString() {
        return "NBT";
    }

    @Override
    public RecordBuilder<Tag> mapBuilder() {
        return new NbtOps.NbtRecordBuilder();
    }

    private static Optional<NbtOps.ListCollector> createCollector(Tag tag) {
        if (tag instanceof EndTag) {
            return Optional.of(new NbtOps.GenericListCollector());
        } else if (tag instanceof CollectionTag collectiontag) {
            if (collectiontag.isEmpty()) {
                return Optional.of(new NbtOps.GenericListCollector());
            } else {
                return switch (collectiontag) {
                    case ListTag listtag -> Optional.of(new NbtOps.GenericListCollector(listtag));
                    case ByteArrayTag bytearraytag -> Optional.of(new NbtOps.ByteListCollector(bytearraytag.getAsByteArray()));
                    case IntArrayTag intarraytag -> Optional.of(new NbtOps.IntListCollector(intarraytag.getAsIntArray()));
                    case LongArrayTag longarraytag -> Optional.of(new NbtOps.LongListCollector(longarraytag.getAsLongArray()));
                    default -> throw new MatchException(null, null);
                };
            }
        } else {
            return Optional.empty();
        }
    }

    static class ByteListCollector implements NbtOps.ListCollector {
        private final ByteArrayList values = new ByteArrayList();

        public ByteListCollector(byte[] values) {
            this.values.addElements(0, values);
        }

        @Override
        public NbtOps.ListCollector accept(Tag tag) {
            if (tag instanceof ByteTag bytetag) {
                this.values.add(bytetag.byteValue());
                return this;
            } else {
                return new NbtOps.GenericListCollector(this.values).accept(tag);
            }
        }

        @Override
        public Tag result() {
            return new ByteArrayTag(this.values.toByteArray());
        }
    }

    static class GenericListCollector implements NbtOps.ListCollector {
        private final ListTag result = new ListTag();

        GenericListCollector() {
        }

        GenericListCollector(ListTag list) {
            this.result.addAll(list);
        }

        public GenericListCollector(IntArrayList list) {
            list.forEach(p_410063_ -> this.result.add(IntTag.valueOf(p_410063_)));
        }

        public GenericListCollector(ByteArrayList list) {
            list.forEach(p_410068_ -> this.result.add(ByteTag.valueOf(p_410068_)));
        }

        public GenericListCollector(LongArrayList list) {
            list.forEach(p_409796_ -> this.result.add(LongTag.valueOf(p_409796_)));
        }

        @Override
        public NbtOps.ListCollector accept(Tag tag) {
            this.result.add(tag);
            return this;
        }

        @Override
        public Tag result() {
            return this.result;
        }
    }

    static class IntListCollector implements NbtOps.ListCollector {
        private final IntArrayList values = new IntArrayList();

        public IntListCollector(int[] values) {
            this.values.addElements(0, values);
        }

        @Override
        public NbtOps.ListCollector accept(Tag tag) {
            if (tag instanceof IntTag inttag) {
                this.values.add(inttag.intValue());
                return this;
            } else {
                return new NbtOps.GenericListCollector(this.values).accept(tag);
            }
        }

        @Override
        public Tag result() {
            return new IntArrayTag(this.values.toIntArray());
        }
    }

    interface ListCollector {
        NbtOps.ListCollector accept(Tag tag);

        default NbtOps.ListCollector acceptAll(Iterable<Tag> tags) {
            NbtOps.ListCollector nbtops$listcollector = this;

            for (Tag tag : tags) {
                nbtops$listcollector = nbtops$listcollector.accept(tag);
            }

            return nbtops$listcollector;
        }

        default NbtOps.ListCollector acceptAll(Stream<Tag> tags) {
            return this.acceptAll(tags::iterator);
        }

        Tag result();
    }

    static class LongListCollector implements NbtOps.ListCollector {
        private final LongArrayList values = new LongArrayList();

        public LongListCollector(long[] values) {
            this.values.addElements(0, values);
        }

        @Override
        public NbtOps.ListCollector accept(Tag tag) {
            if (tag instanceof LongTag longtag) {
                this.values.add(longtag.longValue());
                return this;
            } else {
                return new NbtOps.GenericListCollector(this.values).accept(tag);
            }
        }

        @Override
        public Tag result() {
            return new LongArrayTag(this.values.toLongArray());
        }
    }

    class NbtRecordBuilder extends AbstractStringBuilder<Tag, CompoundTag> {
        protected NbtRecordBuilder() {
            super(NbtOps.this);
        }

        protected CompoundTag initBuilder() {
            return new CompoundTag();
        }

        protected CompoundTag append(String key, Tag value, CompoundTag tag) {
            tag.put(key, value);
            return tag;
        }

        protected DataResult<Tag> build(CompoundTag p_129190_, Tag p_129191_) {
            if (p_129191_ == null || p_129191_ == EndTag.INSTANCE) {
                return DataResult.success(p_129190_);
            } else if (!(p_129191_ instanceof CompoundTag compoundtag)) {
                return DataResult.error(() -> "mergeToMap called with not a map: " + p_129191_, p_129191_);
            } else {
                CompoundTag compoundtag1 = compoundtag.shallowCopy();

                for (Entry<String, Tag> entry : p_129190_.entrySet()) {
                    compoundtag1.put(entry.getKey(), entry.getValue());
                }

                return DataResult.success(compoundtag1);
            }
        }
    }
}
