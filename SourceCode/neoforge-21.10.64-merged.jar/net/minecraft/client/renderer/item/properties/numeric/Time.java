package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Time extends NeedleDirectionHelper implements RangeSelectItemModelProperty {
    public static final MapCodec<Time> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_390087_ -> p_390087_.group(
                Codec.BOOL.optionalFieldOf("wobble", true).forGetter(NeedleDirectionHelper::wobble),
                Time.TimeSource.CODEC.fieldOf("source").forGetter(p_390088_ -> p_390088_.source)
            )
            .apply(p_390087_, Time::new)
    );
    private final Time.TimeSource source;
    private final RandomSource randomSource = RandomSource.create();
    private final NeedleDirectionHelper.Wobbler wobbler;

    public Time(boolean wobble, Time.TimeSource source) {
        super(wobble);
        this.source = source;
        this.wobbler = this.newWobbler(0.9F);
    }

    @Override
    protected float calculate(ItemStack stack, ClientLevel level, int seed, @Nullable ItemOwner owner) {
        float f = this.source.get(level, stack, owner, this.randomSource);
        long i = level.getGameTime();
        if (this.wobbler.shouldUpdate(i)) {
            this.wobbler.update(i, f);
        }

        return this.wobbler.rotation();
    }

    @Override
    public MapCodec<Time> type() {
        return MAP_CODEC;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum TimeSource implements StringRepresentable {
        RANDOM("random") {
            @Override
            public float get(@Nullable ClientLevel p_390411_, ItemStack p_390382_, @Nullable ItemOwner p_432922_, RandomSource p_390409_) {
                return p_390409_.nextFloat();
            }
        },
        DAYTIME("daytime") {
            @Override
            public float get(ClientLevel p_390440_, ItemStack p_390494_, @Nullable ItemOwner p_432770_, RandomSource p_390488_) {
                return p_390440_.getTimeOfDay(1.0F);
            }
        },
        MOON_PHASE("moon_phase") {
            @Override
            public float get(ClientLevel p_390465_, ItemStack p_390476_, @Nullable ItemOwner p_435438_, RandomSource p_390375_) {
                return p_390465_.getMoonPhase() / 8.0F;
            }
        };

        public static final Codec<Time.TimeSource> CODEC = StringRepresentable.fromEnum(Time.TimeSource::values);
        private final String name;

        TimeSource(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        abstract float get(ClientLevel level, ItemStack stack, @Nullable ItemOwner owner, RandomSource random);
    }
}
