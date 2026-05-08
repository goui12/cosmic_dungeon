package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PufferfishBigModel;
import net.minecraft.client.model.PufferfishMidModel;
import net.minecraft.client.model.PufferfishSmallModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.PufferfishRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Pufferfish;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PufferfishRenderer extends MobRenderer<Pufferfish, PufferfishRenderState, EntityModel<EntityRenderState>> {
    private static final ResourceLocation PUFFER_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png");
    private final EntityModel<EntityRenderState> small;
    private final EntityModel<EntityRenderState> mid;
    private final EntityModel<EntityRenderState> big = this.getModel();

    public PufferfishRenderer(EntityRendererProvider.Context p_174358_) {
        super(p_174358_, new PufferfishBigModel(p_174358_.bakeLayer(ModelLayers.PUFFERFISH_BIG)), 0.2F);
        this.mid = new PufferfishMidModel(p_174358_.bakeLayer(ModelLayers.PUFFERFISH_MEDIUM));
        this.small = new PufferfishSmallModel(p_174358_.bakeLayer(ModelLayers.PUFFERFISH_SMALL));
    }

    public ResourceLocation getTextureLocation(PufferfishRenderState p_363552_) {
        return PUFFER_LOCATION;
    }

    public PufferfishRenderState createRenderState() {
        return new PufferfishRenderState();
    }

    protected float getShadowRadius(PufferfishRenderState p_382918_) {
        return 0.1F + 0.1F * p_382918_.puffState;
    }

    public void submit(PufferfishRenderState p_433818_, PoseStack p_434209_, SubmitNodeCollector p_435212_, CameraRenderState p_451103_) {
        this.model = switch (p_433818_.puffState) {
            case 0 -> this.small;
            case 1 -> this.mid;
            default -> this.big;
        };
        super.submit(p_433818_, p_434209_, p_435212_, p_451103_);
    }

    public void extractRenderState(Pufferfish p_362491_, PufferfishRenderState p_362905_, float p_360796_) {
        super.extractRenderState(p_362491_, p_362905_, p_360796_);
        p_362905_.puffState = p_362491_.getPuffState();
    }

    protected void setupRotations(PufferfishRenderState p_362740_, PoseStack p_115763_, float p_115764_, float p_115765_) {
        p_115763_.translate(0.0F, Mth.cos(p_362740_.ageInTicks * 0.05F) * 0.08F, 0.0F);
        super.setupRotations(p_362740_, p_115763_, p_115764_, p_115765_);
    }
}
