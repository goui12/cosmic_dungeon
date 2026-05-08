package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WolfCollarLayer extends RenderLayer<WolfRenderState, WolfModel> {
    private static final ResourceLocation WOLF_COLLAR_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_collar.png");

    public WolfCollarLayer(RenderLayerParent<WolfRenderState, WolfModel> p_117707_) {
        super(p_117707_);
    }

    public void submit(PoseStack p_434880_, SubmitNodeCollector p_435581_, int p_433407_, WolfRenderState p_433463_, float p_434548_, float p_435131_) {
        DyeColor dyecolor = p_433463_.collarColor;
        if (dyecolor != null && !p_433463_.isInvisible) {
            int i = dyecolor.getTextureDiffuseColor();
            p_435581_.order(1)
                .submitModel(
                    this.getParentModel(),
                    p_433463_,
                    p_434880_,
                    RenderType.entityCutoutNoCull(WOLF_COLLAR_LOCATION),
                    p_433407_,
                    OverlayTexture.NO_OVERLAY,
                    i,
                    null,
                    p_433463_.outlineColor,
                    null
                );
        }
    }
}
