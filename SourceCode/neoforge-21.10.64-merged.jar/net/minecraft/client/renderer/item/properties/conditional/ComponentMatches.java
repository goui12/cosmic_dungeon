package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ComponentMatches(DataComponentPredicate.Single<?> predicate) implements ConditionalItemModelProperty {
    public static final MapCodec<ComponentMatches> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_400240_ -> p_400240_.group(DataComponentPredicate.singleCodec("predicate").forGetter(ComponentMatches::predicate))
            .apply(p_400240_, ComponentMatches::new)
    );

    @Override
    public boolean get(ItemStack p_399920_, @Nullable ClientLevel p_399844_, @Nullable LivingEntity p_399573_, int p_400095_, ItemDisplayContext p_400208_) {
        return this.predicate.predicate().matches(p_399920_);
    }

    @Override
    public MapCodec<ComponentMatches> type() {
        return MAP_CODEC;
    }
}
