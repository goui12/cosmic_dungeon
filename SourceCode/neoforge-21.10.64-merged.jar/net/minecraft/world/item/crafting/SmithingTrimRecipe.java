package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;

public class SmithingTrimRecipe implements SmithingRecipe {
    final Ingredient template;
    final Ingredient base;
    final Ingredient addition;
    final Holder<TrimPattern> pattern;
    @Nullable
    private PlacementInfo placementInfo;

    public SmithingTrimRecipe(Ingredient template, Ingredient base, Ingredient addition, Holder<TrimPattern> pattern) {
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.pattern = pattern;
    }

    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        return applyTrim(registries, input.base(), input.addition(), this.pattern);
    }

    public static ItemStack applyTrim(HolderLookup.Provider registries, ItemStack base, ItemStack addition, Holder<TrimPattern> pattern) {
        Optional<Holder<TrimMaterial>> optional = TrimMaterials.getFromIngredient(registries, addition);
        if (optional.isPresent()) {
            ArmorTrim armortrim = base.get(DataComponents.TRIM);
            ArmorTrim armortrim1 = new ArmorTrim(optional.get(), pattern);
            if (Objects.equals(armortrim, armortrim1)) {
                return ItemStack.EMPTY;
            } else {
                ItemStack itemstack = base.copyWithCount(1);
                itemstack.set(DataComponents.TRIM, armortrim1);
                return itemstack;
            }
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public Optional<Ingredient> templateIngredient() {
        return Optional.of(this.template);
    }

    @Override
    public Ingredient baseIngredient() {
        return this.base;
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return Optional.of(this.addition);
    }

    @Override
    public RecipeSerializer<SmithingTrimRecipe> getSerializer() {
        return RecipeSerializer.SMITHING_TRIM;
    }

    @Override
    public PlacementInfo placementInfo() {
        if (this.placementInfo == null) {
            this.placementInfo = PlacementInfo.create(List.of(this.template, this.base, this.addition));
        }

        return this.placementInfo;
    }

    @Override
    public List<RecipeDisplay> display() {
        SlotDisplay slotdisplay = this.base.display();
        SlotDisplay slotdisplay1 = this.addition.display();
        SlotDisplay slotdisplay2 = this.template.display();
        return List.of(
            new SmithingRecipeDisplay(
                slotdisplay2,
                slotdisplay,
                slotdisplay1,
                new SlotDisplay.SmithingTrimDemoSlotDisplay(slotdisplay, slotdisplay1, this.pattern),
                new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)
            )
        );
    }

    public static class Serializer implements RecipeSerializer<SmithingTrimRecipe> {
        private static final MapCodec<SmithingTrimRecipe> CODEC = RecordCodecBuilder.mapCodec(
            p_399429_ -> p_399429_.group(
                    Ingredient.CODEC.fieldOf("template").forGetter(p_399421_ -> p_399421_.template),
                    Ingredient.CODEC.fieldOf("base").forGetter(p_399423_ -> p_399423_.base),
                    Ingredient.CODEC.fieldOf("addition").forGetter(p_399428_ -> p_399428_.addition),
                    TrimPattern.CODEC.fieldOf("pattern").forGetter(p_399425_ -> p_399425_.pattern)
                )
                .apply(p_399429_, SmithingTrimRecipe::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, SmithingTrimRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            p_399427_ -> p_399427_.template,
            Ingredient.CONTENTS_STREAM_CODEC,
            p_399424_ -> p_399424_.base,
            Ingredient.CONTENTS_STREAM_CODEC,
            p_399422_ -> p_399422_.addition,
            TrimPattern.STREAM_CODEC,
            p_399426_ -> p_399426_.pattern,
            SmithingTrimRecipe::new
        );

        @Override
        public MapCodec<SmithingTrimRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SmithingTrimRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
