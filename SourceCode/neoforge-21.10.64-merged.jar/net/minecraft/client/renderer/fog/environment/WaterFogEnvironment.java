package net.minecraft.client.renderer.fog.environment;

import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WaterFogEnvironment extends FogEnvironment {
    private static final int WATER_FOG_DISTANCE = 96;
    private static final float BIOME_FOG_TRANSITION_TIME = 5000.0F;
    private static int targetBiomeFog = -1;
    private static int previousBiomeFog = -1;
    private static long biomeChangedTime = -1L;

    @Override
    public void setupFog(FogData p_423613_, Entity p_423611_, BlockPos p_423436_, ClientLevel p_423530_, float p_423585_, DeltaTracker p_423472_) {
        p_423613_.environmentalStart = -8.0F;
        p_423613_.environmentalEnd = 96.0F;
        if (p_423611_ instanceof LocalPlayer localplayer) {
            p_423613_.environmentalEnd = p_423613_.environmentalEnd * Math.max(0.25F, localplayer.getWaterVision());
            if (p_423530_.getBiome(p_423436_).is(BiomeTags.HAS_CLOSER_WATER_FOG)) {
                p_423613_.environmentalEnd *= 0.85F;
            }
        }

        p_423613_.skyEnd = p_423613_.environmentalEnd;
        p_423613_.cloudEnd = p_423613_.environmentalEnd;
    }

    @Override
    public boolean isApplicable(@Nullable FogType p_423569_, Entity p_423444_) {
        return p_423569_ == FogType.WATER;
    }

    @Override
    public int getBaseColor(ClientLevel p_423612_, Camera p_423473_, int p_423430_, float p_423622_) {
        long i = Util.getMillis();
        int j = p_423612_.getBiome(p_423473_.getBlockPosition()).value().getWaterFogColor();
        if (biomeChangedTime < 0L) {
            targetBiomeFog = j;
            previousBiomeFog = j;
            biomeChangedTime = i;
        }

        float f = Mth.clamp((float)(i - biomeChangedTime) / 5000.0F, 0.0F, 1.0F);
        int k = ARGB.lerp(f, previousBiomeFog, targetBiomeFog);
        if (targetBiomeFog != j) {
            targetBiomeFog = j;
            previousBiomeFog = k;
            biomeChangedTime = i;
        }

        return k;
    }

    @Override
    public void onNotApplicable() {
        biomeChangedTime = -1L;
    }
}
