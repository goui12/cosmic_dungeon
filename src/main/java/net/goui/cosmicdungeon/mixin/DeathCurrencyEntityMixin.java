package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.economy.DeathCurrencyService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Entity.class)
public abstract class DeathCurrencyEntityMixin {
    @Inject(method="shouldBeSaved",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$accountOwnsDrop(CallbackInfoReturnable<Boolean> info){
        if(DeathCurrencyService.logical((Entity)(Object)this))info.setReturnValue(false);
    }
    @Inject(method="saveAsPassenger",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$noWorldCopy(ValueOutput output,CallbackInfoReturnable<Boolean> info){
        if(DeathCurrencyService.logical((Entity)(Object)this))info.setReturnValue(false);
    }
    @Inject(method="onAddedToLevel",at=@At("RETURN"))
    private void cosmicdungeon$logicalAdded(CallbackInfo info){
        if((Object)this instanceof ItemEntity item)DeathCurrencyService.added(item);
    }
    @Inject(method="setRemoved",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$logicalRemoval(Entity.RemovalReason reason,CallbackInfo info){
        if((Object)this instanceof ItemEntity item&&item.getRemovalReason()==null
                &&DeathCurrencyService.logical(item)&&!DeathCurrencyService.removing(item,reason))info.cancel();
    }
}
