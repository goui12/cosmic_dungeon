package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.debug.DebugGoalInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GoalSelectorDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int MAX_RENDER_DIST = 160;
    private final Minecraft minecraft;

    public GoalSelectorDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(
        PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum
    ) {
        Camera camera = this.minecraft.gameRenderer.getMainCamera();
        BlockPos blockpos = BlockPos.containing(camera.getPosition().x, 0.0, camera.getPosition().z);
        debugValueAccess.forEachEntity(DebugSubscriptions.GOAL_SELECTORS, (p_449528_, p_449707_) -> {
            if (blockpos.closerThan(p_449528_.blockPosition(), 160.0)) {
                for (int i = 0; i < p_449707_.goals().size(); i++) {
                    DebugGoalInfo.DebugGoal debuggoalinfo$debuggoal = p_449707_.goals().get(i);
                    double d0 = p_449528_.getBlockX() + 0.5;
                    double d1 = p_449528_.getY() + 2.0 + i * 0.25;
                    double d2 = p_449528_.getBlockZ() + 0.5;
                    int j = debuggoalinfo$debuggoal.isRunning() ? -16711936 : -3355444;
                    DebugRenderer.renderFloatingText(poseStack, bufferSource, debuggoalinfo$debuggoal.name(), d0, d1, d2, j);
                }
            }
        });
    }
}
