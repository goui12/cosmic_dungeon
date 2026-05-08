package net.minecraft.world.level.redstone;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;

public class Orientation {
    public static final StreamCodec<ByteBuf, Orientation> STREAM_CODEC = ByteBufCodecs.idMapper(Orientation::fromIndex, Orientation::getIndex);
    private static final Orientation[] ORIENTATIONS = Util.make(() -> {
        Orientation[] aorientation = new Orientation[48];
        generateContext(new Orientation(Direction.UP, Direction.NORTH, Orientation.SideBias.LEFT), aorientation);
        return aorientation;
    });
    private final Direction up;
    private final Direction front;
    private final Direction side;
    private final Orientation.SideBias sideBias;
    private final int index;
    private final List<Direction> neighbors;
    private final List<Direction> horizontalNeighbors;
    private final List<Direction> verticalNeighbors;
    private final Map<Direction, Orientation> withFront = new EnumMap<>(Direction.class);
    private final Map<Direction, Orientation> withUp = new EnumMap<>(Direction.class);
    private final Map<Orientation.SideBias, Orientation> withSideBias = new EnumMap<>(Orientation.SideBias.class);

    private Orientation(Direction up, Direction front, Orientation.SideBias sideBias) {
        this.up = up;
        this.front = front;
        this.sideBias = sideBias;
        this.index = generateIndex(up, front, sideBias);
        Vec3i vec3i = front.getUnitVec3i().cross(up.getUnitVec3i());
        Direction direction = Direction.getNearest(vec3i, null);
        Objects.requireNonNull(direction);
        if (this.sideBias == Orientation.SideBias.RIGHT) {
            this.side = direction;
        } else {
            this.side = direction.getOpposite();
        }

        this.neighbors = List.of(this.front.getOpposite(), this.front, this.side, this.side.getOpposite(), this.up.getOpposite(), this.up);
        this.horizontalNeighbors = this.neighbors.stream().filter(p_365498_ -> p_365498_.getAxis() != this.up.getAxis()).toList();
        this.verticalNeighbors = this.neighbors.stream().filter(p_360839_ -> p_360839_.getAxis() == this.up.getAxis()).toList();
    }

    public static Orientation of(Direction up, Direction front, Orientation.SideBias sideBias) {
        return ORIENTATIONS[generateIndex(up, front, sideBias)];
    }

    public Orientation withUp(Direction up) {
        return this.withUp.get(up);
    }

    public Orientation withFront(Direction front) {
        return this.withFront.get(front);
    }

    public Orientation withFrontPreserveUp(Direction front) {
        return front.getAxis() == this.up.getAxis() ? this : this.withFront.get(front);
    }

    public Orientation withFrontAdjustSideBias(Direction front) {
        Orientation orientation = this.withFront(front);
        return this.front == orientation.side ? orientation.withMirror() : orientation;
    }

    public Orientation withSideBias(Orientation.SideBias sideBias) {
        return this.withSideBias.get(sideBias);
    }

    public Orientation withMirror() {
        return this.withSideBias(this.sideBias.getOpposite());
    }

    public Direction getFront() {
        return this.front;
    }

    public Direction getUp() {
        return this.up;
    }

    public Direction getSide() {
        return this.side;
    }

    public Orientation.SideBias getSideBias() {
        return this.sideBias;
    }

    public List<Direction> getDirections() {
        return this.neighbors;
    }

    public List<Direction> getHorizontalDirections() {
        return this.horizontalNeighbors;
    }

    public List<Direction> getVerticalDirections() {
        return this.verticalNeighbors;
    }

    @Override
    public String toString() {
        return "[up=" + this.up + ",front=" + this.front + ",sideBias=" + this.sideBias + "]";
    }

    public int getIndex() {
        return this.index;
    }

    public static Orientation fromIndex(int index) {
        return ORIENTATIONS[index];
    }

    public static Orientation random(RandomSource random) {
        return Util.getRandom(ORIENTATIONS, random);
    }

    private static Orientation generateContext(Orientation start, Orientation[] output) {
        if (output[start.getIndex()] != null) {
            return output[start.getIndex()];
        } else {
            output[start.getIndex()] = start;

            for (Orientation.SideBias orientation$sidebias : Orientation.SideBias.values()) {
                start.withSideBias
                    .put(orientation$sidebias, generateContext(new Orientation(start.up, start.front, orientation$sidebias), output));
            }

            for (Direction direction1 : Direction.values()) {
                Direction direction = start.up;
                if (direction1 == start.up) {
                    direction = start.front.getOpposite();
                }

                if (direction1 == start.up.getOpposite()) {
                    direction = start.front;
                }

                start.withFront.put(direction1, generateContext(new Orientation(direction, direction1, start.sideBias), output));
            }

            for (Direction direction2 : Direction.values()) {
                Direction direction3 = start.front;
                if (direction2 == start.front) {
                    direction3 = start.up.getOpposite();
                }

                if (direction2 == start.front.getOpposite()) {
                    direction3 = start.up;
                }

                start.withUp.put(direction2, generateContext(new Orientation(direction2, direction3, start.sideBias), output));
            }

            return start;
        }
    }

    @VisibleForTesting
    protected static int generateIndex(Direction up, Direction front, Orientation.SideBias sideBias) {
        if (up.getAxis() == front.getAxis()) {
            throw new IllegalStateException("Up-vector and front-vector can not be on the same axis");
        } else {
            int i;
            if (up.getAxis() == Direction.Axis.Y) {
                i = front.getAxis() == Direction.Axis.X ? 1 : 0;
            } else {
                i = front.getAxis() == Direction.Axis.Y ? 1 : 0;
            }

            int j = i << 1 | front.getAxisDirection().ordinal();
            return ((up.ordinal() << 2) + j << 1) + sideBias.ordinal();
        }
    }

    public static enum SideBias {
        LEFT("left"),
        RIGHT("right");

        private final String name;

        private SideBias(String name) {
            this.name = name;
        }

        public Orientation.SideBias getOpposite() {
            return this == LEFT ? RIGHT : LEFT;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
