package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.ZombieVillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombieVillagerRenderer extends HumanoidMobRenderer<ZombieVillager, ZombieVillagerRenderState, ZombieVillagerModel<ZombieVillagerRenderState>> {
    private static final ResourceLocation ZOMBIE_VILLAGER_LOCATION = ResourceLocation.withDefaultNamespace(
        "textures/entity/zombie_villager/zombie_villager.png"
    );

    public ZombieVillagerRenderer(EntityRendererProvider.Context p_174463_) {
        super(
            p_174463_,
            new ZombieVillagerModel<>(p_174463_.bakeLayer(ModelLayers.ZOMBIE_VILLAGER)),
            new ZombieVillagerModel<>(p_174463_.bakeLayer(ModelLayers.ZOMBIE_VILLAGER_BABY)),
            0.5F,
            VillagerRenderer.CUSTOM_HEAD_TRANSFORMS
        );
        this.addLayer(
            new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(ModelLayers.ZOMBIE_VILLAGER_ARMOR, p_174463_.getModelSet(), ZombieVillagerModel::new),
                ArmorModelSet.bake(ModelLayers.ZOMBIE_VILLAGER_BABY_ARMOR, p_174463_.getModelSet(), ZombieVillagerModel::new),
                p_174463_.getEquipmentRenderer()
            )
        );
        this.addLayer(
            new VillagerProfessionLayer<>(
                this,
                p_174463_.getResourceManager(),
                "zombie_villager",
                new ZombieVillagerModel<>(p_174463_.bakeLayer(ModelLayers.ZOMBIE_VILLAGER_NO_HAT)),
                new ZombieVillagerModel<>(p_174463_.bakeLayer(ModelLayers.ZOMBIE_VILLAGER_BABY_NO_HAT))
            )
        );
    }

    public ResourceLocation getTextureLocation(ZombieVillagerRenderState p_363256_) {
        return ZOMBIE_VILLAGER_LOCATION;
    }

    public ZombieVillagerRenderState createRenderState() {
        return new ZombieVillagerRenderState();
    }

    public void extractRenderState(ZombieVillager p_364737_, ZombieVillagerRenderState p_363888_, float p_360951_) {
        super.extractRenderState(p_364737_, p_363888_, p_360951_);
        p_363888_.isConverting = p_364737_.isConverting();
        p_363888_.villagerData = p_364737_.getVillagerData();
        p_363888_.isAggressive = p_364737_.isAggressive();
    }

    protected boolean isShaking(ZombieVillagerRenderState p_361391_) {
        return super.isShaking(p_361391_) || p_361391_.isConverting;
    }
}
