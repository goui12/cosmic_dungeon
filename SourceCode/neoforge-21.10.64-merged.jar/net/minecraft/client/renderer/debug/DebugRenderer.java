package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugRenderer {
    private final List<DebugRenderer.SimpleDebugRenderer> opaqueRenderers = new ArrayList<>();
    private final List<DebugRenderer.SimpleDebugRenderer> translucentRenderers = new ArrayList<>();
    private long lastDebugEntriesVersion;

    public DebugRenderer() {
        this.refreshRendererList();
    }

    public void refreshRendererList() {
        Minecraft minecraft = Minecraft.getInstance();
        this.opaqueRenderers.clear();
        this.translucentRenderers.clear();
        if (minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_BORDERS) && !minecraft.showOnlyReducedInfo()) {
            this.opaqueRenderers.add(new ChunkBorderRenderer(minecraft));
        }

        if (minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_SECTION_OCTREE)) {
            this.opaqueRenderers.add(new OctreeDebugRenderer(minecraft));
        }

        if (SharedConstants.DEBUG_PATHFINDING) {
            this.opaqueRenderers.add(new PathfindingRenderer());
        }

        if (SharedConstants.DEBUG_WATER) {
            this.opaqueRenderers.add(new WaterDebugRenderer(minecraft));
        }

        if (SharedConstants.DEBUG_HEIGHTMAP) {
            this.opaqueRenderers.add(new HeightMapRenderer(minecraft));
        }

        if (SharedConstants.DEBUG_COLLISION) {
            this.opaqueRenderers.add(new CollisionBoxRenderer(minecraft));
        }

        if (SharedConstants.DEBUG_SUPPORT_BLOCKS) {
            this.opaqueRenderers.add(new SupportBlockRenderer(minecraft));
        }

        if (SharedConstants.DEBUG_NEIGHBORSUPDATE) {
            this.opaqueRenderers.add(new NeighborsUpdateRenderer());
        }

        if (SharedConstants.DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER) {
            this.opaqueRenderers.add(new RedstoneWireOrientationsRenderer());
        }

        if (SharedConstants.DEBUG_STRUCTURES) {
            this.opaqueRenderers.add(new StructureRenderer());
        }

        if (SharedConstants.DEBUG_LIGHT) {
            this.opaqueRenderers.add(new LightDebugRenderer(minecraft));
        }

        if (SharedConstants.DEBUG_SOLID_FACE) {
            this.opaqueRenderers.add(new SolidFaceRenderer(minecraft));
        }

        if (SharedConstants.DEBUG_VILLAGE_SECTIONS) {
            this.opaqueRenderers.add(new VillageSectionsDebugRenderer());
        }

        if (SharedConstants.DEBUG_BRAIN) {
            this.opaqueRenderers.add(new BrainDebugRenderer(minecraft));
        }

        if (SharedConstants.DEBUG_POI) {
            this.opaqueRenderers.add(new PoiDebugRenderer(new BrainDebugRenderer(minecraft)));
        }

        if (SharedConstants.DEBUG_BEES) {
            this.opaqueRenderers.add(new BeeDebugRenderer(minecraft));
        }

        if (SharedConstants.DEBUG_RAIDS) {
            this.opaqueRenderers.add(new RaidDebugRenderer(minecraft));
        }

        if (SharedConstants.DEBUG_GOAL_SELECTOR) {
            this.opaqueRenderers.add(new GoalSelectorDebugRenderer(minecraft));
        }

        if (SharedConstants.DEBUG_CHUNKS) {
            this.opaqueRenderers.add(new ChunkDebugRenderer(minecraft));
        }

        if (SharedConstants.DEBUG_GAME_EVENT_LISTENERS) {
            this.opaqueRenderers.add(new GameEventListenerRenderer());
        }

        if (SharedConstants.DEBUG_SKY_LIGHT_SECTIONS) {
            this.opaqueRenderers.add(new LightSectionDebugRenderer(minecraft, LightLayer.SKY));
        }

        if (SharedConstants.DEBUG_BREEZE_MOB) {
            this.opaqueRenderers.add(new BreezeDebugRenderer(minecraft));
        }

        if (SharedConstants.DEBUG_ENTITY_BLOCK_INTERSECTION) {
            this.opaqueRenderers.add(new EntityBlockIntersectionDebugRenderer());
        }

        this.translucentRenderers.add(new ChunkCullingDebugRenderer(minecraft));
    }

    public void render(
        PoseStack poseStack,
        Frustum frustum,
        MultiBufferSource.BufferSource bufferSource,
        double camX,
        double camY,
        double camZ,
        boolean translucent
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        DebugValueAccess debugvalueaccess = minecraft.getConnection().createDebugValueAccess();
        if (minecraft.debugEntries.getCurrentlyEnabledVersion() != this.lastDebugEntriesVersion) {
            this.lastDebugEntriesVersion = minecraft.debugEntries.getCurrentlyEnabledVersion();
            this.refreshRendererList();
        }

        for (DebugRenderer.SimpleDebugRenderer debugrenderer$simpledebugrenderer : translucent ? this.translucentRenderers : this.opaqueRenderers) {
            debugrenderer$simpledebugrenderer.render(poseStack, bufferSource, camX, camY, camZ, debugvalueaccess, frustum);
        }
    }

    public static Optional<Entity> getTargetedEntity(@Nullable Entity entity, int distance) {
        if (entity == null) {
            return Optional.empty();
        } else {
            Vec3 vec3 = entity.getEyePosition();
            Vec3 vec31 = entity.getViewVector(1.0F).scale(distance);
            Vec3 vec32 = vec3.add(vec31);
            AABB aabb = entity.getBoundingBox().expandTowards(vec31).inflate(1.0);
            int i = distance * distance;
            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(entity, vec3, vec32, aabb, EntitySelector.CAN_BE_PICKED, i);
            if (entityhitresult == null) {
                return Optional.empty();
            } else {
                return vec3.distanceToSqr(entityhitresult.getLocation()) > i ? Optional.empty() : Optional.of(entityhitresult.getEntity());
            }
        }
    }

    public static void renderFilledUnitCube(
        PoseStack poseStack, MultiBufferSource bufferSource, BlockPos pos, float red, float green, float blue, float alpha
    ) {
        renderFilledBox(poseStack, bufferSource, pos, pos.offset(1, 1, 1), red, green, blue, alpha);
    }

    public static void renderFilledBox(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        BlockPos startPos,
        BlockPos endPos,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera.isInitialized()) {
            Vec3 vec3 = camera.getPosition().reverse();
            AABB aabb = AABB.encapsulatingFullBlocks(startPos, endPos).move(vec3);
            renderFilledBox(poseStack, bufferSource, aabb, red, green, blue, alpha);
        }
    }

    public static void renderFilledBox(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        BlockPos pos,
        float scale,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera.isInitialized()) {
            Vec3 vec3 = camera.getPosition().reverse();
            AABB aabb = new AABB(pos).move(vec3).inflate(scale);
            renderFilledBox(poseStack, bufferSource, aabb, red, green, blue, alpha);
        }
    }

    public static void renderFilledBox(
        PoseStack poseStack, MultiBufferSource bufferSource, AABB boundingBox, float red, float green, float blue, float alpha
    ) {
        renderFilledBox(
            poseStack,
            bufferSource,
            boundingBox.minX,
            boundingBox.minY,
            boundingBox.minZ,
            boundingBox.maxX,
            boundingBox.maxY,
            boundingBox.maxZ,
            red,
            green,
            blue,
            alpha
        );
    }

    public static void renderFilledBox(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.debugFilledBox());
        ShapeRenderer.addChainedFilledBoxVertices(
            poseStack, vertexconsumer, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha
        );
    }

    public static void renderTextOverBlock(
        PoseStack poseStack, MultiBufferSource bufferSource, String text, BlockPos pos, int line, int color, float scale
    ) {
        double d0 = 1.3;
        double d1 = 0.2;
        double d2 = pos.getX() + 0.5;
        double d3 = pos.getY() + 1.3 + line * 0.2;
        double d4 = pos.getZ() + 0.5;
        renderFloatingText(poseStack, bufferSource, text, d2, d3, d4, color, scale, true, 0.0F, true);
    }

    public static void renderTextOverMob(
        PoseStack poseStack, MultiBufferSource bufferSource, Entity entity, int line, String text, int color, float scale
    ) {
        double d0 = 2.4;
        double d1 = 0.25;
        double d2 = entity.getBlockX() + 0.5;
        double d3 = entity.getY() + 2.4 + line * 0.25;
        double d4 = entity.getBlockZ() + 0.5;
        float f = 0.5F;
        renderFloatingText(poseStack, bufferSource, text, d2, d3, d4, color, scale, false, 0.5F, true);
    }

    public static void renderFloatingText(
        PoseStack poseStack, MultiBufferSource bufferSource, String text, int x, int y, int z, int color
    ) {
        renderFloatingText(poseStack, bufferSource, text, x + 0.5, y + 0.5, z + 0.5, color);
    }

    public static void renderFloatingText(
        PoseStack poseStack, MultiBufferSource bufferSource, String text, double x, double y, double z, int color
    ) {
        renderFloatingText(poseStack, bufferSource, text, x, y, z, color, 0.02F);
    }

    public static void renderFloatingText(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        String text,
        double x,
        double y,
        double z,
        int color,
        float scale
    ) {
        renderFloatingText(poseStack, bufferSource, text, x, y, z, color, scale, true, 0.0F, false);
    }

    public static void renderFloatingText(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        String text,
        double x,
        double y,
        double z,
        int color,
        float scale,
        boolean center,
        float xOffset,
        boolean transparent
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (camera.isInitialized() && minecraft.getEntityRenderDispatcher().options != null) {
            Font font = minecraft.font;
            double d0 = camera.getPosition().x;
            double d1 = camera.getPosition().y;
            double d2 = camera.getPosition().z;
            poseStack.pushPose();
            poseStack.translate((float)(x - d0), (float)(y - d1) + 0.07F, (float)(z - d2));
            poseStack.mulPose(camera.rotation());
            poseStack.scale(scale, -scale, scale);
            float f = center ? -font.width(text) / 2.0F : 0.0F;
            f -= xOffset / scale;
            font.drawInBatch(
                text,
                f,
                0.0F,
                color,
                false,
                poseStack.last().pose(),
                bufferSource,
                transparent ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
                0,
                15728880
            );
            poseStack.popPose();
        }
    }

    private static Vec3 mixColor(float shift) {
        float f = 5.99999F;
        int i = (int)(Mth.clamp(shift, 0.0F, 1.0F) * 5.99999F);
        float f1 = shift * 5.99999F - i;

        return switch (i) {
            case 0 -> new Vec3(1.0, f1, 0.0);
            case 1 -> new Vec3(1.0F - f1, 1.0, 0.0);
            case 2 -> new Vec3(0.0, 1.0, f1);
            case 3 -> new Vec3(0.0, 1.0 - f1, 1.0);
            case 4 -> new Vec3(f1, 0.0, 1.0);
            case 5 -> new Vec3(1.0, 0.0, 1.0 - f1);
            default -> throw new IllegalStateException("Unexpected value: " + i);
        };
    }

    private static Vec3 shiftHue(float red, float green, float blue, float shift) {
        Vec3 vec3 = mixColor(shift).scale(red);
        Vec3 vec31 = mixColor((shift + 0.33333334F) % 1.0F).scale(green);
        Vec3 vec32 = mixColor((shift + 0.6666667F) % 1.0F).scale(blue);
        Vec3 vec33 = vec3.add(vec31).add(vec32);
        double d0 = Math.max(Math.max(1.0, vec33.x), Math.max(vec33.y, vec33.z));
        return new Vec3(vec33.x / d0, vec33.y / d0, vec33.z / d0);
    }

    public static void renderVoxelShape(
        PoseStack poseStack,
        VertexConsumer buffer,
        VoxelShape shape,
        double x,
        double y,
        double z,
        float red,
        float green,
        float blue,
        float alpha,
        boolean lowerColorVariance
    ) {
        List<AABB> list = shape.toAabbs();
        if (!list.isEmpty()) {
            int i = lowerColorVariance ? list.size() : list.size() * 8;
            ShapeRenderer.renderShape(
                poseStack,
                buffer,
                Shapes.create(list.get(0)),
                x,
                y,
                z,
                ARGB.colorFromFloat(alpha, red, green, blue)
            );

            for (int j = 1; j < list.size(); j++) {
                AABB aabb = list.get(j);
                float f = (float)j / i;
                Vec3 vec3 = shiftHue(red, green, blue, f);
                ShapeRenderer.renderShape(
                    poseStack,
                    buffer,
                    Shapes.create(aabb),
                    x,
                    y,
                    z,
                    ARGB.colorFromFloat(alpha, (float)vec3.x, (float)vec3.y, (float)vec3.z)
                );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface SimpleDebugRenderer {
        void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            double camX,
            double camY,
            double camZ,
            DebugValueAccess debugValueAccess,
            Frustum frustum
        );
    }
}
