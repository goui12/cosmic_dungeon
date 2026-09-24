package net.goui.cosmicdungeon.mixin.client;

import net.goui.cosmicdungeon.client.TamsinAppearanceRender;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerRenderer.class)
public abstract class TamsinVillagerRendererMixin {
    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/VillagerRenderState;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$tamsinBody(VillagerRenderState state,
                                          CallbackInfoReturnable<ResourceLocation> callback) {
        if (TamsinAppearanceRender.INSTANCE.isTamsin(state))
            callback.setReturnValue(TamsinAppearanceRender.BODY);
    }
}
