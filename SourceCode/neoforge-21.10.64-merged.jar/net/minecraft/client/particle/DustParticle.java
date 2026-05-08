package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class DustParticle extends DustParticleBase<DustParticleOptions> {
    public DustParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        DustParticleOptions options,
        SpriteSet sprites
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, options, sprites);
        float f = this.random.nextFloat() * 0.4F + 0.6F;
        Vector3f vector3f = options.getColor();
        this.rCol = this.randomizeColor(vector3f.x(), f);
        this.gCol = this.randomizeColor(vector3f.y(), f);
        this.bCol = this.randomizeColor(vector3f.z(), f);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<DustParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(
            DustParticleOptions particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            return new DustParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, particleType, this.sprites);
        }
    }
}
