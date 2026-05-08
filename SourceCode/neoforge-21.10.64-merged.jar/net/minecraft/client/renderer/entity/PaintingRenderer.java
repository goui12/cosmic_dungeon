package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PaintingRenderer extends EntityRenderer<Painting, PaintingRenderState> {
    private static final ResourceLocation BACK_SPRITE_LOCATION = ResourceLocation.withDefaultNamespace("back");
    private final TextureAtlas paintingsAtlas;

    public PaintingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.paintingsAtlas = context.getAtlas(AtlasIds.PAINTINGS);
    }

    public void submit(PaintingRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        PaintingVariant paintingvariant = renderState.variant;
        if (paintingvariant != null) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180 - renderState.direction.get2DDataValue() * 90));
            TextureAtlasSprite textureatlassprite = this.paintingsAtlas.getSprite(paintingvariant.assetId());
            TextureAtlasSprite textureatlassprite1 = this.paintingsAtlas.getSprite(BACK_SPRITE_LOCATION);
            this.renderPainting(
                poseStack,
                nodeCollector,
                RenderType.entitySolidZOffsetForward(textureatlassprite1.atlasLocation()),
                renderState.lightCoordsPerBlock,
                paintingvariant.width(),
                paintingvariant.height(),
                textureatlassprite,
                textureatlassprite1
            );
            poseStack.popPose();
            super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        }
    }

    public PaintingRenderState createRenderState() {
        return new PaintingRenderState();
    }

    public void extractRenderState(Painting entity, PaintingRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        Direction direction = entity.getDirection();
        PaintingVariant paintingvariant = entity.getVariant().value();
        reusedState.direction = direction;
        reusedState.variant = paintingvariant;
        int i = paintingvariant.width();
        int j = paintingvariant.height();
        if (reusedState.lightCoordsPerBlock.length != i * j) {
            reusedState.lightCoordsPerBlock = new int[i * j];
        }

        float f = -i / 2.0F;
        float f1 = -j / 2.0F;
        Level level = entity.level();

        for (int k = 0; k < j; k++) {
            for (int l = 0; l < i; l++) {
                float f2 = l + f + 0.5F;
                float f3 = k + f1 + 0.5F;
                int i1 = entity.getBlockX();
                int j1 = Mth.floor(entity.getY() + f3);
                int k1 = entity.getBlockZ();
                switch (direction) {
                    case NORTH:
                        i1 = Mth.floor(entity.getX() + f2);
                        break;
                    case WEST:
                        k1 = Mth.floor(entity.getZ() - f2);
                        break;
                    case SOUTH:
                        i1 = Mth.floor(entity.getX() - f2);
                        break;
                    case EAST:
                        k1 = Mth.floor(entity.getZ() + f2);
                }

                reusedState.lightCoordsPerBlock[l + k * i] = LevelRenderer.getLightColor(level, new BlockPos(i1, j1, k1));
            }
        }
    }

    private void renderPainting(
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        RenderType renderType,
        int[] packedLightPerBlock,
        int width,
        int height,
        TextureAtlasSprite frontSprite,
        TextureAtlasSprite backSprite
    ) {
        nodeCollector.submitCustomGeometry(poseStack, renderType, (p_435197_, p_434479_) -> {
            float f = -width / 2.0F;
            float f1 = -height / 2.0F;
            float f2 = 0.03125F;
            float f3 = backSprite.getU0();
            float f4 = backSprite.getU1();
            float f5 = backSprite.getV0();
            float f6 = backSprite.getV1();
            float f7 = backSprite.getU0();
            float f8 = backSprite.getU1();
            float f9 = backSprite.getV0();
            float f10 = backSprite.getV(0.0625F);
            float f11 = backSprite.getU0();
            float f12 = backSprite.getU(0.0625F);
            float f13 = backSprite.getV0();
            float f14 = backSprite.getV1();
            double d0 = 1.0 / width;
            double d1 = 1.0 / height;

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    float f15 = f + (i + 1);
                    float f16 = f + i;
                    float f17 = f1 + (j + 1);
                    float f18 = f1 + j;
                    int k = packedLightPerBlock[i + j * width];
                    float f19 = frontSprite.getU((float)(d0 * (width - i)));
                    float f20 = frontSprite.getU((float)(d0 * (width - (i + 1))));
                    float f21 = frontSprite.getV((float)(d1 * (height - j)));
                    float f22 = frontSprite.getV((float)(d1 * (height - (j + 1))));
                    this.vertex(p_435197_, p_434479_, f15, f18, f20, f21, -0.03125F, 0, 0, -1, k);
                    this.vertex(p_435197_, p_434479_, f16, f18, f19, f21, -0.03125F, 0, 0, -1, k);
                    this.vertex(p_435197_, p_434479_, f16, f17, f19, f22, -0.03125F, 0, 0, -1, k);
                    this.vertex(p_435197_, p_434479_, f15, f17, f20, f22, -0.03125F, 0, 0, -1, k);
                    this.vertex(p_435197_, p_434479_, f15, f17, f4, f5, 0.03125F, 0, 0, 1, k);
                    this.vertex(p_435197_, p_434479_, f16, f17, f3, f5, 0.03125F, 0, 0, 1, k);
                    this.vertex(p_435197_, p_434479_, f16, f18, f3, f6, 0.03125F, 0, 0, 1, k);
                    this.vertex(p_435197_, p_434479_, f15, f18, f4, f6, 0.03125F, 0, 0, 1, k);
                    this.vertex(p_435197_, p_434479_, f15, f17, f7, f9, -0.03125F, 0, 1, 0, k);
                    this.vertex(p_435197_, p_434479_, f16, f17, f8, f9, -0.03125F, 0, 1, 0, k);
                    this.vertex(p_435197_, p_434479_, f16, f17, f8, f10, 0.03125F, 0, 1, 0, k);
                    this.vertex(p_435197_, p_434479_, f15, f17, f7, f10, 0.03125F, 0, 1, 0, k);
                    this.vertex(p_435197_, p_434479_, f15, f18, f7, f9, 0.03125F, 0, -1, 0, k);
                    this.vertex(p_435197_, p_434479_, f16, f18, f8, f9, 0.03125F, 0, -1, 0, k);
                    this.vertex(p_435197_, p_434479_, f16, f18, f8, f10, -0.03125F, 0, -1, 0, k);
                    this.vertex(p_435197_, p_434479_, f15, f18, f7, f10, -0.03125F, 0, -1, 0, k);
                    this.vertex(p_435197_, p_434479_, f15, f17, f12, f13, 0.03125F, -1, 0, 0, k);
                    this.vertex(p_435197_, p_434479_, f15, f18, f12, f14, 0.03125F, -1, 0, 0, k);
                    this.vertex(p_435197_, p_434479_, f15, f18, f11, f14, -0.03125F, -1, 0, 0, k);
                    this.vertex(p_435197_, p_434479_, f15, f17, f11, f13, -0.03125F, -1, 0, 0, k);
                    this.vertex(p_435197_, p_434479_, f16, f17, f12, f13, -0.03125F, 1, 0, 0, k);
                    this.vertex(p_435197_, p_434479_, f16, f18, f12, f14, -0.03125F, 1, 0, 0, k);
                    this.vertex(p_435197_, p_434479_, f16, f18, f11, f14, 0.03125F, 1, 0, 0, k);
                    this.vertex(p_435197_, p_434479_, f16, f17, f11, f13, 0.03125F, 1, 0, 0, k);
                }
            }
        });
    }

    private void vertex(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        float x,
        float y,
        float u,
        float v,
        float z,
        int normalX,
        int normalY,
        int normalZ,
        int packedLight
    ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(-1)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, normalX, normalY, normalZ);
    }
}
