package net.minecraft.client.gui.screens.dialog;

import javax.annotation.Nullable;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.SimpleDialog;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SimpleDialogScreen<T extends SimpleDialog> extends DialogScreen<T> {
    public SimpleDialogScreen(@Nullable Screen previousScreen, T dialog, DialogConnectionAccess connectionAccess) {
        super(previousScreen, dialog, connectionAccess);
    }

    protected void updateHeaderAndFooter(HeaderAndFooterLayout layout, DialogControlSet controls, T dialog, DialogConnectionAccess connectionAccess) {
        super.updateHeaderAndFooter(layout, controls, dialog, connectionAccess);
        LinearLayout linearlayout = LinearLayout.horizontal().spacing(8);

        for (ActionButton actionbutton : dialog.mainActions()) {
            linearlayout.addChild(controls.createActionButton(actionbutton).build());
        }

        layout.addToFooter(linearlayout);
    }
}
