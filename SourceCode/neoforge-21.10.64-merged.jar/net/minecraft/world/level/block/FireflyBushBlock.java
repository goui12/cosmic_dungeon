package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class FireflyBushBlock extends VegetationBlock implements BonemealableBlock {
    private static final double FIREFLY_CHANCE_PER_TICK = 0.7;
    private static final double FIREFLY_HORIZONTAL_RANGE = 10.0;
    private static final double FIREFLY_VERTICAL_RANGE = 5.0;
    private static final int FIREFLY_SPAWN_MAX_BRIGHTNESS_LEVEL = 13;
    private static final int FIREFLY_AMBIENT_SOUND_CHANCE_ONE_IN = 30;
    public static final MapCodec<FireflyBushBlock> CODEC = simpleCodec(FireflyBushBlock::new);

    public FireflyBushBlock(BlockBehaviour.Properties p_401237_) {
        super(p_401237_);
    }

    @Override
    protected MapCodec<? extends FireflyBushBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState p_401358_, Level p_401092_, BlockPos p_401110_, RandomSource p_401309_) {
        if (p_401309_.nextInt(30) == 0
            && p_401092_.isMoonVisible()
            && p_401092_.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p_401110_) <= p_401110_.getY()) {
            p_401092_.playLocalSound(p_401110_, SoundEvents.FIREFLY_BUSH_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false);
        }

        if (p_401092_.getMaxLocalRawBrightness(p_401110_) <= 13 && p_401309_.nextDouble() <= 0.7) {
            double d0 = p_401110_.getX() + p_401309_.nextDouble() * 10.0 - 5.0;
            double d1 = p_401110_.getY() + p_401309_.nextDouble() * 5.0;
            double d2 = p_401110_.getZ() + p_401309_.nextDouble() * 10.0 - 5.0;
            p_401092_.addParticle(ParticleTypes.FIREFLY, d0, d1, d2, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_401352_, BlockPos p_401332_, BlockState p_401436_) {
        return BonemealableBlock.hasSpreadableNeighbourPos(p_401352_, p_401332_, p_401436_);
    }

    @Override
    public boolean isBonemealSuccess(Level p_401120_, RandomSource p_401417_, BlockPos p_401298_, BlockState p_401423_) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel p_401415_, RandomSource p_401124_, BlockPos p_401112_, BlockState p_401260_) {
        BonemealableBlock.findSpreadableNeighbourPos(p_401415_, p_401112_, p_401260_)
            .ifPresent(p_415478_ -> p_401415_.setBlockAndUpdate(p_415478_, this.defaultBlockState()));
    }
}
