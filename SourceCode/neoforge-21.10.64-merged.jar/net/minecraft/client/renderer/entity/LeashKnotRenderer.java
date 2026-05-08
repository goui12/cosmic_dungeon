package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.LeashKnotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LeashKnotRenderer extends EntityRenderer<LeashFenceKnotEntity, EntityRenderState> {
    private static final ResourceLocation KNOT_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/lead_knot.png");
    private final LeashKnotModel model;

    public LeashKnotRenderer(EntityRendererProvider.Context p_174284_) {
        super(p_174284_);
        this.model = new LeashKnotModel(p_174284_.bakeLayer(ModelLayers.LEASH_KNOT));
    }

    @Override
    public void submit(EntityRenderState p_432885_, PoseStack p_435997_, SubmitNodeCollector p_434621_, CameraRenderState p_451377_) {
        p_435997_.pushPose();
        p_435997_.scale(-1.0F, -1.0F, 1.0F);
        p_434621_.submitModel(
            this.model,
            p_432885_,
            p_435997_,
            this.model.renderType(KNOT_LOCATION),
            p_432885_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            p_432885_.outlineColor,
            null
        );
        p_435997_.popPose();
        super.submit(p_432885_, p_435997_, p_434621_, p_451377_);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
