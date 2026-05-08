package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record ItemCost(Holder<Item> item, int count, DataComponentExactPredicate components, ItemStack itemStack) {
    public static final Codec<ItemCost> CODEC = RecordCodecBuilder.create(
        p_399442_ -> p_399442_.group(
                Item.CODEC.fieldOf("id").forGetter(ItemCost::item),
                ExtraCodecs.POSITIVE_INT.fieldOf("count").orElse(1).forGetter(ItemCost::count),
                DataComponentExactPredicate.CODEC.optionalFieldOf("components", DataComponentExactPredicate.EMPTY).forGetter(ItemCost::components)
            )
            .apply(p_399442_, ItemCost::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemCost> STREAM_CODEC = StreamCodec.composite(
        Item.STREAM_CODEC,
        ItemCost::item,
        ByteBufCodecs.VAR_INT,
        ItemCost::count,
        DataComponentExactPredicate.STREAM_CODEC,
        ItemCost::components,
        ItemCost::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, Optional<ItemCost>> OPTIONAL_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs::optional);

    public ItemCost(ItemLike p_330939_) {
        this(p_330939_, 1);
    }

    public ItemCost(ItemLike p_332181_, int p_330835_) {
        this(p_332181_.asItem().builtInRegistryHolder(), p_330835_, DataComponentExactPredicate.EMPTY);
    }

    public ItemCost(Holder<Item> p_330702_, int p_331182_, DataComponentExactPredicate p_399998_) {
        this(p_330702_, p_331182_, p_399998_, createStack(p_330702_, p_331182_, p_399998_));
    }

    public ItemCost withComponents(UnaryOperator<DataComponentExactPredicate.Builder> components) {
        return new ItemCost(this.item, this.count, components.apply(DataComponentExactPredicate.builder()).build());
    }

    private static ItemStack createStack(Holder<Item> item, int count, DataComponentExactPredicate components) {
        return new ItemStack(item, count, components.asPatch());
    }

    public boolean test(ItemStack stack) {
        return stack.is(this.item) && this.components.test((DataComponentGetter)stack);
    }
}
