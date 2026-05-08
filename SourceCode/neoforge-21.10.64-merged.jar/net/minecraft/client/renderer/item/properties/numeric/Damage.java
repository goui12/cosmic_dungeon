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
public record Damage(boolean normalize) implements RangeSelectItemModelProperty {
    public static final MapCodec<Damage> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_387730_ -> p_387730_.group(Codec.BOOL.optionalFieldOf("normalize", true).forGetter(Damage::normalize)).apply(p_387730_, Damage::new)
    );

    @Override
    public float get(ItemStack p_386977_, @Nullable ClientLevel p_386768_, @Nullable ItemOwner p_432745_, int p_386955_) {
        float f = p_386977_.getDamageValue();
        float f1 = p_386977_.getMaxDamage();
        return this.normalize ? Mth.clamp(f / f1, 0.0F, 1.0F) : Mth.clamp(f, 0.0F, f1);
    }

    @Override
    public MapCodec<Damage> type() {
        return MAP_CODEC;
    }
}
