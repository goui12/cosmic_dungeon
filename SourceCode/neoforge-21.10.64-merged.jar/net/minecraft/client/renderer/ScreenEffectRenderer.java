package net.minecraft.client.renderer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class ScreenEffectRenderer {
    private static final ResourceLocation UNDERWATER_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");
    private final Minecraft minecraft;
    private final MaterialSet materials;
    private final MultiBufferSource bufferSource;
    public static final int ITEM_ACTIVATION_ANIMATION_LENGTH = 40;
    @Nullable
    private ItemStack itemActivationItem;
    private int itemActivationTicks;
    private float itemActivationOffX;
    private float itemActivationOffY;

    public ScreenEffectRenderer(Minecraft minecraft, MaterialSet materials, MultiBufferSource bufferSource) {
        this.minecraft = minecraft;
        this.materials = materials;
        this.bufferSource = bufferSource;
    }

    public void tick() {
        if (this.itemActivationTicks > 0) {
            this.itemActivationTicks--;
            if (this.itemActivationTicks == 0) {
                this.itemActivationItem = null;
            }
        }
    }

    public void renderScreenEffect(boolean sleeping, float partialTick, SubmitNodeCollector nodeCollector) {
        PoseStack posestack = new PoseStack();
        Player player = this.minecraft.player;
        if (this.minecraft.options.getCameraType().isFirstPerson() && !sleeping) {
            if (!player.noPhysics) {
                org.apache.commons.lang3.tuple.Pair<BlockState, BlockPos> overlay = getOverlayBlock(player);
                if (overlay != null && !net.neoforged.neoforge.client.ClientHooks.renderBlockOverlay(player, posestack, net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent.OverlayType.BLOCK, overlay.getLeft(), overlay.getRight(), materials, bufferSource)) {
                    renderTex(this.minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(overlay.getLeft(), this.minecraft.level, overlay.getRight()), posestack, this.bufferSource);
                }
            }

            if (!this.minecraft.player.isSpectator()) {
                if (this.minecraft.player.isEyeInFluid(FluidTags.WATER)) {
                    if (!net.neoforged.neoforge.client.ClientHooks.renderWaterOverlay(player, posestack, materials, bufferSource))
                    renderWater(this.minecraft, posestack, this.bufferSource);
                }
                else if (!player.getEyeInFluidType().isAir()) net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(player.getEyeInFluidType()).renderOverlay(this.minecraft, posestack, this.bufferSource);

                if (this.minecraft.player.isOnFire()) {
                    TextureAtlasSprite textureatlassprite = this.materials.get(ModelBakery.FIRE_1);
                    if (!net.neoforged.neoforge.client.ClientHooks.renderFireOverlay(player, posestack, materials, bufferSource))
                    renderFire(posestack, this.bufferSource, textureatlassprite);
                }
            }
        }

        if (!this.minecraft.options.hideGui) {
            this.renderItemActivationAnimation(posestack, partialTick, nodeCollector);
        }
    }

    private void renderItemActivationAnimation(PoseStack poseStack, float partialTick, SubmitNodeCollector nodeCollector) {
        if (this.itemActivationItem != null && this.itemActivationTicks > 0) {
            int i = 40 - this.itemActivationTicks;
            float f = (i + partialTick) / 40.0F;
            float f1 = f * f;
            float f2 = f * f1;
            float f3 = 10.25F * f2 * f1 - 24.95F * f1 * f1 + 25.5F * f2 - 13.8F * f1 + 4.0F * f;
            float f4 = f3 * (float) Math.PI;
            float f5 = (float)this.minecraft.getWindow().getWidth() / this.minecraft.getWindow().getHeight();
            float f6 = this.itemActivationOffX * 0.3F * f5;
            float f7 = this.itemActivationOffY * 0.3F;
            poseStack.pushPose();
            poseStack.translate(f6 * Mth.abs(Mth.sin(f4 * 2.0F)), f7 * Mth.abs(Mth.sin(f4 * 2.0F)), -10.0F + 9.0F * Mth.sin(f4));
            float f8 = 0.8F;
            poseStack.scale(0.8F, 0.8F, 0.8F);
            poseStack.mulPose(Axis.YP.rotationDegrees(900.0F * Mth.abs(Mth.sin(f4))));
            poseStack.mulPose(Axis.XP.rotationDegrees(6.0F * Mth.cos(f * 8.0F)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.0F * Mth.cos(f * 8.0F)));
            this.minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
            ItemStackRenderState itemstackrenderstate = new ItemStackRenderState();
            this.minecraft
                .getItemModelResolver()
                .updateForTopItem(itemstackrenderstate, this.itemActivationItem, ItemDisplayContext.FIXED, this.minecraft.level, null, 0);
            itemstackrenderstate.submit(poseStack, nodeCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    public void resetItemActivation() {
        this.itemActivationItem = null;
    }

    public void displayItemActivation(ItemStack item, RandomSource random) {
        this.itemActivationItem = item;
        this.itemActivationTicks = 40;
        this.itemActivationOffX = random.nextFloat() * 2.0F - 1.0F;
        this.itemActivationOffY = random.nextFloat() * 2.0F - 1.0F;
    }

    @Nullable
    private static BlockState getViewBlockingState(Player player) {
        return getOverlayBlock(player).getLeft();
    }

    @Nullable
    private static org.apache.commons.lang3.tuple.Pair<BlockState, BlockPos> getOverlayBlock(Player p_110717_) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < 8; i++) {
            double d0 = p_110717_.getX() + ((i >> 0) % 2 - 0.5F) * p_110717_.getBbWidth() * 0.8F;
            double d1 = p_110717_.getEyeY() + ((i >> 1) % 2 - 0.5F) * 0.1F * p_110717_.getScale();
            double d2 = p_110717_.getZ() + ((i >> 2) % 2 - 0.5F) * p_110717_.getBbWidth() * 0.8F;
            blockpos$mutableblockpos.set(d0, d1, d2);
            BlockState blockstate = p_110717_.level().getBlockState(blockpos$mutableblockpos);
            if (blockstate.getRenderShape() != RenderShape.INVISIBLE && blockstate.isViewBlocking(p_110717_.level(), blockpos$mutableblockpos)) {
                return org.apache.commons.lang3.tuple.Pair.of(blockstate, blockpos$mutableblockpos.immutable());
            }
        }

        return null;
    }

    private static void renderTex(TextureAtlasSprite texture, PoseStack poseStack, MultiBufferSource bufferSource) {
        float f = 0.1F;
        int i = ARGB.colorFromFloat(1.0F, 0.1F, 0.1F, 0.1F);
        float f1 = -1.0F;
        float f2 = 1.0F;
        float f3 = -1.0F;
        float f4 = 1.0F;
        float f5 = -0.5F;
        float f6 = texture.getU0();
        float f7 = texture.getU1();
        float f8 = texture.getV0();
        float f9 = texture.getV1();
        Matrix4f matrix4f = poseStack.last().pose();
        VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.blockScreenEffect(texture.atlasLocation()));
        vertexconsumer.addVertex(matrix4f, -1.0F, -1.0F, -0.5F).setUv(f7, f9).setColor(i);
        vertexconsumer.addVertex(matrix4f, 1.0F, -1.0F, -0.5F).setUv(f6, f9).setColor(i);
        vertexconsumer.addVertex(matrix4f, 1.0F, 1.0F, -0.5F).setUv(f6, f8).setColor(i);
        vertexconsumer.addVertex(matrix4f, -1.0F, 1.0F, -0.5F).setUv(f7, f8).setColor(i);
    }

    private static void renderWater(Minecraft minecraft, PoseStack poseStack, MultiBufferSource bufferSource) {
        renderFluid(minecraft, poseStack, bufferSource, UNDERWATER_LOCATION);
    }

    public static void renderFluid(Minecraft p_110726_, PoseStack p_110727_, MultiBufferSource p_383128_, ResourceLocation texture) {
        BlockPos blockpos = BlockPos.containing(p_110726_.player.getX(), p_110726_.player.getEyeY(), p_110726_.player.getZ());
        float f = LightTexture.getBrightness(p_110726_.player.level().dimensionType(), p_110726_.player.level().getMaxLocalRawBrightness(blockpos));
        int i = ARGB.colorFromFloat(0.1F, f, f, f);
        float f1 = 4.0F;
        float f2 = -1.0F;
        float f3 = 1.0F;
        float f4 = -1.0F;
        float f5 = 1.0F;
        float f6 = -0.5F;
        float f7 = -p_110726_.player.getYRot() / 64.0F;
        float f8 = p_110726_.player.getXRot() / 64.0F;
        Matrix4f matrix4f = p_110727_.last().pose();
        VertexConsumer vertexconsumer = p_383128_.getBuffer(RenderType.blockScreenEffect(texture));
        vertexconsumer.addVertex(matrix4f, -1.0F, -1.0F, -0.5F).setUv(4.0F + f7, 4.0F + f8).setColor(i);
        vertexconsumer.addVertex(matrix4f, 1.0F, -1.0F, -0.5F).setUv(0.0F + f7, 4.0F + f8).setColor(i);
        vertexconsumer.addVertex(matrix4f, 1.0F, 1.0F, -0.5F).setUv(0.0F + f7, 0.0F + f8).setColor(i);
        vertexconsumer.addVertex(matrix4f, -1.0F, 1.0F, -0.5F).setUv(4.0F + f7, 0.0F + f8).setColor(i);
    }

    private static void renderFire(PoseStack poseStack, MultiBufferSource bufferSource, TextureAtlasSprite sprite) {
        VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.fireScreenEffect(sprite.atlasLocation()));
        float f = sprite.getU0();
        float f1 = sprite.getU1();
        float f2 = (f + f1) / 2.0F;
        float f3 = sprite.getV0();
        float f4 = sprite.getV1();
        float f5 = (f3 + f4) / 2.0F;
        float f6 = sprite.uvShrinkRatio();
        float f7 = Mth.lerp(f6, f, f2);
        float f8 = Mth.lerp(f6, f1, f2);
        float f9 = Mth.lerp(f6, f3, f5);
        float f10 = Mth.lerp(f6, f4, f5);
        float f11 = 1.0F;

        for (int i = 0; i < 2; i++) {
            poseStack.pushPose();
            float f12 = -0.5F;
            float f13 = 0.5F;
            float f14 = -0.5F;
            float f15 = 0.5F;
            float f16 = -0.5F;
            poseStack.translate(-(i * 2 - 1) * 0.24F, -0.3F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees((i * 2 - 1) * 10.0F));
            Matrix4f matrix4f = poseStack.last().pose();
            vertexconsumer.addVertex(matrix4f, -0.5F, -0.5F, -0.5F).setUv(f8, f10).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            vertexconsumer.addVertex(matrix4f, 0.5F, -0.5F, -0.5F).setUv(f7, f10).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            vertexconsumer.addVertex(matrix4f, 0.5F, 0.5F, -0.5F).setUv(f7, f9).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            vertexconsumer.addVertex(matrix4f, -0.5F, 0.5F, -0.5F).setUv(f8, f9).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            poseStack.popPose();
        }
    }
}
