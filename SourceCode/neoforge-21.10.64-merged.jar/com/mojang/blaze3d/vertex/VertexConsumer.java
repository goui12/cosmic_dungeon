package com.mojang.blaze3d.vertex;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public interface VertexConsumer extends net.neoforged.neoforge.client.extensions.IVertexConsumerExtension {
    VertexConsumer addVertex(float x, float y, float z);

    VertexConsumer setColor(int red, int green, int blue, int alpha);

    VertexConsumer setUv(float u, float v);

    VertexConsumer setUv1(int u, int v);

    VertexConsumer setUv2(int u, int v);

    VertexConsumer setNormal(float normalX, float normalY, float normalZ);

    default void addVertex(
        float x,
        float y,
        float z,
        int color,
        float u,
        float v,
        int packedOverlay,
        int packedLight,
        float normalX,
        float normalY,
        float normalZ
    ) {
        this.addVertex(x, y, z);
        this.setColor(color);
        this.setUv(u, v);
        this.setOverlay(packedOverlay);
        this.setLight(packedLight);
        this.setNormal(normalX, normalY, normalZ);
    }

    default VertexConsumer setColor(float red, float green, float blue, float alpha) {
        return this.setColor((int)(red * 255.0F), (int)(green * 255.0F), (int)(blue * 255.0F), (int)(alpha * 255.0F));
    }

    default VertexConsumer setColor(int color) {
        return this.setColor(ARGB.red(color), ARGB.green(color), ARGB.blue(color), ARGB.alpha(color));
    }

    default VertexConsumer setLight(int packedLight) {
        return this.setUv2(packedLight & 65535, packedLight >> 16 & 65535);
    }

    default VertexConsumer setOverlay(int packedOverlay) {
        return this.setUv1(packedOverlay & 65535, packedOverlay >> 16 & 65535);
    }

    default void putBulkData(
        PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay
    ) {
        this.putBulkData(
            pose,
            quad,
            new float[]{1.0F, 1.0F, 1.0F, 1.0F},
            red,
            green,
            blue,
            alpha,
            new int[]{packedLight, packedLight, packedLight, packedLight},
            packedOverlay,
            false
        );
    }

    default void putBulkData(
        PoseStack.Pose pose,
        BakedQuad quad,
        float[] brightness,
        float red,
        float green,
        float blue,
        float alpha,
        int[] lightmap,
        int packedOverlay,
        boolean readExistingColor
    ) {
        int[] aint = quad.vertices();
        Vector3fc vector3fc = quad.direction().getUnitVec3f();
        Matrix4f matrix4f = pose.pose();
        Vector3f vector3f = pose.transformNormal(vector3fc, new Vector3f());
        int i = 8;
        int j = aint.length / 8;
        int k = (int)(alpha * 255.0F);
        int l = quad.lightEmission();

        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
            IntBuffer intbuffer = bytebuffer.asIntBuffer();

            for (int i1 = 0; i1 < j; i1++) {
                intbuffer.clear();
                intbuffer.put(aint, i1 * 8, 8);
                float f = bytebuffer.getFloat(0);
                float f1 = bytebuffer.getFloat(4);
                float f2 = bytebuffer.getFloat(8);
                float f3;
                float f4;
                float f5;
                if (readExistingColor) {
                    float f6 = bytebuffer.get(12) & 255;
                    float f7 = bytebuffer.get(13) & 255;
                    float f8 = bytebuffer.get(14) & 255;
                    f3 = f6 * brightness[i1] * red;
                    f4 = f7 * brightness[i1] * green;
                    f5 = f8 * brightness[i1] * blue;
                } else {
                    f3 = brightness[i1] * red * 255.0F;
                    f4 = brightness[i1] * green * 255.0F;
                    f5 = brightness[i1] * blue * 255.0F;
                }

                // Neo: also apply alpha that's coming from the baked quad
                int vertexAlpha = readExistingColor ? (int)((alpha * (float) (bytebuffer.get(15) & 255) / 255.0F) * 255) : k;
                int j1 = ARGB.color(vertexAlpha, (int)f3, (int)f4, (int)f5);
                int k1 = applyBakedLighting(LightTexture.lightCoordsWithEmission(lightmap[i1], l), bytebuffer);
                float f10 = bytebuffer.getFloat(16);
                float f9 = bytebuffer.getFloat(20);
                Vector3f vector3f1 = matrix4f.transformPosition(f, f1, f2, new Vector3f());
                applyBakedNormals(vector3f, bytebuffer, pose.normal());
                this.addVertex(vector3f1.x(), vector3f1.y(), vector3f1.z(), j1, f10, f9, packedOverlay, k1, vector3f.x(), vector3f.y(), vector3f.z());
            }
        }
    }

    default VertexConsumer addVertex(Vector3f pos) {
        return this.addVertex(pos.x(), pos.y(), pos.z());
    }

    default VertexConsumer addVertex(PoseStack.Pose pose, Vector3f pos) {
        return this.addVertex(pose, pos.x(), pos.y(), pos.z());
    }

    default VertexConsumer addVertex(PoseStack.Pose pose, float x, float y, float z) {
        return this.addVertex(pose.pose(), x, y, z);
    }

    default VertexConsumer addVertex(Matrix4f pose, float x, float y, float z) {
        Vector3f vector3f = pose.transformPosition(x, y, z, new Vector3f());
        return this.addVertex(vector3f.x(), vector3f.y(), vector3f.z());
    }

    default VertexConsumer addVertexWith2DPose(Matrix3x2f pose, float x, float y) {
        Vector2f vector2f = pose.transformPosition(x, y, new Vector2f());
        return this.addVertex(vector2f.x(), vector2f.y(), 0.0F);
    }

    default VertexConsumer setNormal(PoseStack.Pose pose, float normalX, float normalY, float normalZ) {
        Vector3f vector3f = pose.transformNormal(normalX, normalY, normalZ, new Vector3f());
        return this.setNormal(vector3f.x(), vector3f.y(), vector3f.z());
    }

    default VertexConsumer setNormal(PoseStack.Pose pose, Vector3f normalVector) {
        return this.setNormal(pose, normalVector.x(), normalVector.y(), normalVector.z());
    }
}
