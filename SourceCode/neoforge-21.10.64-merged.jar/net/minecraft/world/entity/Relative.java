package net.minecraft.world.entity;

import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum Relative {
    X(0),
    Y(1),
    Z(2),
    Y_ROT(3),
    X_ROT(4),
    DELTA_X(5),
    DELTA_Y(6),
    DELTA_Z(7),
    ROTATE_DELTA(8);

    public static final Set<Relative> ALL = Set.of(values());
    public static final Set<Relative> ROTATION = Set.of(X_ROT, Y_ROT);
    public static final Set<Relative> DELTA = Set.of(DELTA_X, DELTA_Y, DELTA_Z, ROTATE_DELTA);
    public static final StreamCodec<ByteBuf, Set<Relative>> SET_STREAM_CODEC = ByteBufCodecs.INT.map(Relative::unpack, Relative::pack);
    private final int bit;

    @SafeVarargs
    public static Set<Relative> union(Set<Relative>... sets) {
        HashSet<Relative> hashset = new HashSet<>();

        for (Set<Relative> set : sets) {
            hashset.addAll(set);
        }

        return hashset;
    }

    public static Set<Relative> rotation(boolean yRot, boolean xRot) {
        Set<Relative> set = EnumSet.noneOf(Relative.class);
        if (yRot) {
            set.add(Y_ROT);
        }

        if (xRot) {
            set.add(X_ROT);
        }

        return set;
    }

    public static Set<Relative> position(boolean x, boolean y, boolean z) {
        Set<Relative> set = EnumSet.noneOf(Relative.class);
        if (x) {
            set.add(X);
        }

        if (y) {
            set.add(Y);
        }

        if (z) {
            set.add(Z);
        }

        return set;
    }

    public static Set<Relative> direction(boolean deltaX, boolean deltaY, boolean deltaZ) {
        Set<Relative> set = EnumSet.noneOf(Relative.class);
        if (deltaX) {
            set.add(DELTA_X);
        }

        if (deltaY) {
            set.add(DELTA_Y);
        }

        if (deltaZ) {
            set.add(DELTA_Z);
        }

        return set;
    }

    private Relative(int bit) {
        this.bit = bit;
    }

    private int getMask() {
        return 1 << this.bit;
    }

    private boolean isSet(int data) {
        return (data & this.getMask()) == this.getMask();
    }

    public static Set<Relative> unpack(int data) {
        Set<Relative> set = EnumSet.noneOf(Relative.class);

        for (Relative relative : values()) {
            if (relative.isSet(data)) {
                set.add(relative);
            }
        }

        return set;
    }

    public static int pack(Set<Relative> relatives) {
        int i = 0;

        for (Relative relative : relatives) {
            i |= relative.getMask();
        }

        return i;
    }
}
