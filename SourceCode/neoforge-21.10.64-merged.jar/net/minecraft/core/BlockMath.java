package net.minecraft.core;

import com.google.common.collect.Maps;
import com.mojang.math.MatrixUtil;
import com.mojang.math.Transformation;
import java.util.Map;
import net.minecraft.Util;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BlockMath {
    private static final Map<Direction, Transformation> VANILLA_UV_TRANSFORM_LOCAL_TO_GLOBAL = Maps.newEnumMap(
        Map.of(
            Direction.SOUTH,
            Transformation.identity(),
            Direction.EAST,
            new Transformation(null, new Quaternionf().rotateY((float) (Math.PI / 2)), null, null),
            Direction.WEST,
            new Transformation(null, new Quaternionf().rotateY((float) (-Math.PI / 2)), null, null),
            Direction.NORTH,
            new Transformation(null, new Quaternionf().rotateY((float) Math.PI), null, null),
            Direction.UP,
            new Transformation(null, new Quaternionf().rotateX((float) (-Math.PI / 2)), null, null),
            Direction.DOWN,
            new Transformation(null, new Quaternionf().rotateX((float) (Math.PI / 2)), null, null)
        )
    );
    private static final Map<Direction, Transformation> VANILLA_UV_TRANSFORM_GLOBAL_TO_LOCAL = Maps.newEnumMap(
        Util.mapValues(VANILLA_UV_TRANSFORM_LOCAL_TO_GLOBAL, Transformation::inverse)
    );

    public static Transformation blockCenterToCorner(Transformation transformation) {
        Matrix4f matrix4f = new Matrix4f().translation(0.5F, 0.5F, 0.5F);
        matrix4f.mul(transformation.getMatrix());
        matrix4f.translate(-0.5F, -0.5F, -0.5F);
        return new Transformation(matrix4f);
    }

    public static Transformation blockCornerToCenter(Transformation transformation) {
        Matrix4f matrix4f = new Matrix4f().translation(-0.5F, -0.5F, -0.5F);
        matrix4f.mul(transformation.getMatrix());
        matrix4f.translate(0.5F, 0.5F, 0.5F);
        return new Transformation(matrix4f);
    }

    public static Transformation getFaceTransformation(Transformation p_transformation, Direction p_direction) {
        if (MatrixUtil.isIdentity(p_transformation.getMatrix())) {
            return p_transformation;
        } else {
            Transformation transformation = VANILLA_UV_TRANSFORM_LOCAL_TO_GLOBAL.get(p_direction);
            transformation = p_transformation.compose(transformation);
            Vector3f vector3f = transformation.getMatrix().transformDirection(new Vector3f(0.0F, 0.0F, 1.0F));
            Direction direction = Direction.getApproximateNearest(vector3f.x, vector3f.y, vector3f.z);
            return VANILLA_UV_TRANSFORM_GLOBAL_TO_LOCAL.get(direction).compose(transformation);
        }
    }
}
