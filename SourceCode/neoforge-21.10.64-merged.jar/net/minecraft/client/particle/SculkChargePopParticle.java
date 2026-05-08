package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SculkChargePopParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    public SculkChargePopParticle(
        ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprite
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprite.first());
        this.friction = 0.96F;
        this.sprites = sprite;
        this.scale(1.0F);
        this.hasPhysics = false;
        this.setSpriteFromAge(sprite);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }

    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprite) implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(
            SimpleParticleType p_447163_,
            ClientLevel p_233950_,
            double p_233951_,
            double p_233952_,
            double p_233953_,
            double p_233954_,
            double p_233955_,
            double p_233956_,
            RandomSource p_446929_
        ) {
            SculkChargePopParticle sculkchargepopparticle = new SculkChargePopParticle(
                p_233950_, p_233951_, p_233952_, p_233953_, p_233954_, p_233955_, p_233956_, this.sprite
            );
            sculkchargepopparticle.setAlpha(1.0F);
            sculkchargepopparticle.setParticleSpeed(p_233954_, p_233955_, p_233956_);
            sculkchargepopparticle.setLifetime(p_446929_.nextInt(4) + 6);
            return sculkchargepopparticle;
        }
    }
}
