package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class PowerParticleOption implements ParticleOptions {
    private final ParticleType<PowerParticleOption> type;
    private final float power;

    public static MapCodec<PowerParticleOption> codec(ParticleType<PowerParticleOption> particleType) {
        return Codec.FLOAT
            .xmap(p_445919_ -> new PowerParticleOption(particleType, p_445919_), p_446827_ -> p_446827_.power)
            .optionalFieldOf("power", create(particleType, 1.0F));
    }

    public static StreamCodec<? super ByteBuf, PowerParticleOption> streamCodec(ParticleType<PowerParticleOption> particleType) {
        return ByteBufCodecs.FLOAT.map(p_445551_ -> new PowerParticleOption(particleType, p_445551_), p_447254_ -> p_447254_.power);
    }

    private PowerParticleOption(ParticleType<PowerParticleOption> type, float power) {
        this.type = type;
        this.power = power;
    }

    @Override
    public ParticleType<PowerParticleOption> getType() {
        return this.type;
    }

    public float getPower() {
        return this.power;
    }

    public static PowerParticleOption create(ParticleType<PowerParticleOption> particleType, float power) {
        return new PowerParticleOption(particleType, power);
    }
}
