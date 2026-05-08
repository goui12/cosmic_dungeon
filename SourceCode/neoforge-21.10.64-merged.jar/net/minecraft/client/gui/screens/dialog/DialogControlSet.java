package net.minecraft.client.gui.screens.dialog;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.dialog.input.InputControlHandlers;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.action.Action;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DialogControlSet {
    public static final Supplier<Optional<ClickEvent>> EMPTY_ACTION = Optional::empty;
    private final DialogScreen<?> screen;
    private final Map<String, Action.ValueGetter> valueGetters = new HashMap<>();

    public DialogControlSet(DialogScreen<?> screen) {
        this.screen = screen;
    }

    public void addInput(Input input, Consumer<LayoutElement> adder) {
        String s = input.key();
        InputControlHandlers.createHandler(input.control(), this.screen, (p_428413_, p_428530_) -> {
            this.valueGetters.put(s, p_428530_);
            adder.accept(p_428413_);
        });
    }

    private static Button.Builder createDialogButton(CommonButtonData buttonData, Button.OnPress onPress) {
        Button.Builder button$builder = Button.builder(buttonData.label(), onPress);
        button$builder.width(buttonData.width());
        if (buttonData.tooltip().isPresent()) {
            button$builder = button$builder.tooltip(Tooltip.create(buttonData.tooltip().get()));
        }

        return button$builder;
    }

    public Supplier<Optional<ClickEvent>> bindAction(Optional<Action> p_action) {
        if (p_action.isPresent()) {
            Action action = p_action.get();
            return () -> action.createAction(this.valueGetters);
        } else {
            return EMPTY_ACTION;
        }
    }

    public Button.Builder createActionButton(ActionButton button) {
        Supplier<Optional<ClickEvent>> supplier = this.bindAction(button.action());
        return createDialogButton(button.button(), p_428410_ -> this.screen.runAction(supplier.get()));
    }
}
