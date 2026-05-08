package net.minecraft.client.animation;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public record AnimationChannel(AnimationChannel.Target target, Keyframe... keyframes) {
    @OnlyIn(Dist.CLIENT)
    public interface Interpolation {
        Vector3f apply(Vector3f animationVecCache, float keyframeDelta, Keyframe[] keyframes, int currentKeyframeIdx, int nextKeyframeIdx, float scale);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Interpolations {
        public static final AnimationChannel.Interpolation LINEAR = (p_445149_, p_445150_, p_445151_, p_445152_, p_445153_, p_445154_) -> {
            Vector3fc vector3fc = p_445151_[p_445152_].postTarget();
            Vector3fc vector3fc1 = p_445151_[p_445153_].preTarget();
            return vector3fc.lerp(vector3fc1, p_445150_, p_445149_).mul(p_445154_);
        };
        public static final AnimationChannel.Interpolation CATMULLROM = (p_445155_, p_445156_, p_445157_, p_445158_, p_445159_, p_445160_) -> {
            Vector3fc vector3fc = p_445157_[Math.max(0, p_445158_ - 1)].postTarget();
            Vector3fc vector3fc1 = p_445157_[p_445158_].postTarget();
            Vector3fc vector3fc2 = p_445157_[p_445159_].postTarget();
            Vector3fc vector3fc3 = p_445157_[Math.min(p_445157_.length - 1, p_445159_ + 1)].postTarget();
            p_445155_.set(
                Mth.catmullrom(p_445156_, vector3fc.x(), vector3fc1.x(), vector3fc2.x(), vector3fc3.x()) * p_445160_,
                Mth.catmullrom(p_445156_, vector3fc.y(), vector3fc1.y(), vector3fc2.y(), vector3fc3.y()) * p_445160_,
                Mth.catmullrom(p_445156_, vector3fc.z(), vector3fc1.z(), vector3fc2.z(), vector3fc3.z()) * p_445160_
            );
            return p_445155_;
        };
    }

    @OnlyIn(Dist.CLIENT)
    public interface Target {
        void apply(ModelPart modelPart, Vector3f animationVector);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Targets {
        public static final AnimationChannel.Target POSITION = ModelPart::offsetPos;
        public static final AnimationChannel.Target ROTATION = ModelPart::offsetRotation;
        public static final AnimationChannel.Target SCALE = ModelPart::offsetScale;
    }
}
