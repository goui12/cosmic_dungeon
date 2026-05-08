package net.minecraft.data.worldgen;

import net.minecraft.util.BoundedFloatFunction;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.NoiseRouterData;

public class TerrainProvider {
    private static final float DEEP_OCEAN_CONTINENTALNESS = -0.51F;
    private static final float OCEAN_CONTINENTALNESS = -0.4F;
    private static final float PLAINS_CONTINENTALNESS = 0.1F;
    private static final float BEACH_CONTINENTALNESS = -0.15F;
    private static final BoundedFloatFunction<Float> NO_TRANSFORM = BoundedFloatFunction.IDENTITY;
    private static final BoundedFloatFunction<Float> AMPLIFIED_OFFSET = BoundedFloatFunction.createUnlimited(
        p_236651_ -> p_236651_ < 0.0F ? p_236651_ : p_236651_ * 2.0F
    );
    private static final BoundedFloatFunction<Float> AMPLIFIED_FACTOR = BoundedFloatFunction.createUnlimited(p_236649_ -> 1.25F - 6.25F / (p_236649_ + 5.0F));
    private static final BoundedFloatFunction<Float> AMPLIFIED_JAGGEDNESS = BoundedFloatFunction.createUnlimited(p_236641_ -> p_236641_ * 2.0F);

    public static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> overworldOffset(I continents, I erosion, I ridgesFolded, boolean amplified) {
        BoundedFloatFunction<Float> boundedfloatfunction = amplified ? AMPLIFIED_OFFSET : NO_TRANSFORM;
        CubicSpline<C, I> cubicspline = buildErosionOffsetSpline(
            erosion, ridgesFolded, -0.15F, 0.0F, 0.0F, 0.1F, 0.0F, -0.03F, false, false, boundedfloatfunction
        );
        CubicSpline<C, I> cubicspline1 = buildErosionOffsetSpline(
            erosion, ridgesFolded, -0.1F, 0.03F, 0.1F, 0.1F, 0.01F, -0.03F, false, false, boundedfloatfunction
        );
        CubicSpline<C, I> cubicspline2 = buildErosionOffsetSpline(
            erosion, ridgesFolded, -0.1F, 0.03F, 0.1F, 0.7F, 0.01F, -0.03F, true, true, boundedfloatfunction
        );
        CubicSpline<C, I> cubicspline3 = buildErosionOffsetSpline(
            erosion, ridgesFolded, -0.05F, 0.03F, 0.1F, 1.0F, 0.01F, 0.01F, true, true, boundedfloatfunction
        );
        return CubicSpline.<C, I>builder(continents, boundedfloatfunction)
            .addPoint(-1.1F, 0.044F)
            .addPoint(-1.02F, -0.2222F)
            .addPoint(-0.51F, -0.2222F)
            .addPoint(-0.44F, -0.12F)
            .addPoint(-0.18F, -0.12F)
            .addPoint(-0.16F, cubicspline)
            .addPoint(-0.15F, cubicspline)
            .addPoint(-0.1F, cubicspline1)
            .addPoint(0.25F, cubicspline2)
            .addPoint(1.0F, cubicspline3)
            .build();
    }

    public static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> overworldFactor(
        I continents, I erosion, I ridges, I ridgesFolded, boolean amplified
    ) {
        BoundedFloatFunction<Float> boundedfloatfunction = amplified ? AMPLIFIED_FACTOR : NO_TRANSFORM;
        return CubicSpline.<C, I>builder(continents, NO_TRANSFORM)
            .addPoint(-0.19F, 3.95F)
            .addPoint(-0.15F, getErosionFactor(erosion, ridges, ridgesFolded, 6.25F, true, NO_TRANSFORM))
            .addPoint(-0.1F, getErosionFactor(erosion, ridges, ridgesFolded, 5.47F, true, boundedfloatfunction))
            .addPoint(0.03F, getErosionFactor(erosion, ridges, ridgesFolded, 5.08F, true, boundedfloatfunction))
            .addPoint(0.06F, getErosionFactor(erosion, ridges, ridgesFolded, 4.69F, false, boundedfloatfunction))
            .build();
    }

