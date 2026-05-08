package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.Parrot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParrotOnShoulderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final ParrotModel model;

    public ParrotOnShoulderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new ParrotModel(modelSet.bakeLayer(ModelLayers.PARROT));
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, AvatarRenderState renderState, float yRot, float xRot) {
        Parrot.Variant parrot$variant = renderState.parrotOnLeftShoulder;
        if (parrot$variant != null) {
            this.submitOnShoulder(poseStack, nodeCollector, packedLight, renderState, parrot$variant, yRot, xRot, true);
        }

        Parrot.Variant parrot$variant1 = renderState.parrotOnRightShoulder;
        if (parrot$variant1 != null) {
            this.submitOnShoulder(poseStack, nodeCollector, packedLight, renderState, parrot$variant1, yRot, xRot, false);
        }
    }

    private void submitOnShoulder(
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        AvatarRenderState renderState,
        Parrot.Variant variant,
        float yRot,
        float xRot,
        boolean isLeft
    ) {
        poseStack.pushPose();
        poseStack.translate(isLeft ? 0.4F : -0.4F, renderState.isCrouching ? -1.3F : -1.5F, 0.0F);
        ParrotRenderState parrotrenderstate = new ParrotRenderState();
        parrotrenderstate.pose = ParrotModel.Pose.ON_SHOULDER;
        parrotrenderstate.ageInTicks = renderState.ageInTicks;
        parrotrenderstate.walkAnimationPos = renderState.walkAnimationPos;
        parrotrenderstate.walkAnimationSpeed = renderState.walkAnimationSpeed;
        parrotrenderstate.yRot = yRot;
        parrotrenderstate.xRot = xRot;
        nodeCollector.submitModel(
            this.model,
            parrotrenderstate,
            poseStack,
            this.model.renderType(ParrotRenderer.getVariantTexture(variant)),
            packedLight,
            OverlayTexture.NO_OVERLAY,
            renderState.outlineColor,
            null
        );
        poseStack.popPose();
    }
}
