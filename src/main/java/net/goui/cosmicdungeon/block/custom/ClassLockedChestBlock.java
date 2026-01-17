// file: src/main/java/net/goui/cosmicdungeon/block/custom/ClassLockedChestBlock.java
package net.goui.cosmicdungeon.block.custom;

import net.goui.cosmicdungeon.block.entity.ClassLockedChestBlockEntity;
import net.goui.cosmicdungeon.block.entity.ModBlockEntities;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.Objects;

public class ClassLockedChestBlock extends Block implements EntityBlock, ClassLocked, SimpleWaterloggedBlock {

    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final String requiredClassId;
    private final SoundEvent openSound;
    private final SoundEvent closeSound;

    public ClassLockedChestBlock(Properties props, String requiredClassId, SoundEvent openSound, SoundEvent closeSound) {
        super(props);

        this.requiredClassId = ClassKeys.clamp(requiredClassId);
        this.openSound = Objects.requireNonNull(openSound);
        this.closeSound = Objects.requireNonNull(closeSound);

        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(WATERLOGGED, Boolean.FALSE)
        );
    }

    @Override
    public String requiredClassId() {
        return requiredClassId;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ClassLockedChestBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        if (!net.goui.cosmicdungeon.auth.AccessPolicy.isDeveloper(sp)) {
            String have = ClassNbtUtil.getClassId(sp);
            if (!requiredClassId.equals(have)) {
                sp.displayClientMessage(
                        Component.literal("Only " + requiredClassId + " can open this chest.")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return InteractionResult.CONSUME;
            }
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ClassLockedChestBlockEntity chestBe)) return InteractionResult.PASS;

        sp.openMenu(chestBe);
        return InteractionResult.CONSUME;
    }

    // ---- Waterlogging ----

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
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
        if (state.getValue(WATERLOGGED)) {
            scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // ---- Client animation tick only ----

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.CLASS_LOCKED_CHEST.get()) return null;

        if (level.isClientSide()) {
            return (lvl, pos, st, be) -> ClassLockedChestBlockEntity.clientTick(lvl, pos, st, (ClassLockedChestBlockEntity) be);
        }
        return null;
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.triggerEvent(id, param);
    }

    @Override
    @Nullable
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof MenuProvider mp ? mp : null;
    }

    // ---- Sounds ----

    public SoundEvent getOpenSound() {
        return openSound;
    }

    public SoundEvent getCloseSound() {
        return closeSound;
    }

    public static void playChestSound(Level level, BlockPos pos, SoundEvent sound) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 0.5F,
                level.random.nextFloat() * 0.1F + 0.9F);
    }
}
