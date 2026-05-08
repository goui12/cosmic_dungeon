package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.SalmonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.SalmonRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Salmon;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SalmonRenderer extends MobRenderer<Salmon, SalmonRenderState, SalmonModel> {
    private static final ResourceLocation SALMON_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/fish/salmon.png");
    private final SalmonModel smallSalmonModel;
    private final SalmonModel mediumSalmonModel;
    private final SalmonModel largeSalmonModel;

    public SalmonRenderer(EntityRendererProvider.Context p_174364_) {
        super(p_174364_, new SalmonModel(p_174364_.bakeLayer(ModelLayers.SALMON)), 0.4F);
        this.smallSalmonModel = new SalmonModel(p_174364_.bakeLayer(ModelLayers.SALMON_SMALL));
        this.mediumSalmonModel = new SalmonModel(p_174364_.bakeLayer(ModelLayers.SALMON));
        this.largeSalmonModel = new SalmonModel(p_174364_.bakeLayer(ModelLayers.SALMON_LARGE));
    }

    public void extractRenderState(Salmon p_362010_, SalmonRenderState p_363993_, float p_363648_) {
        super.extractRenderState(p_362010_, p_363993_, p_363648_);
        p_363993_.variant = p_362010_.getVariant();
    }

    public ResourceLocation getTextureLocation(SalmonRenderState p_365406_) {
        return SALMON_LOCATION;
    }

    public SalmonRenderState createRenderState() {
        return new SalmonRenderState();
    }

    protected void setupRotations(SalmonRenderState p_365150_, PoseStack p_115829_, float p_115830_, float p_115831_) {
        super.setupRotations(p_365150_, p_115829_, p_115830_, p_115831_);
        float f = 1.0F;
        float f1 = 1.0F;
        if (!p_365150_.isInWater) {
            f = 1.3F;
            f1 = 1.7F;
        }

        float f2 = f * 4.3F * Mth.sin(f1 * 0.6F * p_365150_.ageInTicks);
        p_115829_.mulPose(Axis.YP.rotationDegrees(f2));
        if (!p_365150_.isInWater) {
            p_115829_.translate(0.2F, 0.1F, 0.0F);
            p_115829_.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }

    public void submit(SalmonRenderState p_451525_, PoseStack p_435355_, SubmitNodeCollector p_434450_, CameraRenderState p_451204_) {
        this.model = switch (p_451525_.variant) {
            case SMALL -> this.smallSalmonModel;
            case MEDIUM -> this.mediumSalmonModel;
            case LARGE -> this.largeSalmonModel;
        };
        super.submit(p_451525_, p_435355_, p_434450_, p_451204_);
    }
}
