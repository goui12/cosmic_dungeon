package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record CustomModelDataProperty(int index) implements RangeSelectItemModelProperty {
    public static final MapCodec<CustomModelDataProperty> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_387227_ -> p_387227_.group(ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelDataProperty::index))
            .apply(p_387227_, CustomModelDataProperty::new)
    );

    @Override
    public float get(ItemStack p_386883_, @Nullable ClientLevel p_387867_, @Nullable ItemOwner p_434671_, int p_388040_) {
        CustomModelData custommodeldata = p_386883_.get(DataComponents.CUSTOM_MODEL_DATA);
        if (custommodeldata != null) {
            Float f = custommodeldata.getFloat(this.index);
            if (f != null) {
                return f;
            }
        }

        return 0.0F;
    }

    @Override
    public MapCodec<CustomModelDataProperty> type() {
        return MAP_CODEC;
    }
}
