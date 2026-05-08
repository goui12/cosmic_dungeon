package com.mojang.blaze3d.vertex;

import com.mojang.math.MatrixUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class PoseStack implements net.neoforged.neoforge.client.extensions.IPoseStackExtension {
    private final List<PoseStack.Pose> poses = new ArrayList<>(16);
    private int lastIndex;

    public PoseStack() {
        this.poses.add(new PoseStack.Pose());
    }

    public void translate(double x, double y, double z) {
        this.translate((float)x, (float)y, (float)z);
    }

    public void translate(float x, float y, float z) {
        this.last().translate(x, y, z);
    }

    public void translate(Vec3 vector) {
        this.translate(vector.x, vector.y, vector.z);
    }

    public void scale(float x, float y, float z) {
        this.last().scale(x, y, z);
    }

    public void mulPose(Quaternionfc pose) {
        this.last().rotate(pose);
    }

    public void rotateAround(Quaternionfc quaternion, float x, float y, float z) {
        this.last().rotateAround(quaternion, x, y, z);
    }

    public void pushPose() {
        PoseStack.Pose posestack$pose = this.last();
        this.lastIndex++;
        if (this.lastIndex >= this.poses.size()) {
            this.poses.add(posestack$pose.copy());
        } else {
            this.poses.get(this.lastIndex).set(posestack$pose);
        }
    }

    public void popPose() {
        if (this.lastIndex == 0) {
            throw new NoSuchElementException();
        } else {
            this.lastIndex--;
        }
    }

    public PoseStack.Pose last() {
        return this.poses.get(this.lastIndex);
    }

    public boolean isEmpty() {
        return this.lastIndex == 0;
    }

    public void setIdentity() {
        this.last().setIdentity();
    }

    public void mulPose(Matrix4fc pose) {
        this.last().mulPose(pose);
    }

    @OnlyIn(Dist.CLIENT)
    public static final class Pose {
        private final Matrix4f pose = new Matrix4f();
        private final Matrix3f normal = new Matrix3f();
        private boolean trustedNormals = true;

        private void computeNormalMatrix() {
            this.normal.set(this.pose).invert().transpose();
            this.trustedNormals = false;
        }

        public void set(PoseStack.Pose pose) {
            this.pose.set(pose.pose);
            this.normal.set(pose.normal);
            this.trustedNormals = pose.trustedNormals;
        }

        public Matrix4f pose() {
            return this.pose;
        }

        public Matrix3f normal() {
            return this.normal;
        }

        public Vector3f transformNormal(Vector3fc pos, Vector3f destination) {
            return this.transformNormal(pos.x(), pos.y(), pos.z(), destination);
        }

        public Vector3f transformNormal(float x, float y, float z, Vector3f destination) {
            Vector3f vector3f = this.normal.transform(x, y, z, destination);
            return this.trustedNormals ? vector3f : vector3f.normalize();
        }

        public Matrix4f translate(float x, float y, float z) {
            return this.pose.translate(x, y, z);
        }

        public void scale(float x, float y, float z) {
            this.pose.scale(x, y, z);
            if (Math.abs(x) == Math.abs(y) && Math.abs(y) == Math.abs(z)) {
                if (x < 0.0F || y < 0.0F || z < 0.0F) {
                    this.normal.scale(Math.signum(x), Math.signum(y), Math.signum(z));
                }
            } else {
                this.normal.scale(1.0F / x, 1.0F / y, 1.0F / z);
                this.trustedNormals = false;
            }
        }

        public void rotate(Quaternionfc pose) {
            this.pose.rotate(pose);
            this.normal.rotate(pose);
        }

        public void rotateAround(Quaternionfc pose, float x, float y, float z) {
            this.pose.rotateAround(pose, x, y, z);
            this.normal.rotate(pose);
        }

        public void setIdentity() {
            this.pose.identity();
            this.normal.identity();
            this.trustedNormals = true;
        }

        public void mulPose(Matrix4fc pose) {
            this.pose.mul(pose);
            if (!MatrixUtil.isPureTranslation(pose)) {
                if (MatrixUtil.isOrthonormal(pose)) {
                    this.normal.mul(new Matrix3f(pose));
                } else {
                    this.computeNormalMatrix();
                }
            }
        }

        public PoseStack.Pose copy() {
            PoseStack.Pose posestack$pose = new PoseStack.Pose();
            posestack$pose.set(this);
            return posestack$pose;
        }
    }
}
