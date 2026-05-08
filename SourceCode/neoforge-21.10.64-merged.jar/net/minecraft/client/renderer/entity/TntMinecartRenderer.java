package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.MinecartTntRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TntMinecartRenderer extends AbstractMinecartRenderer<MinecartTNT, MinecartTntRenderState> {
    public TntMinecartRenderer(EntityRendererProvider.Context p_174424_) {
        super(p_174424_, ModelLayers.TNT_MINECART);
    }

    protected void submitMinecartContents(
        MinecartTntRenderState renderState, BlockState blockState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight
    ) {
        float f = renderState.fuseRemainingInTicks;
        if (f > -1.0F && f < 10.0F) {
            float f1 = 1.0F - f / 10.0F;
            f1 = Mth.clamp(f1, 0.0F, 1.0F);
            f1 *= f1;
            f1 *= f1;
            float f2 = 1.0F + f1 * 0.3F;
            poseStack.scale(f2, f2, f2);
        }

        submitWhiteSolidBlock(blockState, poseStack, nodeCollector, packedLight, f > -1.0F && (int)f / 5 % 2 == 0, renderState.outlineColor);
    }

    public static void submitWhiteSolidBlock(
        BlockState blockState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, boolean flash, int outlineColor
    ) {
        int i;
        if (flash) {
            i = OverlayTexture.pack(OverlayTexture.u(1.0F), 10);
        } else {
            i = OverlayTexture.NO_OVERLAY;
        }

        nodeCollector.submitBlock(poseStack, blockState, packedLight, i, outlineColor);
    }

    public MinecartTntRenderState createRenderState() {
        return new MinecartTntRenderState();
    }

    public void extractRenderState(MinecartTNT entity, MinecartTntRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.fuseRemainingInTicks = entity.getFuse() > -1 ? entity.getFuse() - partialTick + 1.0F : -1.0F;
    }
}
