package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class TestInstanceBlock extends BaseEntityBlock implements GameMasterBlock {
    public static final MapCodec<TestInstanceBlock> CODEC = simpleCodec(TestInstanceBlock::new);

    public TestInstanceBlock(BlockBehaviour.Properties p_397840_) {
        super(p_397840_);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_397973_, BlockState p_397522_) {
        return new TestInstanceBlockEntity(p_397973_, p_397522_);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_397015_, Level p_397062_, BlockPos p_397232_, Player p_397809_, BlockHitResult p_397307_) {
        if (p_397062_.getBlockEntity(p_397232_) instanceof TestInstanceBlockEntity testinstanceblockentity) {
            if (!p_397809_.canUseGameMasterBlocks()) {
                return InteractionResult.PASS;
            } else {
                if (p_397809_.level().isClientSide()) {
                    p_397809_.openTestInstanceBlock(testinstanceblockentity);
                }

                return InteractionResult.SUCCESS;
            }
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    protected MapCodec<TestInstanceBlock> codec() {
        return CODEC;
    }
}
