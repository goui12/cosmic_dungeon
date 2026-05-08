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
public record Broken() implements ConditionalItemModelProperty {
    public static final MapCodec<Broken> MAP_CODEC = MapCodec.unit(new Broken());

    @Override
    public boolean get(ItemStack p_386887_, @Nullable ClientLevel p_388949_, @Nullable LivingEntity p_386628_, int p_387040_, ItemDisplayContext p_389458_) {
        return p_386887_.nextDamageWillBreak();
    }

    @Override
    public MapCodec<Broken> type() {
        return MAP_CODEC;
    }
}
