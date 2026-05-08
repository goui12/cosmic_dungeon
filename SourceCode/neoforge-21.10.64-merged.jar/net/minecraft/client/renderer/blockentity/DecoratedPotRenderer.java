package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.DecoratedPotRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class DecoratedPotRenderer implements BlockEntityRenderer<DecoratedPotBlockEntity, DecoratedPotRenderState> {
    private final MaterialSet materials;
    private static final String NECK = "neck";
    private static final String FRONT = "front";
    private static final String BACK = "back";
    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final String TOP = "top";
    private static final String BOTTOM = "bottom";
    private final ModelPart neck;
    private final ModelPart frontSide;
    private final ModelPart backSide;
    private final ModelPart leftSide;
    private final ModelPart rightSide;
    private final ModelPart top;
    private final ModelPart bottom;
    private static final float WOBBLE_AMPLITUDE = 0.125F;

    public DecoratedPotRenderer(BlockEntityRendererProvider.Context context) {
        this(context.entityModelSet(), context.materials());
    }

    public DecoratedPotRenderer(SpecialModelRenderer.BakingContext context) {
        this(context.entityModelSet(), context.materials());
    }

    public DecoratedPotRenderer(EntityModelSet modelSet, MaterialSet materials) {
        this.materials = materials;
        ModelPart modelpart = modelSet.bakeLayer(ModelLayers.DECORATED_POT_BASE);
        this.neck = modelpart.getChild("neck");
        this.top = modelpart.getChild("top");
        this.bottom = modelpart.getChild("bottom");
        ModelPart modelpart1 = modelSet.bakeLayer(ModelLayers.DECORATED_POT_SIDES);
        this.frontSide = modelpart1.getChild("front");
        this.backSide = modelpart1.getChild("back");
        this.leftSide = modelpart1.getChild("left");
        this.rightSide = modelpart1.getChild("right");
    }

    public static LayerDefinition createBaseLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        CubeDeformation cubedeformation = new CubeDeformation(0.2F);
        CubeDeformation cubedeformation1 = new CubeDeformation(-0.1F);
        partdefinition.addOrReplaceChild(
            "neck",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(4.0F, 17.0F, 4.0F, 8.0F, 3.0F, 8.0F, cubedeformation1)
                .texOffs(0, 5)
                .addBox(5.0F, 20.0F, 5.0F, 6.0F, 1.0F, 6.0F, cubedeformation),
            PartPose.offsetAndRotation(0.0F, 37.0F, 16.0F, (float) Math.PI, 0.0F, 0.0F)
        );
        CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(-14, 13).addBox(0.0F, 0.0F, 0.0F, 14.0F, 0.0F, 14.0F);
        partdefinition.addOrReplaceChild("top", cubelistbuilder, PartPose.offsetAndRotation(1.0F, 16.0F, 1.0F, 0.0F, 0.0F, 0.0F));
        partdefinition.addOrReplaceChild("bottom", cubelistbuilder, PartPose.offsetAndRotation(1.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    public static LayerDefinition createSidesLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(1, 0).addBox(0.0F, 0.0F, 0.0F, 14.0F, 16.0F, 0.0F, EnumSet.of(Direction.NORTH));
        partdefinition.addOrReplaceChild("back", cubelistbuilder, PartPose.offsetAndRotation(15.0F, 16.0F, 1.0F, 0.0F, 0.0F, (float) Math.PI));
        partdefinition.addOrReplaceChild("left", cubelistbuilder, PartPose.offsetAndRotation(1.0F, 16.0F, 1.0F, 0.0F, (float) (-Math.PI / 2), (float) Math.PI));
        partdefinition.addOrReplaceChild(
            "right", cubelistbuilder, PartPose.offsetAndRotation(15.0F, 16.0F, 15.0F, 0.0F, (float) (Math.PI / 2), (float) Math.PI)
        );
        partdefinition.addOrReplaceChild("front", cubelistbuilder, PartPose.offsetAndRotation(1.0F, 16.0F, 15.0F, (float) Math.PI, 0.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    private static Material getSideMaterial(Optional<Item> item) {
        if (item.isPresent()) {
            Material material = Sheets.getDecoratedPotMaterial(DecoratedPotPatterns.getPatternFromItem(item.get()));
            if (material != null) {
                return material;
            }
        }

        return Sheets.DECORATED_POT_SIDE;
    }

    public DecoratedPotRenderState createRenderState() {
        return new DecoratedPotRenderState();
    }

    public void extractRenderState(
        DecoratedPotBlockEntity blockEntity,
        DecoratedPotRenderState renderState,
        float partialTick,
        Vec3 cameraPosition,
        @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.decorations = blockEntity.getDecorations();
        renderState.direction = blockEntity.getDirection();
        DecoratedPotBlockEntity.WobbleStyle decoratedpotblockentity$wobblestyle = blockEntity.lastWobbleStyle;
        if (decoratedpotblockentity$wobblestyle != null && blockEntity.getLevel() != null) {
            renderState.wobbleProgress = ((float)(blockEntity.getLevel().getGameTime() - blockEntity.wobbleStartedAtTick) + partialTick)
                / decoratedpotblockentity$wobblestyle.duration;
        } else {
            renderState.wobbleProgress = 0.0F;
        }
    }

    public void submit(DecoratedPotRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        Direction direction = renderState.direction;
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - direction.toYRot()));
        poseStack.translate(-0.5, 0.0, -0.5);
        if (renderState.wobbleProgress >= 0.0F && renderState.wobbleProgress <= 1.0F) {
            if (renderState.wobbleStyle == DecoratedPotBlockEntity.WobbleStyle.POSITIVE) {
                float f = 0.015625F;
                float f1 = renderState.wobbleProgress * (float) (Math.PI * 2);
                float f2 = -1.5F * (Mth.cos(f1) + 0.5F) * Mth.sin(f1 / 2.0F);
                poseStack.rotateAround(Axis.XP.rotation(f2 * 0.015625F), 0.5F, 0.0F, 0.5F);
                float f3 = Mth.sin(f1);
                poseStack.rotateAround(Axis.ZP.rotation(f3 * 0.015625F), 0.5F, 0.0F, 0.5F);
            } else {
                float f4 = Mth.sin(-renderState.wobbleProgress * 3.0F * (float) Math.PI) * 0.125F;
                float f5 = 1.0F - renderState.wobbleProgress;
                poseStack.rotateAround(Axis.YP.rotation(f4 * f5), 0.5F, 0.0F, 0.5F);
            }
        }

        this.submit(poseStack, nodeCollector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.decorations, 0);
        poseStack.popPose();
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, PotDecorations decorations, int outlineColor) {
        RenderType rendertype = Sheets.DECORATED_POT_BASE.renderType(RenderType::entitySolid);
        TextureAtlasSprite textureatlassprite = this.materials.get(Sheets.DECORATED_POT_BASE);
        nodeCollector.submitModelPart(this.neck, poseStack, rendertype, packedLight, packedOverlay, textureatlassprite, false, false, -1, null, outlineColor);
        nodeCollector.submitModelPart(this.top, poseStack, rendertype, packedLight, packedOverlay, textureatlassprite, false, false, -1, null, outlineColor);
        nodeCollector.submitModelPart(this.bottom, poseStack, rendertype, packedLight, packedOverlay, textureatlassprite, false, false, -1, null, outlineColor);
        Material material = getSideMaterial(decorations.front());
        nodeCollector.submitModelPart(
            this.frontSide,
            poseStack,
            material.renderType(RenderType::entitySolid),
            packedLight,
            packedOverlay,
            this.materials.get(material),
            false,
            false,
            -1,
            null,
            outlineColor
        );
        Material material1 = getSideMaterial(decorations.back());
        nodeCollector.submitModelPart(
            this.backSide,
            poseStack,
            material1.renderType(RenderType::entitySolid),
            packedLight,
            packedOverlay,
            this.materials.get(material1),
            false,
            false,
            -1,
            null,
            outlineColor
        );
        Material material2 = getSideMaterial(decorations.left());
        nodeCollector.submitModelPart(
            this.leftSide,
            poseStack,
            material2.renderType(RenderType::entitySolid),
            packedLight,
            packedOverlay,
            this.materials.get(material2),
            false,
            false,
            -1,
            null,
            outlineColor
        );
        Material material3 = getSideMaterial(decorations.right());
        nodeCollector.submitModelPart(
            this.rightSide,
            poseStack,
            material3.renderType(RenderType::entitySolid),
            packedLight,
            packedOverlay,
            this.materials.get(material3),
            false,
            false,
            -1,
            null,
            outlineColor
        );
    }

    public void getExtents(Set<Vector3f> output) {
        PoseStack posestack = new PoseStack();
        this.neck.getExtentsForGui(posestack, output);
        this.top.getExtentsForGui(posestack, output);
        this.bottom.getExtentsForGui(posestack, output);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(DecoratedPotBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.3, pos.getZ() + 1.0);
    }
}
