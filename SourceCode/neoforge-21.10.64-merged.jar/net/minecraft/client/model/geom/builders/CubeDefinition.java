package net.minecraft.client.model.geom.builders;

import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public final class CubeDefinition {
    @Nullable
    private final String comment;
    private final Vector3fc origin;
    private final Vector3fc dimensions;
    private final CubeDeformation grow;
    private final boolean mirror;
    private final UVPair texCoord;
    private final UVPair texScale;
    private final Set<Direction> visibleFaces;

    protected CubeDefinition(
        @Nullable String comment,
        float texCoordU,
        float texCoordV,
        float originX,
        float originY,
        float originZ,
        float dimensionX,
        float dimensionY,
        float dimensionZ,
        CubeDeformation grow,
        boolean mirror,
        float texScaleU,
        float texScaleV,
        Set<Direction> visibleFaces
    ) {
        this.comment = comment;
        this.texCoord = new UVPair(texCoordU, texCoordV);
        this.origin = new Vector3f(originX, originY, originZ);
        this.dimensions = new Vector3f(dimensionX, dimensionY, dimensionZ);
        this.grow = grow;
        this.mirror = mirror;
        this.texScale = new UVPair(texScaleU, texScaleV);
        this.visibleFaces = visibleFaces;
    }

    public ModelPart.Cube bake(int texWidth, int texHeight) {
        return new ModelPart.Cube(
            (int)this.texCoord.u(),
            (int)this.texCoord.v(),
            this.origin.x(),
            this.origin.y(),
            this.origin.z(),
            this.dimensions.x(),
            this.dimensions.y(),
            this.dimensions.z(),
            this.grow.growX,
            this.grow.growY,
            this.grow.growZ,
            this.mirror,
            texWidth * this.texScale.u(),
            texHeight * this.texScale.v(),
            this.visibleFaces
        );
    }
}
