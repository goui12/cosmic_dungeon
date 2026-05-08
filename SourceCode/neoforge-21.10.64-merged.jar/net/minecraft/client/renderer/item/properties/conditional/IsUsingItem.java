package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record IsUsingItem() implements ConditionalItemModelProperty {
    public static final MapCodec<IsUsingItem> MAP_CODEC = MapCodec.unit(new IsUsingItem());

    @Override
    public boolean get(ItemStack p_386538_, @Nullable ClientLevel p_386504_, @Nullable LivingEntity p_388492_, int p_387916_, ItemDisplayContext p_389393_) {
        return p_388492_ == null ? false : p_388492_.isUsingItem() && p_388492_.getUseItem() == p_386538_;
    }

    @Override
    public MapCodec<IsUsingItem> type() {
        return MAP_CODEC;
    }
}
