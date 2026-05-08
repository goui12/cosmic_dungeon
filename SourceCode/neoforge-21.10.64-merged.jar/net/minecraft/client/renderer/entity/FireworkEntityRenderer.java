package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.FireworkRocketRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireworkEntityRenderer extends EntityRenderer<FireworkRocketEntity, FireworkRocketRenderState> {
    private final ItemModelResolver itemModelResolver;

    public FireworkEntityRenderer(EntityRendererProvider.Context p_174114_) {
        super(p_174114_);
        this.itemModelResolver = p_174114_.getItemModelResolver();
    }

    public void submit(FireworkRocketRenderState p_451097_, PoseStack p_434074_, SubmitNodeCollector p_433623_, CameraRenderState p_451019_) {
        p_434074_.pushPose();
        p_434074_.mulPose(p_451019_.orientation);
        if (p_451097_.isShotAtAngle) {
            p_434074_.mulPose(Axis.ZP.rotationDegrees(180.0F));
            p_434074_.mulPose(Axis.YP.rotationDegrees(180.0F));
            p_434074_.mulPose(Axis.XP.rotationDegrees(90.0F));
        }

        p_451097_.item.submit(p_434074_, p_433623_, p_451097_.lightCoords, OverlayTexture.NO_OVERLAY, p_451097_.outlineColor);
        p_434074_.popPose();
        super.submit(p_451097_, p_434074_, p_433623_, p_451019_);
    }

    public FireworkRocketRenderState createRenderState() {
        return new FireworkRocketRenderState();
    }

    public void extractRenderState(FireworkRocketEntity p_363409_, FireworkRocketRenderState p_360980_, float p_365252_) {
        super.extractRenderState(p_363409_, p_360980_, p_365252_);
        p_360980_.isShotAtAngle = p_363409_.isShotAtAngle();
        this.itemModelResolver.updateForNonLiving(p_360980_.item, p_363409_.getItem(), ItemDisplayContext.GROUND, p_363409_);
    }
}
