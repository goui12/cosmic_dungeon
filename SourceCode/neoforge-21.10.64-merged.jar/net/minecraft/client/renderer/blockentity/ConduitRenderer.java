package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MaterialMapper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.CondiutRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ConduitRenderer implements BlockEntityRenderer<ConduitBlockEntity, CondiutRenderState> {
    public static final MaterialMapper MAPPER = new MaterialMapper(TextureAtlas.LOCATION_BLOCKS, "entity/conduit");
    public static final Material SHELL_TEXTURE = MAPPER.defaultNamespaceApply("base");
    public static final Material ACTIVE_SHELL_TEXTURE = MAPPER.defaultNamespaceApply("cage");
    public static final Material WIND_TEXTURE = MAPPER.defaultNamespaceApply("wind");
    public static final Material VERTICAL_WIND_TEXTURE = MAPPER.defaultNamespaceApply("wind_vertical");
    public static final Material OPEN_EYE_TEXTURE = MAPPER.defaultNamespaceApply("open_eye");
    public static final Material CLOSED_EYE_TEXTURE = MAPPER.defaultNamespaceApply("closed_eye");
    private final MaterialSet materials;
    private final ModelPart eye;
    private final ModelPart wind;
    private final ModelPart shell;
    private final ModelPart cage;

    public ConduitRenderer(BlockEntityRendererProvider.Context context) {
        this.materials = context.materials();
        this.eye = context.bakeLayer(ModelLayers.CONDUIT_EYE);
        this.wind = context.bakeLayer(ModelLayers.CONDUIT_WIND);
        this.shell = context.bakeLayer(ModelLayers.CONDUIT_SHELL);
        this.cage = context.bakeLayer(ModelLayers.CONDUIT_CAGE);
    }

    public static LayerDefinition createEyeLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild(
            "eye", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.01F)), PartPose.ZERO
        );
        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    public static LayerDefinition createWindLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("wind", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F), PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    public static LayerDefinition createShellLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 32, 16);
    }

    public static LayerDefinition createCageLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 32, 16);
    }

    public CondiutRenderState createRenderState() {
        return new CondiutRenderState();
    }

    public void extractRenderState(
        ConduitBlockEntity blockEntity, CondiutRenderState renderState, float partialTick, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.isActive = blockEntity.isActive();
        renderState.activeRotation = blockEntity.getActiveRotation(blockEntity.isActive() ? partialTick : 0.0F);
        renderState.animTime = blockEntity.tickCount + partialTick;
        renderState.animationPhase = blockEntity.tickCount / 66 % 3;
        renderState.isHunting = blockEntity.isHunting();
    }

    public void submit(CondiutRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (!renderState.isActive) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(new Quaternionf().rotationY(renderState.activeRotation * (float) (Math.PI / 180.0)));
            nodeCollector.submitModelPart(
                this.shell,
                poseStack,
                SHELL_TEXTURE.renderType(RenderType::entitySolid),
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                this.materials.get(SHELL_TEXTURE),
                -1,
                renderState.breakProgress
            );
            poseStack.popPose();
        } else {
            float f = renderState.activeRotation * (180.0F / (float)Math.PI);
            float f1 = Mth.sin(renderState.animTime * 0.1F) / 2.0F + 0.5F;
            f1 = f1 * f1 + f1;
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.3F + f1 * 0.2F, 0.5F);
            Vector3f vector3f = new Vector3f(0.5F, 1.0F, 0.5F).normalize();
            poseStack.mulPose(new Quaternionf().rotationAxis(f * (float) (Math.PI / 180.0), vector3f));
            nodeCollector.submitModelPart(
                this.cage,
                poseStack,
                ACTIVE_SHELL_TEXTURE.renderType(RenderType::entityCutoutNoCull),
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                this.materials.get(ACTIVE_SHELL_TEXTURE),
                -1,
                renderState.breakProgress
            );
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            if (renderState.animationPhase == 1) {
                poseStack.mulPose(new Quaternionf().rotationX((float) (Math.PI / 2)));
            } else if (renderState.animationPhase == 2) {
                poseStack.mulPose(new Quaternionf().rotationZ((float) (Math.PI / 2)));
            }

            Material material = renderState.animationPhase == 1 ? VERTICAL_WIND_TEXTURE : WIND_TEXTURE;
            RenderType rendertype = material.renderType(RenderType::entityCutoutNoCull);
            TextureAtlasSprite textureatlassprite = this.materials.get(material);
            nodeCollector.submitModelPart(this.wind, poseStack, rendertype, renderState.lightCoords, OverlayTexture.NO_OVERLAY, textureatlassprite);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.scale(0.875F, 0.875F, 0.875F);
            poseStack.mulPose(new Quaternionf().rotationXYZ((float) Math.PI, 0.0F, (float) Math.PI));
            nodeCollector.submitModelPart(this.wind, poseStack, rendertype, renderState.lightCoords, OverlayTexture.NO_OVERLAY, textureatlassprite);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.3F + f1 * 0.2F, 0.5F);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(cameraRenderState.orientation);
            poseStack.mulPose(new Quaternionf().rotationZ((float) Math.PI).rotateY((float) Math.PI));
            float f2 = 1.3333334F;
            poseStack.scale(1.3333334F, 1.3333334F, 1.3333334F);
            Material material1 = renderState.isHunting ? OPEN_EYE_TEXTURE : CLOSED_EYE_TEXTURE;
            nodeCollector.submitModelPart(
                this.eye,
                poseStack,
                material1.renderType(RenderType::entityCutoutNoCull),
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                this.materials.get(material1)
            );
            poseStack.popPose();
        }
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(ConduitBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX(), pos.getY() - .25, pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.25, pos.getZ() + 1.0);
    }
}
