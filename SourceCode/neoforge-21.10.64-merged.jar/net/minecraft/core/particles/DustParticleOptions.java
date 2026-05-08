package net.minecraft.core.particles;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;

public class DustParticleOptions extends ScalableParticleOptionsBase {
    public static final int REDSTONE_PARTICLE_COLOR = 16711680;
    public static final DustParticleOptions REDSTONE = new DustParticleOptions(16711680, 1.0F);
    public static final MapCodec<DustParticleOptions> CODEC = RecordCodecBuilder.mapCodec(
        p_380814_ -> p_380814_.group(
                ExtraCodecs.RGB_COLOR_CODEC.fieldOf("color").forGetter(p_380815_ -> p_380815_.color),
                SCALE.fieldOf("scale").forGetter(ScalableParticleOptionsBase::getScale)
            )
            .apply(p_380814_, DustParticleOptions::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DustParticleOptions> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, p_380813_ -> p_380813_.color, ByteBufCodecs.FLOAT, ScalableParticleOptionsBase::getScale, DustParticleOptions::new
    );
    private final int color;

    public DustParticleOptions(int color, float scale) {
        super(scale);
        this.color = color;
    }

    @Override
    public ParticleType<DustParticleOptions> getType() {
        return ParticleTypes.DUST;
    }

    public Vector3f getColor() {
        return ARGB.vector3fFromRGB24(this.color);
    }
}
