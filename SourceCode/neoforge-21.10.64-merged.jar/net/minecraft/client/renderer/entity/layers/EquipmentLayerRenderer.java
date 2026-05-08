package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EquipmentLayerRenderer {
    private static final int NO_LAYER_COLOR = 0;
    private final EquipmentAssetManager equipmentAssets;
    private final Function<EquipmentLayerRenderer.LayerTextureKey, ResourceLocation> layerTextureLookup;
    private final Function<EquipmentLayerRenderer.TrimSpriteKey, TextureAtlasSprite> trimSpriteLookup;

    public EquipmentLayerRenderer(EquipmentAssetManager equipmentAssets, TextureAtlas atlas) {
        this.equipmentAssets = equipmentAssets;
        this.layerTextureLookup = Util.memoize(p_386235_ -> p_386235_.layer.getTextureLocation(p_386235_.layerType));
        this.trimSpriteLookup = Util.memoize(p_399319_ -> atlas.getSprite(p_399319_.spriteId()));
    }

    public <S> void renderLayers(
        EquipmentClientInfo.LayerType layerType,
        ResourceKey<EquipmentAsset> equipmentAsset,
        Model<? super S> armorModel,
        S renderState,
        ItemStack item,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        int outlineColor
    ) {
        this.renderLayers(layerType, equipmentAsset, armorModel, renderState, item, poseStack, nodeCollector, packedLight, null, outlineColor, 1);
    }

    public <S> void renderLayers(
        EquipmentClientInfo.LayerType layerType,
        ResourceKey<EquipmentAsset> equipmentAsset,
        Model<? super S> armorModel,
        S renderState,
        ItemStack item,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        @Nullable ResourceLocation texture,
        int outlineColor,
        int key
    ) {
        net.neoforged.neoforge.client.extensions.common.IClientItemExtensions extensions = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(item);
        armorModel = extensions.getGenericArmorModel(item, layerType, armorModel);
        List<EquipmentClientInfo.Layer> list = this.equipmentAssets.get(equipmentAsset).getLayers(layerType);
        if (!list.isEmpty()) {
            int i = extensions.getDefaultDyeColor(item);
            boolean flag = item.hasFoil();
            int j = key;

            int idx = 0;
            for (EquipmentClientInfo.Layer equipmentclientinfo$layer : list) {
                int k = extensions.getArmorLayerTintColor(item, equipmentclientinfo$layer, idx, i);
                if (k != 0) {
                    ResourceLocation resourcelocation = equipmentclientinfo$layer.usePlayerTexture() && texture != null
                        ? texture
                        : this.layerTextureLookup.apply(new EquipmentLayerRenderer.LayerTextureKey(layerType, equipmentclientinfo$layer));
                    resourcelocation = net.neoforged.neoforge.client.ClientHooks.getArmorTexture(item, layerType, equipmentclientinfo$layer, resourcelocation);
                    nodeCollector.order(j++)
                        .submitModel(
                            armorModel,
                            renderState,
                            poseStack,
                            RenderType.armorCutoutNoCull(resourcelocation),
                            packedLight,
                            OverlayTexture.NO_OVERLAY,
                            k,
                            null,
                            outlineColor,
                            null
                        );
                    if (flag) {
                        nodeCollector.order(j++)
                            .submitModel(
                                armorModel, renderState, poseStack, RenderType.armorEntityGlint(), packedLight, OverlayTexture.NO_OVERLAY, k, null, outlineColor, null
                            );
                    }

                    flag = false;
                }
                idx++;
            }

            ArmorTrim armortrim = item.get(DataComponents.TRIM);
            if (armortrim != null) {
                TextureAtlasSprite textureatlassprite = this.trimSpriteLookup.apply(new EquipmentLayerRenderer.TrimSpriteKey(armortrim, layerType, equipmentAsset));
                RenderType rendertype = Sheets.armorTrimsSheet(armortrim.pattern().value().decal());
                nodeCollector.order(j++)
                    .submitModel(armorModel, renderState, poseStack, rendertype, packedLight, OverlayTexture.NO_OVERLAY, -1, textureatlassprite, outlineColor, null);
            }
        }
    }

    public static int getColorForLayer(EquipmentClientInfo.Layer layer, int color) {
        Optional<EquipmentClientInfo.Dyeable> optional = layer.dyeable();
        if (optional.isPresent()) {
            int i = optional.get().colorWhenUndyed().map(ARGB::opaque).orElse(0);
            return color != 0 ? color : i;
        } else {
            return -1;
        }
    }

    @OnlyIn(Dist.CLIENT)
    record LayerTextureKey(EquipmentClientInfo.LayerType layerType, EquipmentClientInfo.Layer layer) {
    }

    @OnlyIn(Dist.CLIENT)
    record TrimSpriteKey(ArmorTrim trim, EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> equipmentAssetId) {
        public ResourceLocation spriteId() {
            return this.trim.layerAssetId(this.layerType.trimAssetPrefix(), this.equipmentAssetId);
        }
    }
}
