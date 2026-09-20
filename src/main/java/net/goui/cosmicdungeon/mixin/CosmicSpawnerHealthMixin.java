package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.block.entity.CosmicSpawnerEntities;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class CosmicSpawnerHealthMixin {
    @Inject(method = "setHealth", at = @At("RETURN"))
    private void cosmicdungeon$spawnerHealth(float health, CallbackInfo info) {
        CosmicSpawnerEntities.healthChanged((LivingEntity)(Object)this);
    }
}
