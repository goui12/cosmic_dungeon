package net.minecraft.world.level.block;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SideChainPart;

public interface SideChainPartBlock {
    SideChainPart getSideChainPart(BlockState state);

    BlockState setSideChainPart(BlockState state, SideChainPart sideChainPart);

    Direction getFacing(BlockState state);

    boolean isConnectable(BlockState state);

    int getMaxChainLength();

    default List<BlockPos> getAllBlocksConnectedTo(LevelAccessor level, BlockPos pos) {
        BlockState blockstate = level.getBlockState(pos);
        if (!this.isConnectable(blockstate)) {
            return List.of();
        } else {
            SideChainPartBlock.Neighbors sidechainpartblock$neighbors = this.getNeighbors(level, pos, this.getFacing(blockstate));
            List<BlockPos> list = new LinkedList<>();
            list.add(pos);
            this.addBlocksConnectingTowards(sidechainpartblock$neighbors::left, SideChainPart.LEFT, list::addFirst);
            this.addBlocksConnectingTowards(sidechainpartblock$neighbors::right, SideChainPart.RIGHT, list::addLast);
            return list;
        }
    }

    private void addBlocksConnectingTowards(IntFunction<SideChainPartBlock.Neighbor> neighborGetter, SideChainPart part, Consumer<BlockPos> output) {
        for (int i = 1; i < this.getMaxChainLength(); i++) {
            SideChainPartBlock.Neighbor sidechainpartblock$neighbor = neighborGetter.apply(i);
            if (sidechainpartblock$neighbor.connectsTowards(part)) {
                output.accept(sidechainpartblock$neighbor.pos());
            }

            if (sidechainpartblock$neighbor.isUnconnectableOrChainEnd()) {
                break;
            }
        }
    }

    default void updateNeighborsAfterPoweringDown(LevelAccessor level, BlockPos pos, BlockState state) {
        SideChainPartBlock.Neighbors sidechainpartblock$neighbors = this.getNeighbors(level, pos, this.getFacing(state));
        sidechainpartblock$neighbors.left().disconnectFromRight();
        sidechainpartblock$neighbors.right().disconnectFromLeft();
    }

    default void updateSelfAndNeighborsOnPoweringUp(LevelAccessor level, BlockPos pos, BlockState state, BlockState oldState) {
        if (this.isConnectable(state)) {
            if (!this.isBeingUpdatedByNeighbor(state, oldState)) {
                SideChainPartBlock.Neighbors sidechainpartblock$neighbors = this.getNeighbors(level, pos, this.getFacing(state));
                SideChainPart sidechainpart = SideChainPart.UNCONNECTED;
                int i = sidechainpartblock$neighbors.left().isConnectable()
                    ? this.getAllBlocksConnectedTo(level, sidechainpartblock$neighbors.left().pos()).size()
                    : 0;
                int j = sidechainpartblock$neighbors.right().isConnectable()
                    ? this.getAllBlocksConnectedTo(level, sidechainpartblock$neighbors.right().pos()).size()
                    : 0;
                int k = 1;
                if (this.canConnect(i, k)) {
                    sidechainpart = sidechainpart.whenConnectedToTheLeft();
                    sidechainpartblock$neighbors.left().connectToTheRight();
                    k += i;
                }

                if (this.canConnect(j, k)) {
                    sidechainpart = sidechainpart.whenConnectedToTheRight();
                    sidechainpartblock$neighbors.right().connectToTheLeft();
                }

                this.setPart(level, pos, sidechainpart);
            }
        }
    }

    private boolean canConnect(int segmentLength, int currentChainLength) {
        return segmentLength > 0 && currentChainLength + segmentLength <= this.getMaxChainLength();
    }

    private boolean isBeingUpdatedByNeighbor(BlockState state, BlockState oldState) {
        boolean flag = this.getSideChainPart(state).isConnected();
        boolean flag1 = this.isConnectable(oldState) && this.getSideChainPart(oldState).isConnected();
        return flag || flag1;
    }

