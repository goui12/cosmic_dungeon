package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.crafting.CraftingPolicy;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;
@Mixin(RecipeManager.class)
public abstract class CraftingLookupMixin {
    @Inject(method="getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;", at=@At("RETURN"), cancellable=true)
    private <I extends RecipeInput,T extends Recipe<I>> void cosmicdungeon$plain(RecipeType<T> type,I input,Level level,CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        check(type,input,cir);
    }
    @Inject(method="getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;", at=@At("RETURN"), cancellable=true)
    private <I extends RecipeInput,T extends Recipe<I>> void cosmicdungeon$key(RecipeType<T> type,I input,Level level,ResourceKey<Recipe<?>> key,CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        check(type,input,cir);
    }
    @Inject(method="getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;", at=@At("RETURN"), cancellable=true)
    private <I extends RecipeInput,T extends Recipe<I>> void cosmicdungeon$holder(RecipeType<T> type,I input,Level level,RecipeHolder<T> holder,CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        check(type,input,cir);
    }
    @org.spongepowered.asm.mixin.Unique
    private static <T extends Recipe<?>> void check(RecipeType<?> type,RecipeInput input,CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        if(cir.getReturnValue().isPresent() && !CraftingPolicy.lookup(type,input,cir.getReturnValue().get())) cir.setReturnValue(Optional.empty());
    }
}
