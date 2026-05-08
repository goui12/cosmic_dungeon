package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Locale;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugPathInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PathfindingRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final float MAX_RENDER_DIST = 80.0F;
    private static final int MAX_TARGETING_DIST = 8;
    private static final boolean SHOW_ONLY_SELECTED = false;
    private static final boolean SHOW_OPEN_CLOSED = true;
    private static final boolean SHOW_OPEN_CLOSED_COST_MALUS = false;
    private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_TEXT = false;
    private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_BOX = true;
    private static final boolean SHOW_GROUND_LABELS = true;
    private static final float TEXT_SCALE = 0.02F;

    @Override
    public void render(
        PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum
    ) {
        debugValueAccess.forEachEntity(
            DebugSubscriptions.ENTITY_PATHS,
            (p_449675_, p_449392_) -> renderPath(poseStack, bufferSource, camX, camY, camZ, p_449392_.path(), p_449392_.maxNodeDistance())
        );
    }

    private static void renderPath(
        PoseStack poseStack, MultiBufferSource bufferSource, double x, double y, double z, Path path, float nodeSize
    ) {
        renderPath(poseStack, bufferSource, path, nodeSize, true, true, x, y, z);
    }

    public static void renderPath(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        Path path,
        float nodeSize,
        boolean renderDebugNodes,
        boolean renderDebugInfo,
        double x,
        double y,
        double z
    ) {
        renderPathLine(poseStack, bufferSource.getBuffer(RenderType.debugLineStrip(6.0)), path, x, y, z);
        BlockPos blockpos = path.getTarget();
        if (distanceToCamera(blockpos, x, y, z) <= 80.0F) {
            DebugRenderer.renderFilledBox(
                poseStack,
                bufferSource,
                new AABB(
                        blockpos.getX() + 0.25F,
                        blockpos.getY() + 0.25F,
                        blockpos.getZ() + 0.25,
                        blockpos.getX() + 0.75F,
                        blockpos.getY() + 0.75F,
                        blockpos.getZ() + 0.75F
                    )
                    .move(-x, -y, -z),
                0.0F,
                1.0F,
                0.0F,
                0.5F
            );

            for (int i = 0; i < path.getNodeCount(); i++) {
                Node node = path.getNode(i);
                if (distanceToCamera(node.asBlockPos(), x, y, z) <= 80.0F) {
                    float f = i == path.getNextNodeIndex() ? 1.0F : 0.0F;
                    float f1 = i == path.getNextNodeIndex() ? 0.0F : 1.0F;
                    DebugRenderer.renderFilledBox(
                        poseStack,
                        bufferSource,
                        new AABB(
                                node.x + 0.5F - nodeSize,
                                node.y + 0.01F * i,
                                node.z + 0.5F - nodeSize,
                                node.x + 0.5F + nodeSize,
                                node.y + 0.25F + 0.01F * i,
                                node.z + 0.5F + nodeSize
                            )
                            .move(-x, -y, -z),
                        f,
                        0.0F,
                        f1,
                        0.5F
                    );
                }
            }
        }

        Path.DebugData path$debugdata = path.debugData();
        if (renderDebugNodes && path$debugdata != null) {
            for (Node node1 : path$debugdata.closedSet()) {
                if (distanceToCamera(node1.asBlockPos(), x, y, z) <= 80.0F) {
                    DebugRenderer.renderFilledBox(
                        poseStack,
                        bufferSource,
                        new AABB(
                                node1.x + 0.5F - nodeSize / 2.0F,
                                node1.y + 0.01F,
                                node1.z + 0.5F - nodeSize / 2.0F,
                                node1.x + 0.5F + nodeSize / 2.0F,
                                node1.y + 0.1,
                                node1.z + 0.5F + nodeSize / 2.0F
                            )
                            .move(-x, -y, -z),
                        1.0F,
                        0.8F,
                        0.8F,
                        0.5F
                    );
                }
            }

            for (Node node3 : path$debugdata.openSet()) {
                if (distanceToCamera(node3.asBlockPos(), x, y, z) <= 80.0F) {
                    DebugRenderer.renderFilledBox(
                        poseStack,
                        bufferSource,
                        new AABB(
                                node3.x + 0.5F - nodeSize / 2.0F,
                                node3.y + 0.01F,
                                node3.z + 0.5F - nodeSize / 2.0F,
                                node3.x + 0.5F + nodeSize / 2.0F,
                                node3.y + 0.1,
                                node3.z + 0.5F + nodeSize / 2.0F
                            )
                            .move(-x, -y, -z),
                        0.8F,
                        1.0F,
                        1.0F,
                        0.5F
                    );
                }
            }
        }

        if (renderDebugInfo) {
            for (int j = 0; j < path.getNodeCount(); j++) {
                Node node2 = path.getNode(j);
                if (distanceToCamera(node2.asBlockPos(), x, y, z) <= 80.0F) {
                    DebugRenderer.renderFloatingText(
                        poseStack, bufferSource, String.valueOf(node2.type), node2.x + 0.5, node2.y + 0.75, node2.z + 0.5, -1, 0.02F, true, 0.0F, true
                    );
                    DebugRenderer.renderFloatingText(
                        poseStack,
                        bufferSource,
                        String.format(Locale.ROOT, "%.2f", node2.costMalus),
                        node2.x + 0.5,
                        node2.y + 0.25,
                        node2.z + 0.5,
                        -1,
                        0.02F,
                        true,
                        0.0F,
                        true
                    );
                }
            }
        }
    }

    public static void renderPathLine(PoseStack poseStack, VertexConsumer consumer, Path path, double x, double y, double z) {
        for (int i = 0; i < path.getNodeCount(); i++) {
            Node node = path.getNode(i);
            if (!(distanceToCamera(node.asBlockPos(), x, y, z) > 80.0F)) {
                float f = (float)i / path.getNodeCount() * 0.33F;
                int j = i == 0 ? -16777216 : ARGB.opaque(Mth.hsvToRgb(f, 0.9F, 0.9F));
                consumer.addVertex(poseStack.last(), (float)(node.x - x + 0.5), (float)(node.y - y + 0.5), (float)(node.z - z + 0.5))
                    .setColor(j);
            }
        }
    }

    private static float distanceToCamera(BlockPos pos, double x, double y, double z) {
        return (float)(Math.abs(pos.getX() - x) + Math.abs(pos.getY() - y) + Math.abs(pos.getZ() - z));
    }
}
