package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.IronGolemCrackinessLayer;
import net.minecraft.client.renderer.entity.layers.IronGolemFlowerLayer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IronGolemRenderer extends MobRenderer<IronGolem, IronGolemRenderState, IronGolemModel> {
    private static final ResourceLocation GOLEM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png");

    public IronGolemRenderer(EntityRendererProvider.Context p_174188_) {
        super(p_174188_, new IronGolemModel(p_174188_.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7F);
        this.addLayer(new IronGolemCrackinessLayer(this));
        this.addLayer(new IronGolemFlowerLayer(this, p_174188_.getBlockRenderDispatcher()));
    }

    public ResourceLocation getTextureLocation(IronGolemRenderState p_362083_) {
        return GOLEM_LOCATION;
    }

    public IronGolemRenderState createRenderState() {
        return new IronGolemRenderState();
    }

    public void extractRenderState(IronGolem p_364735_, IronGolemRenderState p_365108_, float p_365449_) {
        super.extractRenderState(p_364735_, p_365108_, p_365449_);
        p_365108_.attackTicksRemaining = p_364735_.getAttackAnimationTick() > 0.0F ? p_364735_.getAttackAnimationTick() - p_365449_ : 0.0F;
        p_365108_.offerFlowerTick = p_364735_.getOfferFlowerTick();
        p_365108_.crackiness = p_364735_.getCrackiness();
    }

    protected void setupRotations(IronGolemRenderState p_360361_, PoseStack p_115015_, float p_115016_, float p_115017_) {
        super.setupRotations(p_360361_, p_115015_, p_115016_, p_115017_);
        if (!(p_360361_.walkAnimationSpeed < 0.01)) {
            float f = 13.0F;
            float f1 = p_360361_.walkAnimationPos + 6.0F;
            float f2 = (Math.abs(f1 % 13.0F - 6.5F) - 3.25F) / 3.25F;
            p_115015_.mulPose(Axis.ZP.rotationDegrees(6.5F * f2));
        }
    }
}
