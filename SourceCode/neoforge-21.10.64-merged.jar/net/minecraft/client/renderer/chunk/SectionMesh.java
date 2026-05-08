package net.minecraft.client.renderer.chunk;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SectionMesh extends AutoCloseable {
    default boolean isDifferentPointOfView(TranslucencyPointOfView pointOfView) {
        return false;
    }

    default boolean hasRenderableLayers() {
        return false;
    }

    default boolean hasTranslucentGeometry() {
        return false;
    }

    default boolean isEmpty(ChunkSectionLayer layer) {
        return true;
    }

    default List<BlockEntity> getRenderableBlockEntities() {
        return Collections.emptyList();
    }

    boolean facesCanSeeEachother(Direction face1, Direction face2);

    @Nullable
    default SectionBuffers getBuffers(ChunkSectionLayer layer) {
        return null;
    }

    @Override
    default void close() {
    }
}
