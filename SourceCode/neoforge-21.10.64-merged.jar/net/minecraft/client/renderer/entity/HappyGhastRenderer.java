package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HappyGhastHarnessModel;
import net.minecraft.client.model.HappyGhastModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.RopesLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.HappyGhast;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HappyGhastRenderer extends AgeableMobRenderer<HappyGhast, HappyGhastRenderState, HappyGhastModel> {
    private static final ResourceLocation GHAST_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/ghast/happy_ghast.png");
    private static final ResourceLocation GHAST_BABY_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/ghast/happy_ghast_baby.png");
    private static final ResourceLocation GHAST_ROPES = ResourceLocation.withDefaultNamespace("textures/entity/ghast/happy_ghast_ropes.png");

    public HappyGhastRenderer(EntityRendererProvider.Context p_416163_) {
        super(
            p_416163_,
            new HappyGhastModel(p_416163_.bakeLayer(ModelLayers.HAPPY_GHAST)),
            new HappyGhastModel(p_416163_.bakeLayer(ModelLayers.HAPPY_GHAST_BABY)),
            2.0F
        );
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                p_416163_.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.HAPPY_GHAST_BODY,
                p_416172_ -> p_416172_.bodyItem,
                new HappyGhastHarnessModel(p_416163_.bakeLayer(ModelLayers.HAPPY_GHAST_HARNESS)),
                new HappyGhastHarnessModel(p_416163_.bakeLayer(ModelLayers.HAPPY_GHAST_BABY_HARNESS))
            )
        );
        this.addLayer(new RopesLayer<>(this, p_416163_.getModelSet(), GHAST_ROPES));
    }

    public ResourceLocation getTextureLocation(HappyGhastRenderState p_415985_) {
        return p_415985_.isBaby ? GHAST_BABY_LOCATION : GHAST_LOCATION;
    }

    public HappyGhastRenderState createRenderState() {
        return new HappyGhastRenderState();
    }

    protected AABB getBoundingBoxForCulling(HappyGhast p_416147_) {
        AABB aabb = super.getBoundingBoxForCulling(p_416147_);
        float f = p_416147_.getBbHeight();
        return aabb.setMinY(aabb.minY - f / 2.0F);
    }

    public void extractRenderState(HappyGhast p_415945_, HappyGhastRenderState p_415588_, float p_415709_) {
        super.extractRenderState(p_415945_, p_415588_, p_415709_);
        p_415588_.bodyItem = p_415945_.getItemBySlot(EquipmentSlot.BODY).copy();
        p_415588_.isRidden = p_415945_.isVehicle();
        p_415588_.isLeashHolder = p_415945_.isLeashHolder();
    }
}
