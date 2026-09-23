package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.crafting.CraftingPolicy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractCraftingMenu.class)
public abstract class CraftingBookMixin {
    @Inject(method="handlePlacement",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$recipeBook(boolean max,boolean creative,RecipeHolder<?> recipe,ServerLevel level,Inventory inventory,
            CallbackInfoReturnable<RecipeBookMenu.PostPlaceAction> cir) {
        if(inventory.player instanceof ServerPlayer player && !CraftingPolicy.player(player,recipe))
            cir.setReturnValue(RecipeBookMenu.PostPlaceAction.NOTHING);
    }
}
