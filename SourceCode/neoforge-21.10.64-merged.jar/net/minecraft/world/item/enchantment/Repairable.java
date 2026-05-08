package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record Repairable(HolderSet<Item> items) {
    public static final Codec<Repairable> CODEC = RecordCodecBuilder.create(
        p_364058_ -> p_364058_.group(RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(Repairable::items))
            .apply(p_364058_, Repairable::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, Repairable> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.holderSet(Registries.ITEM), Repairable::items, Repairable::new
    );

    public boolean isValidRepairItem(ItemStack stack) {
        return stack.is(this.items);
    }
}
