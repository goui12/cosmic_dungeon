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

public class DustColorTransitionOptions extends ScalableParticleOptionsBase {
    public static final int SCULK_PARTICLE_COLOR = 3790560;
    public static final DustColorTransitionOptions SCULK_TO_REDSTONE = new DustColorTransitionOptions(3790560, 16711680, 1.0F);
    public static final MapCodec<DustColorTransitionOptions> CODEC = RecordCodecBuilder.mapCodec(
        p_380810_ -> p_380810_.group(
                ExtraCodecs.RGB_COLOR_CODEC.fieldOf("from_color").forGetter(p_380812_ -> p_380812_.fromColor),
                ExtraCodecs.RGB_COLOR_CODEC.fieldOf("to_color").forGetter(p_380808_ -> p_380808_.toColor),
                SCALE.fieldOf("scale").forGetter(ScalableParticleOptionsBase::getScale)
            )
            .apply(p_380810_, DustColorTransitionOptions::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DustColorTransitionOptions> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        p_380811_ -> p_380811_.fromColor,
        ByteBufCodecs.INT,
        p_380809_ -> p_380809_.toColor,
        ByteBufCodecs.FLOAT,
        ScalableParticleOptionsBase::getScale,
        DustColorTransitionOptions::new
    );
    private final int fromColor;
    private final int toColor;

    public DustColorTransitionOptions(int fromColor, int toColor, float scale) {
        super(scale);
        this.fromColor = fromColor;
        this.toColor = toColor;
    }

    public Vector3f getFromColor() {
        return ARGB.vector3fFromRGB24(this.fromColor);
    }

    public Vector3f getToColor() {
        return ARGB.vector3fFromRGB24(this.toColor);
    }

    @Override
    public ParticleType<DustColorTransitionOptions> getType() {
        return ParticleTypes.DUST_COLOR_TRANSITION;
    }
}
