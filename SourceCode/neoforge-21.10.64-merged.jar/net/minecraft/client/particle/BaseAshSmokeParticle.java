package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class BaseAshSmokeParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    public BaseAshSmokeParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        float xSeedMultiplier,
        float ySpeedMultiplier,
        float zSpeedMultiplier,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        float quadSizeMultiplier,
        SpriteSet sprites,
        float rColMultiplier,
        int lifetime,
        float gravity,
        boolean hasPhysics
    ) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprites.first());
        this.friction = 0.96F;
        this.gravity = gravity;
        this.speedUpWhenYMotionIsBlocked = true;
        this.sprites = sprites;
        this.xd *= xSeedMultiplier;
        this.yd *= ySpeedMultiplier;
        this.zd *= zSpeedMultiplier;
        this.xd += xSpeed;
        this.yd += ySpeed;
        this.zd += zSpeed;
        float f = this.random.nextFloat() * rColMultiplier;
        this.rCol = f;
        this.gCol = f;
        this.bCol = f;
        this.quadSize *= 0.75F * quadSizeMultiplier;
        this.lifetime = (int)(lifetime / (this.random.nextFloat() * 0.8 + 0.2) * quadSizeMultiplier);
        this.lifetime = Math.max(this.lifetime, 1);
        this.setSpriteFromAge(sprites);
        this.hasPhysics = hasPhysics;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        return this.quadSize * Mth.clamp((this.age + scaleFactor) / this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }
}
