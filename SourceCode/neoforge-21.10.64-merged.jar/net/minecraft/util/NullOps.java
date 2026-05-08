package net.minecraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractUniversalBuilder;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class NullOps implements DynamicOps<Unit> {
    public static final NullOps INSTANCE = new NullOps();
    private static final MapLike<Unit> EMPTY_MAP = new MapLike<Unit>() {
        @Nullable
        public Unit get(Unit p_434201_) {
            return null;
        }

        @Nullable
        public Unit get(String p_435270_) {
            return null;
        }

        @Override
        public Stream<Pair<Unit, Unit>> entries() {
            return Stream.empty();
        }
    };

    private NullOps() {
    }

    public <U> U convertTo(DynamicOps<U> ops, Unit unit) {
        return ops.empty();
    }

    public Unit empty() {
        return Unit.INSTANCE;
    }

    public Unit emptyMap() {
        return Unit.INSTANCE;
    }

    public Unit emptyList() {
        return Unit.INSTANCE;
    }

    public Unit createNumeric(Number value) {
        return Unit.INSTANCE;
    }

    public Unit createByte(byte value) {
        return Unit.INSTANCE;
    }

    public Unit createShort(short value) {
        return Unit.INSTANCE;
    }

    public Unit createInt(int value) {
        return Unit.INSTANCE;
    }

    public Unit createLong(long value) {
        return Unit.INSTANCE;
    }

    public Unit createFloat(float value) {
        return Unit.INSTANCE;
    }

    public Unit createDouble(double value) {
        return Unit.INSTANCE;
    }

    public Unit createBoolean(boolean value) {
        return Unit.INSTANCE;
    }

    public Unit createString(String value) {
        return Unit.INSTANCE;
    }

    public DataResult<Number> getNumberValue(Unit input) {
        return DataResult.success(0);
    }

    public DataResult<Boolean> getBooleanValue(Unit input) {
        return DataResult.success(false);
    }

    public DataResult<String> getStringValue(Unit input) {
        return DataResult.success("");
    }

    public DataResult<Unit> mergeToList(Unit list, Unit value) {
        return DataResult.success(Unit.INSTANCE);
    }

    public DataResult<Unit> mergeToList(Unit list, List<Unit> values) {
        return DataResult.success(Unit.INSTANCE);
    }

    public DataResult<Unit> mergeToMap(Unit map, Unit key, Unit value) {
        return DataResult.success(Unit.INSTANCE);
    }

    public DataResult<Unit> mergeToMap(Unit map, Map<Unit, Unit> values) {
        return DataResult.success(Unit.INSTANCE);
    }

    public DataResult<Unit> mergeToMap(Unit map, MapLike<Unit> values) {
        return DataResult.success(Unit.INSTANCE);
    }

    public DataResult<Stream<Pair<Unit, Unit>>> getMapValues(Unit input) {
        return DataResult.success(Stream.empty());
    }

    public DataResult<Consumer<BiConsumer<Unit, Unit>>> getMapEntries(Unit input) {
        return DataResult.success(p_432499_ -> {});
    }

    public DataResult<MapLike<Unit>> getMap(Unit input) {
        return DataResult.success(EMPTY_MAP);
    }

    public DataResult<Stream<Unit>> getStream(Unit input) {
        return DataResult.success(Stream.empty());
    }

    public DataResult<Consumer<Consumer<Unit>>> getList(Unit input) {
        return DataResult.success(p_432500_ -> {});
    }

    public DataResult<ByteBuffer> getByteBuffer(Unit input) {
        return DataResult.success(ByteBuffer.wrap(new byte[0]));
    }

    public DataResult<IntStream> getIntStream(Unit input) {
        return DataResult.success(IntStream.empty());
    }

    public DataResult<LongStream> getLongStream(Unit input) {
        return DataResult.success(LongStream.empty());
    }

    public Unit createMap(Stream<Pair<Unit, Unit>> map) {
        return Unit.INSTANCE;
    }

    public Unit createMap(Map<Unit, Unit> map) {
        return Unit.INSTANCE;
    }

    public Unit createList(Stream<Unit> input) {
        return Unit.INSTANCE;
    }

    public Unit createByteList(ByteBuffer input) {
        return Unit.INSTANCE;
    }

    public Unit createIntList(IntStream input) {
        return Unit.INSTANCE;
    }

    public Unit createLongList(LongStream input) {
        return Unit.INSTANCE;
    }

    public Unit remove(Unit input, String key) {
        return input;
    }

    @Override
    public RecordBuilder<Unit> mapBuilder() {
        return new NullOps.NullMapBuilder(this);
    }

    @Override
    public ListBuilder<Unit> listBuilder() {
        return new NullOps.NullListBuilder(this);
    }

    @Override
    public String toString() {
        return "Null";
    }

    static final class NullListBuilder extends AbstractListBuilder<Unit, Unit> {
        public NullListBuilder(DynamicOps<Unit> p_412134_) {
            super(p_412134_);
        }

        protected Unit initBuilder() {
            return Unit.INSTANCE;
        }

        protected Unit append(Unit p_412098_, Unit p_412572_) {
            return p_412098_;
        }

        protected DataResult<Unit> build(Unit p_412100_, Unit p_412050_) {
            return DataResult.success(p_412100_);
        }
    }

    static final class NullMapBuilder extends AbstractUniversalBuilder<Unit, Unit> {
        public NullMapBuilder(DynamicOps<Unit> ops) {
            super(ops);
        }

        protected Unit initBuilder() {
            return Unit.INSTANCE;
        }

        protected Unit append(Unit p_341121_, Unit p_341327_, Unit p_341036_) {
            return p_341036_;
        }

        protected DataResult<Unit> build(Unit p_341068_, Unit p_341207_) {
            return DataResult.success(p_341207_);
        }
    }
}
