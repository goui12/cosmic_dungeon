package net.minecraft.server.dialog.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

public record NumberRangeInput(int width, Component label, String labelFormat, NumberRangeInput.RangeInfo rangeInfo) implements InputControl {
    public static final MapCodec<NumberRangeInput> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_426967_ -> p_426967_.group(
                Dialog.WIDTH_CODEC.optionalFieldOf("width", 200).forGetter(NumberRangeInput::width),
                ComponentSerialization.CODEC.fieldOf("label").forGetter(NumberRangeInput::label),
                Codec.STRING.optionalFieldOf("label_format", "options.generic_value").forGetter(NumberRangeInput::labelFormat),
                NumberRangeInput.RangeInfo.MAP_CODEC.forGetter(NumberRangeInput::rangeInfo)
            )
            .apply(p_426967_, NumberRangeInput::new)
    );

    @Override
    public MapCodec<NumberRangeInput> mapCodec() {
        return MAP_CODEC;
    }

    public Component computeLabel(String p_426283_) {
        return Component.translatable(this.labelFormat, this.label, p_426283_);
    }

    public record RangeInfo(float start, float end, Optional<Float> initial, Optional<Float> step) {
        public static final MapCodec<NumberRangeInput.RangeInfo> MAP_CODEC = RecordCodecBuilder.<NumberRangeInput.RangeInfo>mapCodec(
                p_426968_ -> p_426968_.group(
                        Codec.FLOAT.fieldOf("start").forGetter(NumberRangeInput.RangeInfo::start),
                        Codec.FLOAT.fieldOf("end").forGetter(NumberRangeInput.RangeInfo::end),
                        Codec.FLOAT.optionalFieldOf("initial").forGetter(NumberRangeInput.RangeInfo::initial),
                        ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("step").forGetter(NumberRangeInput.RangeInfo::step)
                    )
                    .apply(p_426968_, NumberRangeInput.RangeInfo::new)
            )
            .validate(p_425998_ -> {
                if (p_425998_.initial.isPresent()) {
                    double d0 = p_425998_.initial.get().floatValue();
                    double d1 = Math.min(p_425998_.start, p_425998_.end);
                    double d2 = Math.max(p_425998_.start, p_425998_.end);
                    if (d0 < d1 || d0 > d2) {
                        return DataResult.error(() -> "Initial value " + d0 + " is outside of range [" + d1 + ", " + d2 + "]");
                    }
                }

                return DataResult.success(p_425998_);
            });

        public float computeScaledValue(float p_427226_) {
            float f = Mth.lerp(p_427226_, this.start, this.end);
            if (this.step.isEmpty()) {
                return f;
            } else {
                float f1 = this.step.get();
                float f2 = this.initialScaledValue();
                float f3 = f - f2;
                int i = Math.round(f3 / f1);
                float f4 = f2 + i * f1;
                if (!this.isOutOfRange(f4)) {
                    return f4;
                } else {
                    int j = i - Mth.sign(i);
                    return f2 + j * f1;
                }
            }
        }

        private boolean isOutOfRange(float p_427366_) {
            float f = this.scaledValueToSlider(p_427366_);
            return f < 0.0 || f > 1.0;
        }

        private float initialScaledValue() {
            return this.initial.isPresent() ? this.initial.get() : (this.start + this.end) / 2.0F;
        }

        public float initialSliderValue() {
            float f = this.initialScaledValue();
            return this.scaledValueToSlider(f);
        }

        private float scaledValueToSlider(float p_427373_) {
            return this.start == this.end ? 0.5F : Mth.inverseLerp(p_427373_, this.start, this.end);
        }
    }
}
