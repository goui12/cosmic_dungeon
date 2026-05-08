package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class TintedParticleLeavesBlock extends LeavesBlock {
    public static final MapCodec<TintedParticleLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_432685_ -> p_432685_.group(
                ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("leaf_particle_chance").forGetter(p_399927_ -> p_399927_.leafParticleChance), propertiesCodec()
            )
            .apply(p_432685_, TintedParticleLeavesBlock::new)
    );

    public TintedParticleLeavesBlock(float p_400257_, BlockBehaviour.Properties p_399704_) {
        super(p_400257_, p_399704_);
    }

    @Override
    protected void spawnFallingLeavesParticle(Level p_399553_, BlockPos p_400280_, RandomSource p_400310_) {
        ColorParticleOption colorparticleoption = ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, p_399553_.getClientLeafTintColor(p_400280_));
        ParticleUtils.spawnParticleBelow(p_399553_, p_400280_, p_400310_, colorparticleoption);
    }

    @Override
    public MapCodec<? extends TintedParticleLeavesBlock> codec() {
        return CODEC;
    }
}
