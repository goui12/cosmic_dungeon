package net.minecraft.world.level.redstone;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;

public class ExperimentalRedstoneWireEvaluator extends RedstoneWireEvaluator {
    private final Deque<BlockPos> wiresToTurnOff = new ArrayDeque<>();
    private final Deque<BlockPos> wiresToTurnOn = new ArrayDeque<>();
    private final Object2IntMap<BlockPos> updatedWires = new Object2IntLinkedOpenHashMap<>();

    public ExperimentalRedstoneWireEvaluator(RedStoneWireBlock wireBlock) {
        super(wireBlock);
    }

    @Override
    public void updatePowerStrength(Level level, BlockPos pos, BlockState state, @Nullable Orientation p_orientation, boolean updateShape) {
        Orientation orientation = getInitialOrientation(level, p_orientation);
        this.calculateCurrentChanges(level, pos, orientation);
        ObjectIterator<Entry<BlockPos>> objectiterator = this.updatedWires.object2IntEntrySet().iterator();

        for (boolean flag = true; objectiterator.hasNext(); flag = false) {
            Entry<BlockPos> entry = objectiterator.next();
            BlockPos blockpos = entry.getKey();
            int i = entry.getIntValue();
            int j = unpackPower(i);
            BlockState blockstate = level.getBlockState(blockpos);
            if (blockstate.is(this.wireBlock) && !blockstate.getValue(RedStoneWireBlock.POWER).equals(j)) {
                int k = 2;
                if (!updateShape || !flag) {
                    k |= 128;
                }

                level.setBlock(blockpos, blockstate.setValue(RedStoneWireBlock.POWER, j), k);
            } else {
                objectiterator.remove();
            }
        }

        this.causeNeighborUpdates(level);
    }

    private void causeNeighborUpdates(Level level) {
        this.updatedWires.forEach((p_364111_, p_365025_) -> {
            Orientation orientation = unpackOrientation(p_365025_);
            BlockState blockstate = level.getBlockState(p_364111_);

            for (Direction direction : orientation.getDirections()) {
                if (isConnected(blockstate, direction)) {
                    BlockPos blockpos = p_364111_.relative(direction);
                    BlockState blockstate1 = level.getBlockState(blockpos);
                    Orientation orientation1 = orientation.withFrontPreserveUp(direction);
                    level.neighborChanged(blockstate1, blockpos, this.wireBlock, orientation1, false);
                    if (blockstate1.isRedstoneConductor(level, blockpos)) {
                        for (Direction direction1 : orientation1.getDirections()) {
                            if (direction1 != direction.getOpposite()) {
                                level.neighborChanged(blockpos.relative(direction1), this.wireBlock, orientation1.withFrontPreserveUp(direction1));
                            }
                        }
                    }
                }
            }
        });
        if (level instanceof ServerLevel serverlevel && serverlevel.debugSynchronizers().hasAnySubscriberFor(DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS)
            )
         {
            this.updatedWires
                .forEach(
                    (p_449006_, p_449007_) -> serverlevel.debugSynchronizers()
                        .sendBlockValue(p_449006_, DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS, unpackOrientation(p_449007_))
                );
        }
    }

    private static boolean isConnected(BlockState state, Direction direction) {
        EnumProperty<RedstoneSide> enumproperty = RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(direction);
        return enumproperty == null ? direction == Direction.DOWN : state.getValue(enumproperty).isConnected();
    }

    private static Orientation getInitialOrientation(Level level, @Nullable Orientation p_orientation) {
        Orientation orientation;
        if (p_orientation != null) {
            orientation = p_orientation;
        } else {
            orientation = Orientation.random(level.random);
        }

        return orientation.withUp(Direction.UP).withSideBias(Orientation.SideBias.LEFT);
    }

