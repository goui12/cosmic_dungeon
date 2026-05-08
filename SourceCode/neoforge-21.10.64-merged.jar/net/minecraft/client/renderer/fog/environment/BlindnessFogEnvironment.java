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
public class BlindnessFogEnvironment extends MobEffectFogEnvironment {
    @Override
    public Holder<MobEffect> getMobEffect() {
        return MobEffects.BLINDNESS;
    }

    @Override
    public void setupFog(FogData p_423496_, Entity p_423549_, BlockPos p_423480_, ClientLevel p_423609_, float p_423576_, DeltaTracker p_423570_) {
        if (p_423549_ instanceof LivingEntity livingentity) {
            MobEffectInstance mobeffectinstance = livingentity.getEffect(this.getMobEffect());
            if (mobeffectinstance != null) {
                float f = mobeffectinstance.isInfiniteDuration() ? 5.0F : Mth.lerp(Math.min(1.0F, mobeffectinstance.getDuration() / 20.0F), p_423576_, 5.0F);
                p_423496_.environmentalStart = f * 0.25F;
                p_423496_.environmentalEnd = f;
                p_423496_.skyEnd = f * 0.8F;
                p_423496_.cloudEnd = f * 0.8F;
            }
        }
    }

    @Override
    public float getModifiedDarkness(LivingEntity p_423634_, float p_423626_, float p_423501_) {
        MobEffectInstance mobeffectinstance = p_423634_.getEffect(this.getMobEffect());
        if (mobeffectinstance != null) {
            if (mobeffectinstance.endsWithin(19)) {
                p_423626_ = Math.max(mobeffectinstance.getDuration() / 20.0F, p_423626_);
            } else {
                p_423626_ = 1.0F;
            }
        }

        return p_423626_;
    }
}
