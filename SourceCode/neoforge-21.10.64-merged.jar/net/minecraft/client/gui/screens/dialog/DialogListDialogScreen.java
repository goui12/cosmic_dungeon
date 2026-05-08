package net.minecraft.client.gui.screens.dialog;

import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogListDialog;
import net.minecraft.server.dialog.action.StaticAction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DialogListDialogScreen extends ButtonListDialogScreen<DialogListDialog> {
    public DialogListDialogScreen(@Nullable Screen previousScreen, DialogListDialog dialog, DialogConnectionAccess connectionAccess) {
        super(previousScreen, dialog, connectionAccess);
    }

    protected Stream<ActionButton> createListActions(DialogListDialog dialog, DialogConnectionAccess connectionAccess) {
        return dialog.dialogs().stream().map(p_428062_ -> createDialogClickAction(dialog, (Holder<Dialog>)p_428062_));
    }

    private static ActionButton createDialogClickAction(DialogListDialog dialog, Holder<Dialog> dialogToOpen) {
        return new ActionButton(
            new CommonButtonData(dialogToOpen.value().common().computeExternalTitle(), dialog.buttonWidth()),
            Optional.of(new StaticAction(new ClickEvent.ShowDialog(dialogToOpen)))
        );
    }
}
