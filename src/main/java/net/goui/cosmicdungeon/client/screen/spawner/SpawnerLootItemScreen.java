package net.goui.cosmicdungeon.client.screen.spawner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The loot picker selects registry identity only; it does not clone or grant inventory items. */
final class SpawnerLootItemScreen extends SpawnerEditorChildScreen {
    private record Choice(String id, ItemStack stack) {}
    private record Tile(Button button, Choice choice) {}
    private final Consumer<String> selected;
    private final List<Choice> catalog = new ArrayList<>();
    private final List<Tile> tiles = new ArrayList<>();
    private SpawnerEquipmentLayout layout;
    private String query = "";
    private int page, count;
    private EditBox searchBox;
    SpawnerLootItemScreen(SpawnerEditorScreen owner, Screen back, Consumer<String> selected) {
        super("Choose a loot item", owner, back); this.selected = selected;
        for (var item : BuiltInRegistries.ITEM) {
            var stack = new ItemStack(item);
            if (!stack.isEmpty()) catalog.add(new Choice(BuiltInRegistries.ITEM.getKey(item).toString(), stack));
        }
        catalog.sort(Comparator.comparing((Choice c) -> c.stack().getHoverName().getString()).thenComparing(Choice::id));
    }
    @Override protected void init() {
        layout = SpawnerEquipmentLayout.fit(width, height); panel = layout.panel(); tiles.clear();
        EditBox search = new EditBox(font, x(), panel.y() + 30, w(), 20, Component.literal("Search loot items"));
        search.setMaxLength(100); search.setValue(query); search.setHint(Component.literal("Search item name or ID..."));
        searchBox = search;
        search.setResponder(value -> {
            int cursor = search.getCursorPosition();
            query = value; page = 0; rebuildWidgets();
            searchBox.setCursorPosition(cursor); searchBox.setHighlightPos(cursor); setFocused(searchBox);
        });
        addRenderableWidget(search);
        List<Choice> matches = catalog.stream().filter(c -> SpawnerEditorState.matches(query, c.id(), c.stack().getHoverName().getString())).toList();
        int columns = Math.max(1, w() / 22), rows = Math.max(1, (panel.footerY() - panel.y() - 60) / 22), capacity = columns * rows;
        count = matches.size(); page = SpawnerEditorLayout.page(page, count, capacity);
        for (int i = 0; i < capacity; i++) {
            int index = page * capacity + i; if (index >= count) break;
            Choice choice = matches.get(index);
            Button button = addRenderableWidget(Button.builder(Component.empty(), b -> { selected.accept(choice.id()); minecraft.setScreen(back); })
                    .createNarration(supplier -> Component.literal(choice.stack().getHoverName().getString() + ". Select loot item."))
                    .bounds(x() + i % columns * 22, panel.y() + 55 + i / columns * 22, 20, 20).build());
            tiles.add(new Tile(button, choice));
        }
        button("<", x(), panel.footerY(), 24, () -> { page--; rebuildWidgets(); }, "Previous item page").active = page > 0;
        button(">", x() + 65, panel.footerY(), 24, () -> { page++; rebuildWidgets(); }, "Next item page").active = (page + 1) * capacity < count;
        button("Cancel", x() + w() - 64, panel.footerY(), 64, this::onClose, "Keep the current row item.");
        setInitialFocus(search);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        frame(g, "Choose a loot item / " + count + " matches");
        g.drawCenteredString(font, Integer.toString(page + 1), x() + 45, panel.footerY() + 6, 0xFFBBBBBB);
        super.render(g, mx, my, delta);
        for (Tile tile : tiles) {
            g.renderItem(tile.choice().stack(), tile.button().getX() + 2, tile.button().getY() + 2);
            if (tile.button().isMouseOver(mx, my) || (tile.button().isFocused() && minecraft.getLastInputType().isKeyboard()))
                g.setTooltipForNextFrame(font, tile.choice().stack(), mx, my);
        }
    }
}
