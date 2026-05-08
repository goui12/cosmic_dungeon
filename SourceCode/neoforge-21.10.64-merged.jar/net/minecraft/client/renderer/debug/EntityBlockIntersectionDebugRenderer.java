package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugEntityBlockIntersection;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityBlockIntersectionDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final float PADDING = 0.02F;

    @Override
    public void render(
        PoseStack p_449651_, MultiBufferSource p_449561_, double p_449117_, double p_449076_, double p_449307_, DebugValueAccess p_449408_, Frustum p_451449_
    ) {
        p_449408_.forEachBlock(DebugSubscriptions.ENTITY_BLOCK_INTERSECTIONS, (p_449637_, p_449393_) -> {
            float f = ARGB.redFloat(p_449393_.color());
            float f1 = ARGB.greenFloat(p_449393_.color());
            float f2 = ARGB.blueFloat(p_449393_.color());
            float f3 = ARGB.alphaFloat(p_449393_.color());
            DebugRenderer.renderFilledBox(p_449651_, p_449561_, p_449637_, 0.02F, f, f1, f2, f3);
        });
    }
}
