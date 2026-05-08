package net.minecraft.client.renderer.entity.layers;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Markings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HorseMarkingLayer extends RenderLayer<HorseRenderState, HorseModel> {
    private static final ResourceLocation INVISIBLE_TEXTURE = ResourceLocation.withDefaultNamespace("invisible");
    private static final Map<Markings, ResourceLocation> LOCATION_BY_MARKINGS = Maps.newEnumMap(
        Map.of(
            Markings.NONE,
            INVISIBLE_TEXTURE,
            Markings.WHITE,
            ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_markings_white.png"),
            Markings.WHITE_FIELD,
            ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_markings_whitefield.png"),
            Markings.WHITE_DOTS,
            ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_markings_whitedots.png"),
            Markings.BLACK_DOTS,
            ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_markings_blackdots.png")
        )
    );

    public HorseMarkingLayer(RenderLayerParent<HorseRenderState, HorseModel> p_117045_) {
        super(p_117045_);
    }

    public void submit(PoseStack p_434064_, SubmitNodeCollector p_435700_, int p_434375_, HorseRenderState p_433883_, float p_432898_, float p_432954_) {
        ResourceLocation resourcelocation = LOCATION_BY_MARKINGS.get(p_433883_.markings);
        if (resourcelocation != INVISIBLE_TEXTURE && !p_433883_.isInvisible) {
            p_435700_.order(1)
                .submitModel(
                    this.getParentModel(),
                    p_433883_,
                    p_434064_,
                    RenderType.entityTranslucent(resourcelocation),
                    p_434375_,
                    LivingEntityRenderer.getOverlayCoords(p_433883_, 0.0F),
                    -1,
                    null,
                    p_433883_.outlineColor,
                    null
                );
        }
    }
}
