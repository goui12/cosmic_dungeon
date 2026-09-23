package net.goui.cosmicdungeon.mixin.client;

import net.goui.cosmicdungeon.client.branding.CosmicMenuLogoRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Changes only the title artwork; vanilla/NeoForge continue to own menu controls and credits. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenBrandingMixin {
    @Unique
    private final CosmicMenuLogoRenderer cosmicdungeon$logo = new CosmicMenuLogoRenderer();

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V"))
    private void cosmicdungeon$renderLogo(LogoRenderer original, GuiGraphics graphics,
                                         int screenWidth, float alpha) {
        cosmicdungeon$logo.render(graphics, screenWidth, alpha);
    }
}
