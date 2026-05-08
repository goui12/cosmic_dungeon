package net.minecraft.world.inventory;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameRules;

public interface RecipeCraftingHolder {
    void setRecipeUsed(@Nullable RecipeHolder<?> recipe);

    @Nullable
    RecipeHolder<?> getRecipeUsed();

    default void awardUsedRecipes(Player player, List<ItemStack> items) {
        RecipeHolder<?> recipeholder = this.getRecipeUsed();
        if (recipeholder != null) {
            player.triggerRecipeCrafted(recipeholder, items);
            if (!recipeholder.value().isSpecial()) {
                player.awardRecipes(Collections.singleton(recipeholder));
                this.setRecipeUsed(null);
            }
        }
    }

    default boolean setRecipeUsed(ServerPlayer player, RecipeHolder<?> recipe) {
        if (!recipe.value().isSpecial()
            && player.level().getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING)
            && !player.getRecipeBook().contains(recipe.id())) {
            return false;
        } else {
            this.setRecipeUsed(recipe);
            return true;
        }
    }
}
