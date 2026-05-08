package net.minecraft.world.level.chunk;

public interface PaletteResize<T> {
    /**
     * Called when the underlying palette needs to resize itself to support additional objects.
     * @return The new integer mapping for the object added.
     *
     * @param bits The new palette size, in bits.
     */
    int onResize(int bits, T objectAdded);

    static <T> PaletteResize<T> noResizeExpected() {
        return (p_446326_, p_446950_) -> {
            throw new IllegalArgumentException("Unexpected palette resize, bits = " + p_446326_ + ", added value = " + p_446950_);
        };
    }
}
