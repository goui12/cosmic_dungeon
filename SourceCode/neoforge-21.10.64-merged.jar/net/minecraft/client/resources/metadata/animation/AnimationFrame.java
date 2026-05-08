package net.minecraft.client.resources.metadata.animation;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record AnimationFrame(int index, Optional<Integer> time) {
    public static final Codec<AnimationFrame> FULL_CODEC = RecordCodecBuilder.create(
        p_389594_ -> p_389594_.group(
                ExtraCodecs.NON_NEGATIVE_INT.fieldOf("index").forGetter(AnimationFrame::index),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("time").forGetter(AnimationFrame::time)
            )
            .apply(p_389594_, AnimationFrame::new)
    );
    public static final Codec<AnimationFrame> CODEC = Codec.either(ExtraCodecs.NON_NEGATIVE_INT, FULL_CODEC)
        .xmap(
            p_389578_ -> p_389578_.map(AnimationFrame::new, p_389618_ -> (AnimationFrame)p_389618_),
            p_389536_ -> p_389536_.time.isPresent() ? Either.right(p_389536_) : Either.left(p_389536_.index)
        );

    public AnimationFrame(int p_119004_) {
        this(p_119004_, Optional.empty());
    }

    public int timeOr(int defaultValue) {
        return this.time.orElse(defaultValue);
    }
}
