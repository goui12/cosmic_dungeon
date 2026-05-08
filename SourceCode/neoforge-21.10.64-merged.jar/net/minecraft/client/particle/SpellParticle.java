package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpellParticle extends SingleQuadParticle {
    private static final RandomSource RANDOM = RandomSource.create();
    private final SpriteSet sprites;
    private float originalAlpha = 1.0F;

    public SpellParticle(
        ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites
    ) {
        super(level, x, y, z, 0.5 - RANDOM.nextDouble(), ySpeed, 0.5 - RANDOM.nextDouble(), sprites.first());
        this.friction = 0.96F;
        this.gravity = -0.1F;
        this.speedUpWhenYMotionIsBlocked = true;
        this.sprites = sprites;
        this.yd *= 0.2F;
        if (xSpeed == 0.0 && zSpeed == 0.0) {
            this.xd *= 0.1F;
            this.zd *= 0.1F;
        }

        this.quadSize *= 0.75F;
        this.lifetime = (int)(8.0 / (Math.random() * 0.8 + 0.2));
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
        if (this.isCloseToScopingPlayer()) {
            this.setAlpha(0.0F);
        }
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        if (this.isCloseToScopingPlayer()) {
            this.alpha = 0.0F;
        } else {
            this.alpha = Mth.lerp(0.05F, this.alpha, this.originalAlpha);
        }
    }

    @Override
    protected void setAlpha(float alpha) {
        super.setAlpha(alpha);
        this.originalAlpha = alpha;
    }

    private boolean isCloseToScopingPlayer() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localplayer = minecraft.player;
        return localplayer != null
            && localplayer.getEyePosition().distanceToSqr(this.x, this.y, this.z) <= 9.0
            && minecraft.options.getCameraType().isFirstPerson()
            && localplayer.isScoping();
    }

    @OnlyIn(Dist.CLIENT)
    public static class InstantProvider implements ParticleProvider<SpellParticleOption> {
        private final SpriteSet sprite;

        public InstantProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SpellParticleOption particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            SpellParticle spellparticle = new SpellParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprite);
            spellparticle.setColor(particleType.getRed(), particleType.getGreen(), particleType.getBlue());
            spellparticle.setPower(particleType.getPower());
            return spellparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class MobEffectProvider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprite;

        public MobEffectProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            ColorParticleOption particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            SpellParticle spellparticle = new SpellParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprite);
            spellparticle.setColor(particleType.getRed(), particleType.getGreen(), particleType.getBlue());
            spellparticle.setAlpha(particleType.getAlpha());
            return spellparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprites) {
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
            return new SpellParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprite);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class WitchProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public WitchProvider(SpriteSet sprites) {
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
            SpellParticle spellparticle = new SpellParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprite);
            float f = random.nextFloat() * 0.5F + 0.35F;
            spellparticle.setColor(1.0F * f, 0.0F * f, 1.0F * f);
            return spellparticle;
        }
    }
}
