package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HoglinModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.state.HoglinRenderState;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractHoglinRenderer<T extends Mob & HoglinBase> extends AgeableMobRenderer<T, HoglinRenderState, HoglinModel> {
    public AbstractHoglinRenderer(EntityRendererProvider.Context context, ModelLayerLocation adultModel, ModelLayerLocation babyModel, float shadowRadius) {
        super(context, new HoglinModel(context.bakeLayer(adultModel)), new HoglinModel(context.bakeLayer(babyModel)), shadowRadius);
    }

    public HoglinRenderState createRenderState() {
        return new HoglinRenderState();
    }

    public void extractRenderState(T entity, HoglinRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.attackAnimationRemainingTicks = entity.getAttackAnimationRemainingTicks();
    }
}
