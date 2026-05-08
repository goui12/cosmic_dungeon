package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.TurtleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.TurtleRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Turtle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TurtleRenderer extends AgeableMobRenderer<Turtle, TurtleRenderState, TurtleModel> {
    private static final ResourceLocation TURTLE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/turtle/big_sea_turtle.png");

    public TurtleRenderer(EntityRendererProvider.Context p_174430_) {
        super(p_174430_, new TurtleModel(p_174430_.bakeLayer(ModelLayers.TURTLE)), new TurtleModel(p_174430_.bakeLayer(ModelLayers.TURTLE_BABY)), 0.7F);
    }

    protected float getShadowRadius(TurtleRenderState p_364807_) {
        float f = super.getShadowRadius(p_364807_);
        return p_364807_.isBaby ? f * 0.83F : f;
    }

    public TurtleRenderState createRenderState() {
        return new TurtleRenderState();
    }

    public void extractRenderState(Turtle p_364982_, TurtleRenderState p_362479_, float p_360282_) {
        super.extractRenderState(p_364982_, p_362479_, p_360282_);
        p_362479_.isOnLand = !p_364982_.isInWater() && p_364982_.onGround();
        p_362479_.isLayingEgg = p_364982_.isLayingEgg();
        p_362479_.hasEgg = !p_364982_.isBaby() && p_364982_.hasEgg();
    }

    public ResourceLocation getTextureLocation(TurtleRenderState p_362874_) {
        return TURTLE_LOCATION;
    }
}
