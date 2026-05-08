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
public record Damaged() implements ConditionalItemModelProperty {
    public static final MapCodec<Damaged> MAP_CODEC = MapCodec.unit(new Damaged());

    @Override
    public boolean get(ItemStack p_388323_, @Nullable ClientLevel p_388659_, @Nullable LivingEntity p_386950_, int p_387664_, ItemDisplayContext p_389573_) {
        return p_388323_.isDamaged();
    }

    @Override
    public MapCodec<Damaged> type() {
        return MAP_CODEC;
    }
}
