package net.minecraft.gametest.framework;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Rotation;

public record TestData<EnvironmentType>(
    EnvironmentType environment,
    ResourceLocation structure,
    int maxTicks,
    int setupTicks,
    boolean required,
    Rotation rotation,
    boolean manualOnly,
    int maxAttempts,
    int requiredSuccesses,
    boolean skyAccess
) {
    public static final MapCodec<TestData<Holder<TestEnvironmentDefinition>>> CODEC = RecordCodecBuilder.mapCodec(
        p_397610_ -> p_397610_.group(
                TestEnvironmentDefinition.CODEC.fieldOf("environment").forGetter(TestData::environment),
                ResourceLocation.CODEC.fieldOf("structure").forGetter(TestData::structure),
                ExtraCodecs.POSITIVE_INT.fieldOf("max_ticks").forGetter(TestData::maxTicks),
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("setup_ticks", 0).forGetter(TestData::setupTicks),
                Codec.BOOL.optionalFieldOf("required", true).forGetter(TestData::required),
                Rotation.CODEC.optionalFieldOf("rotation", Rotation.NONE).forGetter(TestData::rotation),
                Codec.BOOL.optionalFieldOf("manual_only", false).forGetter(TestData::manualOnly),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("max_attempts", 1).forGetter(TestData::maxAttempts),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("required_successes", 1).forGetter(TestData::requiredSuccesses),
                Codec.BOOL.optionalFieldOf("sky_access", false).forGetter(TestData::skyAccess)
            )
            .apply(p_397610_, TestData::new)
    );

    public TestData(EnvironmentType p_397869_, ResourceLocation p_397537_, int p_397240_, int p_397081_, boolean p_397911_, Rotation p_397516_) {
        this(p_397869_, p_397537_, p_397240_, p_397081_, p_397911_, p_397516_, false, 1, 1, false);
    }

    public TestData(EnvironmentType p_397623_, ResourceLocation p_397576_, int p_397143_, int p_397037_, boolean p_397352_) {
        this(p_397623_, p_397576_, p_397143_, p_397037_, p_397352_, Rotation.NONE);
    }

    public <T> TestData<T> map(Function<EnvironmentType, T> mapper) {
        return new TestData<>(
            mapper.apply(this.environment),
            this.structure,
            this.maxTicks,
            this.setupTicks,
            this.required,
            this.rotation,
            this.manualOnly,
            this.maxAttempts,
            this.requiredSuccesses,
            this.skyAccess
        );
    }
}
