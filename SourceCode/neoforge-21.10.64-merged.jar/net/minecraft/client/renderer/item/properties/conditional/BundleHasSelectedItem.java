package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record BundleHasSelectedItem() implements ConditionalItemModelProperty {
    public static final MapCodec<BundleHasSelectedItem> MAP_CODEC = MapCodec.unit(new BundleHasSelectedItem());

    @Override
    public boolean get(ItemStack p_387343_, @Nullable ClientLevel p_386467_, @Nullable LivingEntity p_387284_, int p_388377_, ItemDisplayContext p_389630_) {
        return BundleItem.hasSelectedItem(p_387343_);
    }

    @Override
    public MapCodec<BundleHasSelectedItem> type() {
        return MAP_CODEC;
    }
}
