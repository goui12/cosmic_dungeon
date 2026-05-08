package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WolfArmorLayer extends RenderLayer<WolfRenderState, WolfModel> {
    private final WolfModel adultModel;
    private final WolfModel babyModel;
    private final EquipmentLayerRenderer equipmentRenderer;
    private static final Map<Crackiness.Level, ResourceLocation> ARMOR_CRACK_LOCATIONS = Map.of(
        Crackiness.Level.LOW,
        ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_low.png"),
        Crackiness.Level.MEDIUM,
        ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_medium.png"),
        Crackiness.Level.HIGH,
        ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_high.png")
    );

    public WolfArmorLayer(RenderLayerParent<WolfRenderState, WolfModel> renderer, EntityModelSet entityModels, EquipmentLayerRenderer equipmentRenderer) {
        super(renderer);
        this.adultModel = new WolfModel(entityModels.bakeLayer(ModelLayers.WOLF_ARMOR));
        this.babyModel = new WolfModel(entityModels.bakeLayer(ModelLayers.WOLF_BABY_ARMOR));
        this.equipmentRenderer = equipmentRenderer;
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, WolfRenderState renderState, float yRot, float xRot) {
        ItemStack itemstack = renderState.bodyArmorItem;
        Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && !equippable.assetId().isEmpty()) {
            WolfModel wolfmodel = renderState.isBaby ? this.babyModel : this.adultModel;
            this.equipmentRenderer
                .renderLayers(
                    EquipmentClientInfo.LayerType.WOLF_BODY,
                    equippable.assetId().get(),
                    wolfmodel,
                    renderState,
                    itemstack,
                    poseStack,
                    nodeCollector,
                    packedLight,
                    renderState.outlineColor
                );
            this.maybeRenderCracks(poseStack, nodeCollector, packedLight, itemstack, wolfmodel, renderState);
        }
    }

    private void maybeRenderCracks(
        PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, ItemStack armorStack, Model<WolfRenderState> model, WolfRenderState renderState
    ) {
        Crackiness.Level crackiness$level = Crackiness.WOLF_ARMOR.byDamage(armorStack);
        if (crackiness$level != Crackiness.Level.NONE) {
            ResourceLocation resourcelocation = ARMOR_CRACK_LOCATIONS.get(crackiness$level);
            nodeCollector.submitModel(
                model,
                renderState,
                poseStack,
                RenderType.armorTranslucent(resourcelocation),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                renderState.outlineColor,
                null
            );
        }
    }
}
