package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractZombieRenderer<T extends Zombie, S extends ZombieRenderState, M extends ZombieModel<S>> extends HumanoidMobRenderer<T, S, M> {
    private static final ResourceLocation ZOMBIE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    protected AbstractZombieRenderer(EntityRendererProvider.Context context, M adultModel, M babyModel, ArmorModelSet<M> adultArmorModels, ArmorModelSet<M> babyArmorModels) {
        super(context, adultModel, babyModel, 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this, adultArmorModels, babyArmorModels, context.getEquipmentRenderer()));
    }

    public ResourceLocation getTextureLocation(S renderState) {
        return ZOMBIE_LOCATION;
    }

    public void extractRenderState(T entity, S reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.isAggressive = entity.isAggressive();
        reusedState.isConverting = entity.isUnderWaterConverting();
    }

    protected boolean isShaking(S renderState) {
        return super.isShaking(renderState) || renderState.isConverting;
    }
}
