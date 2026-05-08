package net.minecraft.client.particle;

import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SuspendedParticle extends SingleQuadParticle {
    public SuspendedParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y - 0.125, z, sprite);
        this.setSize(0.01F, 0.01F);
        this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.2F);
        this.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.gravity = 0.0F;
    }

    public SuspendedParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        TextureAtlasSprite sprite
    ) {
        super(level, x, y - 0.125, z, xSpeed, ySpeed, zSpeed, sprite);
        this.setSize(0.01F, 0.01F);
        this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.6F);
        this.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.gravity = 0.0F;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class CrimsonSporeProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public CrimsonSporeProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            double d0 = random.nextGaussian() * 1.0E-6F;
            double d1 = random.nextGaussian() * 1.0E-4F;
            double d2 = random.nextGaussian() * 1.0E-6F;
            SuspendedParticle suspendedparticle = new SuspendedParticle(level, x, y, z, d0, d1, d2, this.sprite.get(random));
            suspendedparticle.setColor(0.9F, 0.4F, 0.5F);
            return suspendedparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class SporeBlossomAirProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public SporeBlossomAirProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel p_level,
            double p_x,
            double p_y,
            double p_z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource p_random
        ) {
            SuspendedParticle suspendedparticle = new SuspendedParticle(p_level, p_x, p_y, p_z, 0.0, -0.8F, 0.0, this.sprite.get(p_random)) {
                @Override
                public Optional<ParticleLimit> getParticleLimit() {
                    return Optional.of(ParticleLimit.SPORE_BLOSSOM);
                }
            };
            suspendedparticle.lifetime = Mth.randomBetweenInclusive(p_random, 500, 1000);
            suspendedparticle.gravity = 0.01F;
            suspendedparticle.setColor(0.32F, 0.5F, 0.22F);
            return suspendedparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class UnderwaterProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public UnderwaterProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            SuspendedParticle suspendedparticle = new SuspendedParticle(level, x, y, z, this.sprite.get(random));
            suspendedparticle.setColor(0.4F, 0.4F, 0.7F);
            return suspendedparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class WarpedSporeProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public WarpedSporeProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            double d0 = random.nextFloat() * -1.9 * random.nextFloat() * 0.1;
            SuspendedParticle suspendedparticle = new SuspendedParticle(level, x, y, z, 0.0, d0, 0.0, this.sprite.get(random));
            suspendedparticle.setColor(0.1F, 0.1F, 0.3F);
            suspendedparticle.setSize(0.001F, 0.001F);
            return suspendedparticle;
        }
    }
}
