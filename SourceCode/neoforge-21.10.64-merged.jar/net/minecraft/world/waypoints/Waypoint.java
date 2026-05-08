package net.minecraft.world.waypoints;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.scores.PlayerTeam;

public interface Waypoint {
    int MAX_RANGE = 60000000;
    AttributeModifier WAYPOINT_TRANSMIT_RANGE_HIDE_MODIFIER = new AttributeModifier(
        ResourceLocation.withDefaultNamespace("waypoint_transmit_range_hide"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    static Item.Properties addHideAttribute(Item.Properties properties) {
        return properties.component(
            DataComponents.ATTRIBUTE_MODIFIERS,
            ItemAttributeModifiers.builder()
                .add(
                    Attributes.WAYPOINT_TRANSMIT_RANGE, WAYPOINT_TRANSMIT_RANGE_HIDE_MODIFIER, EquipmentSlotGroup.HEAD, ItemAttributeModifiers.Display.hidden()
                )
                .build()
        );
    }

    public static class Icon {
        public static final Codec<Waypoint.Icon> CODEC = RecordCodecBuilder.create(
            p_419451_ -> p_419451_.group(
                    ResourceKey.codec(WaypointStyleAssets.ROOT_ID).fieldOf("style").forGetter(p_419449_ -> p_419449_.style),
                    ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("color").forGetter(p_415783_ -> p_415783_.color)
                )
                .apply(p_419451_, Waypoint.Icon::new)
        );
        public static final StreamCodec<ByteBuf, Waypoint.Icon> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(WaypointStyleAssets.ROOT_ID),
            p_419450_ -> p_419450_.style,
            ByteBufCodecs.optional(ByteBufCodecs.RGB_COLOR),
            p_415612_ -> p_415612_.color,
            Waypoint.Icon::new
        );
        public static final Waypoint.Icon NULL = new Waypoint.Icon();
        public ResourceKey<WaypointStyleAsset> style = WaypointStyleAssets.DEFAULT;
        public Optional<Integer> color = Optional.empty();

        public Icon() {
        }

        private Icon(ResourceKey<WaypointStyleAsset> style, Optional<Integer> color) {
            this.style = style;
            this.color = color;
        }

        public boolean hasData() {
            return this.style != WaypointStyleAssets.DEFAULT || this.color.isPresent();
        }

        public Waypoint.Icon cloneAndAssignStyle(LivingEntity entity) {
            ResourceKey<WaypointStyleAsset> resourcekey = this.getOverrideStyle();
            Optional<Integer> optional = this.color
                .or(
                    () -> Optional.ofNullable(entity.getTeam())
                        .map(p_415659_ -> p_415659_.getColor().getColor())
                        .map(p_416615_ -> p_416615_ == 0 ? -13619152 : p_416615_)
                );
            return resourcekey == this.style && optional.isEmpty() ? this : new Waypoint.Icon(resourcekey, optional);
        }

        private ResourceKey<WaypointStyleAsset> getOverrideStyle() {
            return this.style != WaypointStyleAssets.DEFAULT ? this.style : WaypointStyleAssets.DEFAULT;
        }
    }
}
