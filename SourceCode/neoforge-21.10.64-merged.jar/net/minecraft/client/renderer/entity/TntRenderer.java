package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.TntRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.PrimedTnt;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TntRenderer extends EntityRenderer<PrimedTnt, TntRenderState> {
    public TntRenderer(EntityRendererProvider.Context p_174426_) {
        super(p_174426_);
        this.shadowRadius = 0.5F;
    }

    public void submit(TntRenderState p_451361_, PoseStack p_432916_, SubmitNodeCollector p_434469_, CameraRenderState p_451505_) {
        p_432916_.pushPose();
        p_432916_.translate(0.0F, 0.5F, 0.0F);
        float f = p_451361_.fuseRemainingInTicks;
        if (p_451361_.fuseRemainingInTicks < 10.0F) {
            float f1 = 1.0F - p_451361_.fuseRemainingInTicks / 10.0F;
            f1 = Mth.clamp(f1, 0.0F, 1.0F);
            f1 *= f1;
            f1 *= f1;
            float f2 = 1.0F + f1 * 0.3F;
            p_432916_.scale(f2, f2, f2);
        }

        p_432916_.mulPose(Axis.YP.rotationDegrees(-90.0F));
        p_432916_.translate(-0.5F, -0.5F, 0.5F);
        p_432916_.mulPose(Axis.YP.rotationDegrees(90.0F));
        if (p_451361_.blockState != null) {
            TntMinecartRenderer.submitWhiteSolidBlock(
                p_451361_.blockState, p_432916_, p_434469_, p_451361_.lightCoords, (int)f / 5 % 2 == 0, p_451361_.outlineColor
            );
        }

        p_432916_.popPose();
        super.submit(p_451361_, p_432916_, p_434469_, p_451505_);
    }

    public TntRenderState createRenderState() {
        return new TntRenderState();
    }

    public void extractRenderState(PrimedTnt p_361380_, TntRenderState p_364625_, float p_360472_) {
        super.extractRenderState(p_361380_, p_364625_, p_360472_);
        p_364625_.fuseRemainingInTicks = p_361380_.getFuse() - p_360472_ + 1.0F;
        p_364625_.blockState = p_361380_.getBlockState();
    }
}
