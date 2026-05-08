package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.model.BannerFlagModel;
import net.minecraft.client.model.BannerModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class BannerRenderer implements BlockEntityRenderer<BannerBlockEntity, BannerRenderState> {
    private static final int MAX_PATTERNS = 16;
    private static final float SIZE = 0.6666667F;
    private final MaterialSet materials;
    private final BannerModel standingModel;
    private final BannerModel wallModel;
    private final BannerFlagModel standingFlagModel;
    private final BannerFlagModel wallFlagModel;

    public BannerRenderer(BlockEntityRendererProvider.Context context) {
        this(context.entityModelSet(), context.materials());
    }

    public BannerRenderer(SpecialModelRenderer.BakingContext context) {
        this(context.entityModelSet(), context.materials());
    }

    public BannerRenderer(EntityModelSet modelSet, MaterialSet materials) {
        this.materials = materials;
        this.standingModel = new BannerModel(modelSet.bakeLayer(ModelLayers.STANDING_BANNER));
        this.wallModel = new BannerModel(modelSet.bakeLayer(ModelLayers.WALL_BANNER));
        this.standingFlagModel = new BannerFlagModel(modelSet.bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
        this.wallFlagModel = new BannerFlagModel(modelSet.bakeLayer(ModelLayers.WALL_BANNER_FLAG));
    }

    public BannerRenderState createRenderState() {
        return new BannerRenderState();
    }

    public void extractRenderState(
        BannerBlockEntity blockEntity, BannerRenderState renderState, float partialTick, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.baseColor = blockEntity.getBaseColor();
        renderState.patterns = blockEntity.getPatterns();
        BlockState blockstate = blockEntity.getBlockState();
        if (blockstate.getBlock() instanceof BannerBlock) {
            renderState.angle = -RotationSegment.convertToDegrees(blockstate.getValue(BannerBlock.ROTATION));
            renderState.standing = true;
        } else {
            renderState.angle = -blockstate.getValue(WallBannerBlock.FACING).toYRot();
            renderState.standing = false;
        }

        long i = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;
        BlockPos blockpos = blockEntity.getBlockPos();
        renderState.phase = ((float)Math.floorMod(blockpos.getX() * 7 + blockpos.getY() * 9 + blockpos.getZ() * 13 + i, 100L) + partialTick) / 100.0F;
    }

    public void submit(BannerRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        BannerModel bannermodel;
        BannerFlagModel bannerflagmodel;
        if (renderState.standing) {
            bannermodel = this.standingModel;
            bannerflagmodel = this.standingFlagModel;
        } else {
            bannermodel = this.wallModel;
            bannerflagmodel = this.wallFlagModel;
        }

        submitBanner(
            this.materials,
            poseStack,
            nodeCollector,
            renderState.lightCoords,
            OverlayTexture.NO_OVERLAY,
            renderState.angle,
            bannermodel,
            bannerflagmodel,
            renderState.phase,
            renderState.baseColor,
            renderState.patterns,
            renderState.breakProgress,
            0
        );
    }

    public void submitSpecial(
        PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, DyeColor baseColor, BannerPatternLayers patterns, int outlineColor
    ) {
        submitBanner(
            this.materials,
            poseStack,
            nodeCollector,
            packedLight,
            packedOverlay,
            0.0F,
            this.standingModel,
            this.standingFlagModel,
            0.0F,
            baseColor,
            patterns,
            null,
            outlineColor
        );
    }

    private static void submitBanner(
        MaterialSet materials,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        int packedOverlay,
        float rotation,
        BannerModel model,
        BannerFlagModel flag,
        float sway,
        DyeColor baseColor,
        BannerPatternLayers patterns,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
        int outlineColor
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.scale(0.6666667F, -0.6666667F, -0.6666667F);
        Material material = ModelBakery.BANNER_BASE;
        nodeCollector.submitModel(
            model,
            Unit.INSTANCE,
            poseStack,
            material.renderType(RenderType::entitySolid),
            packedLight,
            packedOverlay,
            -1,
            materials.get(material),
            outlineColor,
            crumblingOverlay
        );
        submitPatterns(
            materials, poseStack, nodeCollector, packedLight, packedOverlay, flag, sway, material, true, baseColor, patterns, false, crumblingOverlay, outlineColor
        );
        poseStack.popPose();
    }

    public static <S> void submitPatterns(
        MaterialSet materials,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        int packedOverlay,
        Model<S> flag,
        S renderState,
        Material p_material,
        boolean banner,
        DyeColor baseColor,
        BannerPatternLayers patterns,
        boolean withGlint,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
        int outlineColor
    ) {
        nodeCollector.submitModel(
            flag,
            renderState,
            poseStack,
            p_material.renderType(RenderType::entitySolid),
            packedLight,
            packedOverlay,
            -1,
            materials.get(p_material),
            outlineColor,
            crumblingOverlay
        );
        if (withGlint) {
            nodeCollector.submitModel(flag, renderState, poseStack, RenderType.entityGlint(), packedLight, packedOverlay, -1, materials.get(p_material), 0, crumblingOverlay);
        }

        submitPatternLayer(
            materials,
            poseStack,
            nodeCollector,
            packedLight,
            packedOverlay,
            flag,
            renderState,
            banner ? Sheets.BANNER_BASE : Sheets.SHIELD_BASE,
            baseColor,
            crumblingOverlay
        );

        for (int i = 0; i < 16 && i < patterns.layers().size(); i++) {
            BannerPatternLayers.Layer bannerpatternlayers$layer = patterns.layers().get(i);
            Material material = banner
                ? Sheets.getBannerMaterial(bannerpatternlayers$layer.pattern())
                : Sheets.getShieldMaterial(bannerpatternlayers$layer.pattern());
            submitPatternLayer(materials, poseStack, nodeCollector, packedLight, packedOverlay, flag, renderState, material, bannerpatternlayers$layer.color(), null);
        }
    }

    private static <S> void submitPatternLayer(
        MaterialSet materials,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        int packedOverlay,
        Model<S> flagModel,
        S sway,
        Material material,
        DyeColor color,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        int i = color.getTextureDiffuseColor();
        nodeCollector.submitModel(
            flagModel, sway, poseStack, material.renderType(RenderType::entityNoOutline), packedLight, packedOverlay, i, materials.get(material), 0, crumblingOverlay
        );
    }

    public void getExtents(Set<Vector3f> output) {
        PoseStack posestack = new PoseStack();
        posestack.translate(0.5F, 0.0F, 0.5F);
        posestack.scale(0.6666667F, -0.6666667F, -0.6666667F);
        this.standingModel.root().getExtentsForGui(posestack, output);
        this.standingFlagModel.setupAnim(0.0F);
        this.standingFlagModel.root().getExtentsForGui(posestack, output);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(BannerBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        boolean standing = blockEntity.getBlockState().getBlock() instanceof BannerBlock;
        return net.minecraft.world.phys.AABB.encapsulatingFullBlocks(pos, standing ? pos.above() : pos.below());
    }
}
