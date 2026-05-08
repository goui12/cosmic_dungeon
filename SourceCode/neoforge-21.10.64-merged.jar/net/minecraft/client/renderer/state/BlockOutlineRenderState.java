package net.minecraft.client.renderer.state;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record BlockOutlineRenderState(
    BlockPos pos,
    boolean isTranslucent,
    boolean highContrast,
    VoxelShape shape,
    @Nullable VoxelShape collisionShape,
    @Nullable VoxelShape occlusionShape,
    @Nullable VoxelShape interactionShape,
    java.util.List<net.neoforged.neoforge.client.CustomBlockOutlineRenderer> customRenderers
) {
    @Deprecated
    public BlockOutlineRenderState(
            BlockPos pos,
            boolean isTranslucent,
            boolean highContrast,
            VoxelShape shape,
            @Nullable VoxelShape collisionShape,
            @Nullable VoxelShape occlusionShape,
            @Nullable VoxelShape interactionShape
    ) {
        this(pos, isTranslucent, highContrast, shape, collisionShape, occlusionShape, interactionShape, java.util.List.of());
    }

    @Deprecated
    public BlockOutlineRenderState(BlockPos p_451260_, boolean p_451414_, boolean p_451550_, VoxelShape p_451218_) {
        this(p_451260_, p_451414_, p_451550_, p_451218_, java.util.List.of());
    }

    public BlockOutlineRenderState(BlockPos p_451260_, boolean p_451414_, boolean p_451550_, VoxelShape p_451218_, java.util.List<net.neoforged.neoforge.client.CustomBlockOutlineRenderer> customRenderers) {
        this(p_451260_, p_451414_, p_451550_, p_451218_, null, null, null, customRenderers);
    }
}
