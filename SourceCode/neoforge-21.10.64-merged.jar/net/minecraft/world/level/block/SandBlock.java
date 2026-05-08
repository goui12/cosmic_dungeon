package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.sounds.AmbientDesertBlockSoundsPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SandBlock extends ColoredFallingBlock {
    public static final MapCodec<SandBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_432676_ -> p_432676_.group(ColorRGBA.CODEC.fieldOf("falling_dust_color").forGetter(p_401097_ -> p_401097_.dustColor), propertiesCodec())
            .apply(p_432676_, SandBlock::new)
    );

    @Override
    public MapCodec<SandBlock> codec() {
        return CODEC;
    }

    public SandBlock(ColorRGBA p_401085_, BlockBehaviour.Properties p_401375_) {
        super(p_401085_, p_401375_);
    }

    @Override
    public void animateTick(BlockState p_401157_, Level p_401385_, BlockPos p_401240_, RandomSource p_401370_) {
        super.animateTick(p_401157_, p_401385_, p_401240_, p_401370_);
        AmbientDesertBlockSoundsPlayer.playAmbientSandSounds(p_401385_, p_401240_, p_401370_);
    }
}
