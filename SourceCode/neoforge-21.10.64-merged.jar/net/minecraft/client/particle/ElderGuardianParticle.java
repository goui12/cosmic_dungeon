package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.GuardianParticleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ElderGuardianRenderer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ElderGuardianParticle extends Particle {
    protected final GuardianParticleModel model;
    protected final RenderType renderType = RenderType.entityTranslucent(ElderGuardianRenderer.GUARDIAN_ELDER_LOCATION);

    public ElderGuardianParticle(ClientLevel p_445632_, double p_447245_, double p_446032_, double p_447236_) {
        super(p_445632_, p_447245_, p_446032_, p_447236_);
        this.model = new GuardianParticleModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ELDER_GUARDIAN));
        this.gravity = 0.0F;
        this.lifetime = 30;
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.ELDER_GUARDIANS;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(
            SimpleParticleType p_447107_,
            ClientLevel p_447137_,
            double p_446017_,
            double p_446111_,
            double p_447335_,
            double p_446937_,
            double p_445630_,
            double p_445856_,
            RandomSource p_447240_
        ) {
            return new ElderGuardianParticle(p_447137_, p_446017_, p_446111_, p_447335_);
        }
    }
}
