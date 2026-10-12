package net.goui.cosmicdungeon.client.screen.spawner;

import java.util.ArrayList;
import java.util.List;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.Slot;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.IntrinsicDropRule.Kind;
import net.goui.cosmicdungeon.network.SpawnerEditorPayloads.LibraryKind;
import net.goui.cosmicdungeon.spawner.editor.SpawnerLootDraft.Rule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Native loot inspection is distinct from explicit overrides and independent custom rolls. */
final class SpawnerLootScreen extends SpawnerEditorChildScreen {
    enum Page { GEAR, DEFAULTS, RULES }
    private record Row(Button button, ItemStack icon, String detail) {}
    private final SpawnerLootState state;
    private final List<Row> rows = new ArrayList<>();
    private Page page;
    private int index, ruleFilter;
    private String notice = "";
    private Button save;
    SpawnerLootScreen(SpawnerEditorScreen owner, Screen back, Page page) {
        super("Spawner loot", owner, back); this.page = page;
        state = new SpawnerLootState(owner.lootSnapshot(), owner.lootDraft());
    }
    private int capacity() { return Math.max(1, (panel.footerY() - (panel.y() + 79) - 25) / 22); }
    @Override protected void init() {
        panel = SpawnerEditorLayout.fit(width, height); rows.clear();
        int tabWidth = (w() - 8) / 3;
        for (Page value : Page.values()) {
            int ordinal = value.ordinal();
            button((page == value ? "> " : "") + switch(value) { case GEAR -> "Gear"; case DEFAULTS -> "Default"; case RULES -> "Configured"; },
                    x() + ordinal * (tabWidth + 4), panel.y() + 27, tabWidth,
                    () -> { page = value; index = 0; rebuildWidgets(); },
                    switch(value) {
                        case GEAR -> "Independent equipment drop percentages. Empty Native fields preserve existing native behavior.";
                        case DEFAULTS -> "Read-only native table summary. Conditions, ranges and looting may affect these drops. Select a row to draft an explicit override.";
                        case RULES -> "Configured rows are separate from native drops. Each custom row is an independent additional roll.";
                    });
        }
        if (page == Page.GEAR) gear(); else list();
        save = button("Save loot", x() + w() - 148, panel.footerY(), 80, () -> {
            if (!state.error().isEmpty()) { notice = state.error(); return; }
            owner.saveLoot(state.draft()); minecraft.setScreen(back);
        }, "Keep these loot edits in the spawner draft. Main Apply commits them atomically with the other spawner changes.");
        button("Cancel", x() + w() - 64, panel.footerY(), 64, this::onClose, "Discard only this nested loot edit and return.");
        updateEnabled();
    }
    private void gear() {
        int col = (w() - 8) / 2;
        for (Slot slot : Slot.values()) {
            int at = slot.ordinal(), x = x() + (at % 2) * (col + 8), y = panel.y() + 54 + (at / 2) * 27;
            Button label = button(SpawnerEditorScreen.slotName(slot), x, y, Math.max(40, col - 75), () -> {},
                    SpawnerEditorScreen.slotName(slot) + " gear drop chance. Enter 0–100 percent. Native/unchanged values are preserved.");
            label.setMessage(Component.literal(font.plainSubstrByWidth(SpawnerEditorScreen.slotName(slot), label.getWidth() - 6)));
            EditBox field = new EditBox(font, x + col - 71, y, 45, 20, Component.literal(SpawnerEditorScreen.slotName(slot) + " drop percent"));
            field.setMaxLength(12); field.setValue(state.gear(at)); field.setHint(Component.literal("Native"));
            field.setTooltip(Tooltip.create(Component.literal("Percent, 0–100. Native or old values remain unchanged until edited. Reset restores the original.")));
            field.setResponder(value -> { state.gear(at, value); notice = state.error(); });
            addRenderableWidget(field);
            button("R", x + col - 22, y, 22, () -> { state.resetGear(at); notice = ""; rebuildWidgets(); }, "Reset this slot to its original chance, preserving native or legacy behavior.");
        }
    }
    private List<Rule> selectedRules() {
        return state.rules().stream().filter(r -> ruleFilter == 0 || r.kind() == Kind.values()[ruleFilter - 1]).toList();
    }
    private void list() {
        int controlsY = panel.y() + 52;
        if (page == Page.DEFAULTS) {
            button("Inspect selected mob", x(), controlsY, Math.min(148, w() - 90), () -> {
                notice = "Inspecting server loot table...";
                if (!owner.library(LibraryKind.INSPECT, "", 0, view -> {
                    notice = view.message();
                    if (view.success()) state.inspection(view.loot());
                    if (minecraft.screen == this) rebuildWidgets();
                })) notice = "Another server operation is still pending.";
                updateEnabled();
            }, "Read the current selected entity's server loot table. This does not apply any spawner edits.");
        } else {
            button("Add custom row", x(), controlsY, Math.min(140, w() - 170), () -> {
                if (state.rules().size() >= 128) { notice = "At most 128 configured rows."; return; }
                edit(new Rule(SpawnerLootState.newId(), "minecraft:rotten_flesh", 1f, 1, Kind.CUSTOM_ADDITION));
            }, "Add an independent custom roll. It does not remove or replace native drops.");
        }
        if (page == Page.RULES) button(ruleFilter == 0 ? "All rows" : kindLabel(Kind.values()[ruleFilter - 1]),
                x() + Math.min(140, w() - 170) + 4, controlsY, 88,
                () -> { ruleFilter = (ruleFilter + 1) % 4; index = 0; rebuildWidgets(); },
                "Filter configured rows: all, native overrides, independent custom additions or legacy rows.");
        int total = page == Page.DEFAULTS ? state.snapshot().defaults().size() : selectedRules().size();
        index = SpawnerEditorLayout.page(index, total, capacity());
        button("<", x() + w() - 54, controlsY, 24, () -> { index--; rebuildWidgets(); }, "Previous page").active = index > 0;
        button(">", x() + w() - 26, controlsY, 24, () -> { index++; rebuildWidgets(); }, "Next page").active = (index + 1) * capacity() < total;
        for (int row = 0; row < capacity(); row++) {
            int i = index * capacity() + row; if (i >= total) break;
            String item, label, detail; Runnable action;
            if (page == Page.DEFAULTS) {
                var drop = state.snapshot().defaults().get(i); item = drop.itemId();
                label = itemName(item) + " / " + drop.chanceLabel();
                detail = drop.chanceLabel() + "\n" + state.snapshot().note()
                        + "\nNative table for " + state.snapshot().entityId()
                        + "\nSelect to draft an override: replaces matching native item drops and ignores their native conditions.";
                action = () -> edit(new Rule(SpawnerLootState.newId(), drop.itemId(), 1f, 1, Kind.DEFAULT_OVERRIDE));
            } else {
                Rule rule = selectedRules().get(i); item = rule.itemId();
                label = kindLabel(rule.kind()) + ": " + itemName(item) + " x" + rule.count() + " / " + SpawnerLootState.percent(rule.chance()) + "%";
                detail = kindLabel(rule.kind()) + "\nRow ID: " + rule.id() + "\n" + ruleDescription(rule.kind())
                        + "\nChance " + SpawnerLootState.percent(rule.chance()) + "% / Count " + rule.count() + "\nSelect to edit or remove this row only.";
                action = () -> edit(rule);
            }
            Button b = button("     " + font.plainSubstrByWidth(label, w() - 30), x(), panel.y() + 78 + row * 22, w(), action, detail);
            b.setTooltip(null); // The native item tooltip below owns this row.
            rows.add(new Row(b, icon(item), detail));
        }
    }
    private void edit(Rule rule) {
        minecraft.setScreen(new SpawnerLootRuleScreen(owner, this, rule, saved -> {
            if (!state.save(saved)) notice = "At most 128 configured rows.";
        }, () -> state.remove(rule.id())));
    }
    static String kindLabel(Kind kind) {
        return switch(kind) { case DEFAULT_OVERRIDE -> "Override"; case CUSTOM_ADDITION -> "Custom"; case UNKNOWN_CONFIGURED -> "Legacy"; };
    }
    static String ruleDescription(Kind kind) {
        return switch(kind) {
            case DEFAULT_OVERRIDE -> "Replaces all matching native item drops with this configured roll. Native conditions and looting do not apply to the replacement.";
            case CUSTOM_ADDITION -> "Adds one independent roll after native drops. Duplicate item rows remain independent.";
            case UNKNOWN_CONFIGURED -> "Legacy configured behavior retained. It may replace native drops; remove it only deliberately.";
        };
    }
    static ItemStack icon(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.getOptional(key).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }
    static String itemName(String id) { ItemStack stack = icon(id); return stack.isEmpty() ? id : stack.getHoverName().getString(); }
    private void updateEnabled() {
        if (owner.libraryPending()) for (var child : children()) if (child instanceof AbstractWidget widget) widget.active = false;
        if (save != null) save.active = !owner.libraryPending() && state.error().isEmpty();
    }
    @Override public void tick() { super.tick(); updateEnabled(); }
    @Override public void onClose() {
        if (owner.libraryPending()) { notice = "Waiting for the server operation."; return; }
        super.onClose();
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        frame(g, notice.isEmpty() ? "Spawner / " + page.name().toLowerCase(java.util.Locale.ROOT) + " loot" : notice);
        if (page != Page.GEAR) {
            String footer = page == Page.DEFAULTS
                    ? (state.snapshot().entityId().equals(owner.entityId()) ? state.snapshot().note() : "Table is for " + state.snapshot().entityId() + "; Inspect refreshes selected mob.")
                    : "Custom = addition; Override = native replacement.";
            g.drawString(font, font.plainSubstrByWidth(footer, w()), x(), panel.footerY() - 13, 0xFF9CA9B6, false);
            if (mx >= x() && mx < x() + w() && my >= panel.footerY() - 16 && my < panel.footerY())
                g.setTooltipForNextFrame(font, font.split(Component.literal(footer), Math.max(120, width - 32)), mx, my);
            if (rows.isEmpty()) g.drawString(font, page == Page.DEFAULTS ? "No simple item rows; see table note below." : "No configured rows.", x(), panel.y() + 81, 0xFFBBBBBB, false);
        }
        super.render(g, mx, my, delta);
        for (Row row : rows) {
            g.renderItem(row.icon(), row.button().getX() + 3, row.button().getY() + 2);
            if (row.button().isMouseOver(mx, my) || (row.button().isFocused() && minecraft.getLastInputType().isKeyboard())) {
                var lines = row.icon().isEmpty() ? new ArrayList<Component>() : new ArrayList<>(getTooltipFromItem(minecraft, row.icon()));
                for (String line : row.detail().split("\n")) lines.add(Component.literal(line));
                g.setTooltipForNextFrame(font, lines, row.icon().getTooltipImage(), row.icon(), mx, my);
            }
        }
        if (mx >= panel.x() && mx < panel.x() + panel.width() && my >= panel.y() && my < panel.y() + 24 && !notice.isEmpty())
            g.setTooltipForNextFrame(font, font.split(Component.literal(notice), Math.max(120, width - 32)), mx, my);
    }
}
