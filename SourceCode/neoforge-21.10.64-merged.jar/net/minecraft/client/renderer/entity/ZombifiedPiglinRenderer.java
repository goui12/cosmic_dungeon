package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.ZombifiedPiglinModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ZombifiedPiglinRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombifiedPiglinRenderer extends HumanoidMobRenderer<ZombifiedPiglin, ZombifiedPiglinRenderState, ZombifiedPiglinModel> {
    private static final ResourceLocation ZOMBIFIED_PIGLIN_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/piglin/zombified_piglin.png");

    public ZombifiedPiglinRenderer(
        EntityRendererProvider.Context context,
        ModelLayerLocation modelLayer,
        ModelLayerLocation babyModelLayer,
        ArmorModelSet<ModelLayerLocation> armorModelSet,
        ArmorModelSet<ModelLayerLocation> babyArmorModelSet
    ) {
        super(
            context,
            new ZombifiedPiglinModel(context.bakeLayer(modelLayer)),
            new ZombifiedPiglinModel(context.bakeLayer(babyModelLayer)),
            0.5F,
            PiglinRenderer.PIGLIN_CUSTOM_HEAD_TRANSFORMS
        );
        this.addLayer(
            new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(armorModelSet, context.getModelSet(), ZombifiedPiglinModel::new),
                ArmorModelSet.bake(babyArmorModelSet, context.getModelSet(), ZombifiedPiglinModel::new),
                context.getEquipmentRenderer()
            )
        );
    }

    public ResourceLocation getTextureLocation(ZombifiedPiglinRenderState renderState) {
        return ZOMBIFIED_PIGLIN_LOCATION;
    }

    public ZombifiedPiglinRenderState createRenderState() {
        return new ZombifiedPiglinRenderState();
    }

    public void extractRenderState(ZombifiedPiglin entity, ZombifiedPiglinRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.isAggressive = entity.isAggressive();
    }
}
