package net.goui.cosmicdungeon.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class DragoonLightningParticle extends SimpleAnimatedParticle {
    protected DragoonLightningParticle(ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed,
                                       SpriteSet sprites) {
        super(level, x, y, z, sprites, 0.0F);
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.friction = 1.0F;
        this.lifetime = 6;
        this.quadSize = 0.35F;
        this.setAlpha(0.85F);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setAlpha(Math.max(0.0F, 0.85F * (1.0F - (float) this.age / (float) this.lifetime)));
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
            return new DragoonLightningParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
