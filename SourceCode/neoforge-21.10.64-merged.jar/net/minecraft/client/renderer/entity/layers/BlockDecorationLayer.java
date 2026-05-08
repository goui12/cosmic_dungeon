package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockDecorationLayer<S extends EntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    private final Function<S, Optional<BlockState>> blockState;
    private final Consumer<PoseStack> transform;

    public BlockDecorationLayer(RenderLayerParent<S, M> renderer, Function<S, Optional<BlockState>> blockState, Consumer<PoseStack> transform) {
        super(renderer);
        this.blockState = blockState;
        this.transform = transform;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, S renderState, float yRot, float xRot) {
        Optional<BlockState> optional = this.blockState.apply(renderState);
        if (!optional.isEmpty()) {
            BlockState blockstate = optional.get();
            Block block = blockstate.getBlock();
            boolean flag = block instanceof CopperGolemStatueBlock;
            poseStack.pushPose();
            this.transform.accept(poseStack);
            if (!flag) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            }

            if (flag || block instanceof AbstractSkullBlock || block instanceof AbstractBannerBlock || block instanceof AbstractChestBlock) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }

            if (block instanceof FlowerBedBlock) {
                poseStack.translate(-0.25, -1.5, -0.25);
            } else if (!flag) {
                poseStack.translate(-0.5, -1.5, -0.5);
            } else {
                poseStack.translate(-0.5, 0.0, -0.5);
            }

            nodeCollector.submitBlock(poseStack, blockstate, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
            poseStack.popPose();
        }
    }
}
