package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombieRenderer extends AbstractZombieRenderer<Zombie, ZombieRenderState, ZombieModel<ZombieRenderState>> {
    public ZombieRenderer(EntityRendererProvider.Context p_174456_) {
        this(p_174456_, ModelLayers.ZOMBIE, ModelLayers.ZOMBIE_BABY, ModelLayers.ZOMBIE_ARMOR, ModelLayers.ZOMBIE_BABY_ARMOR);
    }

    public ZombieRenderState createRenderState() {
        return new ZombieRenderState();
    }

    public ZombieRenderer(
        EntityRendererProvider.Context context,
        ModelLayerLocation modelLayer,
        ModelLayerLocation babyModelLayer,
        ArmorModelSet<ModelLayerLocation> armorModelSet,
        ArmorModelSet<ModelLayerLocation> babyArmorModelSet
    ) {
        super(
            context,
            new ZombieModel<>(context.bakeLayer(modelLayer)),
            new ZombieModel<>(context.bakeLayer(babyModelLayer)),
            ArmorModelSet.bake(armorModelSet, context.getModelSet(), ZombieModel::new),
            ArmorModelSet.bake(babyArmorModelSet, context.getModelSet(), ZombieModel::new)
        );
    }
}
