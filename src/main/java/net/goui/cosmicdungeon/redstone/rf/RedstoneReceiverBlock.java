package net.goui.cosmicdungeon.redstone.rf;

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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.ScheduledTickAccess;


public class RedstoneReceiverBlock extends Block implements EntityBlock {
    // 1.21.9: EnumProperty<Direction>
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    // --- Thin “plate” on each face (adjust numbers to match your model) ---
// Offset + reshape for a smaller rectangle in the bottom-right corner

    private static final VoxelShape SHAPE_N = Block.box(1,  2, 14, 7,  12, 16); // north (z max)
    private static final VoxelShape SHAPE_S = Block.box(9, 2, 0,  15, 12,  2); // south (z min)
    private static final VoxelShape SHAPE_W = Block.box(14, 2, 9, 16, 12, 15); // west  (x max)
    private static final VoxelShape SHAPE_E = Block.box(0,  2, 1,  2,  12, 7); // east  (x min)
    private static final VoxelShape SHAPE_U = Block.box(1, 0, 1, 7, 2, 14); // up    (y min, ceiling)
    private static final VoxelShape SHAPE_D = Block.box(1, 14, 3, 7, 16, 15); // down  (y max, floor)



    public RedstoneReceiverBlock(BlockBehaviour.Properties props) {
        // Important: pass a non-occluding property set when you create/register this block:
        // e.g., in ModBlocks: Properties.of().noOcclusion()...
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, POWERED);
    }

    // Place on the face the player clicked
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(FACING, ctx.getClickedFace())
                .setValue(POWERED, Boolean.FALSE);
    }

    // Stay attached to a sturdy face; break if support goes away
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
        // If our support is gone, replace with air; otherwise, keep vanilla behavior
        if (!this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, tickAccess, pos, dir, neighborPos, neighborState, rand);
    }



    // Make outline/collision match the real geometry (no full block!)
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

    /* ----- Redstone emission (only into the attached face) ----- */

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction toSide) {
        if (!state.getValue(POWERED)) return 0;
        return (toSide == state.getValue(FACING)) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction toSide) {
        return getSignal(state, level, pos, toSide);
    }

    /* ----- Update neighbors when our powered state changes ----- */

    void setPowered(Level level, BlockPos pos, BlockState state, boolean powered) {
        if (state.getValue(POWERED) == powered) return;

        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);

        Direction toward = state.getValue(FACING);
        BlockPos target = pos.relative(toward);

        level.updateNeighborsAt(target, this);
        level.updateNeighborsAt(pos, this);
        level.updateNeighbourForOutputSignal(pos, this);
    }

    /* ----- Lifetime hooks to maintain the receiver index ----- */

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
    }

    /* ----- Right-click UI ----- */

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            HzConfigScreen.openForReceiver(pos);
        }
        return InteractionResult.SUCCESS;
    }
}
