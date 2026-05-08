package net.minecraft.client.renderer.block.model;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.math.MatrixUtil;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class FaceBakery {
    public static final int VERTEX_INT_SIZE = 8;
    public static final int VERTEX_COUNT = 4;
    private static final int COLOR_INDEX = 3;
    public static final int UV_INDEX = 4;
    private static final Vector3fc NO_RESCALE = new Vector3f(1.0F, 1.0F, 1.0F);
    private static final Vector3fc BLOCK_MIDDLE = new Vector3f(0.5F, 0.5F, 0.5F);

    @VisibleForTesting
    public static BlockElementFace.UVs defaultFaceUV(Vector3fc posFrom, Vector3fc posTo, Direction facing) {
        return switch (facing) {
            case DOWN -> new BlockElementFace.UVs(posFrom.x(), 16.0F - posTo.z(), posTo.x(), 16.0F - posFrom.z());
            case UP -> new BlockElementFace.UVs(posFrom.x(), posFrom.z(), posTo.x(), posTo.z());
            case NORTH -> new BlockElementFace.UVs(16.0F - posTo.x(), 16.0F - posTo.y(), 16.0F - posFrom.x(), 16.0F - posFrom.y());
            case SOUTH -> new BlockElementFace.UVs(posFrom.x(), 16.0F - posTo.y(), posTo.x(), 16.0F - posFrom.y());
            case WEST -> new BlockElementFace.UVs(posFrom.z(), 16.0F - posTo.y(), posTo.z(), 16.0F - posFrom.y());
            case EAST -> new BlockElementFace.UVs(16.0F - posTo.z(), 16.0F - posTo.y(), 16.0F - posFrom.z(), 16.0F - posFrom.y());
        };
    }

    public static BakedQuad bakeQuad(
        Vector3fc posFrom,
        Vector3fc posTo,
        BlockElementFace face,
        TextureAtlasSprite sprite,
        Direction facing,
        ModelState modelState,
        @Nullable BlockElementRotation rotation,
        boolean shade,
        int lightEmission
    ) {
        BlockElementFace.UVs blockelementface$uvs = face.uvs();
        if (blockelementface$uvs == null) {
            blockelementface$uvs = defaultFaceUV(posFrom, posTo, facing);
        }

        blockelementface$uvs = shrinkUVs(sprite, blockelementface$uvs);
        Matrix4fc matrix4fc = modelState.inverseFaceTransformation(facing);
        int[] aint = makeVertices(
            blockelementface$uvs,
            face.rotation(),
            matrix4fc,
            sprite,
            facing,
            setupShape(posFrom, posTo),
            modelState.transformation(),
            rotation
        );
        Direction direction = calculateFacing(aint);
        if (rotation == null) {
            // Neo: Suppress winding re-calculation when the quads may not be axis-aligned due to root transforms
            if (!modelState.mayApplyArbitraryRotation())
            recalculateWinding(aint, direction);
        }

        net.neoforged.neoforge.client.ClientHooks.fillNormal(aint);
        var data = face.faceData();
        var quad = new BakedQuad(aint, face.tintIndex(), direction, sprite, shade, lightEmission, data.ambientOcclusion());
        if (!net.neoforged.neoforge.client.model.ExtraFaceData.DEFAULT.equals(data)) {
            net.neoforged.neoforge.client.model.QuadTransformers.applyingLightmap(data.blockLight(), data.skyLight()).processInPlace(quad);
            net.neoforged.neoforge.client.model.QuadTransformers.applyingColor(data.color()).processInPlace(quad);
        }
        return quad;
    }

    private static BlockElementFace.UVs shrinkUVs(TextureAtlasSprite sprite, BlockElementFace.UVs uvs) {
        float f = uvs.minU();
        float f1 = uvs.minV();
        float f2 = uvs.maxU();
        float f3 = uvs.maxV();
        float f4 = sprite.uvShrinkRatio();
        float f5 = (f + f + f2 + f2) / 4.0F;
        float f6 = (f1 + f1 + f3 + f3) / 4.0F;
        return new BlockElementFace.UVs(Mth.lerp(f4, f, f5), Mth.lerp(f4, f1, f6), Mth.lerp(f4, f2, f5), Mth.lerp(f4, f3, f6));
    }

    private static int[] makeVertices(
        BlockElementFace.UVs uvs,
        Quadrant rotation,
        Matrix4fc inverseFaceTransform,
        TextureAtlasSprite sprite,
        Direction facing,
        float[] shape,
        Transformation transformation,
        @Nullable BlockElementRotation partRotation
    ) {
        FaceInfo faceinfo = FaceInfo.fromFacing(facing);
        int[] aint = new int[32];

        for (int i = 0; i < 4; i++) {
            bakeVertex(aint, i, faceinfo, uvs, rotation, inverseFaceTransform, shape, sprite, transformation, partRotation);
        }

        return aint;
    }

    private static float[] setupShape(Vector3fc posFrom, Vector3fc posTo) {
        float[] afloat = new float[Direction.values().length];
        afloat[FaceInfo.Constants.MIN_X] = posFrom.x() / 16.0F;
        afloat[FaceInfo.Constants.MIN_Y] = posFrom.y() / 16.0F;
        afloat[FaceInfo.Constants.MIN_Z] = posFrom.z() / 16.0F;
        afloat[FaceInfo.Constants.MAX_X] = posTo.x() / 16.0F;
        afloat[FaceInfo.Constants.MAX_Y] = posTo.y() / 16.0F;
        afloat[FaceInfo.Constants.MAX_Z] = posTo.z() / 16.0F;
        return afloat;
    }

    private static void bakeVertex(
        int[] vertexData,
        int vertexIndex,
        FaceInfo faceInfo,
        BlockElementFace.UVs uvs,
        Quadrant rotation,
        Matrix4fc inverseFaceTransform,
        float[] shape,
        TextureAtlasSprite sprite,
        Transformation transformation,
        @Nullable BlockElementRotation partRotation
    ) {
        FaceInfo.VertexInfo faceinfo$vertexinfo = faceInfo.getVertexInfo(vertexIndex);
        Vector3f vector3f = new Vector3f(shape[faceinfo$vertexinfo.xFace], shape[faceinfo$vertexinfo.yFace], shape[faceinfo$vertexinfo.zFace]);
        applyElementRotation(vector3f, partRotation);
        applyModelRotation(vector3f, transformation);
        float f = BlockElementFace.getU(uvs, rotation, vertexIndex);
        float f1 = BlockElementFace.getV(uvs, rotation, vertexIndex);
        float f2;
        float f3;
        if (MatrixUtil.isIdentity(inverseFaceTransform)) {
            f3 = f;
            f2 = f1;
        } else {
            Vector3f vector3f1 = inverseFaceTransform.transformPosition(new Vector3f(cornerToCenter(f), cornerToCenter(f1), 0.0F));
            f3 = centerToCorner(vector3f1.x);
            f2 = centerToCorner(vector3f1.y);
        }

        fillVertex(vertexData, vertexIndex, vector3f, sprite, f3, f2);
    }

    private static float cornerToCenter(float coord) {
        return coord - 0.5F;
    }

    private static float centerToCorner(float coord) {
        return coord + 0.5F;
    }

    private static void fillVertex(int[] vertexData, int vertexIndex, Vector3f pos, TextureAtlasSprite sprite, float u, float v) {
        int i = vertexIndex * 8;
        vertexData[i] = Float.floatToRawIntBits(pos.x());
        vertexData[i + 1] = Float.floatToRawIntBits(pos.y());
        vertexData[i + 2] = Float.floatToRawIntBits(pos.z());
        vertexData[i + 3] = -1;
        vertexData[i + 4] = Float.floatToRawIntBits(sprite.getU(u));
        vertexData[i + 4 + 1] = Float.floatToRawIntBits(sprite.getV(v));
    }

    private static void applyElementRotation(Vector3f vec, @Nullable BlockElementRotation partRotation) {
        if (partRotation != null) {
            Vector3fc vector3fc = partRotation.axis().getPositive().getUnitVec3f();
            Matrix4fc matrix4fc = new Matrix4f().rotation(partRotation.angle() * (float) (Math.PI / 180.0), vector3fc);
            Vector3fc vector3fc1 = partRotation.rescale() ? computeRescale(partRotation) : NO_RESCALE;
            rotateVertexBy(vec, partRotation.origin(), matrix4fc, vector3fc1);
        }
    }

    private static Vector3fc computeRescale(BlockElementRotation partRotation) {
        if (partRotation.angle() == 0.0F) {
            return NO_RESCALE;
        } else {
            float f = Math.abs(partRotation.angle());
            float f1 = 1.0F / Mth.cos(f * (float) (Math.PI / 180.0));

            return switch (partRotation.axis()) {
                case X -> new Vector3f(1.0F, f1, f1);
                case Y -> new Vector3f(f1, 1.0F, f1);
                case Z -> new Vector3f(f1, f1, 1.0F);
            };
        }
    }

    private static void applyModelRotation(Vector3f pos, Transformation transform) {
        if (transform != Transformation.identity()) {
            rotateVertexBy(pos, BLOCK_MIDDLE, transform.getMatrix(), NO_RESCALE);
        }
    }

    private static void rotateVertexBy(Vector3f pos, Vector3fc origin, Matrix4fc transform, Vector3fc scale) {
        pos.sub(origin);
        transform.transformPosition(pos);
        pos.mul(scale);
        pos.add(origin);
    }

    private static Direction calculateFacing(int[] faceData) {
        Vector3f vector3f = vectorFromData(faceData, 0);
        Vector3f vector3f1 = vectorFromData(faceData, 8);
        Vector3f vector3f2 = vectorFromData(faceData, 16);
        Vector3f vector3f3 = new Vector3f(vector3f).sub(vector3f1);
        Vector3f vector3f4 = new Vector3f(vector3f2).sub(vector3f1);
        Vector3f vector3f5 = new Vector3f(vector3f4).cross(vector3f3).normalize();
        if (!vector3f5.isFinite()) {
            return Direction.UP;
        } else {
            Direction direction = null;
            float f = 0.0F;

            for (Direction direction1 : Direction.values()) {
                float f1 = vector3f5.dot(direction1.getUnitVec3f());
                if (f1 >= 0.0F && f1 > f) {
                    f = f1;
                    direction = direction1;
                }
            }

            return direction == null ? Direction.UP : direction;
        }
    }

    private static float xFromData(int[] faceData, int index) {
        return Float.intBitsToFloat(faceData[index]);
    }

    private static float yFromData(int[] faceData, int index) {
        return Float.intBitsToFloat(faceData[index + 1]);
    }

    private static float zFromData(int[] faceData, int index) {
        return Float.intBitsToFloat(faceData[index + 2]);
    }

    private static Vector3f vectorFromData(int[] faceData, int index) {
        return new Vector3f(xFromData(faceData, index), yFromData(faceData, index), zFromData(faceData, index));
    }

    private static void recalculateWinding(int[] vertices, Direction direction) {
        int[] aint = new int[vertices.length];
        System.arraycopy(vertices, 0, aint, 0, vertices.length);
        float[] afloat = new float[Direction.values().length];
        afloat[FaceInfo.Constants.MIN_X] = 999.0F;
        afloat[FaceInfo.Constants.MIN_Y] = 999.0F;
        afloat[FaceInfo.Constants.MIN_Z] = 999.0F;
        afloat[FaceInfo.Constants.MAX_X] = -999.0F;
        afloat[FaceInfo.Constants.MAX_Y] = -999.0F;
        afloat[FaceInfo.Constants.MAX_Z] = -999.0F;

        for (int i = 0; i < 4; i++) {
            int j = 8 * i;
            float f = xFromData(aint, j);
            float f1 = yFromData(aint, j);
            float f2 = zFromData(aint, j);
            if (f < afloat[FaceInfo.Constants.MIN_X]) {
                afloat[FaceInfo.Constants.MIN_X] = f;
            }

            if (f1 < afloat[FaceInfo.Constants.MIN_Y]) {
                afloat[FaceInfo.Constants.MIN_Y] = f1;
            }

            if (f2 < afloat[FaceInfo.Constants.MIN_Z]) {
                afloat[FaceInfo.Constants.MIN_Z] = f2;
            }

            if (f > afloat[FaceInfo.Constants.MAX_X]) {
                afloat[FaceInfo.Constants.MAX_X] = f;
            }

            if (f1 > afloat[FaceInfo.Constants.MAX_Y]) {
                afloat[FaceInfo.Constants.MAX_Y] = f1;
            }

            if (f2 > afloat[FaceInfo.Constants.MAX_Z]) {
                afloat[FaceInfo.Constants.MAX_Z] = f2;
            }
        }

        FaceInfo faceinfo = FaceInfo.fromFacing(direction);

        for (int i1 = 0; i1 < 4; i1++) {
            int j1 = 8 * i1;
            FaceInfo.VertexInfo faceinfo$vertexinfo = faceinfo.getVertexInfo(i1);
            float f8 = afloat[faceinfo$vertexinfo.xFace];
            float f3 = afloat[faceinfo$vertexinfo.yFace];
            float f4 = afloat[faceinfo$vertexinfo.zFace];
            vertices[j1] = Float.floatToRawIntBits(f8);
            vertices[j1 + 1] = Float.floatToRawIntBits(f3);
            vertices[j1 + 2] = Float.floatToRawIntBits(f4);

            for (int k = 0; k < 4; k++) {
                int l = 8 * k;
                float f5 = xFromData(aint, l);
                float f6 = yFromData(aint, l);
                float f7 = zFromData(aint, l);
                if (Mth.equal(f8, f5) && Mth.equal(f3, f6) && Mth.equal(f4, f7)) {
                    vertices[j1 + 4] = aint[l + 4];
                    vertices[j1 + 4 + 1] = aint[l + 4 + 1];
                }
            }
        }
    }

    public static void extractPositions(int[] faceData, Consumer<Vector3f> output) {
        for (int i = 0; i < 4; i++) {
            output.accept(vectorFromData(faceData, 8 * i));
        }
    }
}
