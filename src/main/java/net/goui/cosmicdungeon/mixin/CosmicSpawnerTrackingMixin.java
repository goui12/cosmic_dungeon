package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.block.entity.CosmicSpawnerEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mirrors the visible-entity set formerly enumerated by ServerLevel.getAllEntities(). */
@Mixin(PersistentEntitySectionManager.class)
public abstract class CosmicSpawnerTrackingMixin {
    @Inject(method = "startTracking", at = @At("RETURN"))
    private void cosmicdungeon$spawnerVisible(EntityAccess access, CallbackInfo info) {
        if (access instanceof Entity entity) CosmicSpawnerEntities.addedOrRetagged(entity);
    }
    @Inject(method = "stopTracking", at = @At("HEAD"))
    private void cosmicdungeon$spawnerHidden(EntityAccess access, CallbackInfo info) {
        if (access instanceof Entity entity) CosmicSpawnerEntities.removed(entity);
    }
}
