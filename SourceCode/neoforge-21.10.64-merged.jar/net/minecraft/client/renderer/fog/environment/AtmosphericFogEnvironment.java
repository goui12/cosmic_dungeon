package net.minecraft.client.renderer.fog.environment;

import javax.annotation.Nullable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AtmosphericFogEnvironment extends AirBasedFogEnvironment {
    private static final int MIN_RAIN_FOG_SKY_LIGHT = 8;
    private static final float RAIN_FOG_START_OFFSET = -160.0F;
    private static final float RAIN_FOG_END_OFFSET = -256.0F;
    private float rainFogMultiplier;

    @Override
    public void setupFog(FogData p_423515_, Entity p_423566_, BlockPos p_423678_, ClientLevel p_423511_, float p_423456_, DeltaTracker p_423432_) {
        Biome biome = p_423511_.getBiome(p_423678_).value();
        float f = p_423432_.getGameTimeDeltaTicks();
        boolean flag = biome.hasPrecipitation();
        float f1 = Mth.clamp((p_423511_.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(p_423678_) - 8.0F) / 7.0F, 0.0F, 1.0F);
        float f2 = p_423511_.getRainLevel(p_423432_.getGameTimeDeltaPartialTick(false)) * f1 * (flag ? 1.0F : 0.5F);
        this.rainFogMultiplier = this.rainFogMultiplier + (f2 - this.rainFogMultiplier) * f * 0.2F;
        p_423515_.environmentalStart = this.rainFogMultiplier * -160.0F;
        p_423515_.environmentalEnd = 1024.0F + -256.0F * this.rainFogMultiplier;
        p_423515_.skyEnd = p_423456_;
        p_423515_.cloudEnd = Minecraft.getInstance().options.cloudRange().get() * 16;
    }

    @Override
    public boolean isApplicable(@Nullable FogType p_423642_, Entity p_423662_) {
        return p_423642_ == FogType.ATMOSPHERIC;
    }
}
