package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.model.AxolotlModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.AxolotlRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AxolotlRenderer extends AgeableMobRenderer<Axolotl, AxolotlRenderState, AxolotlModel> {
    private static final Map<Axolotl.Variant, ResourceLocation> TEXTURE_BY_TYPE = Util.make(
        Maps.newHashMap(),
        p_349898_ -> {
            for (Axolotl.Variant axolotl$variant : Axolotl.Variant.values()) {
                p_349898_.put(
                    axolotl$variant,
                    ResourceLocation.withDefaultNamespace(String.format(Locale.ROOT, "textures/entity/axolotl/axolotl_%s.png", axolotl$variant.getName()))
                );
            }
        }
    );

    public AxolotlRenderer(EntityRendererProvider.Context p_173921_) {
        super(p_173921_, new AxolotlModel(p_173921_.bakeLayer(ModelLayers.AXOLOTL)), new AxolotlModel(p_173921_.bakeLayer(ModelLayers.AXOLOTL_BABY)), 0.5F);
    }

    public ResourceLocation getTextureLocation(AxolotlRenderState p_362427_) {
        return TEXTURE_BY_TYPE.get(p_362427_.variant);
    }

    public AxolotlRenderState createRenderState() {
        return new AxolotlRenderState();
    }

    public void extractRenderState(Axolotl p_363864_, AxolotlRenderState p_364989_, float p_363532_) {
        super.extractRenderState(p_363864_, p_364989_, p_363532_);
        p_364989_.variant = p_363864_.getVariant();
        p_364989_.playingDeadFactor = p_363864_.playingDeadAnimator.getFactor(p_363532_);
        p_364989_.inWaterFactor = p_363864_.inWaterAnimator.getFactor(p_363532_);
        p_364989_.onGroundFactor = p_363864_.onGroundAnimator.getFactor(p_363532_);
        p_364989_.movingFactor = p_363864_.movingAnimator.getFactor(p_363532_);
    }
}
