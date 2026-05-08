package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ShapeRenderer {
    public static void renderShape(
        PoseStack poseStack, VertexConsumer buffer, VoxelShape shape, double x, double y, double z, int color
    ) {
        PoseStack.Pose posestack$pose = poseStack.last();
        shape.forAllEdges(
            (p_365487_, p_362038_, p_363457_, p_361045_, p_364563_, p_362830_) -> {
                Vector3f vector3f = new Vector3f((float)(p_361045_ - p_365487_), (float)(p_364563_ - p_362038_), (float)(p_362830_ - p_363457_)).normalize();
                buffer.addVertex(posestack$pose, (float)(p_365487_ + x), (float)(p_362038_ + y), (float)(p_363457_ + z))
                    .setColor(color)
                    .setNormal(posestack$pose, vector3f);
                buffer.addVertex(posestack$pose, (float)(p_361045_ + x), (float)(p_364563_ + y), (float)(p_362830_ + z))
                    .setColor(color)
                    .setNormal(posestack$pose, vector3f);
            }
        );
    }

    public static void renderLineBox(
        PoseStack.Pose pose, VertexConsumer consumer, AABB box, float red, float green, float blue, float alpha
    ) {
        renderLineBox(
            pose,
            consumer,
            box.minX,
            box.minY,
            box.minZ,
            box.maxX,
            box.maxY,
            box.maxZ,
            red,
            green,
            blue,
            alpha,
            red,
            green,
            blue
        );
    }

    public static void renderLineBox(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        renderLineBox(
            pose,
            consumer,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            red,
            green,
            blue,
            alpha,
            red,
            green,
            blue
        );
    }

    public static void renderLineBox(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        float red,
        float green,
        float blue,
        float alpha,
        float red2,
        float green2,
        float blue2
    ) {
        float f = (float)minX;
        float f1 = (float)minY;
        float f2 = (float)minZ;
        float f3 = (float)maxX;
        float f4 = (float)maxY;
        float f5 = (float)maxZ;
        consumer.addVertex(pose, f, f1, f2).setColor(red, green2, blue2, alpha).setNormal(pose, 1.0F, 0.0F, 0.0F);
        consumer.addVertex(pose, f3, f1, f2).setColor(red, green2, blue2, alpha).setNormal(pose, 1.0F, 0.0F, 0.0F);
        consumer.addVertex(pose, f, f1, f2).setColor(red2, green, blue2, alpha).setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(pose, f, f4, f2).setColor(red2, green, blue2, alpha).setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(pose, f, f1, f2).setColor(red2, green2, blue, alpha).setNormal(pose, 0.0F, 0.0F, 1.0F);
        consumer.addVertex(pose, f, f1, f5).setColor(red2, green2, blue, alpha).setNormal(pose, 0.0F, 0.0F, 1.0F);
        consumer.addVertex(pose, f3, f1, f2).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(pose, f3, f4, f2).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(pose, f3, f4, f2).setColor(red, green, blue, alpha).setNormal(pose, -1.0F, 0.0F, 0.0F);
        consumer.addVertex(pose, f, f4, f2).setColor(red, green, blue, alpha).setNormal(pose, -1.0F, 0.0F, 0.0F);
        consumer.addVertex(pose, f, f4, f2).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 0.0F, 1.0F);
        consumer.addVertex(pose, f, f4, f5).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 0.0F, 1.0F);
        consumer.addVertex(pose, f, f4, f5).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, -1.0F, 0.0F);
        consumer.addVertex(pose, f, f1, f5).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, -1.0F, 0.0F);
        consumer.addVertex(pose, f, f1, f5).setColor(red, green, blue, alpha).setNormal(pose, 1.0F, 0.0F, 0.0F);
        consumer.addVertex(pose, f3, f1, f5).setColor(red, green, blue, alpha).setNormal(pose, 1.0F, 0.0F, 0.0F);
        consumer.addVertex(pose, f3, f1, f5).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 0.0F, -1.0F);
        consumer.addVertex(pose, f3, f1, f2).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 0.0F, -1.0F);
        consumer.addVertex(pose, f, f4, f5).setColor(red, green, blue, alpha).setNormal(pose, 1.0F, 0.0F, 0.0F);
        consumer.addVertex(pose, f3, f4, f5).setColor(red, green, blue, alpha).setNormal(pose, 1.0F, 0.0F, 0.0F);
        consumer.addVertex(pose, f3, f1, f5).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(pose, f3, f4, f5).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(pose, f3, f4, f2).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 0.0F, 1.0F);
        consumer.addVertex(pose, f3, f4, f5).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    public static void addChainedFilledBoxVertices(
        PoseStack poseStack,
        VertexConsumer buffer,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        addChainedFilledBoxVertices(
            poseStack,
            buffer,
            (float)minX,
            (float)minY,
            (float)minZ,
            (float)maxX,
            (float)maxY,
            (float)maxZ,
            red,
            green,
            blue,
            alpha
        );
    }

    public static void addChainedFilledBoxVertices(
        PoseStack poseStack,
        VertexConsumer buffer,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        Matrix4f matrix4f = poseStack.last().pose();
        buffer.addVertex(matrix4f, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix4f, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
    }

    public static void renderFace(
        Matrix4f pose,
        VertexConsumer consumer,
        Direction direction,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        switch (direction) {
            case DOWN:
                consumer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x2, y1, z1).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x2, y1, z2).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x1, y1, z2).setColor(red, green, blue, alpha);
                break;
            case UP:
                consumer.addVertex(pose, x1, y2, z1).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x1, y2, z2).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x2, y2, z2).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x2, y2, z1).setColor(red, green, blue, alpha);
                break;
            case NORTH:
                consumer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x1, y2, z1).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x2, y2, z1).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x2, y1, z1).setColor(red, green, blue, alpha);
                break;
            case SOUTH:
                consumer.addVertex(pose, x1, y1, z2).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x2, y1, z2).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x2, y2, z2).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x1, y2, z2).setColor(red, green, blue, alpha);
                break;
            case WEST:
                consumer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x1, y1, z2).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x1, y2, z2).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x1, y2, z1).setColor(red, green, blue, alpha);
                break;
            case EAST:
                consumer.addVertex(pose, x2, y1, z1).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x2, y2, z1).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x2, y2, z2).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x2, y1, z2).setColor(red, green, blue, alpha);
        }
    }

    public static void renderVector(PoseStack poseStack, VertexConsumer buffer, Vector3f startPos, Vec3 vector, int color) {
        PoseStack.Pose posestack$pose = poseStack.last();
        buffer.addVertex(posestack$pose, startPos)
            .setColor(color)
            .setNormal(posestack$pose, (float)vector.x, (float)vector.y, (float)vector.z);
        buffer.addVertex(posestack$pose, (float)(startPos.x() + vector.x), (float)(startPos.y() + vector.y), (float)(startPos.z() + vector.z))
            .setColor(color)
            .setNormal(posestack$pose, (float)vector.x, (float)vector.y, (float)vector.z);
    }
}
