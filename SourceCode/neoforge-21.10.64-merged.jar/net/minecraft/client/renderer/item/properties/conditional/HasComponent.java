package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record HasComponent(DataComponentType<?> componentType, boolean ignoreDefault) implements ConditionalItemModelProperty {
    public static final MapCodec<HasComponent> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_389343_ -> p_389343_.group(
                BuiltInRegistries.DATA_COMPONENT_TYPE.byNameCodec().fieldOf("component").forGetter(HasComponent::componentType),
                Codec.BOOL.optionalFieldOf("ignore_default", false).forGetter(HasComponent::ignoreDefault)
            )
            .apply(p_389343_, HasComponent::new)
    );

    @Override
    public boolean get(ItemStack p_388222_, @Nullable ClientLevel p_386715_, @Nullable LivingEntity p_386871_, int p_387461_, ItemDisplayContext p_389562_) {
        return this.ignoreDefault ? p_388222_.hasNonDefault(this.componentType) : p_388222_.has(this.componentType);
    }

    @Override
    public MapCodec<HasComponent> type() {
        return MAP_CODEC;
    }
}
