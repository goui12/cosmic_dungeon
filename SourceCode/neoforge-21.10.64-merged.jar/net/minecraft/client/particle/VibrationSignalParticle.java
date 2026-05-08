package net.minecraft.client.particle;

import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class VibrationSignalParticle extends SingleQuadParticle {
    private final PositionSource target;
    private float rot;
    private float rotO;
    private float pitch;
    private float pitchO;

    public VibrationSignalParticle(
        ClientLevel level, double x, double y, double z, PositionSource target, int lifetime, TextureAtlasSprite sprite
    ) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
        this.quadSize = 0.3F;
        this.target = target;
        this.lifetime = lifetime;
        Optional<Vec3> optional = target.getPosition(level);
        if (optional.isPresent()) {
            Vec3 vec3 = optional.get();
            double d0 = x - vec3.x();
            double d1 = y - vec3.y();
            double d2 = z - vec3.z();
            this.rotO = this.rot = (float)Mth.atan2(d0, d2);
            this.pitchO = this.pitch = (float)Mth.atan2(d1, Math.sqrt(d0 * d0 + d2 * d2));
        }
    }

    @Override
    public void extract(QuadParticleRenderState reusedState, Camera camera, float partialTick) {
        float f = Mth.sin((this.age + partialTick - (float) (Math.PI * 2)) * 0.05F) * 2.0F;
        float f1 = Mth.lerp(partialTick, this.rotO, this.rot);
        float f2 = Mth.lerp(partialTick, this.pitchO, this.pitch) + (float) (Math.PI / 2);
        Quaternionf quaternionf = new Quaternionf();
        quaternionf.rotationY(f1).rotateX(-f2).rotateY(f);
        this.extractRotatedQuad(reusedState, camera, quaternionf, partialTick);
        quaternionf.rotationY((float) -Math.PI + f1).rotateX(f2).rotateY(f);
        this.extractRotatedQuad(reusedState, camera, quaternionf, partialTick);
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
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            Optional<Vec3> optional = this.target.getPosition(this.level);
            if (optional.isEmpty()) {
                this.remove();
            } else {
                int i = this.lifetime - this.age;
                double d0 = 1.0 / i;
                Vec3 vec3 = optional.get();
                this.x = Mth.lerp(d0, this.x, vec3.x());
                this.y = Mth.lerp(d0, this.y, vec3.y());
                this.z = Mth.lerp(d0, this.z, vec3.z());
                this.setPos(this.x, this.y, this.z); // FORGE: Update the particle's bounding box
                double d1 = this.x - vec3.x();
                double d2 = this.y - vec3.y();
                double d3 = this.z - vec3.z();
                this.rotO = this.rot;
                this.rot = (float)Mth.atan2(d1, d3);
                this.pitchO = this.pitch;
                this.pitch = (float)Mth.atan2(d2, Math.sqrt(d1 * d1 + d3 * d3));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<VibrationParticleOption> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            VibrationParticleOption particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            VibrationSignalParticle vibrationsignalparticle = new VibrationSignalParticle(
                level, x, y, z, particleType.getDestination(), particleType.getArrivalInTicks(), this.sprite.get(random)
            );
            vibrationsignalparticle.setAlpha(1.0F);
            return vibrationsignalparticle;
        }
    }
}
