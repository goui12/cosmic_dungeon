package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record SimpleBlockConfiguration(BlockStateProvider toPlace, boolean scheduleTick) implements FeatureConfiguration {
    public static final Codec<SimpleBlockConfiguration> CODEC = RecordCodecBuilder.create(
        p_382778_ -> p_382778_.group(
                BlockStateProvider.CODEC.fieldOf("to_place").forGetter(p_161168_ -> p_161168_.toPlace),
                Codec.BOOL.optionalFieldOf("schedule_tick", false).forGetter(p_382777_ -> p_382777_.scheduleTick)
            )
            .apply(p_382778_, SimpleBlockConfiguration::new)
    );

    public SimpleBlockConfiguration(BlockStateProvider p_161155_) {
        this(p_161155_, false);
    }
}
