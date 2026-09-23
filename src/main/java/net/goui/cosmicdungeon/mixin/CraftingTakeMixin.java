package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.crafting.CraftingPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Each vanilla shift-transfer iteration is reauthorized before items move. */
@Mixin({CraftingMenu.class,InventoryMenu.class,StonecutterMenu.class,ItemCombinerMenu.class})
public abstract class CraftingTakeMixin {
    @Inject(method="quickMoveStack",at=@At("HEAD"))
    private void cosmicdungeon$quickMove(Player player,int index,CallbackInfoReturnable<ItemStack> cir) {
        if(player instanceof ServerPlayer server) CraftingPolicy.refresh((AbstractContainerMenu)(Object)this,server);
    }
}
