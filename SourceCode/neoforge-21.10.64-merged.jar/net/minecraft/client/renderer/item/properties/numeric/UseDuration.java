package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record UseDuration(boolean remaining) implements RangeSelectItemModelProperty {
    public static final MapCodec<UseDuration> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_387574_ -> p_387574_.group(Codec.BOOL.optionalFieldOf("remaining", false).forGetter(UseDuration::remaining)).apply(p_387574_, UseDuration::new)
    );

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        LivingEntity livingentity = owner == null ? null : owner.asLivingEntity();
        if (livingentity != null && livingentity.getUseItem() == stack) {
            return this.remaining ? livingentity.getUseItemRemainingTicks() : useDuration(stack, livingentity);
        } else {
            return 0.0F;
        }
    }

    @Override
    public MapCodec<UseDuration> type() {
        return MAP_CODEC;
    }

    public static int useDuration(ItemStack stack, LivingEntity entity) {
        return stack.getUseDuration(entity) - entity.getUseItemRemainingTicks();
    }
}
