package net.minecraft.client.particle;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public abstract class SingleQuadParticle extends Particle {
    protected float quadSize;
    protected float rCol = 1.0F;
    protected float gCol = 1.0F;
    protected float bCol = 1.0F;
    protected float alpha = 1.0F;
    protected float roll;
    protected float oRoll;
    protected TextureAtlasSprite sprite;

    public SingleQuadParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z);
        this.sprite = sprite;
        this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
    }

    public SingleQuadParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        TextureAtlasSprite sprite
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprite = sprite;
        this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
    }

    public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
        return SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ;
    }

    public void extract(QuadParticleRenderState reusedState, Camera camera, float partialTick) {
        Quaternionf quaternionf = new Quaternionf();
        this.getFacingCameraMode().setRotation(quaternionf, camera, partialTick);
        if (this.roll != 0.0F) {
            quaternionf.rotateZ(Mth.lerp(partialTick, this.oRoll, this.roll));
        }

        this.extractRotatedQuad(reusedState, camera, quaternionf, partialTick);
    }

    protected void extractRotatedQuad(QuadParticleRenderState reusedState, Camera camera, Quaternionf orientation, float partialTick) {
        Vec3 vec3 = camera.getPosition();
        float f = (float)(Mth.lerp((double)partialTick, this.xo, this.x) - vec3.x());
        float f1 = (float)(Mth.lerp((double)partialTick, this.yo, this.y) - vec3.y());
        float f2 = (float)(Mth.lerp((double)partialTick, this.zo, this.z) - vec3.z());
        this.extractRotatedQuad(reusedState, orientation, f, f1, f2, partialTick);
    }

    protected void extractRotatedQuad(
        QuadParticleRenderState reusedState, Quaternionf orientation, float x, float y, float z, float partialTick
    ) {
        reusedState.add(
            this.getLayer(),
            x,
            y,
            z,
            orientation.x,
            orientation.y,
            orientation.z,
            orientation.w,
            this.getQuadSize(partialTick),
            this.getU0(),
            this.getU1(),
            this.getV0(),
            this.getV1(),
            ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol),
            this.getLightColor(partialTick)
        );
    }

    public float getQuadSize(float scaleFactor) {
        return this.quadSize;
    }

    @Override
    public Particle scale(float scale) {
        this.quadSize *= scale;
        return super.scale(scale);
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.SINGLE_QUADS;
    }

    public void setSpriteFromAge(SpriteSet sprites) {
        if (!this.removed) {
            this.setSprite(sprites.get(this.age, this.lifetime));
        }
    }

    protected void setSprite(TextureAtlasSprite sprite) {
        this.sprite = sprite;
    }

    protected float getU0() {
        return this.sprite.getU0();
    }

    protected float getU1() {
        return this.sprite.getU1();
    }

    protected float getV0() {
        return this.sprite.getV0();
    }

    protected float getV1() {
        return this.sprite.getV1();
    }

    protected abstract SingleQuadParticle.Layer getLayer();

    public void setColor(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    protected void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
            + ", Pos ("
            + this.x
            + ","
            + this.y
            + ","
            + this.z
            + "), RGBA ("
            + this.rCol
            + ","
            + this.gCol
            + ","
            + this.bCol
            + ","
            + this.alpha
            + "), Age "
            + this.age;
    }

    @OnlyIn(Dist.CLIENT)
    public interface FacingCameraMode {
        SingleQuadParticle.FacingCameraMode LOOKAT_XYZ = (p_312316_, p_311843_, p_312119_) -> p_312316_.set(p_311843_.rotation());
        SingleQuadParticle.FacingCameraMode LOOKAT_Y = (p_312695_, p_312346_, p_312064_) -> p_312695_.set(
            0.0F, p_312346_.rotation().y, 0.0F, p_312346_.rotation().w
        );

        void setRotation(Quaternionf quaternion, Camera camera, float partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    public record Layer(boolean translucent, ResourceLocation textureAtlasLocation, RenderPipeline pipeline) {
        public static final SingleQuadParticle.Layer TERRAIN = new SingleQuadParticle.Layer(
            true, TextureAtlas.LOCATION_BLOCKS, RenderPipelines.TRANSLUCENT_PARTICLE
        );
        public static final SingleQuadParticle.Layer OPAQUE = new SingleQuadParticle.Layer(
            false, TextureAtlas.LOCATION_PARTICLES, RenderPipelines.OPAQUE_PARTICLE
        );
        public static final SingleQuadParticle.Layer TRANSLUCENT = new SingleQuadParticle.Layer(
            true, TextureAtlas.LOCATION_PARTICLES, RenderPipelines.TRANSLUCENT_PARTICLE
        );
    }
}
