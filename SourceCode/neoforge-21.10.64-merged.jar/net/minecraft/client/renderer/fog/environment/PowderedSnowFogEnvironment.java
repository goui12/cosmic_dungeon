package net.minecraft.client.renderer.fog.environment;

import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PowderedSnowFogEnvironment extends FogEnvironment {
    private static final int COLOR = -6308916;

    @Override
    public int getBaseColor(ClientLevel p_423494_, Camera p_423581_, int p_423616_, float p_423521_) {
        return -6308916;
    }

    @Override
    public void setupFog(FogData p_423470_, Entity p_423599_, BlockPos p_423534_, ClientLevel p_423516_, float p_423481_, DeltaTracker p_423655_) {
        if (p_423599_.isSpectator()) {
            p_423470_.environmentalStart = -8.0F;
            p_423470_.environmentalEnd = p_423481_ * 0.5F;
        } else {
            p_423470_.environmentalStart = 0.0F;
            p_423470_.environmentalEnd = 2.0F;
        }

        p_423470_.skyEnd = p_423470_.environmentalEnd;
        p_423470_.cloudEnd = p_423470_.environmentalEnd;
    }

    @Override
    public boolean isApplicable(@Nullable FogType p_423443_, Entity p_423658_) {
        return p_423443_ == FogType.POWDER_SNOW;
    }
}
