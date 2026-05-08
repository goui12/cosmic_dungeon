package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SimpleEquipmentLayer<S extends LivingEntityRenderState, RM extends EntityModel<? super S>, EM extends EntityModel<? super S>>
    extends RenderLayer<S, RM> {
    private final EquipmentLayerRenderer equipmentRenderer;
    private final EquipmentClientInfo.LayerType layer;
    private final Function<S, ItemStack> itemGetter;
    private final EM adultModel;
    private final EM babyModel;
    private final int order;

    public SimpleEquipmentLayer(
        RenderLayerParent<S, RM> renderer,
        EquipmentLayerRenderer equipmentRenderer,
        EquipmentClientInfo.LayerType layer,
        Function<S, ItemStack> itemGetter,
        EM adultModel,
        EM babyModel,
        int order
    ) {
        super(renderer);
        this.equipmentRenderer = equipmentRenderer;
        this.layer = layer;
        this.itemGetter = itemGetter;
        this.adultModel = adultModel;
        this.babyModel = babyModel;
        this.order = order;
    }

    public SimpleEquipmentLayer(
        RenderLayerParent<S, RM> renderer,
        EquipmentLayerRenderer equipmentRenderer,
        EquipmentClientInfo.LayerType layer,
        Function<S, ItemStack> itemGetter,
        EM adultModel,
        EM babyModel
    ) {
        this(renderer, equipmentRenderer, layer, itemGetter, adultModel, babyModel, 0);
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, S renderState, float yRot, float xRot) {
        ItemStack itemstack = this.itemGetter.apply(renderState);
        Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && !equippable.assetId().isEmpty()) {
            EM em = renderState.isBaby ? this.babyModel : this.adultModel;
            this.equipmentRenderer
                .renderLayers(
                    this.layer, equippable.assetId().get(), em, renderState, itemstack, poseStack, nodeCollector, packedLight, null, renderState.outlineColor, this.order
                );
        }
    }
}
