package net.goui.cosmicdungeon.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.Screen;

/** A read-only modal retains its parent menu, including Creative's client-only ItemPickerMenu. */
final class InventoryModalContext {
    private InventoryModalContext() {}
    static boolean current(Screen parent, LocalPlayer player) {
        return player != null && (parent == null ? player.containerMenu == player.inventoryMenu
                : PartyInspectionHover.currentMenu(parent, player));
    }
    static void restore(Screen parent) {
        var mc = Minecraft.getInstance();
        mc.setScreen(current(parent, mc.player) ? parent : null);
    }
    static void dismiss(Screen parent) {
        var mc = Minecraft.getInstance();
        if (parent != null && current(parent, mc.player)) {
            mc.setScreen(parent);
            parent.onClose(); // Native close restores Creative's inventoryMenu and closes ExtraInventory.
        } else mc.setScreen(null);
    }
}
