// file: src/main/java/net/goui/cosmicdungeon/client/render/blockentity/ClassLockedChestRenderer.java
package net.goui.cosmicdungeon.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.custom.ClassLocked;
import net.goui.cosmicdungeon.block.custom.ClassLockedChestBlock;
import net.goui.cosmicdungeon.block.entity.ClassLockedChestBlockEntity;
import net.goui.cosmicdungeon.client.model.ClassLockedChestModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassLockedChestRenderer implements BlockEntityRenderer<ClassLockedChestBlockEntity, ClassLockedChestRenderer.State> {

    private final ClassLockedChestModel model;
    private static final Map<String, ResourceLocation> TEX_CACHE = new ConcurrentHashMap<>();

    public ClassLockedChestRenderer(BlockEntityRendererProvider.Context ctx) {
        ModelPart root = ctx.bakeLayer(ClassLockedChestModel.LAYER_LOCATION);
        this.model = new ClassLockedChestModel(root);
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float lidProgress = 0.0F; // 0..1
        public ResourceLocation texture = defaultTexture();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            ClassLockedChestBlockEntity be,
            State rs,
            float partialTick,
            Vec3 cameraPosition,
            @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderState.extractBase(be, rs, breakProgress);

        BlockState state = be.getBlockState();
        if (state == null) return;

        rs.facing = state.hasProperty(ClassLockedChestBlock.FACING)
                ? state.getValue(ClassLockedChestBlock.FACING)
                : Direction.NORTH;

        rs.texture = textureFor(state);
        rs.lidProgress = be.getLidProgress(partialTick);
    }

    @Override
    public void submit(State rs, PoseStack pose, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        // IMPORTANT:
        // In the deferred SubmitNodeCollector pipeline, you must NOT mutate shared ModelPart rotations
        // (like model.lid.xRot) because the draw can happen later after another chest overwrote it.
        // Instead, apply per-instance animation using PoseStack transforms.

        float xRotRad = keyframedOpenRadians(rs.lidProgress);

        pose.pushPose();

        // Transform to entity-model space
        pose.translate(0.5D, 1.5D, 0.5D);
        pose.scale(1.0F, -1.0F, -1.0F);

        float yRot = switch (rs.facing) {
            case NORTH -> 180.0F;
            case SOUTH -> 0.0F;
            case WEST  -> 90.0F;
            case EAST  -> -90.0F;
            default    -> 0.0F;
        };
        pose.mulPose(Axis.YP.rotationDegrees(yRot));

        RenderType rt = RenderType.entityCutout(rs.texture);
        int packedOverlay = OverlayTexture.NO_OVERLAY;

        // BASE (no animation)
        collector.submitModelPart(this.model.base, pose, rt, rs.lightCoords, packedOverlay, null);

        // LID (+ knob child) animated via PoseStack around hinge pivot
        // Lid pivot from your model: PartPose.offset(0.0F, 15.0F, 7.0F)
        // ModelPart translations are in "pixels" and are applied as /16 internally,
        // so we do the same for our manual pivot transforms.
        final float pivotX = 0.0F / 16.0F;
        final float pivotY = 15.0F / 16.0F;
        final float pivotZ = 7.0F / 16.0F;

        pose.pushPose();
        pose.translate(pivotX, pivotY, pivotZ);
        pose.mulPose(Axis.XP.rotation(xRotRad));
        pose.translate(-pivotX, -pivotY, -pivotZ);

        // Ensure we are NOT also rotating the model part itself.
        this.model.lid.xRot = 0.0F;

        // Submitting lid renders knob too (knob is child of lid)
        collector.submitModelPart(this.model.lid, pose, rt, rs.lightCoords, packedOverlay, null);

        pose.popPose();
        pose.popPose();
    }

    private static ResourceLocation textureFor(BlockState state) {
        String cls = "none";
        if (state.getBlock() instanceof ClassLocked locked) {
            String req = locked.requiredClassId();
            if (req != null && !req.isBlank()) cls = req;
        }
        final String key = cls;
        return TEX_CACHE.computeIfAbsent(key, k ->
                ResourceLocation.fromNamespaceAndPath(
                        CosmicDungeonMod.MOD_ID,
                        "textures/entity/chest/" + k + "_chest.png"
                )
        );
    }

    private static ResourceLocation defaultTexture() {
        return ResourceLocation.fromNamespaceAndPath(
                CosmicDungeonMod.MOD_ID,
                "textures/entity/chest/none_chest.png"
        );
    }

    // Keyframes (same as your version)
    private static final float[] T = {
            0.0F, 0.0417F, 0.0833F, 0.125F, 0.1667F, 0.2083F, 0.25F, 0.2917F, 0.3333F, 0.375F, 0.4167F, 0.4583F
    };

    private static final float[] X_DEG = {
            0.0F, -3.10132F, -9.50804F, -17.4419F, -26.19058F, -35.3541F, -44.6459F, -53.80942F, -62.5581F, -70.49196F, -76.89868F, -80.0F
    };

    private static final float KEYED_END = 0.4583F;

    private static float keyframedOpenRadians(float progress01) {
        progress01 = clamp01(progress01);
        float t = progress01 * KEYED_END;

        int i = 0;
        while (i < T.length - 1 && t > T[i + 1]) i++;

        float t0 = T[i];
        float t1 = T[Math.min(i + 1, T.length - 1)];
        float x0 = X_DEG[i];
        float x1 = X_DEG[Math.min(i + 1, X_DEG.length - 1)];

        float a = (t1 <= t0) ? 0.0F : (t - t0) / (t1 - t0);
        float xDeg = x0 + (x1 - x0) * a;

        return xDeg * ((float) Math.PI / 180.0F);
    }

    private static float clamp01(float v) {
        if (v < 0.0F) return 0.0F;
        if (v > 1.0F) return 1.0F;
        return v;
    }
}
