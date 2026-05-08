package net.minecraft.world.level.saveddata;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.util.datafix.DataFixTypes;

public record SavedDataType<T extends SavedData>(
    String id, Function<SavedData.Context, T> constructor, Function<SavedData.Context, Codec<T>> codec, @org.jetbrains.annotations.Nullable DataFixTypes dataFixType // Neo: We do not have update logic compatible with DFU, several downstream patches from this record are made to support a nullable type.
) {
    public SavedDataType(String id, Function<SavedData.Context, T> constructor, Function<SavedData.Context, Codec<T>> codec) {
        this(id, constructor, codec, null);
    }

    public SavedDataType(String id, Supplier<T> constructor, Codec<T> codec) {
        this(id, p_401100_ -> constructor.get(), p_401036_ -> codec, null);
    }

    public SavedDataType(String p_401076_, Supplier<T> p_401163_, Codec<T> p_401275_, @org.jetbrains.annotations.Nullable DataFixTypes p_401088_) {
        this(p_401076_, p_401100_ -> p_401163_.get(), p_401036_ -> p_401275_, p_401088_);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SavedDataType<?> saveddatatype && this.id.equals(saveddatatype.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "SavedDataType[" + this.id + "]";
    }
}
