package net.minecraft.client.renderer.fog.environment;

import javax.annotation.Nullable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DimensionOrBossFogEnvironment extends AirBasedFogEnvironment {
    @Override
    public void setupFog(FogData p_423624_, Entity p_423437_, BlockPos p_423499_, ClientLevel p_423676_, float p_423577_, DeltaTracker p_423458_) {
        p_423624_.environmentalStart = p_423577_ * 0.05F;
        p_423624_.environmentalEnd = Math.min(p_423577_, 192.0F) * 0.5F;
        p_423624_.skyEnd = p_423624_.environmentalEnd;
        p_423624_.cloudEnd = p_423624_.environmentalEnd;
    }

    @Override
    public boolean isApplicable(@Nullable FogType p_423493_, Entity p_423455_) {
        return p_423493_ == FogType.DIMENSION_OR_BOSS;
    }
}
