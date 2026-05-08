package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BushBlock extends VegetationBlock implements BonemealableBlock {
    public static final MapCodec<BushBlock> CODEC = simpleCodec(BushBlock::new);
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 13.0);

    @Override
    public MapCodec<BushBlock> codec() {
        return CODEC;
    }

    public BushBlock(BlockBehaviour.Properties p_51021_) {
        super(p_51021_);
    }

    @Override
    protected VoxelShape getShape(BlockState p_401432_, BlockGetter p_401175_, BlockPos p_401162_, CollisionContext p_401402_) {
        return SHAPE;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_401250_, BlockPos p_401361_, BlockState p_401191_) {
        return BonemealableBlock.hasSpreadableNeighbourPos(p_401250_, p_401361_, p_401191_);
    }

    @Override
    public boolean isBonemealSuccess(Level p_401200_, RandomSource p_401387_, BlockPos p_401374_, BlockState p_401380_) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel p_401091_, RandomSource p_401012_, BlockPos p_401218_, BlockState p_401130_) {
        BonemealableBlock.findSpreadableNeighbourPos(p_401091_, p_401218_, p_401130_)
            .ifPresent(p_415467_ -> p_401091_.setBlockAndUpdate(p_415467_, this.defaultBlockState()));
    }
}