    private SideChainPartBlock.Neighbors getNeighbors(LevelAccessor level, BlockPos pos, Direction facing) {
        return new SideChainPartBlock.Neighbors(this, level, facing, pos, new HashMap<>());
    }

    default void setPart(LevelAccessor level, BlockPos pos, SideChainPart part) {
        BlockState blockstate = level.getBlockState(pos);
        if (this.getSideChainPart(blockstate) != part) {
            level.setBlock(pos, this.setSideChainPart(blockstate, part), 3);
        }
    }

    public record EmptyNeighbor(BlockPos pos) implements SideChainPartBlock.Neighbor {
        @Override
        public boolean isConnectable() {
            return false;
        }

        @Override
        public boolean isUnconnectableOrChainEnd() {
            return true;
        }

        @Override
        public boolean connectsTowards(SideChainPart p_433702_) {
            return false;
        }
    }

    public sealed interface Neighbor permits SideChainPartBlock.EmptyNeighbor, SideChainPartBlock.SideChainNeighbor {
        BlockPos pos();

        boolean isConnectable();

        boolean isUnconnectableOrChainEnd();

        boolean connectsTowards(SideChainPart part);

        default void connectToTheRight() {
        }

        default void connectToTheLeft() {
        }

        default void disconnectFromRight() {
        }

        default void disconnectFromLeft() {
        }
    }

    public record Neighbors(SideChainPartBlock block, LevelAccessor level, Direction facing, BlockPos center, Map<BlockPos, SideChainPartBlock.Neighbor> cache) {
        private boolean isConnectableToThisBlock(BlockState state) {
            return this.block.isConnectable(state) && this.block.getFacing(state) == this.facing;
        }

        private SideChainPartBlock.Neighbor createNewNeighbor(BlockPos pos) {
            BlockState blockstate = this.level.getBlockState(pos);
            SideChainPart sidechainpart = this.isConnectableToThisBlock(blockstate) ? this.block.getSideChainPart(blockstate) : null;
            return (SideChainPartBlock.Neighbor)(sidechainpart == null
                ? new SideChainPartBlock.EmptyNeighbor(pos)
                : new SideChainPartBlock.SideChainNeighbor(this.level, this.block, pos, sidechainpart));
        }

        private SideChainPartBlock.Neighbor getOrCreateNeighbor(Direction direction, Integer distance) {
            return this.cache.computeIfAbsent(this.center.relative(direction, distance), this::createNewNeighbor);
        }

        public SideChainPartBlock.Neighbor left(int distance) {
            return this.getOrCreateNeighbor(this.facing.getClockWise(), distance);
        }

        public SideChainPartBlock.Neighbor right(int distance) {
            return this.getOrCreateNeighbor(this.facing.getCounterClockWise(), distance);
        }

        public SideChainPartBlock.Neighbor left() {
            return this.left(1);
        }

        public SideChainPartBlock.Neighbor right() {
            return this.right(1);
        }
    }

    public record SideChainNeighbor(LevelAccessor level, SideChainPartBlock block, BlockPos pos, SideChainPart part) implements SideChainPartBlock.Neighbor {
        @Override
        public boolean isConnectable() {
            return true;
        }

        @Override
        public boolean isUnconnectableOrChainEnd() {
            return this.part.isChainEnd();
        }

        @Override
        public boolean connectsTowards(SideChainPart p_434316_) {
            return this.part.isConnectionTowards(p_434316_);
        }

        @Override
        public void connectToTheRight() {
            this.block.setPart(this.level, this.pos, this.part.whenConnectedToTheRight());
        }

        @Override
        public void connectToTheLeft() {
            this.block.setPart(this.level, this.pos, this.part.whenConnectedToTheLeft());
        }

        @Override
        public void disconnectFromRight() {
            this.block.setPart(this.level, this.pos, this.part.whenDisconnectedFromTheRight());
        }

        @Override
        public void disconnectFromLeft() {
            this.block.setPart(this.level, this.pos, this.part.whenDisconnectedFromTheLeft());
        }
    }
}
