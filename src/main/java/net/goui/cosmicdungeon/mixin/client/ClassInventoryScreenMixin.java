package net.goui.cosmicdungeon.mixin.client;

import net.goui.cosmicdungeon.client.screen.ClassInventoryPresentation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.pipeline.RenderPipeline;

@Mixin(InventoryScreen.class)
public abstract class ClassInventoryScreenMixin {
    // Replace only the background blit: retain native mouse-following avatar/effect rendering.
    @Redirect(method = "renderBg", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"))
    private void cosmicdungeon$theme(GuiGraphics g, RenderPipeline pipeline, ResourceLocation oldTexture,
                                     int x, int y, float u, float v, int w, int h, int tw, int th) {
        ClassInventoryPresentation.background(g, (AbstractContainerScreen<?>)(Object)this);
    }
    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$labels(GuiGraphics g, int mx, int my, CallbackInfo ci) {
        ClassInventoryPresentation.labels(g);
        ci.cancel();
    }
}
