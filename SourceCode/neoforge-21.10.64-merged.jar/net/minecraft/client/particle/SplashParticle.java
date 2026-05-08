package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SplashParticle extends WaterDropParticle {
    public SplashParticle(
        ClientLevel p_107929_,
        double p_107930_,
        double p_107931_,
        double p_107932_,
        double p_107933_,
        double p_107934_,
        double p_107935_,
        TextureAtlasSprite p_446974_
    ) {
        super(p_107929_, p_107930_, p_107931_, p_107932_, p_446974_);
        this.gravity = 0.04F;
        if (p_107934_ == 0.0 && (p_107933_ != 0.0 || p_107935_ != 0.0)) {
            this.xd = p_107933_;
            this.yd = 0.1;
            this.zd = p_107935_;
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
            return new SplashParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprite.get(random));
        }
    }
}
