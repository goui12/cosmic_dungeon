package net.goui.cosmicdungeon.datagen;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
        super(provider, recipeOutput);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> provider) {
            super(packOutput, provider);
        }
        @Override protected RecipeProvider createRecipeProvider(HolderLookup.Provider provider, RecipeOutput out) {
            return new ModRecipeProvider(provider, out);
        }
        @Override public String getName() { return "CosmicDungeon Recipes"; }
    }

    @Override
    protected void buildRecipes() {
        // Smeltables list (use .get())
        // Chicken block -> eggs
        shapeless(RecipeCategory.MISC, Items.EGG, 10)
                .requires(ModBlocks.CHICKEN_BLOCK.get())
                .unlockedBy("has_chicken_block", has(ModBlocks.CHICKEN_BLOCK.get()))
                .save(output, CosmicDungeonMod.MOD_ID + ":chicken_block");
    }

    protected void oreSmelting(RecipeOutput out, List<ItemLike> ingredients, RecipeCategory cat, ItemLike result,
                               float xp, int time, String group) {
        oreCooking(out, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, ingredients, cat, result,
                xp, time, group, "_from_smelting");
    }

    protected void oreBlasting(RecipeOutput out, List<ItemLike> ingredients, RecipeCategory cat, ItemLike result,
                               float xp, int time, String group) {
        oreCooking(out, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, ingredients, cat, result,
                xp, time, group, "_from_blasting");
    }

    protected <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput out,
                                                                RecipeSerializer<T> serializer,
                                                                AbstractCookingRecipe.Factory<T> factory,
                                                                List<ItemLike> ingredients,
                                                                RecipeCategory cat, ItemLike result,
                                                                float xp, int time, String group, String suffix) {
        for (ItemLike il : ingredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(il), cat, result, xp, time, serializer, factory)
                    .group(group)
                    .unlockedBy(getHasName(il), has(il))
                    .save(out, CosmicDungeonMod.MOD_ID + ":" + getItemName(result) + suffix + "_" + getItemName(il));
        }
    }
}
