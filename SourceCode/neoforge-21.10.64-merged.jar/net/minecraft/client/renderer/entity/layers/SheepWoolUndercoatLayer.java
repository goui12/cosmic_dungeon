package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SheepWoolUndercoatLayer extends RenderLayer<SheepRenderState, SheepModel> {
    private static final ResourceLocation SHEEP_WOOL_UNDERCOAT_LOCATION = ResourceLocation.withDefaultNamespace(
        "textures/entity/sheep/sheep_wool_undercoat.png"
    );
    private final EntityModel<SheepRenderState> adultModel;
    private final EntityModel<SheepRenderState> babyModel;

    public SheepWoolUndercoatLayer(RenderLayerParent<SheepRenderState, SheepModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.adultModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_WOOL_UNDERCOAT));
        this.babyModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_BABY_WOOL_UNDERCOAT));
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, SheepRenderState renderState, float yRot, float xRot) {
        if (!renderState.isInvisible && (renderState.isJebSheep || renderState.woolColor != DyeColor.WHITE)) {
            EntityModel<SheepRenderState> entitymodel = renderState.isBaby ? this.babyModel : this.adultModel;
            coloredCutoutModelCopyLayerRender(
                entitymodel, SHEEP_WOOL_UNDERCOAT_LOCATION, poseStack, nodeCollector, packedLight, renderState, renderState.getWoolColor(), 1
            );
        }
    }
}
