package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.CreakingModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.CreakingRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreakingRenderer<T extends Creaking> extends MobRenderer<T, CreakingRenderState, CreakingModel> {
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/creaking/creaking.png");
    private static final ResourceLocation EYES_TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/creaking/creaking_eyes.png");

    public CreakingRenderer(EntityRendererProvider.Context p_379341_) {
        super(p_379341_, new CreakingModel(p_379341_.bakeLayer(ModelLayers.CREAKING)), 0.6F);
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this,
                p_432292_ -> EYES_TEXTURE_LOCATION,
                (p_432290_, p_432291_) -> p_432290_.eyesGlowing ? 1.0F : 0.0F,
                new CreakingModel(p_379341_.bakeLayer(ModelLayers.CREAKING_EYES)),
                RenderType::eyes,
                true
            )
        );
    }

    public ResourceLocation getTextureLocation(CreakingRenderState p_379686_) {
        return TEXTURE_LOCATION;
    }

    public CreakingRenderState createRenderState() {
        return new CreakingRenderState();
    }

    public void extractRenderState(T p_379591_, CreakingRenderState p_380210_, float p_379411_) {
        super.extractRenderState(p_379591_, p_380210_, p_379411_);
        p_380210_.attackAnimationState.copyFrom(p_379591_.attackAnimationState);
        p_380210_.invulnerabilityAnimationState.copyFrom(p_379591_.invulnerabilityAnimationState);
        p_380210_.deathAnimationState.copyFrom(p_379591_.deathAnimationState);
        if (p_379591_.isTearingDown()) {
            p_380210_.deathTime = 0.0F;
            p_380210_.hasRedOverlay = false;
            p_380210_.eyesGlowing = p_379591_.hasGlowingEyes();
        } else {
            p_380210_.eyesGlowing = p_379591_.isActive();
        }

        p_380210_.canMove = p_379591_.canMove();
    }
}
