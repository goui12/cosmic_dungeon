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
import net.minecraft.world.item.component.MapItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record MapColor(int defaultColor) implements ItemTintSource {
    public static final MapCodec<MapColor> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_386787_ -> p_386787_.group(ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(MapColor::defaultColor)).apply(p_386787_, MapColor::new)
    );

    public MapColor() {
        this(MapItemColor.DEFAULT.rgb());
    }

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
        MapItemColor mapitemcolor = stack.get(DataComponents.MAP_COLOR);
        return mapitemcolor != null ? ARGB.opaque(mapitemcolor.rgb()) : ARGB.opaque(this.defaultColor);
    }

    @Override
    public MapCodec<MapColor> type() {
        return MAP_CODEC;
    }
}
