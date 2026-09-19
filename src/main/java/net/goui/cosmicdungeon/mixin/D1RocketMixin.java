package net.goui.cosmicdungeon.mixin;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FireworkRocketEntity.class)
public abstract class D1RocketMixin {
    @Inject(method="dealExplosionDamage",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$damage(ServerLevel level,CallbackInfo ci){
        if(net.goui.cosmicdungeon.playerclass.d1.D1RocketAbilities.explode((FireworkRocketEntity)(Object)this,level))ci.cancel();
    }
}
