package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.MushroomCowMushroomLayer;
import net.minecraft.client.renderer.entity.state.MushroomCowRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.MushroomCow;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MushroomCowRenderer extends AgeableMobRenderer<MushroomCow, MushroomCowRenderState, CowModel> {
    private static final Map<MushroomCow.Variant, ResourceLocation> TEXTURES = Util.make(Maps.newHashMap(), p_359287_ -> {
        p_359287_.put(MushroomCow.Variant.BROWN, ResourceLocation.withDefaultNamespace("textures/entity/cow/brown_mooshroom.png"));
        p_359287_.put(MushroomCow.Variant.RED, ResourceLocation.withDefaultNamespace("textures/entity/cow/red_mooshroom.png"));
    });

    public MushroomCowRenderer(EntityRendererProvider.Context p_174324_) {
        super(p_174324_, new CowModel(p_174324_.bakeLayer(ModelLayers.MOOSHROOM)), new CowModel(p_174324_.bakeLayer(ModelLayers.MOOSHROOM_BABY)), 0.7F);
        this.addLayer(new MushroomCowMushroomLayer(this, p_174324_.getBlockRenderDispatcher()));
    }

    public ResourceLocation getTextureLocation(MushroomCowRenderState renderState) {
        return TEXTURES.get(renderState.variant);
    }

    public MushroomCowRenderState createRenderState() {
        return new MushroomCowRenderState();
    }

    public void extractRenderState(MushroomCow entity, MushroomCowRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.variant = entity.getVariant();
    }
}
