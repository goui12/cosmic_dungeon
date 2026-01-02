package net.goui.cosmicdungeon.redstone.rf;

import net.goui.cosmicdungeon.auth.Authority;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
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

    private static final VoxelShape SHAPE_HALF_BLOCK = Block.box(0, 0, 0, 16, 8, 16);

    public RedstoneTransmitterBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, Boolean.FALSE));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE_HALF_BLOCK;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE_HALF_BLOCK;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(POWERED);
    }

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

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction side) {
        return state.getValue(POWERED) ? 15 : 0;
    }

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

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                if (!net.goui.cosmicdungeon.auth.AccessPolicy.isDeveloper(sp)) {
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.SUCCESS;
        }

        HzConfigScreen.openForTransmitter(pos);
        return InteractionResult.SUCCESS;
    }


    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
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
        var be = level.getBlockEntity(pos);
        if (be instanceof RedstoneTransmitterBE tbe) {
            boolean wasPowered = state.hasProperty(POWERED) && state.getValue(POWERED);
            if (wasPowered) RfBusManager.get(level).removeActive(level, tbe.getHz());
        }
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }
}