    public static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> overworldJaggedness(
        I continents, I erosion, I ridges, I ridgesFolded, boolean amplified
    ) {
        BoundedFloatFunction<Float> boundedfloatfunction = amplified ? AMPLIFIED_JAGGEDNESS : NO_TRANSFORM;
        float f = 0.65F;
        return CubicSpline.<C, I>builder(continents, boundedfloatfunction)
            .addPoint(-0.11F, 0.0F)
            .addPoint(0.03F, buildErosionJaggednessSpline(erosion, ridges, ridgesFolded, 1.0F, 0.5F, 0.0F, 0.0F, boundedfloatfunction))
            .addPoint(0.65F, buildErosionJaggednessSpline(erosion, ridges, ridgesFolded, 1.0F, 1.0F, 1.0F, 0.0F, boundedfloatfunction))
            .build();
    }

    private static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> buildErosionJaggednessSpline(
        I erosion, I ridges, I ridgesFolded, float highErosionHighWeirdness, float lowErosionHighWeirdness, float highErosionMidWeirdness, float lowErosionMidWeirdness, BoundedFloatFunction<Float> transform
    ) {
        float f = -0.5775F;
        CubicSpline<C, I> cubicspline = buildRidgeJaggednessSpline(ridges, ridgesFolded, highErosionHighWeirdness, highErosionMidWeirdness, transform);
        CubicSpline<C, I> cubicspline1 = buildRidgeJaggednessSpline(ridges, ridgesFolded, lowErosionHighWeirdness, lowErosionMidWeirdness, transform);
        return CubicSpline.<C, I>builder(erosion, transform)
            .addPoint(-1.0F, cubicspline)
            .addPoint(-0.78F, cubicspline1)
            .addPoint(-0.5775F, cubicspline1)
            .addPoint(-0.375F, 0.0F)
            .build();
    }

    private static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> buildRidgeJaggednessSpline(
        I ridges, I ridgesFolded, float highWeirdnessMagnitude, float midWeirdnessMagnitude, BoundedFloatFunction<Float> transform
    ) {
        float f = NoiseRouterData.peaksAndValleys(0.4F);
        float f1 = NoiseRouterData.peaksAndValleys(0.56666666F);
        float f2 = (f + f1) / 2.0F;
        CubicSpline.Builder<C, I> builder = CubicSpline.builder(ridgesFolded, transform);
        builder.addPoint(f, 0.0F);
        if (midWeirdnessMagnitude > 0.0F) {
            builder.addPoint(f2, buildWeirdnessJaggednessSpline(ridges, midWeirdnessMagnitude, transform));
        } else {
            builder.addPoint(f2, 0.0F);
        }

        if (highWeirdnessMagnitude > 0.0F) {
            builder.addPoint(1.0F, buildWeirdnessJaggednessSpline(ridges, highWeirdnessMagnitude, transform));
        } else {
            builder.addPoint(1.0F, 0.0F);
        }

        return builder.build();
    }

    private static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> buildWeirdnessJaggednessSpline(
        I ridges, float magnitude, BoundedFloatFunction<Float> transform
    ) {
        float f = 0.63F * magnitude;
        float f1 = 0.3F * magnitude;
        return CubicSpline.<C, I>builder(ridges, transform).addPoint(-0.01F, f).addPoint(0.01F, f1).build();
    }

