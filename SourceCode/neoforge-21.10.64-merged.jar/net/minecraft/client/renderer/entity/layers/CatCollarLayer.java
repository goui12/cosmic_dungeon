package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CatCollarLayer extends RenderLayer<CatRenderState, CatModel> {
    private static final ResourceLocation CAT_COLLAR_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/cat/cat_collar.png");
    private final CatModel adultModel;
    private final CatModel babyModel;

    public CatCollarLayer(RenderLayerParent<CatRenderState, CatModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.adultModel = new CatModel(modelSet.bakeLayer(ModelLayers.CAT_COLLAR));
        this.babyModel = new CatModel(modelSet.bakeLayer(ModelLayers.CAT_BABY_COLLAR));
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, CatRenderState renderState, float yRot, float xRot) {
        DyeColor dyecolor = renderState.collarColor;
        if (dyecolor != null) {
            int i = dyecolor.getTextureDiffuseColor();
            CatModel catmodel = renderState.isBaby ? this.babyModel : this.adultModel;
            coloredCutoutModelCopyLayerRender(catmodel, CAT_COLLAR_LOCATION, poseStack, nodeCollector, packedLight, renderState, i, 1);
        }
    }
}
