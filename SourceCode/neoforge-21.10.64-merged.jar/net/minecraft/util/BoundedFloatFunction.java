package net.minecraft.util;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import java.util.function.Function;

public interface BoundedFloatFunction<C> {
    BoundedFloatFunction<Float> IDENTITY = createUnlimited(p_447155_ -> p_447155_);

    float apply(C input);

    float minValue();

    float maxValue();

    static BoundedFloatFunction<Float> createUnlimited(final Float2FloatFunction function) {
        return new BoundedFloatFunction<Float>() {
            public float apply(Float p_446486_) {
                return function.apply(p_446486_);
            }

            @Override
            public float minValue() {
                return Float.NEGATIVE_INFINITY;
            }

            @Override
            public float maxValue() {
                return Float.POSITIVE_INFINITY;
            }
        };
    }

    default <C2> BoundedFloatFunction<C2> comap(final Function<C2, C> mapper) {
        final BoundedFloatFunction<C> boundedfloatfunction = this;
        return new BoundedFloatFunction<C2>() {
            @Override
            public float apply(C2 p_446357_) {
                return boundedfloatfunction.apply(mapper.apply(p_446357_));
            }

            @Override
            public float minValue() {
                return boundedfloatfunction.minValue();
            }

            @Override
            public float maxValue() {
                return boundedfloatfunction.maxValue();
            }
        };
    }
}
