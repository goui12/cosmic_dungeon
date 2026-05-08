package net.minecraft.client.renderer.fog.environment;

import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LavaFogEnvironment extends FogEnvironment {
    private static final int COLOR = -6743808;

    @Override
    public int getBaseColor(ClientLevel p_423583_, Camera p_423592_, int p_423673_, float p_423463_) {
        return -6743808;
    }

    @Override
    public void setupFog(FogData p_423517_, Entity p_423438_, BlockPos p_423587_, ClientLevel p_423495_, float p_423631_, DeltaTracker p_423491_) {
        if (p_423438_.isSpectator()) {
            p_423517_.environmentalStart = -8.0F;
            p_423517_.environmentalEnd = p_423631_ * 0.5F;
        } else if (p_423438_ instanceof LivingEntity livingentity && livingentity.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            p_423517_.environmentalStart = 0.0F;
            p_423517_.environmentalEnd = 5.0F;
        } else {
            p_423517_.environmentalStart = 0.25F;
            p_423517_.environmentalEnd = 1.0F;
        }

        p_423517_.skyEnd = p_423517_.environmentalEnd;
        p_423517_.cloudEnd = p_423517_.environmentalEnd;
    }

    @Override
    public boolean isApplicable(@Nullable FogType p_423685_, Entity p_423469_) {
        return p_423685_ == FogType.LAVA;
    }
}
