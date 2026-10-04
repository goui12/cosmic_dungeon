package net.goui.cosmicdungeon.mixin.client;

import net.goui.cosmicdungeon.block.custom.ClassSelectorTargeting;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class ClassSelectorTargetingMixin {
    @Inject(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At("RETURN"), cancellable = true)
    private void cosmicdungeon$selectorOverhang(Entity camera, double blockReach, double entityReach,
                                                float partialTick, CallbackInfoReturnable<HitResult> callback) {
        callback.setReturnValue(ClassSelectorTargeting.INSTANCE.refine(camera.level(),
                camera.getEyePosition(partialTick), camera.getViewVector(partialTick),
                blockReach, callback.getReturnValue()));
    }
}
