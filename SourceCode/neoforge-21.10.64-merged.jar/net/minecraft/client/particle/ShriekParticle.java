package net.minecraft.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class ShriekParticle extends SingleQuadParticle {
    private static final float MAGICAL_X_ROT = 1.0472F;
    private int delay;

    public ShriekParticle(ClientLevel level, double x, double y, double z, int delay, TextureAtlasSprite sprite) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
        this.quadSize = 0.85F;
        this.delay = delay;
        this.lifetime = 30;
        this.gravity = 0.0F;
        this.xd = 0.0;
        this.yd = 0.1;
        this.zd = 0.0;
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        return this.quadSize * Mth.clamp((this.age + scaleFactor) / this.lifetime * 0.75F, 0.0F, 1.0F);
    }

    @Override
    public void extract(QuadParticleRenderState reusedState, Camera camera, float partialTick) {
        if (this.delay <= 0) {
            this.alpha = 1.0F - Mth.clamp((this.age + partialTick) / this.lifetime, 0.0F, 1.0F);
            Quaternionf quaternionf = new Quaternionf();
            quaternionf.rotationX(-1.0472F);
            this.extractRotatedQuad(reusedState, camera, quaternionf, partialTick);
            quaternionf.rotationYXZ((float) -Math.PI, 1.0472F, 0.0F);
            this.extractRotatedQuad(reusedState, camera, quaternionf, partialTick);
        }
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
        if (this.delay > 0) {
            this.delay--;
        } else {
            super.tick();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<ShriekParticleOption> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            ShriekParticleOption particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            ShriekParticle shriekparticle = new ShriekParticle(level, x, y, z, particleType.getDelay(), this.sprite.get(random));
            shriekparticle.setAlpha(1.0F);
            return shriekparticle;
        }
    }
}
