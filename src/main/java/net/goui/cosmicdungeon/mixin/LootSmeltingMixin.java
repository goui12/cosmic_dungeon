package net.goui.cosmicdungeon.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Optional;
import net.goui.cosmicdungeon.crafting.LootSmeltingLookup;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Conditions, output assembly, count multiplication and missing-recipe behavior remain native. */
@Mixin(SmeltItemFunction.class)
public abstract class LootSmeltingMixin {
    @WrapOperation(method="run",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/crafting/RecipeManager;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"))
    private <I extends RecipeInput,T extends Recipe<I>> Optional<RecipeHolder<T>> cosmicdungeon$lootLookup(
            RecipeManager recipes,RecipeType<T> type,I input,Level level,Operation<Optional<RecipeHolder<T>>> original){
        return LootSmeltingLookup.query(input,()->original.call(recipes,type,input,level));
    }
}
