package net.minecraft.client.model.geom;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record PartPose(float x, float y, float z, float xRot, float yRot, float zRot, float xScale, float yScale, float zScale) {
    public static final PartPose ZERO = offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    public static PartPose offset(float x, float y, float z) {
        return offsetAndRotation(x, y, z, 0.0F, 0.0F, 0.0F);
    }

    public static PartPose rotation(float xRot, float yRot, float zRot) {
        return offsetAndRotation(0.0F, 0.0F, 0.0F, xRot, yRot, zRot);
    }

    public static PartPose offsetAndRotation(float x, float y, float z, float xRot, float yRot, float zRot) {
        return new PartPose(x, y, z, xRot, yRot, zRot, 1.0F, 1.0F, 1.0F);
    }

    public PartPose translated(float x, float y, float z) {
        return new PartPose(this.x + x, this.y + y, this.z + z, this.xRot, this.yRot, this.zRot, this.xScale, this.yScale, this.zScale);
    }

    public PartPose withScale(float scale) {
        return new PartPose(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot, scale, scale, scale);
    }

    public PartPose scaled(float scale) {
        return scale == 1.0F ? this : this.scaled(scale, scale, scale);
    }

    public PartPose scaled(float x, float y, float z) {
        return new PartPose(
            this.x * x,
            this.y * y,
            this.z * z,
            this.xRot,
            this.yRot,
            this.zRot,
            this.xScale * x,
            this.yScale * y,
            this.zScale * z
        );
    }
}
