package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;

public class GlobalPalette<T> implements Palette<T> {
    private final IdMap<T> registry;

    public GlobalPalette(IdMap<T> registry) {
        this.registry = registry;
    }

    @Override
    public int idFor(T value, PaletteResize<T> resize) {
        int i = this.registry.getId(value);
        return i == -1 ? 0 : i;
    }

    @Override
    public boolean maybeHas(Predicate<T> filter) {
        return true;
    }

    @Override
    public T valueFor(int id) {
        T t = this.registry.byId(id);
        if (t == null) {
            throw new MissingPaletteEntryException(id);
        } else {
            return t;
        }
    }

    @Override
    public void read(FriendlyByteBuf buffer, IdMap<T> idMap) {
    }

    @Override
    public void write(FriendlyByteBuf buffer, IdMap<T> idMap) {
    }

    @Override
    public int getSerializedSize(IdMap<T> idMap) {
        return 0;
    }

    @Override
    public int getSize() {
        return this.registry.size();
    }

    @Override
    public Palette<T> copy() {
        return this;
    }
}
