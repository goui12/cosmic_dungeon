package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.BlockHitResult;

public class PumpkinBlock extends Block {
    public static final MapCodec<PumpkinBlock> CODEC = simpleCodec(PumpkinBlock::new);

    @Override
    public MapCodec<PumpkinBlock> codec() {
        return CODEC;
    }

    public PumpkinBlock(BlockBehaviour.Properties p_55284_) {
        super(p_55284_);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_316383_, BlockState p_316676_, Level p_316272_, BlockPos p_316484_, Player p_316367_, InteractionHand p_316216_, BlockHitResult p_316827_
    ) {
        if (!p_316383_.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHEARS_CARVE)) {
            return super.useItemOn(p_316383_, p_316676_, p_316272_, p_316484_, p_316367_, p_316216_, p_316827_);
        } else if (p_316272_ instanceof ServerLevel serverlevel) {
            Direction direction = p_316827_.getDirection();
            Direction direction1 = direction.getAxis() == Direction.Axis.Y ? p_316367_.getDirection().getOpposite() : direction;
            dropFromBlockInteractLootTable(
                serverlevel,
                BuiltInLootTables.CARVE_PUMPKIN,
                p_316676_,
                p_316272_.getBlockEntity(p_316484_),
                p_316383_,
                p_316367_,
                (p_433465_, p_433891_) -> {
                    ItemEntity itementity = new ItemEntity(
                        p_316272_,
                        p_316484_.getX() + 0.5 + direction1.getStepX() * 0.65,
                        p_316484_.getY() + 0.1,
                        p_316484_.getZ() + 0.5 + direction1.getStepZ() * 0.65,
                        p_433891_
                    );
                    itementity.setDeltaMovement(
                        0.05 * direction1.getStepX() + p_316272_.random.nextDouble() * 0.02,
                        0.05,
                        0.05 * direction1.getStepZ() + p_316272_.random.nextDouble() * 0.02
                    );
                    p_316272_.addFreshEntity(itementity);
                }
            );
            p_316272_.playSound(null, p_316484_, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, 1.0F);
            p_316272_.setBlock(p_316484_, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, direction1), 11);
            p_316383_.hurtAndBreak(1, p_316367_, p_316216_.asEquipmentSlot());
            p_316272_.gameEvent(p_316367_, GameEvent.SHEAR, p_316484_);
            p_316367_.awardStat(Stats.ITEM_USED.get(Items.SHEARS));
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.SUCCESS;
        }
    }
}
