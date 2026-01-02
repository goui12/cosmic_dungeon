package net.goui.cosmicdungeon.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SpectralBloomBlock extends BushBlock {

    public SpectralBloomBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rng) {
        if (!level.isClientSide()) return;

        double baseX = pos.getX() + 0.5;
        double baseY = pos.getY() + 0.15;
        double baseZ = pos.getZ() + 0.5;

        // Soul fire flames rising from the center
        for (int i = 0; i < 6; i++) {
            double ox = (rng.nextDouble() - 0.5) * 0.35;
            double oy = rng.nextDouble() * 0.60;
            double oz = (rng.nextDouble() - 0.5) * 0.35;

            double vx = (rng.nextDouble() - 0.5) * 0.01;
            double vy = 0.02 + rng.nextDouble() * 0.02;
            double vz = (rng.nextDouble() - 0.5) * 0.01;

            level.addParticle(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    baseX + ox, baseY + oy, baseZ + oz,
                    vx, vy, vz
            );
        }

        // Enchant sparkle halo
        for (int i = 0; i < 3; i++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double radius = 0.35 + rng.nextDouble() * 0.15;

            double x = baseX + Math.cos(angle) * radius;
            double z = baseZ + Math.sin(angle) * radius;
            double y = baseY + 0.25 + rng.nextDouble() * 0.55;

            level.addParticle(
                    ParticleTypes.ENCHANT,
                    x, y, z,
                    0.0, 0.02 + rng.nextDouble() * 0.02, 0.0
            );
        }

        // Rare extra pop
        if (rng.nextInt(10) == 0) {
            level.addParticle(
                    ParticleTypes.SCULK_SOUL,
                    baseX + (rng.nextDouble() - 0.5) * 0.2,
                    baseY + 0.35 + rng.nextDouble() * 0.5,
                    baseZ + (rng.nextDouble() - 0.5) * 0.2,
                    0.0, 0.02, 0.0
            );
        }

        // Occasional chime
        if (rng.nextInt(120) == 0) {
            level.playLocalSound(
                    baseX, baseY, baseZ,
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS,
                    0.35f,
                    0.75f + rng.nextFloat() * 0.25f,
                    false
            );
        }
    }
}
