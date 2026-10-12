package net.goui.cosmicdungeon.client.screen.spawner;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** All editor children belong to the same nonce and close with their owning editor. */
abstract class SpawnerEditorChildScreen extends Screen {
    protected final SpawnerEditorScreen owner;
    protected final Screen back;
    protected SpawnerEditorLayout panel;
    protected SpawnerEditorChildScreen(String title, SpawnerEditorScreen owner, Screen back) {
        super(Component.literal(title)); this.owner = owner; this.back = back;
    }
    final SpawnerEditorScreen owner() { return owner; }
    protected int x() { return panel.x() + 8; }
    protected int w() { return panel.width() - 16; }
    protected Button button(String label, int x, int y, int width, Runnable action, String tip) {
        Button result = addRenderableWidget(Button.builder(Component.literal(label), b -> action.run()).bounds(x, y, width, 20).build());
        result.setTooltip(Tooltip.create(Component.literal(tip))); return result;
    }
    protected void frame(GuiGraphics g, String heading) {
        g.fill(panel.x(), panel.y(), panel.x() + panel.width(), panel.y() + panel.height(), 0xF016202C);
        g.fill(panel.x(), panel.y(), panel.x() + panel.width(), panel.y() + 24, 0xFF293B50);
        g.drawString(font, font.plainSubstrByWidth(heading, w()), x(), panel.y() + 8, 0xFFE4C98A, false);
    }
    @Override public void onClose() { if (!owner.closed()) minecraft.setScreen(back); }
    @Override public void tick() {
        if (minecraft.player == null || minecraft.level == null || owner.closed()) owner.finished();
    }
    @Override public boolean isPauseScreen() { return false; }
}
