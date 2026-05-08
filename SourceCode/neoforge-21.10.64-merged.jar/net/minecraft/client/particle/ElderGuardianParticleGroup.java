package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ElderGuardianParticleGroup extends ParticleGroup<ElderGuardianParticle> {
    public ElderGuardianParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTick) {
        return new ElderGuardianParticleGroup.State(
            this.particles
                .stream()
                .map(p_445817_ -> ElderGuardianParticleGroup.ElderGuardianParticleRenderState.fromParticle(p_445817_, camera, partialTick))
                .toList()
        );
    }

    @OnlyIn(Dist.CLIENT)
    record ElderGuardianParticleRenderState(Model<Unit> model, PoseStack poseStack, RenderType renderType, int color) {
        public static ElderGuardianParticleGroup.ElderGuardianParticleRenderState fromParticle(
            ElderGuardianParticle particle, Camera camera, float partialTick
        ) {
            float f = (particle.age + partialTick) / particle.lifetime;
            float f1 = 0.05F + 0.5F * Mth.sin(f * (float) Math.PI);
            int i = ARGB.colorFromFloat(f1, 1.0F, 1.0F, 1.0F);
            PoseStack posestack = new PoseStack();
            posestack.pushPose();
            posestack.mulPose(camera.rotation());
            posestack.mulPose(Axis.XP.rotationDegrees(60.0F - 150.0F * f));
            float f2 = 0.42553192F;
            posestack.scale(0.42553192F, -0.42553192F, -0.42553192F);
            posestack.translate(0.0F, -0.56F, 3.5F);
            return new ElderGuardianParticleGroup.ElderGuardianParticleRenderState(particle.model, posestack, particle.renderType, i);
        }
    }

    @OnlyIn(Dist.CLIENT)
    record State(List<ElderGuardianParticleGroup.ElderGuardianParticleRenderState> states) implements ParticleGroupRenderState {
        @Override
        public void submit(SubmitNodeCollector p_446065_, CameraRenderState p_451135_) {
            for (ElderGuardianParticleGroup.ElderGuardianParticleRenderState elderguardianparticlegroup$elderguardianparticlerenderstate : this.states) {
                p_446065_.submitModel(
                    elderguardianparticlegroup$elderguardianparticlerenderstate.model,
                    Unit.INSTANCE,
                    elderguardianparticlegroup$elderguardianparticlerenderstate.poseStack,
                    elderguardianparticlegroup$elderguardianparticlerenderstate.renderType,
                    15728880,
                    OverlayTexture.NO_OVERLAY,
                    elderguardianparticlegroup$elderguardianparticlerenderstate.color,
                    null,
                    0,
                    null
                );
            }
        }
    }
}
