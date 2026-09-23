package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.block.entity.CosmicSpawnerEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class CosmicSpawnerEntityMixin {
    @Inject(method = "onRemovedFromLevel", at = @At("RETURN"))
    private void cosmicdungeon$spawnerUnloaded(CallbackInfo info) { CosmicSpawnerEntities.removed((Entity)(Object)this); }
    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void cosmicdungeon$spawnerRemoved(Entity.RemovalReason reason, CallbackInfo info) {
        CosmicSpawnerEntities.removed((Entity)(Object)this);
    }
    @Inject(method = {"addTag", "removeTag"}, at = @At("RETURN"))
    private void cosmicdungeon$spawnerTag(String tag, CallbackInfoReturnable<Boolean> info) {
        if (info.getReturnValueZ()) CosmicSpawnerEntities.tagChanged((Entity)(Object)this, tag);
    }
    // Vanilla /data restores the original UUID after load(); re-read Tags at that final boundary.
    @Inject(method = "setUUID", at = @At("RETURN"))
    private void cosmicdungeon$spawnerIdentityRestored(java.util.UUID uuid, CallbackInfo info) {
        CosmicSpawnerEntities.addedOrRetagged((Entity)(Object)this);
    }
    @Inject(method = "load", at = @At("RETURN"))
    private void cosmicdungeon$spawnerReload(ValueInput input, CallbackInfo info) {
        CosmicSpawnerEntities.addedOrRetagged((Entity)(Object)this);
    }
}
