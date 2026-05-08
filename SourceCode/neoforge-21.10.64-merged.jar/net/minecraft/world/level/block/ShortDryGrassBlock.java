package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.sounds.AmbientDesertBlockSoundsPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShortDryGrassBlock extends DryVegetationBlock implements BonemealableBlock {
    public static final MapCodec<ShortDryGrassBlock> CODEC = simpleCodec(ShortDryGrassBlock::new);
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 10.0);

    @Override
    public MapCodec<ShortDryGrassBlock> codec() {
        return CODEC;
    }

    public ShortDryGrassBlock(BlockBehaviour.Properties p_401946_) {
        super(p_401946_);
    }

    @Override
    protected VoxelShape getShape(BlockState p_401780_, BlockGetter p_401807_, BlockPos p_401895_, CollisionContext p_401802_) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState p_416439_, Level p_415933_, BlockPos p_416136_, RandomSource p_415976_) {
        AmbientDesertBlockSoundsPlayer.playAmbientDryGrassSounds(p_415933_, p_416136_, p_415976_);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_401814_, BlockPos p_401760_, BlockState p_401924_) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level p_401806_, RandomSource p_401772_, BlockPos p_401791_, BlockState p_401942_) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel p_401950_, RandomSource p_401831_, BlockPos p_401948_, BlockState p_401868_) {
        p_401950_.setBlockAndUpdate(p_401948_, Blocks.TALL_DRY_GRASS.defaultBlockState());
    }
}
