package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HumanoidArmorLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private final ArmorModelSet<A> modelSet;
    private final ArmorModelSet<A> babyModelSet;
    private final EquipmentLayerRenderer equipmentRenderer;

    public HumanoidArmorLayer(RenderLayerParent<S, M> renderer, ArmorModelSet<A> modelSet, EquipmentLayerRenderer equipmentRenderer) {
        this(renderer, modelSet, modelSet, equipmentRenderer);
    }

    public HumanoidArmorLayer(RenderLayerParent<S, M> renderer, ArmorModelSet<A> modelSet, ArmorModelSet<A> babyModelSet, EquipmentLayerRenderer equipmentRenderer) {
        super(renderer);
        this.modelSet = modelSet;
        this.babyModelSet = babyModelSet;
        this.equipmentRenderer = equipmentRenderer;
    }

    public static boolean shouldRender(ItemStack stack, EquipmentSlot slot) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && shouldRender(equippable, slot);
    }

    private static boolean shouldRender(Equippable equippable, EquipmentSlot slot) {
        return equippable.assetId().isPresent() && equippable.slot() == slot;
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, S renderState, float yRot, float xRot) {
        this.renderArmorPiece(poseStack, nodeCollector, renderState.chestEquipment, EquipmentSlot.CHEST, packedLight, renderState);
        this.renderArmorPiece(poseStack, nodeCollector, renderState.legsEquipment, EquipmentSlot.LEGS, packedLight, renderState);
        this.renderArmorPiece(poseStack, nodeCollector, renderState.feetEquipment, EquipmentSlot.FEET, packedLight, renderState);
        this.renderArmorPiece(poseStack, nodeCollector, renderState.headEquipment, EquipmentSlot.HEAD, packedLight, renderState);
    }

    private void renderArmorPiece(PoseStack poseStack, SubmitNodeCollector nodeCollector, ItemStack item, EquipmentSlot slot, int packedLight, S renderState) {
        Equippable equippable = item.get(DataComponents.EQUIPPABLE);
        if (equippable != null && shouldRender(equippable, slot)) {
            A a = this.getArmorModel(renderState, slot);
            EquipmentClientInfo.LayerType equipmentclientinfo$layertype = this.usesInnerModel(slot)
                ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
                : EquipmentClientInfo.LayerType.HUMANOID;
            this.equipmentRenderer
                .renderLayers(
                    equipmentclientinfo$layertype,
                    equippable.assetId().orElseThrow(),
                    a,
                    renderState,
                    item,
                    poseStack,
                    nodeCollector,
                    packedLight,
                    renderState.outlineColor
                );
        }
    }

    private A getArmorModel(S renderState, EquipmentSlot slot) {
        return (renderState.isBaby ? this.babyModelSet : this.modelSet).get(slot);
    }

    private boolean usesInnerModel(EquipmentSlot slot) {
        return slot == EquipmentSlot.LEGS;
    }
}
