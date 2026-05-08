package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FenceGateBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<FenceGateBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_308823_ -> p_308823_.group(WoodType.CODEC.optionalFieldOf("wood_type").forGetter(p_304842_ -> java.util.Optional.ofNullable(p_304842_.type)), propertiesCodec(),
                        net.minecraft.sounds.SoundEvent.DIRECT_CODEC.optionalFieldOf("open_sound").forGetter(fence -> java.util.Optional.of(fence.openSound).filter(s -> fence.type == null || s != fence.type.fenceGateOpen())),
                        net.minecraft.sounds.SoundEvent.DIRECT_CODEC.optionalFieldOf("close_sound").forGetter(fence -> java.util.Optional.of(fence.closeSound).filter(s -> fence.type == null || s != fence.type.fenceGateClose())))
                .apply(p_308823_, FenceGateBlock::new)
    );
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty IN_WALL = BlockStateProperties.IN_WALL;
    private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(Block.cube(16.0, 16.0, 4.0));
    private static final Map<Direction.Axis, VoxelShape> SHAPES_WALL = Maps.newEnumMap(
        Util.mapValues(SHAPES, p_393358_ -> Shapes.join(p_393358_, Block.column(16.0, 13.0, 16.0), BooleanOp.ONLY_FIRST))
    );
    private static final Map<Direction.Axis, VoxelShape> SHAPE_COLLISION = Shapes.rotateHorizontalAxis(Block.column(16.0, 4.0, 0.0, 24.0));
    private static final Map<Direction.Axis, VoxelShape> SHAPE_SUPPORT = Shapes.rotateHorizontalAxis(Block.column(16.0, 4.0, 5.0, 24.0));
    private static final Map<Direction.Axis, VoxelShape> SHAPE_OCCLUSION = Shapes.rotateHorizontalAxis(
        Shapes.or(Block.box(0.0, 5.0, 7.0, 2.0, 16.0, 9.0), Block.box(14.0, 5.0, 7.0, 16.0, 16.0, 9.0))
    );
    private static final Map<Direction.Axis, VoxelShape> SHAPE_OCCLUSION_WALL = Maps.newEnumMap(
        Util.mapValues(SHAPE_OCCLUSION, p_393359_ -> p_393359_.move(0.0, -0.1875, 0.0).optimize())
    );
    public final net.minecraft.sounds.SoundEvent openSound, closeSound;
    @org.jetbrains.annotations.Nullable
    private final WoodType type;

    @Override
    public MapCodec<FenceGateBlock> codec() {
        return CODEC;
    }

    public FenceGateBlock(WoodType type, BlockBehaviour.Properties properties) {
        this(java.util.Optional.of(type), properties.sound(type.soundType()), java.util.Optional.of(type.fenceGateOpen()), java.util.Optional.of(type.fenceGateClose()));
    }

    public FenceGateBlock(BlockBehaviour.Properties p_273352_, net.minecraft.sounds.SoundEvent openSound, net.minecraft.sounds.SoundEvent closeSound) {
        this(java.util.Optional.empty(), p_273352_, java.util.Optional.of(openSound), java.util.Optional.of(closeSound));
    }

    public FenceGateBlock(java.util.Optional<WoodType> p_273340_, BlockBehaviour.Properties p_273352_, java.util.Optional<net.minecraft.sounds.SoundEvent> openSound, java.util.Optional<net.minecraft.sounds.SoundEvent> closeSound) {
        super(p_273352_);
        com.google.common.base.Preconditions.checkArgument(p_273340_.isPresent() || (openSound.isPresent() && closeSound.isPresent()), "Fence gates must have sounds set");
        this.type = p_273340_.orElse(null);
        this.openSound = openSound.orElseGet(() -> type.fenceGateOpen()); // Type may be null, so we cannot do a method ref
        this.closeSound = closeSound.orElseGet(() -> type.fenceGateClose());
        this.registerDefaultState(this.stateDefinition.any().setValue(OPEN, false).setValue(POWERED, false).setValue(IN_WALL, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction.Axis direction$axis = state.getValue(FACING).getAxis();
        return (state.getValue(IN_WALL) ? SHAPES_WALL : SHAPES).get(direction$axis);
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess scheduledTickAccess,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        RandomSource random
    ) {
        Direction.Axis direction$axis = direction.getAxis();
        if (state.getValue(FACING).getClockWise().getAxis() != direction$axis) {
            return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
        } else {
            boolean flag = this.isWall(neighborState) || this.isWall(level.getBlockState(pos.relative(direction.getOpposite())));
            return state.setValue(IN_WALL, flag);
        }
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        Direction.Axis direction$axis = state.getValue(FACING).getAxis();
        return state.getValue(OPEN) ? Shapes.empty() : SHAPE_SUPPORT.get(direction$axis);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction.Axis direction$axis = state.getValue(FACING).getAxis();
        return state.getValue(OPEN) ? Shapes.empty() : SHAPE_COLLISION.get(direction$axis);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        Direction.Axis direction$axis = state.getValue(FACING).getAxis();
        return (state.getValue(IN_WALL) ? SHAPE_OCCLUSION_WALL : SHAPE_OCCLUSION).get(direction$axis);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        switch (pathComputationType) {
            case LAND:
                return state.getValue(OPEN);
            case WATER:
                return false;
            case AIR:
                return state.getValue(OPEN);
            default:
                return false;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        boolean flag = level.hasNeighborSignal(blockpos);
        Direction direction = context.getHorizontalDirection();
        Direction.Axis direction$axis = direction.getAxis();
        boolean flag1 = direction$axis == Direction.Axis.Z
                && (this.isWall(level.getBlockState(blockpos.west())) || this.isWall(level.getBlockState(blockpos.east())))
            || direction$axis == Direction.Axis.X && (this.isWall(level.getBlockState(blockpos.north())) || this.isWall(level.getBlockState(blockpos.south())));
        return this.defaultBlockState().setValue(FACING, direction).setValue(OPEN, flag).setValue(POWERED, flag).setValue(IN_WALL, flag1);
    }

    private boolean isWall(BlockState state) {
        return state.is(BlockTags.WALLS);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(OPEN)) {
            state = state.setValue(OPEN, false);
            level.setBlock(pos, state, 10);
        } else {
            Direction direction = player.getDirection();
            if (state.getValue(FACING) == direction.getOpposite()) {
                state = state.setValue(FACING, direction);
            }

            state = state.setValue(OPEN, true);
            level.setBlock(pos, state, 10);
        }

        boolean flag = state.getValue(OPEN);
        level.playSound(
            player,
            pos,
            flag ? openSound : closeSound,
            SoundSource.BLOCKS,
            1.0F,
            level.getRandom().nextFloat() * 0.1F + 0.9F
        );
        level.gameEvent(player, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onExplosionHit(
        BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> dropConsumer
    ) {
        if (explosion.canTriggerBlocks() && !state.getValue(POWERED)) {
            boolean flag = state.getValue(OPEN);
            level.setBlockAndUpdate(pos, state.setValue(OPEN, !flag));
            level.playSound(
                null,
                pos,
                flag ? closeSound : openSound,
                SoundSource.BLOCKS,
                1.0F,
                level.getRandom().nextFloat() * 0.1F + 0.9F
            );
            level.gameEvent(flag ? GameEvent.BLOCK_CLOSE : GameEvent.BLOCK_OPEN, pos, GameEvent.Context.of(state));
        }

        super.onExplosionHit(state, level, pos, explosion, dropConsumer);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) {
            boolean flag = level.hasNeighborSignal(pos);
            if (state.getValue(POWERED) != flag) {
                level.setBlock(pos, state.setValue(POWERED, flag).setValue(OPEN, flag), 2);
                if (state.getValue(OPEN) != flag) {
                    level.playSound(
                        null,
                        pos,
                        flag ? openSound : closeSound,
                        SoundSource.BLOCKS,
                        1.0F,
                        level.getRandom().nextFloat() * 0.1F + 0.9F
                    );
                    level.gameEvent(null, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POWERED, IN_WALL);
    }

    public static boolean connectsToDirection(BlockState state, Direction direction) {
        return state.getValue(FACING).getAxis() == direction.getClockWise().getAxis();
    }
}
