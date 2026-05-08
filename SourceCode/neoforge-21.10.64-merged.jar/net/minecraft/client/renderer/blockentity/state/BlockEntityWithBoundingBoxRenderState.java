package net.minecraft.client.renderer.blockentity.state;

import javax.annotation.Nullable;
import net.minecraft.world.level.block.entity.BoundingBoxRenderable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockEntityWithBoundingBoxRenderState extends BlockEntityRenderState {
    public boolean isVisible;
    public BoundingBoxRenderable.Mode mode;
    public BoundingBoxRenderable.RenderableBox box;
    @Nullable
    public BlockEntityWithBoundingBoxRenderState.InvisibleBlockType[] invisibleBlocks;
    @Nullable
    public boolean[] structureVoids;

    @OnlyIn(Dist.CLIENT)
    public static enum InvisibleBlockType {
        AIR,
        BARRIER,
        LIGHT,
        STRUCUTRE_VOID;
    }
}
