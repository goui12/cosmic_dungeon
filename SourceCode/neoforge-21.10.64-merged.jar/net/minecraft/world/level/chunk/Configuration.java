package net.minecraft.world.level.chunk;

import java.util.List;

public interface Configuration {
    boolean alwaysRepack();

    int bitsInMemory();

    int bitsInStorage();

    <T> Palette<T> createPalette(Strategy<T> strategy, List<T> values);

    public record Global(int bitsInMemory, int bitsInStorage) implements Configuration {
        @Override
        public boolean alwaysRepack() {
            return true;
        }

        @Override
        public <T> Palette<T> createPalette(Strategy<T> p_446902_, List<T> p_445918_) {
            return p_446902_.globalPalette();
        }
    }

    public record Simple(Palette.Factory factory, int bits) implements Configuration {
        @Override
        public boolean alwaysRepack() {
            return false;
        }

        @Override
        public <T> Palette<T> createPalette(Strategy<T> p_446781_, List<T> p_445514_) {
            return this.factory.create(this.bits, p_445514_);
        }

        @Override
        public int bitsInMemory() {
            return this.bits;
        }

        @Override
        public int bitsInStorage() {
            return this.bits;
        }
    }
}
