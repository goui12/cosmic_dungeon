package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RaidDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int MAX_RENDER_DIST = 160;
    private static final float TEXT_SCALE = 0.04F;
    private final Minecraft minecraft;

    public RaidDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(
        PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum
    ) {
        BlockPos blockpos = this.getCamera().getBlockPosition();
        debugValueAccess.forEachChunk(DebugSubscriptions.RAIDS, (p_449862_, p_449236_) -> {
            for (BlockPos blockpos1 : p_449236_) {
                if (blockpos.closerThan(blockpos1, 160.0)) {
                    highlightRaidCenter(poseStack, bufferSource, blockpos1);
                }
            }
        });
    }

    private static void highlightRaidCenter(PoseStack poseStack, MultiBufferSource buffer, BlockPos pos) {
        DebugRenderer.renderFilledUnitCube(poseStack, buffer, pos, 1.0F, 0.0F, 0.0F, 0.15F);
        renderTextOverBlock(poseStack, buffer, "Raid center", pos, -65536);
    }

    private static void renderTextOverBlock(PoseStack poseStack, MultiBufferSource buffer, String text, BlockPos pos, int color) {
        double d0 = pos.getX() + 0.5;
        double d1 = pos.getY() + 1.3;
        double d2 = pos.getZ() + 0.5;
        DebugRenderer.renderFloatingText(poseStack, buffer, text, d0, d1, d2, color, 0.04F, true, 0.0F, true);
    }

    private Camera getCamera() {
        return this.minecraft.gameRenderer.getMainCamera();
    }
}
