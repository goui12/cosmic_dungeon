package net.minecraft.world.level.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;

public class HashMapPalette<T> implements Palette<T> {
    private final CrudeIncrementalIntIdentityHashBiMap<T> values;
    private final int bits;

    public HashMapPalette(int bits, List<T> values) {
        this(bits);
        values.forEach(this.values::add);
    }

    public HashMapPalette(int bits) {
        this(bits, CrudeIncrementalIntIdentityHashBiMap.create(1 << bits));
    }

    private HashMapPalette(int bits, CrudeIncrementalIntIdentityHashBiMap<T> values) {
        this.bits = bits;
        this.values = values;
    }

    public static <A> Palette<A> create(int bits, List<A> values) {
        return new HashMapPalette<>(bits, values);
    }

    @Override
    public int idFor(T value, PaletteResize<T> resize) {
        int i = this.values.getId(value);
        if (i == -1) {
            i = this.values.add(value);
            if (i >= 1 << this.bits) {
                i = resize.onResize(this.bits + 1, value);
            }
        }

        return i;
    }

    @Override
    public boolean maybeHas(Predicate<T> filter) {
        for (int i = 0; i < this.getSize(); i++) {
            if (filter.test(this.values.byId(i))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public T valueFor(int id) {
        T t = this.values.byId(id);
        if (t == null) {
            throw new MissingPaletteEntryException(id);
        } else {
            return t;
        }
    }

    @Override
    public void read(FriendlyByteBuf buffer, IdMap<T> idMap) {
        this.values.clear();
        int i = buffer.readVarInt();

        for (int j = 0; j < i; j++) {
            this.values.add(idMap.byIdOrThrow(buffer.readVarInt()));
        }
    }

    @Override
    public void write(FriendlyByteBuf buffer, IdMap<T> idMap) {
        int i = this.getSize();
        buffer.writeVarInt(i);

        for (int j = 0; j < i; j++) {
            buffer.writeVarInt(idMap.getId(this.values.byId(j)));
        }
    }

    @Override
    public int getSerializedSize(IdMap<T> idMap) {
        int i = VarInt.getByteSize(this.getSize());

        for (int j = 0; j < this.getSize(); j++) {
            i += VarInt.getByteSize(idMap.getId(this.values.byId(j)));
        }

        return i;
    }

    public List<T> getEntries() {
        ArrayList<T> arraylist = new ArrayList<>();
        this.values.iterator().forEachRemaining(arraylist::add);
        return arraylist;
    }

    @Override
    public int getSize() {
        return this.values.size();
    }

    @Override
    public Palette<T> copy() {
        return new HashMapPalette<>(this.bits, this.values.copy());
    }
}
