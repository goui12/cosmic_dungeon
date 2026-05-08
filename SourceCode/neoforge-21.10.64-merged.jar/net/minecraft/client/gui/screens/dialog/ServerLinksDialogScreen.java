package net.minecraft.client.gui.screens.dialog;

import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.ServerLinksDialog;
import net.minecraft.server.dialog.action.StaticAction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ServerLinksDialogScreen extends ButtonListDialogScreen<ServerLinksDialog> {
    public ServerLinksDialogScreen(@Nullable Screen previousScreen, ServerLinksDialog dialog, DialogConnectionAccess connectionAccess) {
        super(previousScreen, dialog, connectionAccess);
    }

    protected Stream<ActionButton> createListActions(ServerLinksDialog dialog, DialogConnectionAccess connectionAccess) {
        return connectionAccess.serverLinks().entries().stream().map(p_428067_ -> createDialogClickAction(dialog, p_428067_));
    }

    private static ActionButton createDialogClickAction(ServerLinksDialog dialog, ServerLinks.Entry entry) {
        return new ActionButton(
            new CommonButtonData(entry.displayName(), dialog.buttonWidth()), Optional.of(new StaticAction(new ClickEvent.OpenUrl(entry.link())))
        );
    }
}
