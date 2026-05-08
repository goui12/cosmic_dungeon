package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyParticle extends SingleQuadParticle {
    private static final float PARTICLE_FADE_OUT_LIGHT_TIME = 0.3F;
    private static final float PARTICLE_FADE_IN_LIGHT_TIME = 0.1F;
    private static final float PARTICLE_FADE_OUT_ALPHA_TIME = 0.5F;
    private static final float PARTICLE_FADE_IN_ALPHA_TIME = 0.3F;
    private static final int PARTICLE_MIN_LIFETIME = 200;
    private static final int PARTICLE_MAX_LIFETIME = 300;

    public FireflyParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        TextureAtlasSprite sprite
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprite);
        this.speedUpWhenYMotionIsBlocked = true;
        this.friction = 0.96F;
        this.quadSize *= 0.75F;
        this.yd *= 0.8F;
        this.xd *= 0.8F;
        this.zd *= 0.8F;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return (int)(255.0F * getFadeAmount(this.getLifetimeProgress(this.age + partialTick), 0.1F, 0.3F));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).isAir()) {
            this.remove();
        } else {
            this.setAlpha(getFadeAmount(this.getLifetimeProgress(this.age), 0.3F, 0.5F));
            if (Math.random() > 0.95 || this.age == 1) {
                this.setParticleSpeed(-0.05F + 0.1F * Math.random(), -0.05F + 0.1F * Math.random(), -0.05F + 0.1F * Math.random());
            }
        }
    }

    private float getLifetimeProgress(float age) {
        return Mth.clamp(age / this.lifetime, 0.0F, 1.0F);
    }

    private static float getFadeAmount(float lifetimeProgress, float fadeIn, float fadeOut) {
        if (lifetimeProgress >= 1.0F - fadeIn) {
            return (1.0F - lifetimeProgress) / fadeIn;
        } else {
            return lifetimeProgress <= fadeOut ? lifetimeProgress / fadeOut : 1.0F;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class FireflyProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public FireflyProvider(SpriteSet sprite) {
            this.sprite = sprite;
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
            FireflyParticle fireflyparticle = new FireflyParticle(
                level,
                x,
                y,
                z,
                0.5 - random.nextDouble(),
                random.nextBoolean() ? ySpeed : -ySpeed,
                0.5 - random.nextDouble(),
                this.sprite.get(random)
            );
            fireflyparticle.setLifetime(random.nextIntBetweenInclusive(200, 300));
            fireflyparticle.scale(1.5F);
            fireflyparticle.setAlpha(0.0F);
            return fireflyparticle;
        }
    }
}
