package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.mercenary.MercenaryAntiFarming;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Snapshot before Entity stores NeoForgeData; restore after the entire native effect tree loads. */
@Mixin(Entity.class)
public abstract class MercenaryEffectSaveMixin {
    @Inject(method="saveWithoutId",at=@At("HEAD"))
    private void cosmicdungeon$saveEffects(ValueOutput output,CallbackInfo ci){
        if((Object)this instanceof LivingEntity living)MercenaryAntiFarming.saveEffects(living);
    }
    @Inject(method="load",at=@At("TAIL"))
    private void cosmicdungeon$loadEffects(ValueInput input,CallbackInfo ci){
        if((Object)this instanceof LivingEntity living)MercenaryAntiFarming.restoreEffects(living);
    }
}
