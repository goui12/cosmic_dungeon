package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.util.debug.DebugBrainDump;
import net.minecraft.util.debug.DebugPoiInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PoiDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int MAX_RENDER_DIST_FOR_POI_INFO = 30;
    private static final float TEXT_SCALE = 0.02F;
    private static final int ORANGE = -23296;
    private final BrainDebugRenderer brainRenderer;

    public PoiDebugRenderer(BrainDebugRenderer brainRenderer) {
        this.brainRenderer = brainRenderer;
    }

    @Override
    public void render(
        PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum
    ) {
        BlockPos blockpos = BlockPos.containing(camX, camY, camZ);
        debugValueAccess.forEachBlock(DebugSubscriptions.POIS, (p_449286_, p_449587_) -> {
            if (blockpos.closerThan(p_449286_, 30.0)) {
                highlightPoi(poseStack, bufferSource, p_449286_);
                this.renderPoiInfo(poseStack, bufferSource, p_449587_, debugValueAccess);
            }
        });
        this.brainRenderer.getGhostPois(debugValueAccess).forEach((p_449616_, p_449193_) -> {
            if (debugValueAccess.getBlockValue(DebugSubscriptions.POIS, p_449616_) == null) {
                if (blockpos.closerThan(p_449616_, 30.0)) {
                    this.renderGhostPoi(poseStack, bufferSource, p_449616_, (List<String>)p_449193_);
                }
            }
        });
    }

    private static void highlightPoi(PoseStack poseStack, MultiBufferSource bufferSource, BlockPos pos) {
        float f = 0.05F;
        DebugRenderer.renderFilledBox(poseStack, bufferSource, pos, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
    }

    private void renderGhostPoi(PoseStack poseStack, MultiBufferSource bufferSource, BlockPos pos, List<String> entities) {
        float f = 0.05F;
        DebugRenderer.renderFilledBox(poseStack, bufferSource, pos, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
        DebugRenderer.renderTextOverBlock(poseStack, bufferSource, entities.toString(), pos, 0, -256, 0.02F);
        DebugRenderer.renderTextOverBlock(poseStack, bufferSource, "Ghost POI", pos, 1, -65536, 0.02F);
    }

    private void renderPoiInfo(PoseStack poseStack, MultiBufferSource bufferSource, DebugPoiInfo poiInfo, DebugValueAccess debugValueAccess) {
        int i = 0;
        if (SharedConstants.DEBUG_BRAIN) {
            List<String> list = this.getTicketHolderNames(poiInfo, false, debugValueAccess);
            if (list.size() < 4) {
                renderTextOverPoi(poseStack, bufferSource, "Owners: " + list, poiInfo, i, -256);
            } else {
                renderTextOverPoi(poseStack, bufferSource, list.size() + " ticket holders", poiInfo, i, -256);
            }

            i++;
            List<String> list1 = this.getTicketHolderNames(poiInfo, true, debugValueAccess);
            if (list1.size() < 4) {
                renderTextOverPoi(poseStack, bufferSource, "Candidates: " + list1, poiInfo, i, -23296);
            } else {
                renderTextOverPoi(poseStack, bufferSource, list1.size() + " potential owners", poiInfo, i, -23296);
            }

            i++;
        }

        renderTextOverPoi(poseStack, bufferSource, "Free tickets: " + poiInfo.freeTicketCount(), poiInfo, i, -256);
        renderTextOverPoi(poseStack, bufferSource, poiInfo.poiType().getRegisteredName(), poiInfo, ++i, -1);
    }

    private static void renderTextOverPoi(
        PoseStack poseStack, MultiBufferSource bufferSource, String text, DebugPoiInfo poiInfo, int line, int color
    ) {
        DebugRenderer.renderTextOverBlock(poseStack, bufferSource, text, poiInfo.pos(), line, color, 0.02F);
    }

    private List<String> getTicketHolderNames(DebugPoiInfo poiInfo, boolean potential, DebugValueAccess debugValueAccess) {
        List<String> list = new ArrayList<>();
        debugValueAccess.forEachEntity(DebugSubscriptions.BRAINS, (p_449314_, p_449925_) -> {
            boolean flag = potential ? p_449925_.hasPotentialPoi(poiInfo.pos()) : p_449925_.hasPoi(poiInfo.pos());
            if (flag) {
                list.add(DebugEntityNameGenerator.getEntityName(p_449314_.getUUID()));
            }
        });
        return list;
    }
}
