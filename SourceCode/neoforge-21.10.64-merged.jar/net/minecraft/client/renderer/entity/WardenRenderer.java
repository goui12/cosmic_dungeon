package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.WardenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.WardenRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WardenRenderer extends MobRenderer<Warden, WardenRenderState, WardenModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/warden/warden.png");
    private static final ResourceLocation BIOLUMINESCENT_LAYER_TEXTURE = ResourceLocation.withDefaultNamespace(
        "textures/entity/warden/warden_bioluminescent_layer.png"
    );
    private static final ResourceLocation HEART_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/warden/warden_heart.png");
    private static final ResourceLocation PULSATING_SPOTS_TEXTURE_1 = ResourceLocation.withDefaultNamespace(
        "textures/entity/warden/warden_pulsating_spots_1.png"
    );
    private static final ResourceLocation PULSATING_SPOTS_TEXTURE_2 = ResourceLocation.withDefaultNamespace(
        "textures/entity/warden/warden_pulsating_spots_2.png"
    );

    public WardenRenderer(EntityRendererProvider.Context p_234787_) {
        super(p_234787_, new WardenModel(p_234787_.bakeLayer(ModelLayers.WARDEN)), 0.9F);
        WardenModel wardenmodel = new WardenModel(p_234787_.bakeLayer(ModelLayers.WARDEN_BIOLUMINESCENT));
        WardenModel wardenmodel1 = new WardenModel(p_234787_.bakeLayer(ModelLayers.WARDEN_PULSATING_SPOTS));
        WardenModel wardenmodel2 = new WardenModel(p_234787_.bakeLayer(ModelLayers.WARDEN_TENDRILS));
        WardenModel wardenmodel3 = new WardenModel(p_234787_.bakeLayer(ModelLayers.WARDEN_HEART));
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this, p_432300_ -> BIOLUMINESCENT_LAYER_TEXTURE, (p_360647_, p_234810_) -> 1.0F, wardenmodel, RenderType::entityTranslucentEmissive, false
            )
        );
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this,
                p_432297_ -> PULSATING_SPOTS_TEXTURE_1,
                (p_360462_, p_234806_) -> Math.max(0.0F, Mth.cos(p_234806_ * 0.045F) * 0.25F),
                wardenmodel1,
                RenderType::entityTranslucentEmissive,
                false
            )
        );
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this,
                p_432296_ -> PULSATING_SPOTS_TEXTURE_2,
                (p_364045_, p_234802_) -> Math.max(0.0F, Mth.cos(p_234802_ * 0.045F + (float) Math.PI) * 0.25F),
                wardenmodel1,
                RenderType::entityTranslucentEmissive,
                false
            )
        );
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this, p_432299_ -> TEXTURE, (p_359288_, p_359289_) -> p_359288_.tendrilAnimation, wardenmodel2, RenderType::entityTranslucentEmissive, false
            )
        );
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this,
                p_432298_ -> HEART_TEXTURE,
                (p_359290_, p_359291_) -> p_359290_.heartAnimation,
                wardenmodel3,
                RenderType::entityTranslucentEmissive,
                false
            )
        );
    }

    public ResourceLocation getTextureLocation(WardenRenderState renderState) {
        return TEXTURE;
    }

    public WardenRenderState createRenderState() {
        return new WardenRenderState();
    }

    public void extractRenderState(Warden entity, WardenRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.tendrilAnimation = entity.getTendrilAnimation(partialTick);
        reusedState.heartAnimation = entity.getHeartAnimation(partialTick);
        reusedState.roarAnimationState.copyFrom(entity.roarAnimationState);
        reusedState.sniffAnimationState.copyFrom(entity.sniffAnimationState);
        reusedState.emergeAnimationState.copyFrom(entity.emergeAnimationState);
        reusedState.diggingAnimationState.copyFrom(entity.diggingAnimationState);
        reusedState.attackAnimationState.copyFrom(entity.attackAnimationState);
        reusedState.sonicBoomAnimationState.copyFrom(entity.sonicBoomAnimationState);
    }
}
