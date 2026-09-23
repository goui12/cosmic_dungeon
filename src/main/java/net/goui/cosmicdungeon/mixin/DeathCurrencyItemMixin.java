package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.economy.DeathCurrencyService;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ItemEntity.class)
public abstract class DeathCurrencyItemMixin {
    @Inject(method="isMergable",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$noCurrencyMerge(CallbackInfoReturnable<Boolean> info){
        if(DeathCurrencyService.logical((ItemEntity)(Object)this))info.setReturnValue(false);
    }
    @Inject(method="playerTouch",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$logicalPickup(Player player,CallbackInfo info){
        var item=(ItemEntity)(Object)this;
        if(DeathCurrencyService.logical(item)){DeathCurrencyService.touch(item,player);info.cancel();}
    }
}
