package net.goui.cosmicdungeon.redstone.rf;

import net.goui.cosmicdungeon.auth.Authority;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.ScheduledTickAccess;

public class RedstoneReceiverBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShape SHAPE_N = Block.box(1,  2, 14, 7,  12, 16);
    private static final VoxelShape SHAPE_S = Block.box(9, 2, 0,  15, 12,  2);
    private static final VoxelShape SHAPE_W = Block.box(14, 2, 9, 16, 12, 15);
    private static final VoxelShape SHAPE_E = Block.box(0,  2, 1,  2,  12, 7);
    private static final VoxelShape SHAPE_U = Block.box(1, 0, 1, 7, 2, 14);
    private static final VoxelShape SHAPE_D = Block.box(1, 14, 3, 7, 16, 15);

    public RedstoneReceiverBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(FACING, ctx.getClickedFace())
                .setValue(POWERED, Boolean.FALSE);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction face = state.getValue(FACING);
        BlockPos behind = pos.relative(face.getOpposite());
        return level.getBlockState(behind).isFaceSturdy(level, behind, face);
    }

    @Override
    protected BlockState updateShape(BlockState state,
                                     LevelReader level,
                                     ScheduledTickAccess tickAccess,
                                     BlockPos pos,
                                     Direction dir,
                                     BlockPos neighborPos,
                                     BlockState neighborState,
                                     RandomSource rand) {
        if (!this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, tickAccess, pos, dir, neighborPos, neighborState, rand);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_N;
            case SOUTH -> SHAPE_S;
            case WEST  -> SHAPE_W;
            case EAST  -> SHAPE_E;
            case UP    -> SHAPE_U;
            case DOWN  -> SHAPE_D;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneReceiverBE(pos, state);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction toSide) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction toSide) {
        if (!state.getValue(POWERED)) return 0;
        // Match button/lever-like behavior: strongly power the block this receiver is attached to.
        return toSide == state.getValue(FACING).getOpposite() ? 15 : 0;
    }

    void setPowered(Level level, BlockPos pos, BlockState state, boolean powered) {
        if (state.getValue(POWERED) == powered) return;

        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);

        Direction outward = state.getValue(FACING);
        BlockPos outwardPos = pos.relative(outward);
        BlockPos attachedPos = pos.relative(outward.getOpposite());

        // Notify both front and attached blocks so vanilla redstone reevaluates like a lever/button update.
        level.updateNeighborsAt(outwardPos, this);
        level.updateNeighborsAt(attachedPos, this);
        level.updateNeighborsAt(pos, this);
        level.updateNeighbourForOutputSignal(pos, this);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (level.isClientSide()) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RedstoneReceiverBE rbe) {
            rbe.register((net.minecraft.server.level.ServerLevel) level);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean moved) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RedstoneReceiverBE rbe) {
            rbe.unregister(level);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RedstoneReceiverBE rbe) {
                rbe.unregister((net.minecraft.server.level.ServerLevel) level);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Always block on server (authoritative gate)
        if (!level.isClientSide()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                if (!net.goui.cosmicdungeon.auth.AccessPolicy.isDeveloper(sp)) {
                    // Optional: just deny silently, since DeviceAccessEvents already messages
                    // sp.displayClientMessage(Component.literal("You do not have permission to configure RF.").withStyle(ChatFormatting.RED), true);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Client side: only open if this player is allowed
        // NOTE: client does not know rank reliably, so we rely on server to refuse packets.
        // But to prevent "GUI flash" for dungeoneers, you can still do a lightweight check:
        // If you want ZERO client assumptions, remove this and let server refuse packets.
        HzConfigScreen.openForReceiver(pos);
        return InteractionResult.SUCCESS;
    }

}
