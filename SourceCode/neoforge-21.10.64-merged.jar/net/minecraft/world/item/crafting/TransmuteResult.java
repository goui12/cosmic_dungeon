package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public record TransmuteResult(Holder<Item> item, int count, DataComponentPatch components) {
    private static final Codec<TransmuteResult> FULL_CODEC = RecordCodecBuilder.create(
        p_393986_ -> p_393986_.group(
                Item.CODEC.fieldOf("id").forGetter(TransmuteResult::item),
                ExtraCodecs.intRange(1, 99).optionalFieldOf("count", 1).forGetter(TransmuteResult::count),
                DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(TransmuteResult::components)
            )
            .apply(p_393986_, TransmuteResult::new)
    );
    public static final Codec<TransmuteResult> CODEC = Codec.<TransmuteResult, Holder<Item>>withAlternative(
            FULL_CODEC, Item.CODEC, p_393819_ -> new TransmuteResult(p_393819_.value())
        )
        .validate(TransmuteResult::validate);
    public static final StreamCodec<RegistryFriendlyByteBuf, TransmuteResult> STREAM_CODEC = StreamCodec.composite(
        Item.STREAM_CODEC,
        TransmuteResult::item,
        ByteBufCodecs.VAR_INT,
        TransmuteResult::count,
        DataComponentPatch.STREAM_CODEC,
        TransmuteResult::components,
        TransmuteResult::new
    );

    public TransmuteResult(Item p_394464_) {
        this(p_394464_.builtInRegistryHolder(), 1, DataComponentPatch.EMPTY);
    }

    private static DataResult<TransmuteResult> validate(TransmuteResult result) {
        return ItemStack.validateStrict(new ItemStack(result.item, result.count, result.components)).map(p_394610_ -> result);
    }

    public ItemStack apply(ItemStack stack) {
        ItemStack itemstack = stack.transmuteCopy(this.item.value(), this.count);
        itemstack.applyComponents(this.components);
        return itemstack;
    }

    public boolean isResultUnchanged(ItemStack stack) {
        ItemStack itemstack = this.apply(stack);
        return itemstack.getCount() == 1 && ItemStack.isSameItemSameComponents(stack, itemstack);
    }

    public SlotDisplay display() {
        return new SlotDisplay.ItemStackSlotDisplay(new ItemStack(this.item, this.count, this.components));
    }
}
