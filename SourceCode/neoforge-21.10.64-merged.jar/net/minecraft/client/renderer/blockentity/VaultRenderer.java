package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.VaultRenderState;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultClientData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VaultRenderer implements BlockEntityRenderer<VaultBlockEntity, VaultRenderState> {
    private final ItemModelResolver itemModelResolver;
    private final RandomSource random = RandomSource.create();

    public VaultRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public VaultRenderState createRenderState() {
        return new VaultRenderState();
    }

    public void extractRenderState(
        VaultBlockEntity blockEntity, VaultRenderState renderState, float partialTick, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        ItemStack itemstack = blockEntity.getSharedData().getDisplayItem();
        if (VaultBlockEntity.Client.shouldDisplayActiveEffects(blockEntity.getSharedData()) && !itemstack.isEmpty() && blockEntity.getLevel() != null) {
            renderState.displayItem = new ItemClusterRenderState();
            this.itemModelResolver.updateForTopItem(renderState.displayItem.item, itemstack, ItemDisplayContext.GROUND, blockEntity.getLevel(), null, 0);
            renderState.displayItem.count = ItemClusterRenderState.getRenderedAmount(itemstack.getCount());
            renderState.displayItem.seed = ItemClusterRenderState.getSeedForItemStack(itemstack);
            VaultClientData vaultclientdata = blockEntity.getClientData();
            renderState.spin = Mth.rotLerp(partialTick, vaultclientdata.previousSpin(), vaultclientdata.currentSpin());
        }
    }

    public void submit(VaultRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.displayItem != null) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.4F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(renderState.spin));
            ItemEntityRenderer.renderMultipleFromCount(poseStack, nodeCollector, renderState.lightCoords, renderState.displayItem, this.random);
            poseStack.popPose();
        }
    }
}
