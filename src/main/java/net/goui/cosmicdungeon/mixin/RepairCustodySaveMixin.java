package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCustody;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class RepairCustodySaveMixin {
    @Inject(method="saveWithoutId",at=@At("HEAD"))
    private void cosmicdungeon$captureOwnedRepair(ValueOutput output,CallbackInfo ci){
        if((Object)this instanceof ServerPlayer player){
            RepairCustody.capture(player);
            net.goui.cosmicdungeon.trade.TradeCustody.capture(player);
        }
    }
}
