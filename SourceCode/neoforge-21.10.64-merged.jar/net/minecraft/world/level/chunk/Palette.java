package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;

public interface Palette<T> {
    int idFor(T value, PaletteResize<T> resize);

    boolean maybeHas(Predicate<T> filter);

    T valueFor(int id);

    void read(FriendlyByteBuf buffer, IdMap<T> idMap);

    void write(FriendlyByteBuf buffer, IdMap<T> idMap);

    int getSerializedSize(IdMap<T> idMap);

    int getSize();

    Palette<T> copy();

    public interface Factory {
        <A> Palette<A> create(int bits, List<A> values);
    }
}
