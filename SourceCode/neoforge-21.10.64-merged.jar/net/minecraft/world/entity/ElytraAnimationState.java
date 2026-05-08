package net.minecraft.world.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ElytraAnimationState {
    private static final float DEFAULT_X_ROT = (float) (Math.PI / 12);
    private static final float DEFAULT_Z_ROT = (float) (-Math.PI / 12);
    private float rotX;
    private float rotY;
    private float rotZ;
    private float rotXOld;
    private float rotYOld;
    private float rotZOld;
    private final LivingEntity entity;

    public ElytraAnimationState(LivingEntity entity) {
        this.entity = entity;
    }

    public void tick() {
        this.rotXOld = this.rotX;
        this.rotYOld = this.rotY;
        this.rotZOld = this.rotZ;
        float f;
        float f1;
        float f2;
        if (this.entity.isFallFlying()) {
            float f3 = 1.0F;
            Vec3 vec3 = this.entity.getDeltaMovement();
            if (vec3.y < 0.0) {
                Vec3 vec31 = vec3.normalize();
                f3 = 1.0F - (float)Math.pow(-vec31.y, 1.5);
            }

            f = Mth.lerp(f3, (float) (Math.PI / 12), (float) (Math.PI / 9));
            f1 = Mth.lerp(f3, (float) (-Math.PI / 12), (float) (-Math.PI / 2));
            f2 = 0.0F;
        } else if (this.entity.isCrouching()) {
            f = (float) (Math.PI * 2.0 / 9.0);
            f1 = (float) (-Math.PI / 4);
            f2 = 0.08726646F;
        } else {
            f = (float) (Math.PI / 12);
            f1 = (float) (-Math.PI / 12);
            f2 = 0.0F;
        }

        this.rotX = this.rotX + (f - this.rotX) * 0.3F;
        this.rotY = this.rotY + (f2 - this.rotY) * 0.3F;
        this.rotZ = this.rotZ + (f1 - this.rotZ) * 0.3F;
    }

    public float getRotX(float partialTick) {
        return Mth.lerp(partialTick, this.rotXOld, this.rotX);
    }

    public float getRotY(float partialTick) {
        return Mth.lerp(partialTick, this.rotYOld, this.rotY);
    }

    public float getRotZ(float partialTick) {
        return Mth.lerp(partialTick, this.rotZOld, this.rotZ);
    }
}
