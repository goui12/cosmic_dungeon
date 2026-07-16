package net.goui.cosmicdungeon.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.goui.cosmicdungeon.client.screen.settings.CosmicDungeonOptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin {
    @Inject(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToContents(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    shift = At.Shift.BEFORE
            )
    )
    private void cosmicdungeon$addSettingsButton(
            CallbackInfo ci,
            @Local GridLayout.RowHelper rowHelper
    ) {
        OptionsScreen parent = (OptionsScreen) (Object) this;

        rowHelper.addChild(
                Button.builder(
                        Component.translatable("screen.cosmicdungeon.options.button"),
                        button -> Minecraft.getInstance().setScreen(
                                new CosmicDungeonOptionsScreen(parent)
                        )
                ).build()
        );
    }
}
