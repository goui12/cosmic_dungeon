package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DecoratedPotBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<DecoratedPotBlock> CODEC = simpleCodec(DecoratedPotBlock::new);
    public static final ResourceLocation SHERDS_DYNAMIC_DROP_ID = ResourceLocation.withDefaultNamespace("sherds");
    public static final EnumProperty<Direction> HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CRACKED = BlockStateProperties.CRACKED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

    @Override
    public MapCodec<DecoratedPotBlock> codec() {
        return CODEC;
    }

    public DecoratedPotBlock(BlockBehaviour.Properties p_273064_) {
        super(p_273064_);
        this.registerDefaultState(this.stateDefinition.any().setValue(HORIZONTAL_FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(CRACKED, false));
    }

    @Override
    protected BlockState updateShape(
        BlockState p_276307_,
        LevelReader p_374037_,
        ScheduledTickAccess p_374267_,
        BlockPos p_276270_,
        Direction p_276322_,
        BlockPos p_276312_,
        BlockState p_276280_,
        RandomSource p_374464_
    ) {
        if (p_276307_.getValue(WATERLOGGED)) {
            p_374267_.scheduleTick(p_276270_, Fluids.WATER, Fluids.WATER.getTickDelay(p_374037_));
        }

        return super.updateShape(p_276307_, p_374037_, p_374267_, p_276270_, p_276322_, p_276312_, p_276280_, p_374464_);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_272711_) {
        FluidState fluidstate = p_272711_.getLevel().getFluidState(p_272711_.getClickedPos());
        return this.defaultBlockState()
            .setValue(HORIZONTAL_FACING, p_272711_.getHorizontalDirection())
            .setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER)
            .setValue(CRACKED, false);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_316569_, BlockState p_316562_, Level p_316177_, BlockPos p_316898_, Player p_316632_, InteractionHand p_316424_, BlockHitResult p_316345_
    ) {
        if (p_316177_.getBlockEntity(p_316898_) instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
            if (p_316177_.isClientSide()) {
                return InteractionResult.SUCCESS;
            } else {
                ItemStack itemstack1 = decoratedpotblockentity.getTheItem();
                if (!p_316569_.isEmpty()
                    && (
                        itemstack1.isEmpty()
                            || ItemStack.isSameItemSameComponents(itemstack1, p_316569_) && itemstack1.getCount() < itemstack1.getMaxStackSize()
                    )) {
                    decoratedpotblockentity.wobble(DecoratedPotBlockEntity.WobbleStyle.POSITIVE);
                    p_316632_.awardStat(Stats.ITEM_USED.get(p_316569_.getItem()));
                    ItemStack itemstack = p_316569_.consumeAndReturn(1, p_316632_);
                    float f;
                    if (decoratedpotblockentity.isEmpty()) {
                        decoratedpotblockentity.setTheItem(itemstack);
                        f = (float)itemstack.getCount() / itemstack.getMaxStackSize();
                    } else {
                        itemstack1.grow(1);
                        f = (float)itemstack1.getCount() / itemstack1.getMaxStackSize();
                    }

                    p_316177_.playSound(null, p_316898_, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 1.0F, 0.7F + 0.5F * f);
                    if (p_316177_ instanceof ServerLevel serverlevel) {
                        serverlevel.sendParticles(
                            ParticleTypes.DUST_PLUME, p_316898_.getX() + 0.5, p_316898_.getY() + 1.2, p_316898_.getZ() + 0.5, 7, 0.0, 0.0, 0.0, 0.0
                        );
                    }

                    decoratedpotblockentity.setChanged();
                    p_316177_.gameEvent(p_316632_, GameEvent.BLOCK_CHANGE, p_316898_);
                    return InteractionResult.SUCCESS;
                } else {
                    return InteractionResult.TRY_WITH_EMPTY_HAND;
                }
            }
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_316866_, Level p_316544_, BlockPos p_316541_, Player p_316732_, BlockHitResult p_316860_) {
        if (p_316544_.getBlockEntity(p_316541_) instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
            p_316544_.playSound(null, p_316541_, SoundEvents.DECORATED_POT_INSERT_FAIL, SoundSource.BLOCKS, 1.0F, 1.0F);
            decoratedpotblockentity.wobble(DecoratedPotBlockEntity.WobbleStyle.NEGATIVE);
            p_316544_.gameEvent(p_316732_, GameEvent.BLOCK_CHANGE, p_316541_);
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    protected boolean isPathfindable(BlockState p_276295_, PathComputationType p_276303_) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState p_273112_, BlockGetter p_273055_, BlockPos p_273137_, CollisionContext p_273151_) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_273169_) {
        p_273169_.add(HORIZONTAL_FACING, WATERLOGGED, CRACKED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_273396_, BlockState p_272674_) {
        return new DecoratedPotBlockEntity(p_273396_, p_272674_);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_394575_, ServerLevel p_393957_, BlockPos p_393972_, boolean p_393685_) {
        Containers.updateNeighboursAfterDestroy(p_394575_, p_393957_, p_393972_);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState p_287683_, LootParams.Builder p_287582_) {
        BlockEntity blockentity = p_287582_.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockentity instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
            p_287582_.withDynamicDrop(SHERDS_DYNAMIC_DROP_ID, p_330132_ -> {
                for (Item item : decoratedpotblockentity.getDecorations().ordered()) {
                    p_330132_.accept(item.getDefaultInstance());
                }
            });
        }

        return super.getDrops(p_287683_, p_287582_);
    }

    @Override
    public BlockState playerWillDestroy(Level p_273590_, BlockPos p_273343_, BlockState p_272869_, Player p_273002_) {
        ItemStack itemstack = p_273002_.getMainHandItem();
        BlockState blockstate = p_272869_;
        if (itemstack.is(ItemTags.BREAKS_DECORATED_POTS) && !EnchantmentHelper.hasTag(itemstack, EnchantmentTags.PREVENTS_DECORATED_POT_SHATTERING)) {
            blockstate = p_272869_.setValue(CRACKED, true);
            p_273590_.setBlock(p_273343_, blockstate, 260);
        }

        return super.playerWillDestroy(p_273590_, p_273343_, blockstate, p_273002_);
    }

    @Override
    protected FluidState getFluidState(BlockState p_272593_) {
        return p_272593_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_272593_);
    }

    @Override
    protected SoundType getSoundType(BlockState p_277561_) {
        return p_277561_.getValue(CRACKED) ? SoundType.DECORATED_POT_CRACKED : SoundType.DECORATED_POT;
    }

    @Override
    protected void onProjectileHit(Level p_306322_, BlockState p_306005_, BlockHitResult p_306105_, Projectile p_305851_) {
        BlockPos blockpos = p_306105_.getBlockPos();
        if (p_306322_ instanceof ServerLevel serverlevel && p_305851_.mayInteract(serverlevel, blockpos) && p_305851_.mayBreak(serverlevel)) {
            p_306322_.setBlock(blockpos, p_306005_.setValue(CRACKED, true), 260);
            p_306322_.destroyBlock(blockpos, true, p_305851_);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader p_304622_, BlockPos p_294412_, BlockState p_294723_, boolean p_387769_) {
        if (p_304622_.getBlockEntity(p_294412_) instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
            PotDecorations potdecorations = decoratedpotblockentity.getDecorations();
            return DecoratedPotBlockEntity.createDecoratedPotItem(potdecorations);
        } else {
            return super.getCloneItemStack(p_304622_, p_294412_, p_294723_, p_387769_);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState p_305995_) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_306206_, Level p_306113_, BlockPos p_306305_, Direction p_434577_) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(p_306113_.getBlockEntity(p_306305_));
    }

    @Override
    protected BlockState rotate(BlockState p_333895_, Rotation p_333806_) {
        return p_333895_.setValue(HORIZONTAL_FACING, p_333806_.rotate(p_333895_.getValue(HORIZONTAL_FACING)));
    }

    @Override
    protected BlockState mirror(BlockState p_334078_, Mirror p_333905_) {
        return p_334078_.rotate(p_333905_.getRotation(p_334078_.getValue(HORIZONTAL_FACING)));
    }
}
