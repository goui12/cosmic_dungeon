package net.minecraft.client.renderer;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class DimensionSpecialEffects implements net.neoforged.neoforge.client.extensions.IDimensionSpecialEffectsExtension {
    private static final Object2ObjectMap<ResourceLocation, DimensionSpecialEffects> EFFECTS = Util.make(new Object2ObjectArrayMap<>(), p_108881_ -> {
        DimensionSpecialEffects.OverworldEffects dimensionspecialeffects$overworldeffects = new DimensionSpecialEffects.OverworldEffects();
        p_108881_.defaultReturnValue(dimensionspecialeffects$overworldeffects);
        p_108881_.put(BuiltinDimensionTypes.OVERWORLD_EFFECTS, dimensionspecialeffects$overworldeffects);
        p_108881_.put(BuiltinDimensionTypes.NETHER_EFFECTS, new DimensionSpecialEffects.NetherEffects());
        p_108881_.put(BuiltinDimensionTypes.END_EFFECTS, new DimensionSpecialEffects.EndEffects());
    });
    private final DimensionSpecialEffects.SkyType skyType;
    private final boolean constantAmbientLight;
    private final boolean hasEndFlashes;

    public DimensionSpecialEffects(DimensionSpecialEffects.SkyType skyType, boolean forceBrightLightmap, boolean constantAmbientLight) {
        this.skyType = skyType;
        this.constantAmbientLight = forceBrightLightmap;
        this.hasEndFlashes = constantAmbientLight;
    }

    public static DimensionSpecialEffects forType(DimensionType dimensionType) {
        return net.neoforged.neoforge.client.DimensionSpecialEffectsManager.getForType(dimensionType.effectsLocation());
    }

    public boolean isSunriseOrSunset(float timeOfDay) {
        return false;
    }

    public int getSunriseOrSunsetColor(float timeOfDay) {
        return 0;
    }

    public abstract Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness);

    public abstract boolean isFoggyAt(int x, int y);

    public DimensionSpecialEffects.SkyType skyType() {
        return this.skyType;
    }

    public boolean constantAmbientLight() {
        return this.constantAmbientLight;
    }

    public boolean hasEndFlashes() {
        return this.hasEndFlashes;
    }

    @OnlyIn(Dist.CLIENT)
    public static class EndEffects extends DimensionSpecialEffects {
        public EndEffects() {
            super(DimensionSpecialEffects.SkyType.END, false, true);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 p_108894_, float p_108895_) {
            return p_108894_.scale(0.15F);
        }

        @Override
        public boolean isFoggyAt(int p_108891_, int p_108892_) {
            return false;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class NetherEffects extends DimensionSpecialEffects {
        public NetherEffects() {
            super(DimensionSpecialEffects.SkyType.NONE, true, false);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 p_108901_, float p_108902_) {
            return p_108901_;
        }

        @Override
        public boolean isFoggyAt(int p_108898_, int p_108899_) {
            return true;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class OverworldEffects extends DimensionSpecialEffects {
        private static final float SUNRISE_AND_SUNSET_TIMESPAN = 0.4F;

        public OverworldEffects() {
            super(DimensionSpecialEffects.SkyType.OVERWORLD, false, false);
        }

        @Override
        public boolean isSunriseOrSunset(float p_361608_) {
            float f = Mth.cos(p_361608_ * (float) (Math.PI * 2));
            return f >= -0.4F && f <= 0.4F;
        }

        @Override
        public int getSunriseOrSunsetColor(float p_362041_) {
            float f = Mth.cos(p_362041_ * (float) (Math.PI * 2));
            float f1 = f / 0.4F * 0.5F + 0.5F;
            float f2 = Mth.square(1.0F - (1.0F - Mth.sin(f1 * (float) Math.PI)) * 0.99F);
            return ARGB.colorFromFloat(f2, f1 * 0.3F + 0.7F, f1 * f1 * 0.7F + 0.2F, 0.2F);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 p_108908_, float p_108909_) {
            return p_108908_.multiply(p_108909_ * 0.94F + 0.06F, p_108909_ * 0.94F + 0.06F, p_108909_ * 0.91F + 0.09F);
        }

        @Override
        public boolean isFoggyAt(int p_108905_, int p_108906_) {
            return false;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum SkyType {
        NONE,
        OVERWORLD,
        END;
    }
}
