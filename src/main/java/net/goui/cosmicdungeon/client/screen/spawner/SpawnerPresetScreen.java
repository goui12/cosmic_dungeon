package net.goui.cosmicdungeon.client.screen.spawner;

import java.util.List;
import net.goui.cosmicdungeon.network.SpawnerEditorPayloads.LibraryKind;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** File operations are explicit; loading a file replaces the detached editor draft only. */
final class SpawnerPresetScreen extends SpawnerEditorChildScreen {
    private enum Page { BROWSE, SAVE, SHORTCUTS }
    private Page page = Page.BROWSE;
    private String query = "", selected = "", name = "", notice = "";
    private int index;
    private EditBox searchBox;
    SpawnerPresetScreen(SpawnerEditorScreen owner, Screen back) { super("Spawner preset files", owner, back); }
    private int capacity() { return Math.max(1, (panel.footerY() - panel.y() - 82) / 22); }
    @Override protected void init() {
        panel = SpawnerEditorLayout.fit(width, height);
        int tabWidth = (w() - 8) / 3;
        for (Page value : Page.values()) {
            String label = switch(value) { case BROWSE -> "Browse"; case SAVE -> "Save draft"; case SHORTCUTS -> "5 shortcuts"; };
            button((page == value ? "> " : "") + label, x() + value.ordinal() * (tabWidth + 4), panel.y() + 27, tabWidth,
                    () -> { page = value; index = 0; rebuildWidgets(); },
                    switch(value) {
                        case BROWSE -> "Load a named file into the editor draft, or explicitly delete that file. Neither action changes a live spawner.";
                        case SAVE -> "Save the complete current spawner draft to a named server file. This does not apply it to the world.";
                        case SHORTCUTS -> "Assign or clear the five existing preset shortcuts. Choose a file on Browse first.";
                    });
        }
        switch(page) { case BROWSE -> browse(); case SAVE -> save(); case SHORTCUTS -> shortcuts(); }
        button("Back", x() + w() - 62, panel.footerY(), 62, this::onClose, "Return to the spawner editor tools. File operations already completed are retained.");
        lockPending();
    }
    private void browse() {
        EditBox search = new EditBox(font, x(), panel.y() + 52, w() - 85, 20, Component.literal("Search preset names"));
        search.setMaxLength(64); search.setValue(query); search.setHint(Component.literal("Search presets..."));
        searchBox = search;
        search.setResponder(value -> {
            int cursor = search.getCursorPosition();
            query = value; index = 0; rebuildWidgets();
            searchBox.setCursorPosition(cursor); searchBox.setHighlightPos(cursor); setFocused(searchBox);
        });
        addRenderableWidget(search);
        button("Refresh", x() + w() - 81, panel.y() + 52, 81, () -> request(LibraryKind.LIST, "", 0), "Refresh named server files and the five shortcut assignments.");
        List<String> matches = owner.presetNames().stream().filter(n -> SpawnerEditorState.matches(query, n, n)).toList();
        index = SpawnerEditorLayout.page(index, matches.size(), capacity());
        for (int row = 0; row < capacity(); row++) {
            int at = index * capacity() + row; if (at >= matches.size()) break;
            String entry = matches.get(at);
            button(font.plainSubstrByWidth((entry.equals(selected) ? "> " : "") + entry, w() - 8), x(), panel.y() + 77 + row * 22, w(),
                    () -> { selected = entry; name = entry; rebuildWidgets(); }, entry + "\nSelect this named preset file. Loading requires a separate action.");
        }
        button("<", x(), panel.footerY(), 20, () -> { index--; rebuildWidgets(); }, "Previous preset page").active = index > 0;
        button(">", x() + 23, panel.footerY(), 20, () -> { index++; rebuildWidgets(); }, "Next preset page").active = (index + 1) * capacity() < matches.size();
        button("Load", x() + 47, panel.footerY(), 48, this::load, "Replace the editor draft with the selected file. The live spawner changes only after main Apply.").active = !selected.isBlank();
        button("Delete", x() + 99, panel.footerY(), 56, () -> owner.confirm("Delete preset file?",
                "Delete '" + selected + "' from the server library? The live spawner and current draft remain unchanged.",
                () -> request(LibraryKind.DELETE, selected, 0), this), "Delete only the selected named file after confirmation.").active = !selected.isBlank();
        setInitialFocus(search);
    }
    private void save() {
        EditBox input = new EditBox(font, x(), panel.y() + 64, w(), 20, Component.literal("Preset file name"));
        input.setMaxLength(64); input.setValue(name); input.setHint(Component.literal("Letters, digits, underscore or hyphen"));
        input.setResponder(value -> name = value); addRenderableWidget(input);
        button("Save named draft", x(), panel.footerY(), Math.min(150, w() - 70), () -> {
            if (name.isBlank()) { notice = "Enter a preset name."; return; }
            if (owner.presetNames().stream().anyMatch(existing -> existing.equalsIgnoreCase(name))) owner.confirm("Overwrite preset file?",
                    "Replace the saved file '" + name + "' with the entire current draft? The live spawner is unchanged.",
                    () -> request(LibraryKind.OVERWRITE, name, 0), this);
            else request(LibraryKind.SAVE, name, 0);
        }, "Save the complete current editor draft, including equipment, loot and preserved native spawner data. Existing names require explicit overwrite confirmation.");
    }
    private void shortcuts() {
        int col = (w() - 8) / 2;
        for (int i = 0; i < 5; i++) {
            int slot = i + 1, x = x() + i % 2 * (col + 8), y = panel.y() + 66 + i / 2 * 23;
            String bound = owner.shortcuts().size() >= slot ? owner.shortcuts().get(i) : "";
            String text = slot + ": " + (bound.isBlank() ? "(empty)" : bound);
            button(font.plainSubstrByWidth(text, col - 31), x, y, col - 26, () -> {
                if (selected.isBlank()) { notice = "Select a preset on Browse first."; return; }
                request(LibraryKind.BIND, selected, slot);
            }, "Shortcut " + slot + ": " + (bound.isBlank() ? "unassigned" : bound)
                    + "\nAssign the selected file: " + (selected.isBlank() ? "(choose on Browse)" : selected));
            button("X", x + col - 22, y, 22, () -> request(LibraryKind.BIND, "", slot), "Clear shortcut " + slot + " without deleting its saved file.");
        }
    }
    private void load() {
        if (selected.isBlank()) return;
        Runnable load = () -> request(LibraryKind.LOAD, selected, 0);
        if (owner.dirty()) owner.confirm("Replace unapplied spawner draft?",
                "Load '" + selected + "' and discard the current unapplied edits? The live spawner remains unchanged until Apply.",
                load, this);
        else load.run();
    }
    private void request(LibraryKind kind, String name, int slot) {
        notice = "Waiting for server...";
        if (!owner.library(kind, name, slot, view -> {
            notice = view.message();
            if (!owner.presetNames().contains(selected)) selected = "";
            if (minecraft.screen == this) rebuildWidgets();
        })) notice = "A server operation is pending or the connection is unavailable.";
        lockPending();
    }
    private void lockPending() {
        if (owner.libraryPending()) for (var child : children()) if (child instanceof AbstractWidget widget) {
            widget.active = false;
            if (widget instanceof EditBox field) field.setEditable(false);
        }
    }
    @Override public void tick() { super.tick(); lockPending(); }
    @Override public void onClose() {
        if (owner.libraryPending()) { notice = "Waiting for the server file operation."; return; }
        super.onClose();
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        frame(g, notice.isEmpty() ? "Spawner / preset files" : notice);
        if (page == Page.SAVE) {
            g.drawString(font, "File name", x(), panel.y() + 53, 0xFFDDE5EB, false);
            var lines = font.split(Component.literal("Saves the draft to a file. Main Apply is still required to change the live spawner."), w());
            for (int i = 0; i < Math.min(3, lines.size()); i++) g.drawString(font, lines.get(i), x(), panel.y() + 92 + i * 10, 0xFF9CA9B6, false);
        } else if (page == Page.SHORTCUTS) {
            g.drawString(font, font.plainSubstrByWidth("Assign: " + (selected.isBlank() ? "choose a file on Browse" : selected), w()), x(), panel.y() + 54, 0xFF9CA9B6, false);
        } else if (owner.presetNames().isEmpty()) {
            g.drawString(font, "No named files. Use Save draft.", x(), panel.y() + 81, 0xFF9CA9B6, false);
        }
        super.render(g, mx, my, delta);
        if (mx >= panel.x() && mx < panel.x() + panel.width() && my >= panel.y() && my < panel.y() + 24 && !notice.isEmpty())
            g.setTooltipForNextFrame(font, font.split(Component.literal(notice), Math.max(120, width - 32)), mx, my);
    }
}
