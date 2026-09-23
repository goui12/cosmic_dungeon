package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.economy.DeathCurrencyService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayer.class)
public abstract class DeathCurrencyPlayerMixin {
    // TAIL is the final return only. NeoForge's cancelled-death early return must never debit Trace.
    @Inject(method="die",at=@At("TAIL"))
    private void cosmicdungeon$confirmedDeath(DamageSource source,CallbackInfo info){
        DeathCurrencyService.died((ServerPlayer)(Object)this);
    }
}
