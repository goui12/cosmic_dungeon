package net.minecraft.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class DustColorTransitionParticle extends DustParticleBase<DustColorTransitionOptions> {
    private final Vector3f fromColor;
    private final Vector3f toColor;

    public DustColorTransitionParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        DustColorTransitionOptions options,
        SpriteSet sprites
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, options, sprites);
        float f = this.random.nextFloat() * 0.4F + 0.6F;
        this.fromColor = this.randomizeColor(options.getFromColor(), f);
        this.toColor = this.randomizeColor(options.getToColor(), f);
    }

    private Vector3f randomizeColor(Vector3f vector, float multiplier) {
        return new Vector3f(
            this.randomizeColor(vector.x(), multiplier), this.randomizeColor(vector.y(), multiplier), this.randomizeColor(vector.z(), multiplier)
        );
    }

    private void lerpColors(float partialTick) {
        float f = (this.age + partialTick) / (this.lifetime + 1.0F);
        Vector3f vector3f = new Vector3f(this.fromColor).lerp(this.toColor, f);
        this.rCol = vector3f.x();
        this.gCol = vector3f.y();
        this.bCol = vector3f.z();
    }

    @Override
    public void extract(QuadParticleRenderState reusedState, Camera camera, float partialTick) {
        this.lerpColors(partialTick);
        super.extract(reusedState, camera, partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<DustColorTransitionOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(
            DustColorTransitionOptions particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            return new DustColorTransitionParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, particleType, this.sprites);
        }
    }
}
