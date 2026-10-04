package net.goui.cosmicdungeon.mixin.client;
import com.llamalad7.mixinextras.sugar.Local;
import net.goui.cosmicdungeon.client.screen.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PauseScreen.class)
public abstract class LeaderboardPauseMixin{
    @Inject(method="createPauseMenu",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/layouts/GridLayout;arrangeElements()V"))
    private void cosmicdungeon$leaderboard(CallbackInfo ci,@Local GridLayout.RowHelper row){
        var parent=(PauseScreen)(Object)this;
        row.addChild(new TealLeaderboardButton(0,0,204,b->Minecraft.getInstance().setScreen(new LeaderboardScreen(parent))),2);
    }
}
