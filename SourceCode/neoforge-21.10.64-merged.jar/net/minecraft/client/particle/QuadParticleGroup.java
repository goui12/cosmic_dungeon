package net.minecraft.client.particle;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class QuadParticleGroup extends ParticleGroup<SingleQuadParticle> {
    private final ParticleRenderType particleType;
    final QuadParticleRenderState particleTypeRenderState = new QuadParticleRenderState();

    public QuadParticleGroup(ParticleEngine engine, ParticleRenderType particleType) {
        super(engine);
        this.particleType = particleType;
    }

    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTick) {
        for (SingleQuadParticle singlequadparticle : this.particles) {
            if (frustum.pointInFrustum(singlequadparticle.x, singlequadparticle.y, singlequadparticle.z)) {
                try {
                    singlequadparticle.extract(this.particleTypeRenderState, camera, partialTick);
                } catch (Throwable throwable) {
                    CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering Particle");
                    CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being rendered");
                    crashreportcategory.setDetail("Particle", singlequadparticle::toString);
                    crashreportcategory.setDetail("Particle Type", this.particleType::toString);
                    throw new ReportedException(crashreport);
                }
            }
        }

        return this.particleTypeRenderState;
    }
}
