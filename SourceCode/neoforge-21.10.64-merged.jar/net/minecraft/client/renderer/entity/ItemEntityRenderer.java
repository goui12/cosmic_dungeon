package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemEntityRenderer extends EntityRenderer<ItemEntity, ItemEntityRenderState> {
    private static final float ITEM_MIN_HOVER_HEIGHT = 0.0625F;
    private static final float ITEM_BUNDLE_OFFSET_SCALE = 0.15F;
    private static final float FLAT_ITEM_DEPTH_THRESHOLD = 0.0625F;
    private final ItemModelResolver itemModelResolver;
    private final RandomSource random = RandomSource.create();

    public ItemEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }

    public ItemEntityRenderState createRenderState() {
        return new ItemEntityRenderState();
    }

    public void extractRenderState(ItemEntity entity, ItemEntityRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.bobOffset = entity.bobOffs;
        reusedState.shouldBob = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(entity.getItem()).shouldBobAsEntity(entity.getItem());
        reusedState.shouldSpread = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(entity.getItem()).shouldSpreadAsEntity(entity.getItem());
        reusedState.extractItemGroupRenderState(entity, entity.getItem(), this.itemModelResolver);
    }

    public void submit(ItemEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (!renderState.item.isEmpty()) {
            poseStack.pushPose();
            AABB aabb = renderState.item.getModelBoundingBox();
            float f = -((float)aabb.minY) + 0.0625F;
            float f1 = renderState.shouldBob ? Mth.sin(renderState.ageInTicks / 10.0F + renderState.bobOffset) * 0.1F + 0.1F : 0;
            poseStack.translate(0.0F, f1 + f, 0.0F);
            float f2 = ItemEntity.getSpin(renderState.ageInTicks, renderState.bobOffset);
            poseStack.mulPose(Axis.YP.rotation(f2));
            submitMultipleFromCount(poseStack, nodeCollector, renderState.lightCoords, renderState, this.random, aabb);
            poseStack.popPose();
            super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        }
    }

    public static void submitMultipleFromCount(
        PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, ItemClusterRenderState renderState, RandomSource random
    ) {
        submitMultipleFromCount(poseStack, nodeCollector, packedLight, renderState, random, renderState.item.getModelBoundingBox());
    }

    public static void submitMultipleFromCount(
        PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, ItemClusterRenderState renderState, RandomSource random, AABB boundingBox
    ) {
        int i = renderState.count;
        if (i != 0) {
            random.setSeed(renderState.seed);
            ItemStackRenderState itemstackrenderstate = renderState.item;
            float f = (float)boundingBox.getZsize();
            if (f > 0.0625F) {
                itemstackrenderstate.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);

                for (int j = 1; j < i; j++) {
                    poseStack.pushPose();
                    float f1 = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float f2 = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float f3 = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    if (renderState.shouldSpread) {
                        poseStack.translate(f1, f2, f3);
                    }
                    itemstackrenderstate.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
                    poseStack.popPose();
                }
            } else {
                float f4 = f * 1.5F;
                poseStack.translate(0.0F, 0.0F, -(f4 * (i - 1) / 2.0F));
                itemstackrenderstate.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
                poseStack.translate(0.0F, 0.0F, f4);

                for (int k = 1; k < i; k++) {
                    poseStack.pushPose();
                    float f5 = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    float f6 = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    if (renderState.shouldSpread) {
                        poseStack.translate(f5, f6, 0.0F);
                    }
                    itemstackrenderstate.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
                    poseStack.popPose();
                    poseStack.translate(0.0F, 0.0F, f4);
                }
            }
        }
    }

    public static void renderMultipleFromCount(
        PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, ItemClusterRenderState renderState, RandomSource random
    ) {
        AABB aabb = renderState.item.getModelBoundingBox();
        int i = renderState.count;
        if (i != 0) {
            random.setSeed(renderState.seed);
            ItemStackRenderState itemstackrenderstate = renderState.item;
            float f = (float)aabb.getZsize();
            if (f > 0.0625F) {
                itemstackrenderstate.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);

                for (int j = 1; j < i; j++) {
                    poseStack.pushPose();
                    float f1 = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float f2 = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float f3 = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    poseStack.translate(f1, f2, f3);
                    itemstackrenderstate.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
                    poseStack.popPose();
                }
            } else {
                float f4 = f * 1.5F;
                poseStack.translate(0.0F, 0.0F, -(f4 * (i - 1) / 2.0F));
                itemstackrenderstate.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
                poseStack.translate(0.0F, 0.0F, f4);

                for (int k = 1; k < i; k++) {
                    poseStack.pushPose();
                    float f5 = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    float f6 = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    poseStack.translate(f5, f6, 0.0F);
                    itemstackrenderstate.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
                    poseStack.popPose();
                    poseStack.translate(0.0F, 0.0F, f4);
                }
            }
        }
    }
}
