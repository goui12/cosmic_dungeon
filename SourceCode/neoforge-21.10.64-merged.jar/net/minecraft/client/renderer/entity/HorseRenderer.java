package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.client.model.EquineSaddleModel;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.HorseMarkingLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Variant;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class HorseRenderer extends AbstractHorseRenderer<Horse, HorseRenderState, HorseModel> {
    private static final Map<Variant, ResourceLocation> LOCATION_BY_VARIANT = Maps.newEnumMap(
        Map.of(
            Variant.WHITE,
            ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_white.png"),
            Variant.CREAMY,
            ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_creamy.png"),
            Variant.CHESTNUT,
            ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_chestnut.png"),
            Variant.BROWN,
            ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_brown.png"),
            Variant.BLACK,
            ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_black.png"),
            Variant.GRAY,
            ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_gray.png"),
            Variant.DARK_BROWN,
            ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_darkbrown.png")
        )
    );

    public HorseRenderer(EntityRendererProvider.Context p_174167_) {
        super(p_174167_, new HorseModel(p_174167_.bakeLayer(ModelLayers.HORSE)), new HorseModel(p_174167_.bakeLayer(ModelLayers.HORSE_BABY)));
        this.addLayer(new HorseMarkingLayer(this));
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                p_174167_.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.HORSE_BODY,
                p_397943_ -> p_397943_.bodyArmorItem,
                new HorseModel(p_174167_.bakeLayer(ModelLayers.HORSE_ARMOR)),
                new HorseModel(p_174167_.bakeLayer(ModelLayers.HORSE_BABY_ARMOR)),
                2
            )
        );
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                p_174167_.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.HORSE_SADDLE,
                p_397146_ -> p_397146_.saddle,
                new EquineSaddleModel(p_174167_.bakeLayer(ModelLayers.HORSE_SADDLE)),
                new EquineSaddleModel(p_174167_.bakeLayer(ModelLayers.HORSE_BABY_SADDLE)),
                2
            )
        );
    }

    public ResourceLocation getTextureLocation(HorseRenderState p_365094_) {
        return LOCATION_BY_VARIANT.get(p_365094_.variant);
    }

    public HorseRenderState createRenderState() {
        return new HorseRenderState();
    }

    public void extractRenderState(Horse p_362522_, HorseRenderState p_363732_, float p_362557_) {
        super.extractRenderState(p_362522_, p_363732_, p_362557_);
        p_363732_.variant = p_362522_.getVariant();
        p_363732_.markings = p_362522_.getMarkings();
        p_363732_.bodyArmorItem = p_362522_.getBodyArmorItem().copy();
    }
}
