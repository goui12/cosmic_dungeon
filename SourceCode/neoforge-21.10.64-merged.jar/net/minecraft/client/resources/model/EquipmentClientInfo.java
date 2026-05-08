package net.minecraft.client.resources.model;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record EquipmentClientInfo(Map<EquipmentClientInfo.LayerType, List<EquipmentClientInfo.Layer>> layers) {
    private static final Codec<List<EquipmentClientInfo.Layer>> LAYER_LIST_CODEC = ExtraCodecs.nonEmptyList(EquipmentClientInfo.Layer.CODEC.listOf());
    public static final Codec<EquipmentClientInfo> CODEC = RecordCodecBuilder.create(
        p_388468_ -> p_388468_.group(
                ExtraCodecs.nonEmptyMap(Codec.unboundedMap(EquipmentClientInfo.LayerType.CODEC, LAYER_LIST_CODEC))
                    .fieldOf("layers")
                    .forGetter(EquipmentClientInfo::layers)
            )
            .apply(p_388468_, EquipmentClientInfo::new)
    );

    public static EquipmentClientInfo.Builder builder() {
        return new EquipmentClientInfo.Builder();
    }

    public List<EquipmentClientInfo.Layer> getLayers(EquipmentClientInfo.LayerType type) {
        return this.layers.getOrDefault(type, List.of());
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final Map<EquipmentClientInfo.LayerType, List<EquipmentClientInfo.Layer>> layersByType = new EnumMap<>(EquipmentClientInfo.LayerType.class);

        Builder() {
        }

        public EquipmentClientInfo.Builder addHumanoidLayers(ResourceLocation textureId) {
            return this.addHumanoidLayers(textureId, false);
        }

        public EquipmentClientInfo.Builder addHumanoidLayers(ResourceLocation textureId, boolean dyeable) {
            this.addLayers(EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, EquipmentClientInfo.Layer.leatherDyeable(textureId, dyeable));
            this.addMainHumanoidLayer(textureId, dyeable);
            return this;
        }

        public EquipmentClientInfo.Builder addMainHumanoidLayer(ResourceLocation textureId, boolean dyeable) {
            return this.addLayers(EquipmentClientInfo.LayerType.HUMANOID, EquipmentClientInfo.Layer.leatherDyeable(textureId, dyeable));
        }

        public EquipmentClientInfo.Builder addLayers(EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer... layers) {
            Collections.addAll(this.layersByType.computeIfAbsent(type, p_387338_ -> new ArrayList<>()), layers);
            return this;
        }

        public EquipmentClientInfo build() {
            return new EquipmentClientInfo(
                this.layersByType.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, p_386826_ -> List.copyOf(p_386826_.getValue())))
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Dyeable(Optional<Integer> colorWhenUndyed) {
        public static final Codec<EquipmentClientInfo.Dyeable> CODEC = RecordCodecBuilder.create(
            p_387846_ -> p_387846_.group(
                    ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("color_when_undyed").forGetter(EquipmentClientInfo.Dyeable::colorWhenUndyed)
                )
                .apply(p_387846_, EquipmentClientInfo.Dyeable::new)
        );
    }

    @OnlyIn(Dist.CLIENT)
    public record Layer(ResourceLocation textureId, Optional<EquipmentClientInfo.Dyeable> dyeable, boolean usePlayerTexture) {
        public static final Codec<EquipmentClientInfo.Layer> CODEC = RecordCodecBuilder.create(
            p_388785_ -> p_388785_.group(
                    ResourceLocation.CODEC.fieldOf("texture").forGetter(EquipmentClientInfo.Layer::textureId),
                    EquipmentClientInfo.Dyeable.CODEC.optionalFieldOf("dyeable").forGetter(EquipmentClientInfo.Layer::dyeable),
                    Codec.BOOL.optionalFieldOf("use_player_texture", false).forGetter(EquipmentClientInfo.Layer::usePlayerTexture)
                )
                .apply(p_388785_, EquipmentClientInfo.Layer::new)
        );

        public Layer(ResourceLocation p_386866_) {
            this(p_386866_, Optional.empty(), false);
        }

        public static EquipmentClientInfo.Layer leatherDyeable(ResourceLocation textureId, boolean dyeable) {
            return new EquipmentClientInfo.Layer(
                textureId, dyeable ? Optional.of(new EquipmentClientInfo.Dyeable(Optional.of(-6265536))) : Optional.empty(), false
            );
        }

        public static EquipmentClientInfo.Layer onlyIfDyed(ResourceLocation textureId, boolean dyeable) {
            return new EquipmentClientInfo.Layer(
                textureId, dyeable ? Optional.of(new EquipmentClientInfo.Dyeable(Optional.empty())) : Optional.empty(), false
            );
        }

        public ResourceLocation getTextureLocation(EquipmentClientInfo.LayerType type) {
            return this.textureId.withPath(p_386699_ -> "textures/entity/equipment/" + type.getSerializedName() + "/" + p_386699_ + ".png");
        }
    }

    @OnlyIn(Dist.CLIENT)
    @net.neoforged.fml.common.asm.enumextension.NamedEnum
    public static enum LayerType implements StringRepresentable, net.neoforged.fml.common.asm.enumextension.IExtensibleEnum {
        HUMANOID("humanoid"),
        HUMANOID_LEGGINGS("humanoid_leggings"),
        WINGS("wings"),
        WOLF_BODY("wolf_body"),
        HORSE_BODY("horse_body"),
        LLAMA_BODY("llama_body"),
        PIG_SADDLE("pig_saddle"),
        STRIDER_SADDLE("strider_saddle"),
        CAMEL_SADDLE("camel_saddle"),
        HORSE_SADDLE("horse_saddle"),
        DONKEY_SADDLE("donkey_saddle"),
        MULE_SADDLE("mule_saddle"),
        ZOMBIE_HORSE_SADDLE("zombie_horse_saddle"),
        SKELETON_HORSE_SADDLE("skeleton_horse_saddle"),
        HAPPY_GHAST_BODY("happy_ghast_body");

        public static final Codec<EquipmentClientInfo.LayerType> CODEC = StringRepresentable.fromEnum(EquipmentClientInfo.LayerType::values);
        private final String id;

        private LayerType(String id) {
            this.id = id.replaceAll(":", "/");
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }

        public String trimAssetPrefix() {
            return "trims/entity/" + this.id;
        }

        public static net.neoforged.fml.common.asm.enumextension.ExtensionInfo getExtensionInfo() {
            return net.neoforged.fml.common.asm.enumextension.ExtensionInfo.nonExtended(LayerType.class);
        }
    }
}
