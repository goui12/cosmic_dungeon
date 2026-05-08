package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import org.apache.commons.lang3.Validate;

public class LinearPalette<T> implements Palette<T> {
    private final T[] values;
    private final int bits;
    private int size;

    private LinearPalette(int bits, List<T> values) {
        this.values = (T[])(new Object[1 << bits]);
        this.bits = bits;
        Validate.isTrue(
            values.size() <= this.values.length, "Can't initialize LinearPalette of size %d with %d entries", this.values.length, values.size()
        );

        for (int i = 0; i < values.size(); i++) {
            this.values[i] = values.get(i);
        }

        this.size = values.size();
    }

    private LinearPalette(T[] values, int bits, int size) {
        this.values = values;
        this.bits = bits;
        this.size = size;
    }

    public static <A> Palette<A> create(int bits, List<A> values) {
        return new LinearPalette<>(bits, values);
    }

    @Override
    public int idFor(T value, PaletteResize<T> resize) {
        for (int i = 0; i < this.size; i++) {
            if (this.values[i] == value) {
                return i;
            }
        }

        int j = this.size;
        if (j < this.values.length) {
            this.values[j] = value;
            this.size++;
            return j;
        } else {
            return resize.onResize(this.bits + 1, value);
        }
    }

    @Override
    public boolean maybeHas(Predicate<T> filter) {
        for (int i = 0; i < this.size; i++) {
            if (filter.test(this.values[i])) {
                return true;
            }
        }

        return false;
    }

    @Override
    public T valueFor(int id) {
        if (id >= 0 && id < this.size) {
            return this.values[id];
        } else {
            throw new MissingPaletteEntryException(id);
        }
    }

    @Override
    public void read(FriendlyByteBuf buffer, IdMap<T> idMap) {
        this.size = buffer.readVarInt();

        for (int i = 0; i < this.size; i++) {
            this.values[i] = idMap.byIdOrThrow(buffer.readVarInt());
        }
    }

    @Override
    public void write(FriendlyByteBuf buffer, IdMap<T> idMap) {
        buffer.writeVarInt(this.size);

        for (int i = 0; i < this.size; i++) {
            buffer.writeVarInt(idMap.getId(this.values[i]));
        }
    }

    @Override
    public int getSerializedSize(IdMap<T> idMap) {
        int i = VarInt.getByteSize(this.getSize());

        for (int j = 0; j < this.getSize(); j++) {
            i += VarInt.getByteSize(idMap.getId(this.values[j]));
        }

        return i;
    }

    @Override
    public int getSize() {
        return this.size;
    }

    @Override
    public Palette<T> copy() {
        return new LinearPalette<>((T[])((Object[])this.values.clone()), this.bits, this.size);
    }
}
