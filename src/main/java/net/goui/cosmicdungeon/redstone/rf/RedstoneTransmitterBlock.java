package net.goui.cosmicdungeon.redstone.rf;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RedstoneTransmitterBlock extends Block implements EntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public RedstoneTransmitterBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, Boolean.FALSE));
    }
    // inside RedstoneTransmitterBlock
    private static final VoxelShape SHAPE_HALF_BLOCK = Block.box(0, 0, 0, 16, 8, 16); // 0..8 = half-height

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE_HALF_BLOCK; // outline / “hitbox”
    }

    // RedstoneTransmitterBlock.java

// RedstoneTransmitterBlock.java

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Called BEFORE the block becomes air; old state is valid here.
        if (!level.isClientSide()) {
            var be = level.getBlockEntity(pos);
            if (be instanceof RedstoneTransmitterBE tbe) {
                boolean wasPowered = state.hasProperty(POWERED) && state.getValue(POWERED);
                if (wasPowered) {
                    RfBusManager.get((net.minecraft.server.level.ServerLevel) level)
                            .removeActive((net.minecraft.server.level.ServerLevel) level, tbe.getHz());
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state,
                                               net.minecraft.server.level.ServerLevel level,
                                               BlockPos pos,
                                               boolean moved) {
        // Fallback path (pistons/support loss/commands). BE may or may not still exist here.
        var be = level.getBlockEntity(pos);
        if (be instanceof RedstoneTransmitterBE tbe) {
            boolean wasPowered = state.hasProperty(POWERED) && state.getValue(POWERED);
            if (wasPowered) RfBusManager.get(level).removeActive(level, tbe.getHz());
        }
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }


    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE_HALF_BLOCK; // physical collision
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(POWERED);
    }

    // Placement doesn’t need facing; keep default state
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneTransmitterBE(pos, state);
    }

    /* ----- Redstone I/O ----- */

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction side) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    // 1.21.9 signature includes Orientation + movedByPiston
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation ori, boolean movedByPiston) {
        if (level.isClientSide()) return;

        boolean now = level.hasNeighborSignal(pos);
        boolean was = state.getValue(POWERED);
        if (now != was) {
            level.setBlock(pos, state.setValue(POWERED, now), Block.UPDATE_ALL);
            var be = level.getBlockEntity(pos);
            if (be instanceof RedstoneTransmitterBE tbe) {
                tbe.onPowerChanged((net.minecraft.server.level.ServerLevel) level, now);
            }
            level.updateNeighbourForOutputSignal(pos, this);
            level.updateNeighborsAt(pos, this);
        }
        super.neighborChanged(state, level, pos, neighborBlock, ori, movedByPiston);
    }

    /* ----- Right-click: open tiny Hz screen (client-only UI) ----- */

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            HzConfigScreen.openForTransmitter(pos);
        }
        return InteractionResult.SUCCESS;
    }
}