    private static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> getErosionFactor(
        I erosion, I ridges, I ridgesFolded, float value, boolean higherValues, BoundedFloatFunction<Float> transform
    ) {
        CubicSpline<C, I> cubicspline = CubicSpline.<C, I>builder(ridges, transform).addPoint(-0.2F, 6.3F).addPoint(0.2F, value).build();
        CubicSpline.Builder<C, I> builder = CubicSpline.<C, I>builder(erosion, transform)
            .addPoint(-0.6F, cubicspline)
            .addPoint(-0.5F, CubicSpline.<C, I>builder(ridges, transform).addPoint(-0.05F, 6.3F).addPoint(0.05F, 2.67F).build())
            .addPoint(-0.35F, cubicspline)
            .addPoint(-0.25F, cubicspline)
            .addPoint(-0.1F, CubicSpline.<C, I>builder(ridges, transform).addPoint(-0.05F, 2.67F).addPoint(0.05F, 6.3F).build())
            .addPoint(0.03F, cubicspline);
        if (higherValues) {
            CubicSpline<C, I> cubicspline1 = CubicSpline.<C, I>builder(ridges, transform).addPoint(0.0F, value).addPoint(0.1F, 0.625F).build();
            CubicSpline<C, I> cubicspline2 = CubicSpline.<C, I>builder(ridgesFolded, transform).addPoint(-0.9F, value).addPoint(-0.69F, cubicspline1).build();
            builder.addPoint(0.35F, value).addPoint(0.45F, cubicspline2).addPoint(0.55F, cubicspline2).addPoint(0.62F, value);
        } else {
            CubicSpline<C, I> cubicspline3 = CubicSpline.<C, I>builder(ridgesFolded, transform).addPoint(-0.7F, cubicspline).addPoint(-0.15F, 1.37F).build();
            CubicSpline<C, I> cubicspline4 = CubicSpline.<C, I>builder(ridgesFolded, transform).addPoint(0.45F, cubicspline).addPoint(0.7F, 1.56F).build();
            builder.addPoint(0.05F, cubicspline4)
                .addPoint(0.4F, cubicspline4)
                .addPoint(0.45F, cubicspline3)
                .addPoint(0.55F, cubicspline3)
                .addPoint(0.58F, value);
        }

        return builder.build();
    }

    private static float calculateSlope(float y1, float y2, float x1, float x2) {
        return (y2 - y1) / (x2 - x1);
    }

    private static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> buildMountainRidgeSplineWithPoints(
        I ridgesFolded, float magnitude, boolean useMaxSlope, BoundedFloatFunction<Float> transform
    ) {
        CubicSpline.Builder<C, I> builder = CubicSpline.builder(ridgesFolded, transform);
        float f = -0.7F;
        float f1 = -1.0F;
        float f2 = mountainContinentalness(-1.0F, magnitude, -0.7F);
        float f3 = 1.0F;
        float f4 = mountainContinentalness(1.0F, magnitude, -0.7F);
        float f5 = calculateMountainRidgeZeroContinentalnessPoint(magnitude);
        float f6 = -0.65F;
        if (-0.65F < f5 && f5 < 1.0F) {
            float f14 = mountainContinentalness(-0.65F, magnitude, -0.7F);
            float f8 = -0.75F;
            float f9 = mountainContinentalness(-0.75F, magnitude, -0.7F);
            float f10 = calculateSlope(f2, f9, -1.0F, -0.75F);
            builder.addPoint(-1.0F, f2, f10);
            builder.addPoint(-0.75F, f9);
            builder.addPoint(-0.65F, f14);
            float f11 = mountainContinentalness(f5, magnitude, -0.7F);
            float f12 = calculateSlope(f11, f4, f5, 1.0F);
            float f13 = 0.01F;
            builder.addPoint(f5 - 0.01F, f11);
            builder.addPoint(f5, f11, f12);
            builder.addPoint(1.0F, f4, f12);
        } else {
            float f7 = calculateSlope(f2, f4, -1.0F, 1.0F);
            if (useMaxSlope) {
                builder.addPoint(-1.0F, Math.max(0.2F, f2));
                builder.addPoint(0.0F, Mth.lerp(0.5F, f2, f4), f7);
            } else {
                builder.addPoint(-1.0F, f2, f7);
            }

            builder.addPoint(1.0F, f4, f7);
        }

        return builder.build();
    }

    private static float mountainContinentalness(float heightFactor, float magnitude, float cutoffHeight) {
        float f = 1.17F;
        float f1 = 0.46082947F;
        float f2 = 1.0F - (1.0F - magnitude) * 0.5F;
        float f3 = 0.5F * (1.0F - magnitude);
        float f4 = (heightFactor + 1.17F) * 0.46082947F;
        float f5 = f4 * f2 - f3;
        return heightFactor < cutoffHeight ? Math.max(f5, -0.2222F) : Math.max(f5, 0.0F);
    }

    private static float calculateMountainRidgeZeroContinentalnessPoint(float input) {
        float f = 1.17F;
        float f1 = 0.46082947F;
        float f2 = 1.0F - (1.0F - input) * 0.5F;
        float f3 = 0.5F * (1.0F - input);
        return f3 / (0.46082947F * f2) - 1.17F;
    }

