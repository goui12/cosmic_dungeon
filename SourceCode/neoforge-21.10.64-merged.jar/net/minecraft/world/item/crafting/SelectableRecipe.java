package net.minecraft.world.item.crafting;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public record SelectableRecipe<T extends Recipe<?>>(SlotDisplay optionDisplay, Optional<RecipeHolder<T>> recipe) {
    public static <T extends Recipe<?>> StreamCodec<RegistryFriendlyByteBuf, SelectableRecipe<T>> noRecipeCodec() {
        return StreamCodec.composite(
            SlotDisplay.STREAM_CODEC, SelectableRecipe::optionDisplay, p_379713_ -> new SelectableRecipe<>(p_379713_, Optional.empty())
        );
    }

    public record SingleInputEntry<T extends Recipe<?>>(Ingredient input, SelectableRecipe<T> recipe) {
        public static <T extends Recipe<?>> StreamCodec<RegistryFriendlyByteBuf, SelectableRecipe.SingleInputEntry<T>> noRecipeCodec() {
            return StreamCodec.<RegistryFriendlyByteBuf, SingleInputEntry<T>, Ingredient, SelectableRecipe<T>>composite(
                Ingredient.CONTENTS_STREAM_CODEC,
                SelectableRecipe.SingleInputEntry::input,
                SelectableRecipe.noRecipeCodec(),
                SelectableRecipe.SingleInputEntry::recipe,
                SelectableRecipe.SingleInputEntry::new
            );
        }
    }

    public record SingleInputSet<T extends Recipe<?>>(List<SelectableRecipe.SingleInputEntry<T>> entries) {
        public static <T extends Recipe<?>> SelectableRecipe.SingleInputSet<T> empty() {
            return new SelectableRecipe.SingleInputSet<>(List.of());
        }

        public static <T extends Recipe<?>> StreamCodec<RegistryFriendlyByteBuf, SelectableRecipe.SingleInputSet<T>> noRecipeCodec() {
            return StreamCodec.composite(
                SelectableRecipe.SingleInputEntry.<T>noRecipeCodec().apply(ByteBufCodecs.list()),
                SelectableRecipe.SingleInputSet::entries,
                SelectableRecipe.SingleInputSet::new
            );
        }

        public boolean acceptsInput(ItemStack stack) {
            return this.entries.stream().anyMatch(p_379482_ -> p_379482_.input.test(stack));
        }

        public SelectableRecipe.SingleInputSet<T> selectByInput(ItemStack stack) {
            return new SelectableRecipe.SingleInputSet<>(this.entries.stream().filter(p_379328_ -> p_379328_.input.test(stack)).toList());
        }

        public boolean isEmpty() {
            return this.entries.isEmpty();
        }

        public int size() {
            return this.entries.size();
        }
    }
}
