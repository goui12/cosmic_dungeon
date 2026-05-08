package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.redstone.Orientation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class RedstoneWireOrientationsRenderer implements DebugRenderer.SimpleDebugRenderer {
    @Override
    public void render(
        PoseStack p_363820_, MultiBufferSource p_363043_, double p_364261_, double p_361975_, double p_365341_, DebugValueAccess p_449239_, Frustum p_451316_
    ) {
        VertexConsumer vertexconsumer = p_363043_.getBuffer(RenderType.lines());
        p_449239_.forEachBlock(DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS, (p_449730_, p_449705_) -> {
            Vector3f vector3f = p_449730_.getBottomCenter().subtract(p_364261_, p_361975_ - 0.1, p_365341_).toVector3f();
            ShapeRenderer.renderVector(p_363820_, vertexconsumer, vector3f, p_449705_.getFront().getUnitVec3().scale(0.5), -16776961);
            ShapeRenderer.renderVector(p_363820_, vertexconsumer, vector3f, p_449705_.getUp().getUnitVec3().scale(0.4), -65536);
            ShapeRenderer.renderVector(p_363820_, vertexconsumer, vector3f, p_449705_.getSide().getUnitVec3().scale(0.3), -256);
        });
    }
}
