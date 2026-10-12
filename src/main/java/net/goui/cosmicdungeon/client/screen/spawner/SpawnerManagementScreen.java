package net.goui.cosmicdungeon.client.screen.spawner;

import net.minecraft.client.gui.GuiGraphics;

final class SpawnerManagementScreen extends SpawnerEditorChildScreen {
    SpawnerManagementScreen(SpawnerEditorScreen owner) { super("Drops and preset files", owner, owner); }
    @Override protected void init() {
        panel = SpawnerEditorLayout.fit(width, height);
        int y = panel.y() + 34, col = (w() - 8) / 2;
        button("Gear drops", x(), y, col, () -> open(SpawnerLootScreen.Page.GEAR), "Six independent equipment drop percentages. Draft changes apply only with the main Apply button.");
        button("Default loot", x() + col + 8, y, col, () -> open(SpawnerLootScreen.Page.DEFAULTS), "Inspect the entity's native loot table. Conditional or complex chances are labeled honestly.");
        button("Configured loot", x(), y + 28, col, () -> open(SpawnerLootScreen.Page.RULES), "Manage overrides and independent additions. Duplicate item rows keep independent identities.");
        button("Preset files", x() + col + 8, y + 28, col, () -> minecraft.setScreen(new SpawnerPresetScreen(owner, this)),
                "Browse, save, load or delete named server files and assign the five existing shortcuts. Loading changes only the editor draft.");
        button("Back", x() + w() - 70, panel.footerY(), 70, this::onClose, "Return to the main spawner draft.");
    }
    private void open(SpawnerLootScreen.Page page) {
        minecraft.setScreen(new SpawnerLootScreen(owner, this, page));
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        frame(g, "Spawner / drops and preset files"); super.render(g, mx, my, delta);
    }
}
