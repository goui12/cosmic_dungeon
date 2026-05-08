package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class StuckInBodyLayer<M extends PlayerModel, S> extends RenderLayer<AvatarRenderState, M> {
    private final Model<S> model;
    private final S modelState;
    private final ResourceLocation texture;
    private final StuckInBodyLayer.PlacementStyle placementStyle;

    public StuckInBodyLayer(
        LivingEntityRenderer<?, AvatarRenderState, M> renderer,
        Model<S> model,
        S modelState,
        ResourceLocation texture,
        StuckInBodyLayer.PlacementStyle placementStyle
    ) {
        super(renderer);
        this.model = model;
        this.modelState = modelState;
        this.texture = texture;
        this.placementStyle = placementStyle;
    }

    protected abstract int numStuck(AvatarRenderState renderState);

    private void submitStuckItem(
        PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, float x, float y, float z, int outlineColor
    ) {
        float f = Mth.sqrt(x * x + z * z);
        float f1 = (float)(Math.atan2(x, z) * 180.0F / (float)Math.PI);
        float f2 = (float)(Math.atan2(y, f) * 180.0F / (float)Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(f1 - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(f2));
        nodeCollector.submitModel(
            this.model, this.modelState, poseStack, this.model.renderType(this.texture), packedLight, OverlayTexture.NO_OVERLAY, outlineColor, null
        );
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, AvatarRenderState renderState, float yRot, float xRot) {
        int i = this.numStuck(renderState);
        if (i > 0) {
            RandomSource randomsource = RandomSource.create(renderState.id);

            for (int j = 0; j < i; j++) {
                poseStack.pushPose();
                ModelPart modelpart = this.getParentModel().getRandomBodyPart(randomsource);
                ModelPart.Cube modelpart$cube = modelpart.getRandomCube(randomsource);
                modelpart.translateAndRotate(poseStack);
                float f = randomsource.nextFloat();
                float f1 = randomsource.nextFloat();
                float f2 = randomsource.nextFloat();
                if (this.placementStyle == StuckInBodyLayer.PlacementStyle.ON_SURFACE) {
                    int k = randomsource.nextInt(3);
                    switch (k) {
                        case 0:
                            f = snapToFace(f);
                            break;
                        case 1:
                            f1 = snapToFace(f1);
                            break;
                        default:
                            f2 = snapToFace(f2);
                    }
                }

                poseStack.translate(
                    Mth.lerp(f, modelpart$cube.minX, modelpart$cube.maxX) / 16.0F,
                    Mth.lerp(f1, modelpart$cube.minY, modelpart$cube.maxY) / 16.0F,
                    Mth.lerp(f2, modelpart$cube.minZ, modelpart$cube.maxZ) / 16.0F
                );
                this.submitStuckItem(poseStack, nodeCollector, packedLight, -(f * 2.0F - 1.0F), -(f1 * 2.0F - 1.0F), -(f2 * 2.0F - 1.0F), renderState.outlineColor);
                poseStack.popPose();
            }
        }
    }

    private static float snapToFace(float value) {
        return value > 0.5F ? 1.0F : 0.5F;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum PlacementStyle {
        IN_CUBE,
        ON_SURFACE;
    }
}