    public static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> buildErosionOffsetSpline(
        I erosion,
        I ridgesFolded,
        float ridgeBaseOffset,
        float ridgeMidOffset,
        float ridgePeakOffset,
        float magnitude,
        float ridgeInnerOffset,
        float ridgeOuterOffset,
        boolean extended,
        boolean useMaxSlope,
        BoundedFloatFunction<Float> transform
    ) {
        float f = 0.6F;
        float f1 = 0.5F;
        float f2 = 0.5F;
        CubicSpline<C, I> cubicspline = buildMountainRidgeSplineWithPoints(ridgesFolded, Mth.lerp(magnitude, 0.6F, 1.5F), useMaxSlope, transform);
        CubicSpline<C, I> cubicspline1 = buildMountainRidgeSplineWithPoints(ridgesFolded, Mth.lerp(magnitude, 0.6F, 1.0F), useMaxSlope, transform);
        CubicSpline<C, I> cubicspline2 = buildMountainRidgeSplineWithPoints(ridgesFolded, magnitude, useMaxSlope, transform);
        CubicSpline<C, I> cubicspline3 = ridgeSpline(
            ridgesFolded, ridgeBaseOffset - 0.15F, 0.5F * magnitude, Mth.lerp(0.5F, 0.5F, 0.5F) * magnitude, 0.5F * magnitude, 0.6F * magnitude, 0.5F, transform
        );
        CubicSpline<C, I> cubicspline4 = ridgeSpline(
            ridgesFolded, ridgeBaseOffset, ridgeInnerOffset * magnitude, ridgeMidOffset * magnitude, 0.5F * magnitude, 0.6F * magnitude, 0.5F, transform
        );
        CubicSpline<C, I> cubicspline5 = ridgeSpline(ridgesFolded, ridgeBaseOffset, ridgeInnerOffset, ridgeInnerOffset, ridgeMidOffset, ridgePeakOffset, 0.5F, transform);
        CubicSpline<C, I> cubicspline6 = ridgeSpline(ridgesFolded, ridgeBaseOffset, ridgeInnerOffset, ridgeInnerOffset, ridgeMidOffset, ridgePeakOffset, 0.5F, transform);
        CubicSpline<C, I> cubicspline7 = CubicSpline.<C, I>builder(ridgesFolded, transform)
            .addPoint(-1.0F, ridgeBaseOffset)
            .addPoint(-0.4F, cubicspline5)
            .addPoint(0.0F, ridgePeakOffset + 0.07F)
            .build();
        CubicSpline<C, I> cubicspline8 = ridgeSpline(ridgesFolded, -0.02F, ridgeOuterOffset, ridgeOuterOffset, ridgeMidOffset, ridgePeakOffset, 0.0F, transform);
        CubicSpline.Builder<C, I> builder = CubicSpline.<C, I>builder(erosion, transform)
            .addPoint(-0.85F, cubicspline)
            .addPoint(-0.7F, cubicspline1)
            .addPoint(-0.4F, cubicspline2)
            .addPoint(-0.35F, cubicspline3)
            .addPoint(-0.1F, cubicspline4)
            .addPoint(0.2F, cubicspline5);
        if (extended) {
            builder.addPoint(0.4F, cubicspline6).addPoint(0.45F, cubicspline7).addPoint(0.55F, cubicspline7).addPoint(0.58F, cubicspline6);
        }

        builder.addPoint(0.7F, cubicspline8);
        return builder.build();
    }

    private static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> ridgeSpline(
        I ridgesFolded,
        float y1,
        float y2,
        float y3,
        float y4,
        float y5,
        float minSmoothing,
        BoundedFloatFunction<Float> transform
    ) {
        float f = Math.max(0.5F * (y2 - y1), minSmoothing);
        float f1 = 5.0F * (y3 - y2);
        return CubicSpline.<C, I>builder(ridgesFolded, transform)
            .addPoint(-1.0F, y1, f)
            .addPoint(-0.4F, y2, Math.min(f, f1))
            .addPoint(0.0F, y3, f1)
            .addPoint(0.4F, y4, 2.0F * (y4 - y3))
            .addPoint(1.0F, y5, 0.7F * (y5 - y4))
            .build();
    }
}
