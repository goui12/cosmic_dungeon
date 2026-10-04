package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.block.entity.CosmicSpawnerAwareness;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pause both goal and brain AI while a Cosmic spawn awaits its own player detection. */
@Mixin(Mob.class)
public abstract class CosmicSpawnerAwarenessMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$waitForPlayer(CallbackInfo ci) {
        if (CosmicSpawnerAwareness.waitForPlayer((Mob)(Object)this)) ci.cancel();
    }
}
