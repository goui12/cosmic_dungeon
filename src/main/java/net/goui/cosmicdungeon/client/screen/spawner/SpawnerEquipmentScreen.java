package net.goui.cosmicdungeon.client.screen.spawner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.Slot;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft.Edit;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentRules;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Detached item picker. Buttons select draft data and never grant player inventory items. */
public final class SpawnerEquipmentScreen extends Screen {
    private record Choice(String id, String label, ItemStack icon, Holder<Enchantment> enchantment) {}
    private record Tile(Button button, Choice choice) {}
    private final SpawnerEditorScreen parent;
    private final Slot slot;
    private final ItemStack original, held;
    private final SpawnerEquipmentState state;
    private final List<Choice> items = new ArrayList<>(), books = new ArrayList<>();
    private final List<Tile> tiles = new ArrayList<>();
    private List<Choice> matches = List.of();
    private ItemStack preview = ItemStack.EMPTY;
    private SpawnerEquipmentLayout layout;
    private Button previous, next;
    private boolean catalogReady;
    private int count;

    public SpawnerEquipmentScreen(SpawnerEditorScreen parent, Slot slot, ItemStack original, ItemStack held, Edit edit) {
        super(Component.literal(SpawnerEditorScreen.slotName(slot) + " equipment"));
        this.parent = parent; this.slot = slot; this.original = original.copy(); this.held = held.copy();
        this.state = new SpawnerEquipmentState(edit);
    }
    @Override protected void init() {
        layout = SpawnerEquipmentLayout.fit(width, height);
        if (!catalogReady) {
            for (var item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty()) items.add(new Choice(BuiltInRegistries.ITEM.getKey(item).toString(),
                        stack.getHoverName().getString(), stack, null));
            }
            minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(holder -> {
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK); book.enchant(holder, 1);
                books.add(new Choice(holder.key().location().toString(),
                        Enchantment.getFullname(holder, 1).getString(), book, holder));
            });
            items.sort(Comparator.comparing(Choice::label).thenComparing(Choice::id));
            books.sort(Comparator.comparing(Choice::label).thenComparing(Choice::id));
            catalogReady = true;
        }
        try { preview = resolve(state.edit()); }
        catch (IllegalArgumentException invalid) { state.notice(invalid.getMessage()); }
        int x = layout.x(), w = layout.width(), y = layout.tabsY();
        int modeWidth = Math.min(112, w / 3), tabWidth = (w - modeWidth - 8) / 2;
        button((state.page() == SpawnerEquipmentState.Page.ITEMS ? "> " : "") + "Items", x, y, tabWidth, () -> {
            state.page(SpawnerEquipmentState.Page.ITEMS); rebuildWidgets();
        }, "Browse item icons. Selecting one changes only this nested draft.");
        button((state.page() == SpawnerEquipmentState.Page.BOOKS ? "> " : "") + "Books",
                x + tabWidth + 4, y, tabWidth, () -> {
                    state.page(SpawnerEquipmentState.Page.BOOKS); rebuildWidgets();
                }, "Enchantment books: left +1, right -1, Shift-left normal maximum, Shift-right remove.");
        button(state.edit().advanced() ? "Advanced" : "Normal", x + w - modeWidth, y, modeWidth, () -> {
            if (accept(state.mode(!state.edit().advanced()))) rebuildWidgets();
        }, "Normal enforces slot, item compatibility, maximum levels and conflicts. Advanced allows deliberate exceptions up to level 255. Switching modes never removes edits.");
        EditBox search = new EditBox(font, x, layout.searchY(), w - 90, 20, Component.literal("Search item or enchantment"));
        search.setMaxLength(100); search.setValue(state.query()); search.setHint(Component.literal("Search name or ID..."));
        search.setResponder(value -> { state.query(value); refresh(); });
        addRenderableWidget(search);
        button(state.slotOnly() ? "Slot filter" : "Show all", x + w - 86, layout.searchY(), 86, () -> {
            state.slotOnly(!state.slotOnly()); rebuildWidgets();
        }, "Items: only equipment fitting this slot, or all items. Books: supported by the selected item, or all enchantments. Advanced is required for exceptions.");
        tiles.clear();
        for (int i = 0; i < layout.capacity(); i++) {
            int index = i;
            Button button = addRenderableWidget(Button.builder(Component.empty(), b -> choose(index, 0))
                    .createNarration(supplier -> Component.literal(catalogNarration(index)))
                    .bounds(layout.tileX(i), layout.tileY(i), 20, 20).build());
            tiles.add(new Tile(button, null));
        }
        previous = button("<", x + w - 70, layout.previewY(), 22, () -> {
            state.index(state.index() - 1); refresh();
        }, "Previous page");
        next = button(">", x + w - 22, layout.previewY(), 22, () -> {
            state.index(state.index() + 1); refresh();
        }, "Next page");
        int footer = layout.panel().footerY();
        button("Held copy", x, footer, 76, () -> { if (accept(state.held())) refresh(); },
                "Copy the actual main-hand stack captured when this editor opened, including its name, lore, count and components. Apply checks that the held stack has not changed.")
                .active = !held.isEmpty();
        button("Clear", x + 80, footer, 42, () -> { if (accept(state.clear())) refresh(); }, "Clear this equipment slot in the draft." + (parent.captainHeadFallback(slot)
                        ? " A Captain still wears its ominous banner when Head is empty; disable Captain on the Entity page to remove it." : ""));
        button("Save item", x + w - 142, footer, 78, () -> {
            if (accept(state.edit())) { parent.saveEquipment(slot, state.edit()); minecraft.setScreen(parent); }
        }, "Save the item back to the spawner draft. Only the main Apply button changes the live spawner.");
        button("Cancel", x + w - 60, footer, 60, this::onClose,
                "Return to the spawner editor and discard only this item's unsaved edits.");
        refresh();
    }
    private Button button(String label, int x, int y, int width, Runnable action, String tooltip) {
        Button button = addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, width, 20).build());
        button.setTooltip(Tooltip.create(Component.literal(tooltip)));
        return button;
    }
    private ItemStack resolve(Edit edit) {
        ItemStack result = SpawnerEquipmentRules.resolve(original, held, edit, minecraft.level.registryAccess());
        if (edit.changed() && !edit.advanced() && !SpawnerEquipmentRules.fits(slot, result))
            throw new IllegalArgumentException("This item does not fit " + SpawnerEditorScreen.slotName(slot) + ". Use Advanced for a deliberate exception.");
        return result;
    }
    private boolean accept(Edit edit) {
        try { ItemStack result = resolve(edit); state.accept(edit); preview = result; return true; }
        catch (IllegalArgumentException invalid) { state.notice(invalid.getMessage()); return false; }
    }
    private int level(Choice choice) {
        if (choice.enchantment() == null) return 0;
        var component = preview.is(Items.ENCHANTED_BOOK) ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
        return preview.getOrDefault(component, ItemEnchantments.EMPTY).getLevel(choice.enchantment());
    }
    private void refresh() {
        matches = (state.page() == SpawnerEquipmentState.Page.ITEMS ? items : books).stream()
                .filter(choice -> SpawnerEditorState.matches(state.query(), choice.id(), choice.label()))
                .filter(choice -> !state.slotOnly() || (choice.enchantment() == null
                        ? SpawnerEquipmentRules.fits(slot, choice.icon())
                        : preview.is(Items.ENCHANTED_BOOK) || choice.enchantment().value().isSupportedItem(preview) || level(choice) > 0))
                .toList();
        count = matches.size();
        state.index(SpawnerEditorLayout.page(state.index(), count, layout.capacity()));
        for (int i = 0; i < tiles.size(); i++) {
            Tile old = tiles.get(i);
            int index = state.index() * layout.capacity() + i;
            Choice choice = index < count ? matches.get(index) : null;
            old.button().visible = choice != null;
            tiles.set(i, new Tile(old.button(), choice));
        }
        previous.active = state.index() > 0; next.active = (state.index() + 1) * layout.capacity() < count;
    }
    private String catalogNarration(int index) {
        if (index >= tiles.size() || tiles.get(index).choice() == null) return "Catalog entry";
        Choice choice = tiles.get(index).choice();
        return choice.label() + (choice.enchantment() == null ? ". Select item." : ". Current level " + level(choice) + ". Left increases; right decreases. Shift-left sets normal maximum; Shift-right removes.");
    }
    private void choose(int tile, int mouseButton) {
        Choice choice = tiles.get(tile).choice(); if (choice == null) return;
        if (choice.enchantment() == null) {
            if (mouseButton == 0 && accept(state.item(choice.id()))) refresh();
        } else {
            int target = SpawnerEquipmentState.level(level(choice), choice.enchantment().value().getMaxLevel(),
                    mouseButton, minecraft.hasShiftDown(), state.edit().advanced());
            try { if (accept(state.enchant(choice.id(), target))) refresh(); }
            catch (IllegalArgumentException bounded) { state.notice(bounded.getMessage()); }
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1 && state.page() == SpawnerEquipmentState.Page.BOOKS) {
            for (int i = 0; i < tiles.size(); i++) if (tiles.get(i).button().visible
                    && tiles.get(i).button().isMouseOver(event.x(), event.y())) {
                choose(i, 1); return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override public void onClose() { if (!parent.closed()) minecraft.setScreen(parent); }
    @Override public void tick() {
        if (minecraft.player == null || minecraft.level == null || parent.closed()) parent.finished();
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        var p = layout.panel(); int x = layout.x(), w = layout.width();
        g.fill(p.x(), p.y(), p.x() + p.width(), p.y() + p.height(), 0xF016202C);
        g.fill(p.x(), p.y(), p.x() + p.width(), p.y() + 24, 0xFF293B50);
        String title = state.notice().isEmpty() ? SpawnerEditorScreen.slotName(slot) + " / "
                + (state.edit().advanced() ? "Advanced" : "Normal") + " / item draft" : state.notice();
        g.drawString(font, font.plainSubstrByWidth(title, w), x, p.y() + 8,
                state.notice().isEmpty() ? 0xFFE4C98A : 0xFFFFC680, false);
        if (count == 0) g.drawString(font, "No matching entries", x, layout.gridY() + 6, 0xFFBBBBBB, false);
        g.fill(x, layout.previewY() - 1, x + w - 78, layout.previewY() + 21, 0xFF101822);
        if (!preview.isEmpty()) {
            g.renderItem(preview, x + 2, layout.previewY() + 2);
            g.renderItemDecorations(font, preview, x + 2, layout.previewY() + 2);
        }
        g.drawString(font, font.plainSubstrByWidth(preview.isEmpty() ? "(empty slot)" : preview.getHoverName().getString(), w - 108),
                x + 23, layout.previewY() + 6, 0xFFF0F0F0, false);
        g.drawCenteredString(font, Integer.toString(state.index() + 1), x + w - 35, layout.previewY() + 6, 0xFFBBBBBB);
        super.render(g, mouseX, mouseY, partialTick);
        for (Tile tile : tiles) if (tile.choice() != null) {
            Choice choice = tile.choice(); Button button = tile.button();
            boolean selected = choice.enchantment() != null ? level(choice) > 0 : ItemStack.isSameItemSameComponents(preview, choice.icon());
            g.fill(button.getX() + 1, button.getY() + 1, button.getX() + 19, button.getY() + 19,
                    selected ? 0xFF426B78 : 0xFF354252);
            g.renderItem(choice.icon(), button.getX() + 2, button.getY() + 2);
            if (choice.enchantment() != null && level(choice) > 0) {
                String number = Integer.toString(level(choice));
                g.drawString(font, number, button.getX() + 20 - font.width(number), button.getY() + 12, 0xFFFFFFFF, true);
            }
            if (button.isMouseOver(mouseX, mouseY) || (button.isFocused() && minecraft.getLastInputType().isKeyboard())) {
                var lines = new ArrayList<>(getTooltipFromItem(minecraft, choice.icon()));
                lines.add(Component.literal(choice.id()).withStyle(ChatFormatting.DARK_GRAY));
                if (choice.enchantment() != null) {
                    lines.add(Component.literal("Current: " + level(choice) + " | Normal max: " + choice.enchantment().value().getMaxLevel()).withStyle(ChatFormatting.GRAY));
                    lines.add(Component.literal("Left +1 / Right -1").withStyle(ChatFormatting.YELLOW));
                    lines.add(Component.literal("Shift-left: normal max / Shift-right: remove").withStyle(ChatFormatting.YELLOW));
                    lines.add(Component.literal(state.edit().advanced() ? "Advanced: bounded at level 255." : "Normal: compatibility, limits and conflicts enforced.").withStyle(ChatFormatting.GRAY));
                } else lines.add(Component.literal("Select for this item draft. No inventory items are granted.").withStyle(ChatFormatting.GRAY));
                g.setTooltipForNextFrame(font, lines, choice.icon().getTooltipImage(), choice.icon(), mouseX, mouseY);
            }
        }
        if (mouseX >= x && mouseX < x + w - 78 && mouseY >= layout.previewY() && mouseY < layout.previewY() + 21) {
            if (!preview.isEmpty()) g.setTooltipForNextFrame(font, preview, mouseX, mouseY);
            else g.setTooltipForNextFrame(font, Component.literal("Empty slot. Save item then Apply to clear equipment."
                    + (parent.captainHeadFallback(slot) ? " Captain supplies an ominous banner for an empty Head slot. Disable Captain on the Entity page to remove that fallback." : "")), mouseX, mouseY);
        }
        if (mouseX >= p.x() && mouseX < p.x() + p.width() && mouseY >= p.y() && mouseY < p.y() + 24)
            g.setTooltipForNextFrame(font, Component.literal(title), mouseX, mouseY);
    }
    @Override public boolean isPauseScreen() { return false; }
}
