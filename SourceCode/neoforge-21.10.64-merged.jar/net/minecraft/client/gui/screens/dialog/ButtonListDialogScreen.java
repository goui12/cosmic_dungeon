package net.minecraft.client.gui.screens.dialog;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.ButtonListDialog;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ButtonListDialogScreen<T extends ButtonListDialog> extends DialogScreen<T> {
    public static final int FOOTER_MARGIN = 5;

    public ButtonListDialogScreen(@Nullable Screen previousScreen, T dialog, DialogConnectionAccess connectionAccess) {
        super(previousScreen, dialog, connectionAccess);
    }

    protected void populateBodyElements(LinearLayout layout, DialogControlSet controls, T dialog, DialogConnectionAccess connectionAccess) {
        super.populateBodyElements(layout, controls, dialog, connectionAccess);
        List<Button> list = this.createListActions(dialog, connectionAccess).map(p_428060_ -> controls.createActionButton(p_428060_).build()).toList();
        layout.addChild(packControlsIntoColumns(list, dialog.columns()));
    }

    protected abstract Stream<ActionButton> createListActions(T dialog, DialogConnectionAccess connectionAccess);

    protected void updateHeaderAndFooter(HeaderAndFooterLayout layout, DialogControlSet controls, T dialog, DialogConnectionAccess connectionAccess) {
        super.updateHeaderAndFooter(layout, controls, dialog, connectionAccess);
        dialog.exitAction()
            .ifPresentOrElse(p_428057_ -> layout.addToFooter(controls.createActionButton(p_428057_).build()), () -> layout.setFooterHeight(5));
    }
}
