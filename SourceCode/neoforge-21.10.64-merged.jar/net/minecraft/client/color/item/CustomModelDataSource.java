package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record CustomModelDataSource(int index, int defaultColor) implements ItemTintSource {
    public static final MapCodec<CustomModelDataSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_387447_ -> p_387447_.group(
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelDataSource::index),
                ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(CustomModelDataSource::defaultColor)
            )
            .apply(p_387447_, CustomModelDataSource::new)
    );

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
        CustomModelData custommodeldata = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (custommodeldata != null) {
            Integer integer = custommodeldata.getColor(this.index);
            if (integer != null) {
                return ARGB.opaque(integer);
            }
        }

        return ARGB.opaque(this.defaultColor);
    }

    @Override
    public MapCodec<CustomModelDataSource> type() {
        return MAP_CODEC;
    }
}
