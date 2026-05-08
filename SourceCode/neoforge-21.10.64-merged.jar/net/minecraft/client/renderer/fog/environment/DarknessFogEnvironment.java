package net.minecraft.client.renderer.fog.environment;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DarknessFogEnvironment extends MobEffectFogEnvironment {
    @Override
    public Holder<MobEffect> getMobEffect() {
        return MobEffects.DARKNESS;
    }

    @Override
    public void setupFog(FogData p_423539_, Entity p_423433_, BlockPos p_423453_, ClientLevel p_423550_, float p_423598_, DeltaTracker p_423497_) {
        if (p_423433_ instanceof LivingEntity livingentity) {
            MobEffectInstance mobeffectinstance = livingentity.getEffect(this.getMobEffect());
            if (mobeffectinstance != null) {
                float f = Mth.lerp(mobeffectinstance.getBlendFactor(livingentity, p_423497_.getGameTimeDeltaPartialTick(false)), p_423598_, 15.0F);
                p_423539_.environmentalStart = f * 0.75F;
                p_423539_.environmentalEnd = f;
                p_423539_.skyEnd = f;
                p_423539_.cloudEnd = f;
            }
        }
    }

    @Override
    public float getModifiedDarkness(LivingEntity p_423586_, float p_423483_, float p_423558_) {
        MobEffectInstance mobeffectinstance = p_423586_.getEffect(this.getMobEffect());
        return mobeffectinstance != null ? Math.max(mobeffectinstance.getBlendFactor(p_423586_, p_423558_), p_423483_) : p_423483_;
    }
}
