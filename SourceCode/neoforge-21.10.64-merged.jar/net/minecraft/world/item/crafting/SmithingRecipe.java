package net.minecraft.world.item.crafting;

import java.util.Optional;
import net.minecraft.world.level.Level;

public interface SmithingRecipe extends Recipe<SmithingRecipeInput> {
    @Override
    default RecipeType<SmithingRecipe> getType() {
        return RecipeType.SMITHING;
    }

    @Override
    RecipeSerializer<? extends SmithingRecipe> getSerializer();

    default boolean matches(SmithingRecipeInput p_362233_, Level p_361570_) {
        return Ingredient.testOptionalIngredient(this.templateIngredient(), p_362233_.template())
            && this.baseIngredient().test(p_362233_.base())
            && Ingredient.testOptionalIngredient(this.additionIngredient(), p_362233_.addition());
    }

    Optional<Ingredient> templateIngredient();

    Ingredient baseIngredient();

    Optional<Ingredient> additionIngredient();

    @Override
    default RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.SMITHING;
    }
}
