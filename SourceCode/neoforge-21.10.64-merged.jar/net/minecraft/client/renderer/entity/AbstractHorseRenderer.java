package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractHorseRenderer<T extends AbstractHorse, S extends EquineRenderState, M extends EntityModel<? super S>>
    extends AgeableMobRenderer<T, S, M> {
    public AbstractHorseRenderer(EntityRendererProvider.Context context, M adultModel, M babyModel) {
        super(context, adultModel, babyModel, 0.75F);
    }

    public void extractRenderState(T entity, S reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.saddle = entity.getItemBySlot(EquipmentSlot.SADDLE).copy();
        reusedState.isRidden = entity.isVehicle();
        reusedState.eatAnimation = entity.getEatAnim(partialTick);
        reusedState.standAnimation = entity.getStandAnim(partialTick);
        reusedState.feedingAnimation = entity.getMouthAnim(partialTick);
        reusedState.animateTail = entity.tailCounter > 0;
    }
}
