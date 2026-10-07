package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.economy.DeathCurrencyService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayer.class)
public abstract class DeathCurrencyPlayerMixin {
    // This point is after NeoForge's cancellable death return and before native loot mutates Inventory.
    @Inject(method="die",at=@At(value="INVOKE",
            target="Lnet/minecraft/server/level/ServerPlayer;dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V",
            shift=At.Shift.BEFORE))
    private void cosmicdungeon$captureDeathInventory(DamageSource source,CallbackInfo info){
        net.goui.cosmicdungeon.dungeon.d1.DeathInventoryRecovery.prepareDeath((ServerPlayer)(Object)this);
    }
    // TAIL is the final return only. NeoForge's cancelled-death early return must never debit Trace.
    @Inject(method="die",at=@At("TAIL"))
    private void cosmicdungeon$confirmedDeath(DamageSource source,CallbackInfo info){
        net.goui.cosmicdungeon.dungeon.d1.RunMemberStats.died((ServerPlayer)(Object)this);
        DeathCurrencyService.died((ServerPlayer)(Object)this);
        net.goui.cosmicdungeon.mercenary.MercenaryResurrection.died((ServerPlayer)(Object)this);
    }
}
