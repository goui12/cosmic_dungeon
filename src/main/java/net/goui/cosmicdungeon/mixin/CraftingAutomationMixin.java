package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.crafting.CraftingPolicy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.item.crafting.*;
import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(CrafterBlock.class)
public abstract class CraftingAutomationMixin {
    @Inject(method="getPotentialResults",at=@At("HEAD"),cancellable=true)
    private static void cosmicdungeon$currentRecipe(ServerLevel level,CraftingInput input,CallbackInfoReturnable<Optional<RecipeHolder<CraftingRecipe>>> cir) {
        // Avoid vanilla RecipeCache retaining an approved result after config/datapack reload.
        cir.setReturnValue(level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level)
                .filter(CraftingPolicy::automated));
    }
}
