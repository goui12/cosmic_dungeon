package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record Count(boolean normalize) implements RangeSelectItemModelProperty {
    public static final MapCodec<Count> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_387175_ -> p_387175_.group(Codec.BOOL.optionalFieldOf("normalize", true).forGetter(Count::normalize)).apply(p_387175_, Count::new)
    );

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        float f = stack.getCount();
        float f1 = stack.getMaxStackSize();
        return this.normalize ? Mth.clamp(f / f1, 0.0F, 1.0F) : Mth.clamp(f, 0.0F, f1);
    }

    @Override
    public MapCodec<Count> type() {
        return MAP_CODEC;
    }
}
