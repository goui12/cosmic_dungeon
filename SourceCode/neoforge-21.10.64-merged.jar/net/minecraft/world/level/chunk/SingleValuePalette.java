package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import org.apache.commons.lang3.Validate;

public class SingleValuePalette<T> implements Palette<T> {
    @Nullable
    private T value;

    public SingleValuePalette(List<T> values) {
        if (values.size() > 0) {
            Validate.isTrue(values.size() <= 1, "Can't initialize SingleValuePalette with %d values.", (long)values.size());
            this.value = values.get(0);
        }
    }

    public static <A> Palette<A> create(int bits, List<A> values) {
        return new SingleValuePalette<>(values);
    }

    @Override
    public int idFor(T value, PaletteResize<T> resize) {
        if (this.value != null && this.value != value) {
            return resize.onResize(1, value);
        } else {
            this.value = value;
            return 0;
        }
    }

    @Override
    public boolean maybeHas(Predicate<T> filter) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return filter.test(this.value);
        }
    }

    @Override
    public T valueFor(int id) {
        if (this.value != null && id == 0) {
            return this.value;
        } else {
            throw new IllegalStateException("Missing Palette entry for id " + id + ".");
        }
    }

    @Override
    public void read(FriendlyByteBuf buffer, IdMap<T> idMap) {
        this.value = idMap.byIdOrThrow(buffer.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buffer, IdMap<T> idMap) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            buffer.writeVarInt(idMap.getId(this.value));
        }
    }

    @Override
    public int getSerializedSize(IdMap<T> idMap) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return VarInt.getByteSize(idMap.getId(this.value));
        }
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public Palette<T> copy() {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return this;
        }
    }
}
