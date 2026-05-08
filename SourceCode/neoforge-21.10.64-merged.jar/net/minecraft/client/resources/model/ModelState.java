package net.minecraft.client.resources.model;

import com.mojang.math.Transformation;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@OnlyIn(Dist.CLIENT)
public interface ModelState extends net.neoforged.neoforge.client.extensions.ModelStateExtension {
    Matrix4fc NO_TRANSFORM = new Matrix4f();

    default Transformation transformation() {
        return Transformation.identity();
    }

    default Matrix4fc faceTransformation(Direction facing) {
        return NO_TRANSFORM;
    }

    default Matrix4fc inverseFaceTransformation(Direction facing) {
        return NO_TRANSFORM;
    }
}
