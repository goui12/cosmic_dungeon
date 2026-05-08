package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ThrownTridentRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ThrownTridentRenderer extends EntityRenderer<ThrownTrident, ThrownTridentRenderState> {
    public static final ResourceLocation TRIDENT_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/trident.png");
    private final TridentModel model;

    public ThrownTridentRenderer(EntityRendererProvider.Context p_174420_) {
        super(p_174420_);
        this.model = new TridentModel(p_174420_.bakeLayer(ModelLayers.TRIDENT));
    }

    public void submit(ThrownTridentRenderState p_434752_, PoseStack p_435381_, SubmitNodeCollector p_435880_, CameraRenderState p_451376_) {
        p_435381_.pushPose();
        p_435381_.mulPose(Axis.YP.rotationDegrees(p_434752_.yRot - 90.0F));
        p_435381_.mulPose(Axis.ZP.rotationDegrees(p_434752_.xRot + 90.0F));
        List<RenderType> list = ItemRenderer.getFoilRenderTypes(this.model.renderType(TRIDENT_LOCATION), false, p_434752_.isFoil);

        for (int i = 0; i < list.size(); i++) {
            p_435880_.order(i)
                .submitModel(
                    this.model, Unit.INSTANCE, p_435381_, list.get(i), p_434752_.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, p_434752_.outlineColor, null
                );
        }

        p_435381_.popPose();
        super.submit(p_434752_, p_435381_, p_435880_, p_451376_);
    }

    public ThrownTridentRenderState createRenderState() {
        return new ThrownTridentRenderState();
    }

    public void extractRenderState(ThrownTrident p_362162_, ThrownTridentRenderState p_360843_, float p_361066_) {
        super.extractRenderState(p_362162_, p_360843_, p_361066_);
        p_360843_.yRot = p_362162_.getYRot(p_361066_);
        p_360843_.xRot = p_362162_.getXRot(p_361066_);
        p_360843_.isFoil = p_362162_.isFoil();
    }
}
