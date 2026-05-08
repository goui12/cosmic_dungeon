package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;

public class SpellParticleOption implements ParticleOptions {
    private final ParticleType<SpellParticleOption> type;
    private final int color;
    private final float power;

    public static MapCodec<SpellParticleOption> codec(ParticleType<SpellParticleOption> particleType) {
        return RecordCodecBuilder.mapCodec(
            p_445952_ -> p_445952_.group(
                    ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("color", -1).forGetter(p_447303_ -> p_447303_.color),
                    Codec.FLOAT.optionalFieldOf("power", 1.0F).forGetter(p_446927_ -> p_446927_.power)
                )
                .apply(p_445952_, (p_447356_, p_446433_) -> new SpellParticleOption(particleType, p_447356_, p_446433_))
        );
    }

    public static StreamCodec<? super ByteBuf, SpellParticleOption> streamCodec(ParticleType<SpellParticleOption> particleType) {
        return StreamCodec.composite(
            ByteBufCodecs.INT,
            p_447209_ -> p_447209_.color,
            ByteBufCodecs.FLOAT,
            p_445780_ -> p_445780_.power,
            (p_446915_, p_446565_) -> new SpellParticleOption(particleType, p_446915_, p_446565_)
        );
    }

    private SpellParticleOption(ParticleType<SpellParticleOption> type, int color, float power) {
        this.type = type;
        this.color = color;
        this.power = power;
    }

    @Override
    public ParticleType<SpellParticleOption> getType() {
        return this.type;
    }

    public float getRed() {
        return ARGB.red(this.color) / 255.0F;
    }

    public float getGreen() {
        return ARGB.green(this.color) / 255.0F;
    }

    public float getBlue() {
        return ARGB.blue(this.color) / 255.0F;
    }

    public float getPower() {
        return this.power;
    }

    public static SpellParticleOption create(ParticleType<SpellParticleOption> type, int color, float power) {
        return new SpellParticleOption(type, color, power);
    }

    public static SpellParticleOption create(ParticleType<SpellParticleOption> type, float r, float g, float b, float power) {
        return create(type, ARGB.colorFromFloat(1.0F, r, g, b), power);
    }
}
