package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

public class ValueInputContextHelper {
    final HolderLookup.Provider lookup;
    private final DynamicOps<Tag> ops;
    final ValueInput.ValueInputList emptyChildList = new ValueInput.ValueInputList() {
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Stream<ValueInput> stream() {
            return Stream.empty();
        }

        @Override
        public Iterator<ValueInput> iterator() {
            return Collections.emptyIterator();
        }
    };
    private final ValueInput.TypedInputList<Object> emptyTypedList = new ValueInput.TypedInputList<Object>() {
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Stream<Object> stream() {
            return Stream.empty();
        }

        @Override
        public Iterator<Object> iterator() {
            return Collections.emptyIterator();
        }
    };
    private final ValueInput empty = new ValueInput() {
        @Override
        public <T> Optional<T> read(String p_421648_, Codec<T> p_421691_) {
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> read(MapCodec<T> p_422454_) {
            return Optional.empty();
        }

        @Override
        public Optional<ValueInput> child(String p_422303_) {
            return Optional.empty();
        }

        @Override
        public ValueInput childOrEmpty(String p_421498_) {
            return this;
        }

        @Override
        public Optional<ValueInput.ValueInputList> childrenList(String p_421555_) {
            return Optional.empty();
        }

        @Override
        public ValueInput.ValueInputList childrenListOrEmpty(String p_421888_) {
            return ValueInputContextHelper.this.emptyChildList;
        }

        @Override
        public <T> Optional<ValueInput.TypedInputList<T>> list(String p_422680_, Codec<T> p_422257_) {
            return Optional.empty();
        }

        @Override
        public <T> ValueInput.TypedInputList<T> listOrEmpty(String p_422233_, Codec<T> p_421929_) {
            return ValueInputContextHelper.this.emptyTypedList();
        }

        @Override
        public boolean getBooleanOr(String p_422486_, boolean p_422290_) {
            return p_422290_;
        }

        @Override
        public byte getByteOr(String p_421973_, byte p_421620_) {
            return p_421620_;
        }

        @Override
        public int getShortOr(String p_421823_, short p_422359_) {
            return p_422359_;
        }

        @Override
        public Optional<Integer> getInt(String p_421713_) {
            return Optional.empty();
        }

        @Override
        public int getIntOr(String p_422236_, int p_421815_) {
            return p_421815_;
        }

        @Override
        public long getLongOr(String p_422533_, long p_422471_) {
            return p_422471_;
        }

        @Override
        public Optional<Long> getLong(String p_421635_) {
            return Optional.empty();
        }

        @Override
        public float getFloatOr(String p_421809_, float p_421919_) {
            return p_421919_;
        }

        @Override
        public double getDoubleOr(String p_422117_, double p_422194_) {
            return p_422194_;
        }

        @Override
        public Optional<String> getString(String p_422250_) {
            return Optional.empty();
        }

        @Override
        public String getStringOr(String p_422013_, String p_421650_) {
            return p_421650_;
        }

        @Override
        public HolderLookup.Provider lookup() {
            return ValueInputContextHelper.this.lookup;
        }

        @Override
        public Optional<int[]> getIntArray(String p_422458_) {
            return Optional.empty();
        }
    };

    public ValueInputContextHelper(HolderLookup.Provider lookup, DynamicOps<Tag> ops) {
        this.lookup = lookup;
        this.ops = lookup.createSerializationContext(ops);
    }

    public DynamicOps<Tag> ops() {
        return this.ops;
    }

    public HolderLookup.Provider lookup() {
        return this.lookup;
    }

    public ValueInput empty() {
        return this.empty;
    }

    public ValueInput.ValueInputList emptyList() {
        return this.emptyChildList;
    }

    public <T> ValueInput.TypedInputList<T> emptyTypedList() {
        return (ValueInput.TypedInputList<T>)this.emptyTypedList;
    }
}
