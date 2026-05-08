package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;

public final class Ingredient implements StackedContents.IngredientInfo<Holder<Item>>, Predicate<ItemStack> {
    public static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> CONTENTS_STREAM_CODEC = net.neoforged.neoforge.common.crafting.IngredientCodecs.streamCodec(ByteBufCodecs.holderSet(Registries.ITEM)
        .map(Ingredient::new, p_360055_ -> p_360055_.getValuesForSync()));
    public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Ingredient>> OPTIONAL_CONTENTS_STREAM_CODEC = net.neoforged.neoforge.common.crafting.IngredientCodecs.optionalStreamCodec(ByteBufCodecs.holderSet(Registries.ITEM)
        .map(
            p_360058_ -> p_360058_.size() == 0 ? Optional.empty() : Optional.of(new Ingredient((HolderSet<Item>)p_360058_)),
            p_360056_ -> p_360056_.map(p_360062_ -> p_360062_.getValuesForSync()).orElse(HolderSet.direct())
        ));
    public static final Codec<HolderSet<Item>> NON_AIR_HOLDER_SET_CODEC = HolderSetCodec.create(Registries.ITEM, Item.CODEC, false);
    public static final Codec<Ingredient> CODEC = net.neoforged.neoforge.common.crafting.IngredientCodecs.codec(ExtraCodecs.nonEmptyHolderSet(NON_AIR_HOLDER_SET_CODEC).xmap(Ingredient::new, p_360061_ -> p_360061_.values));
    private final HolderSet<Item> values;
    @org.jetbrains.annotations.Nullable
    private net.neoforged.neoforge.common.crafting.ICustomIngredient customIngredient = null;
    @org.jetbrains.annotations.Nullable
    private List<Holder<Item>> customIngredientValues;

    private Ingredient(HolderSet<Item> values) {
        values.unwrap().ifRight(p_360057_ -> {
            if (p_360057_.isEmpty()) {
                throw new UnsupportedOperationException("Ingredients can't be empty");
            } else if (p_360057_.contains(Items.AIR.builtInRegistryHolder())) {
                throw new UnsupportedOperationException("Ingredient can't contain air");
            }
        });
        this.values = values;
    }

    public Ingredient(net.neoforged.neoforge.common.crafting.ICustomIngredient customIngredient) {
        this.values = HolderSet.empty();
        this.customIngredient = customIngredient;
    }

    public static boolean testOptionalIngredient(Optional<Ingredient> ingredient, ItemStack stack) {
        return ingredient.<Boolean>map(p_360060_ -> p_360060_.test(stack)).orElseGet(stack::isEmpty);
    }

    @Deprecated
    public Stream<Holder<Item>> items() {
        if (this.customIngredient != null) {
            return updateCustomIngredientValues().stream();
        }
        return this.values.stream();
    }

    public boolean isEmpty() {
        if (this.customIngredient != null) {
            return updateCustomIngredientValues().isEmpty();
        }
        return this.values.size() == 0;
    }

    public boolean test(ItemStack stack) {
        if (this.customIngredient != null) {
            return this.customIngredient.test(stack);
        }
        return stack.is(this.values);
    }

    public boolean acceptsItem(Holder<Item> item) {
        if (this.customIngredient != null) {
            return updateCustomIngredientValues().contains(item);
        }
        return this.values.contains(item);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Ingredient ingredient ? java.util.Objects.equals(this.customIngredient, ingredient.customIngredient) && Objects.equals(this.values, ingredient.values) : false;
    }

    @Override
    public int hashCode() {
        if (this.customIngredient != null) {
            return this.customIngredient.hashCode();
        }
        return this.values.hashCode();
    }

    /**
      * Retrieves the underlying values of this ingredient.
      * If this is a {@linkplain #isCustom custom ingredient}, an exception is thrown.
      */
    public HolderSet<Item> getValues() {
        if (isCustom()) {
            throw new IllegalStateException("Cannot retrieve values from custom ingredient!");
        }
        return this.values;
    }

    /**
     * Retrieves the holder set to use for syncing {@linkplain #isSimple() simple} ingredients
     */
    private HolderSet<Item> getValuesForSync() {
        if (isCustom()) {
            return HolderSet.direct(this.items().toList());
        }
        return this.values;
    }

    public boolean isSimple() {
        return this.customIngredient == null || this.customIngredient.isSimple();
    }

    @org.jetbrains.annotations.Nullable
    public net.neoforged.neoforge.common.crafting.ICustomIngredient getCustomIngredient() {
        return this.customIngredient;
    }

    public boolean isCustom() {
        return this.customIngredient != null;
    }

    private List<Holder<Item>> updateCustomIngredientValues() {
        if (this.customIngredientValues == null) {
            this.customIngredientValues = this.customIngredient.items().toList();
        }
        return this.customIngredientValues;
    }

    public static Ingredient of(ItemLike item) {
        return new Ingredient(HolderSet.direct(item.asItem().builtInRegistryHolder()));
    }

    public static Ingredient of(ItemLike... items) {
        return of(Arrays.stream(items));
    }

    public static Ingredient of(Stream<? extends ItemLike> items) {
        return new Ingredient(HolderSet.direct(items.map(p_360054_ -> p_360054_.asItem().builtInRegistryHolder()).toList()));
    }

    public static Ingredient of(HolderSet<Item> items) {
        return new Ingredient(items);
    }

    public SlotDisplay display() {
        if (this.customIngredient != null) {
            return this.customIngredient.display();
        }
        return (SlotDisplay)this.values
            .unwrap()
            .map(SlotDisplay.TagSlotDisplay::new, p_380837_ -> new SlotDisplay.Composite(p_380837_.stream().map(Ingredient::displayForSingleItem).toList()));
    }

    public static SlotDisplay optionalIngredientToDisplay(Optional<Ingredient> ingredient) {
        return ingredient.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE);
    }

    public static SlotDisplay displayForSingleItem(Holder<Item> item) {
        SlotDisplay slotdisplay = new SlotDisplay.ItemSlotDisplay(item);
        ItemStack itemstack = item.value().getCraftingRemainder();
        if (!itemstack.isEmpty()) {
            SlotDisplay slotdisplay1 = new SlotDisplay.ItemStackSlotDisplay(itemstack);
            return new SlotDisplay.WithRemainder(slotdisplay, slotdisplay1);
        } else {
            return slotdisplay;
        }
    }
}
