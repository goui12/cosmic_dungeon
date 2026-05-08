package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CactusFlowerBlock extends VegetationBlock {
    public static final MapCodec<CactusFlowerBlock> CODEC = simpleCodec(CactusFlowerBlock::new);
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 12.0);

    @Override
    public MapCodec<? extends CactusFlowerBlock> codec() {
        return CODEC;
    }

    public CactusFlowerBlock(BlockBehaviour.Properties p_401939_) {
        super(p_401939_);
    }

    @Override
    protected VoxelShape getShape(BlockState p_401923_, BlockGetter p_401936_, BlockPos p_401926_, CollisionContext p_401860_) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState p_401846_, BlockGetter p_401825_, BlockPos p_401934_) {
        BlockState blockstate = p_401825_.getBlockState(p_401934_);
        return blockstate.is(Blocks.CACTUS)
            || blockstate.is(Blocks.FARMLAND)
            || blockstate.isFaceSturdy(p_401825_, p_401934_, Direction.UP, SupportType.CENTER);
    }
}
