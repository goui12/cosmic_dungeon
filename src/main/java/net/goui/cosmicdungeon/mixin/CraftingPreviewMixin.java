package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.crafting.CraftingPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(CraftingMenu.class)
public abstract class CraftingPreviewMixin {
    @Redirect(method="slotChangedCraftingGrid",at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/ResultContainer;setRecipeUsed(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/crafting/RecipeHolder;)Z"))
    private static boolean cosmicdungeon$preview(ResultContainer result,ServerPlayer player,RecipeHolder<?> recipe) {
        return CraftingPolicy.player(player,recipe) && result.setRecipeUsed(player,recipe);
    }
}
