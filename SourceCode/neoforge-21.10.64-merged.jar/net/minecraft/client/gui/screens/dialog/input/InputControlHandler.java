package net.minecraft.client.gui.screens.dialog.input;

import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.input.InputControl;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface InputControlHandler<T extends InputControl> {
    void addControl(T p_426301_, Screen p_426218_, InputControlHandler.Output p_425742_);

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface Output {
        void accept(LayoutElement p_425779_, Action.ValueGetter p_428548_);
    }
}
