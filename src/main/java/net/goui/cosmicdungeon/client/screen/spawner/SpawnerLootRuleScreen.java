package net.goui.cosmicdungeon.client.screen.spawner;

import java.util.function.Consumer;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.IntrinsicDropRule.Kind;
import net.goui.cosmicdungeon.spawner.editor.SpawnerLootDraft.Rule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Editing or removing one stable row never conflates other rows that use the same item. */
final class SpawnerLootRuleScreen extends SpawnerEditorChildScreen {
    private final String id;
    private final Consumer<Rule> save;
    private final Runnable remove;
    private String item, chance, count;
    private Kind kind;
    private Button itemButton, saveButton;
    SpawnerLootRuleScreen(SpawnerEditorScreen owner, Screen back, Rule rule, Consumer<Rule> save, Runnable remove) {
        super("Loot row", owner, back);
        id = rule.id(); item = rule.itemId(); chance = SpawnerLootState.percent(rule.chance());
        count = Integer.toString(rule.count()); kind = rule.kind(); this.save = save; this.remove = remove;
    }
    @Override protected void init() {
        panel = SpawnerEditorLayout.fit(width, height);
        int x = x(), y = panel.y() + 30, half = (w() - 8) / 2;
        itemButton = button("     " + font.plainSubstrByWidth(SpawnerLootScreen.itemName(item), w() - 32), x, y, w(),
                () -> minecraft.setScreen(new SpawnerLootItemScreen(owner, this, selected -> { item = selected; })),
                "Choose the item for this row. No inventory items are granted.");
        itemButton.setTooltip(null); // Preserve the native item tooltip instead of a competing widget tooltip.
        button(SpawnerLootScreen.kindLabel(kind), x, y + 27, half, () -> {
            kind = kind == Kind.CUSTOM_ADDITION ? Kind.DEFAULT_OVERRIDE : Kind.CUSTOM_ADDITION; rebuildWidgets();
        }, SpawnerLootScreen.ruleDescription(kind) + " Click to change explicitly between Custom addition and Override.");
        button("Behavior details", x + half + 8, y + 27, half, () -> {},
                SpawnerLootScreen.ruleDescription(kind) + "\nStable row ID: " + id);
        field(x, y + 64, half, chance, "Drop chance (percent 0–100)", value -> chance = value);
        field(x + half + 8, y + 64, half, count, "Item count (1–64)", value -> count = value);
        button("Remove row", x, panel.footerY(), 86, () -> { remove.run(); minecraft.setScreen(back); },
                "Remove this row from the nested draft. Other rows for the same item remain. Main Apply is still required.");
        saveButton = button("Save row", x + w() - 148, panel.footerY(), 80, () -> {
            if (error().isEmpty()) {
                save.accept(new Rule(id, item, SpawnerLootState.chance(chance), Integer.parseInt(count), kind));
                minecraft.setScreen(back);
            }
        }, "Save only this row to the nested loot draft, then Save loot and main Apply.");
        button("Cancel", x + w() - 64, panel.footerY(), 64, this::onClose, "Discard only this row's changes.");
    }
    private void field(int x, int y, int width, String value, String label, Consumer<String> change) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.literal(label));
        field.setMaxLength(12); field.setValue(value); field.setResponder(change);
        field.setTooltip(Tooltip.create(Component.literal(label))); addRenderableWidget(field);
    }
    private String error() {
        if (SpawnerLootScreen.icon(item).isEmpty()) return "Select an existing non-air item.";
        if (SpawnerLootState.chance(chance) < 0) return "Chance must be a percentage from 0 to 100.";
        try {
            int number = Integer.parseInt(count);
            if (number < 1 || number > 64 || !count.chars().allMatch(Character::isDigit)) return "Count must be a whole number from 1 to 64.";
        } catch (NumberFormatException invalid) { return "Count must be a whole number from 1 to 64."; }
        return "";
    }
    @Override public void tick() { super.tick(); if (saveButton != null) saveButton.active = error().isEmpty(); }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        frame(g, error().isEmpty() ? "Loot row / " + SpawnerLootScreen.kindLabel(kind) : error());
        int y = panel.y() + 83;
        g.drawString(font, "Chance %", x(), y, 0xFFDDE5EB, false);
        g.drawString(font, "Count", x() + (w() - 8) / 2 + 8, y, 0xFFDDE5EB, false);
        super.render(g, mx, my, delta);
        var stack = SpawnerLootScreen.icon(item);
        g.renderItem(stack, itemButton.getX() + 3, itemButton.getY() + 2);
        if (itemButton.isMouseOver(mx, my) || (itemButton.isFocused() && minecraft.getLastInputType().isKeyboard()))
            g.setTooltipForNextFrame(font, stack, mx, my);
        if (mx >= panel.x() && mx < panel.x() + panel.width() && my >= panel.y() && my < panel.y() + 24 && !error().isEmpty())
            g.setTooltipForNextFrame(font, Component.literal(error()), mx, my);
    }
}
