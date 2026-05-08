package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record CustomModelDataProperty(int index) implements SelectItemModelProperty<String> {
    public static final PrimitiveCodec<String> VALUE_CODEC = Codec.STRING;
    public static final SelectItemModelProperty.Type<CustomModelDataProperty, String> TYPE = SelectItemModelProperty.Type.create(
        RecordCodecBuilder.mapCodec(
            p_388460_ -> p_388460_.group(ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelDataProperty::index))
                .apply(p_388460_, CustomModelDataProperty::new)
        ),
        VALUE_CODEC
    );

    @Nullable
    public String get(ItemStack p_388036_, @Nullable ClientLevel p_387600_, @Nullable LivingEntity p_388624_, int p_388055_, ItemDisplayContext p_387132_) {
        CustomModelData custommodeldata = p_388036_.get(DataComponents.CUSTOM_MODEL_DATA);
        return custommodeldata != null ? custommodeldata.getString(this.index) : null;
    }

    @Override
    public SelectItemModelProperty.Type<CustomModelDataProperty, String> type() {
        return TYPE;
    }

    @Override
    public Codec<String> valueCodec() {
        return VALUE_CODEC;
    }
}