    private void calculateCurrentChanges(Level level, BlockPos pos, Orientation p_orientation) {
        BlockState blockstate = level.getBlockState(pos);
        if (blockstate.is(this.wireBlock)) {
            this.setPower(pos, blockstate.getValue(RedStoneWireBlock.POWER), p_orientation);
            this.wiresToTurnOff.add(pos);
        } else {
            this.propagateChangeToNeighbors(level, pos, 0, p_orientation, true);
        }

        while (!this.wiresToTurnOff.isEmpty()) {
            BlockPos blockpos = this.wiresToTurnOff.removeFirst();
            int i = this.updatedWires.getInt(blockpos);
            Orientation orientation = unpackOrientation(i);
            int j = unpackPower(i);
            int k = this.getBlockSignal(level, blockpos);
            int l = this.getIncomingWireSignal(level, blockpos);
            int i1 = Math.max(k, l);
            int j1;
            if (i1 < j) {
                if (k > 0 && !this.wiresToTurnOn.contains(blockpos)) {
                    this.wiresToTurnOn.add(blockpos);
                }

                j1 = 0;
            } else {
                j1 = i1;
            }

            if (j1 != j) {
                this.setPower(blockpos, j1, orientation);
            }

            this.propagateChangeToNeighbors(level, blockpos, j1, orientation, j > i1);
        }

        while (!this.wiresToTurnOn.isEmpty()) {
            BlockPos blockpos1 = this.wiresToTurnOn.removeFirst();
            int k1 = this.updatedWires.getInt(blockpos1);
            int l1 = unpackPower(k1);
            int i2 = this.getBlockSignal(level, blockpos1);
            int j2 = this.getIncomingWireSignal(level, blockpos1);
            int k2 = Math.max(i2, j2);
            Orientation orientation1 = unpackOrientation(k1);
            if (k2 > l1) {
                this.setPower(blockpos1, k2, orientation1);
            } else if (k2 < l1) {
                throw new IllegalStateException("Turning off wire while trying to turn it on. Should not happen.");
            }

            this.propagateChangeToNeighbors(level, blockpos1, k2, orientation1, false);
        }
    }

    private static int packOrientationAndPower(Orientation orientation, int power) {
        return orientation.getIndex() << 4 | power;
    }

    private static Orientation unpackOrientation(int data) {
        return Orientation.fromIndex(data >> 4);
    }

    private static int unpackPower(int data) {
        return data & 15;
    }

    private void setPower(BlockPos pos, int power, Orientation orientation) {
        this.updatedWires
            .compute(
                pos,
                (p_362131_, p_363114_) -> p_363114_ == null
                    ? packOrientationAndPower(orientation, power)
                    : packOrientationAndPower(unpackOrientation(p_363114_), power)
            );
    }

    private void propagateChangeToNeighbors(Level level, BlockPos pos, int power, Orientation orientation, boolean canTurnOff) {
        for (Direction direction : orientation.getHorizontalDirections()) {
            BlockPos blockpos = pos.relative(direction);
            this.enqueueNeighborWire(level, blockpos, power, orientation.withFront(direction), canTurnOff);
        }

        for (Direction direction2 : orientation.getVerticalDirections()) {
            BlockPos blockpos3 = pos.relative(direction2);
            boolean flag = level.getBlockState(blockpos3).isRedstoneConductor(level, blockpos3);

            for (Direction direction1 : orientation.getHorizontalDirections()) {
                BlockPos blockpos1 = pos.relative(direction1);
                if (direction2 == Direction.UP && !flag) {
                    BlockPos blockpos4 = blockpos3.relative(direction1);
                    this.enqueueNeighborWire(level, blockpos4, power, orientation.withFront(direction1), canTurnOff);
                } else if (direction2 == Direction.DOWN && !level.getBlockState(blockpos1).isRedstoneConductor(level, blockpos1)) {
                    BlockPos blockpos2 = blockpos3.relative(direction1);
                    this.enqueueNeighborWire(level, blockpos2, power, orientation.withFront(direction1), canTurnOff);
                }
            }
        }
    }

    private void enqueueNeighborWire(Level level, BlockPos pos, int power, Orientation orientation, boolean canTurnOff) {
        BlockState blockstate = level.getBlockState(pos);
        if (blockstate.is(this.wireBlock)) {
            int i = this.getWireSignal(pos, blockstate);
            if (i < power - 1 && !this.wiresToTurnOn.contains(pos)) {
                this.wiresToTurnOn.add(pos);
                this.setPower(pos, i, orientation);
            }

            if (canTurnOff && i > power && !this.wiresToTurnOff.contains(pos)) {
                this.wiresToTurnOff.add(pos);
                this.setPower(pos, i, orientation);
            }
        }
    }

    @Override
    protected int getWireSignal(BlockPos pos, BlockState state) {
        int i = this.updatedWires.getOrDefault(pos, -1);
        return i != -1 ? unpackPower(i) : super.getWireSignal(pos, state);
    }
}
