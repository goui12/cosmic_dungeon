package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record BundleFullness() implements RangeSelectItemModelProperty {
    public static final MapCodec<BundleFullness> MAP_CODEC = MapCodec.unit(new BundleFullness());

    @Override
    public float get(ItemStack p_388015_, @Nullable ClientLevel p_386753_, @Nullable ItemOwner p_435228_, int p_386639_) {
        return BundleItem.getFullnessDisplay(p_388015_);
    }

    @Override
    public MapCodec<BundleFullness> type() {
        return MAP_CODEC;
    }
}
