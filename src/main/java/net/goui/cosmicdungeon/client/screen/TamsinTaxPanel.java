package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.network.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/** Client presentation only. Server owns choices, exact stack snapshots, expiry and payment. */
final class TamsinTaxPanel {
    private TamsinTaxPayloads.View view;
    private int page;
    private static final int ROWS = 5;
    void setView(TamsinTaxPayloads.View next) {
        view = next; page = Math.min(page, Math.max(0, (next.choices().size() - 1) / ROWS));
    }
    private void action(int container, String action, int slot, String token) {
        ModNetwork.sendToServer(new TamsinTaxPayloads.Action(container, action, slot, token));
    }
    void build(Font font, Consumer<Button> add, Runnable rebuild, int x, int y, int container) {
        if (view == null) return;
        if (!view.token().isEmpty()) {
            add.accept(Button.builder(Component.literal("Confirm"), b -> action(container, "confirm", -1, view.token()))
                    .bounds(x + 18, y + 184, 150, 20).build());
            add.accept(Button.builder(Component.literal("Cancel"), b -> action(container, "cancel", -1, ""))
                    .bounds(x + 178, y + 184, 164, 20).build());
        } else {
            for (int row = 0; row < ROWS; row++) {
                int index = page * ROWS + row;
                if (index >= view.choices().size()) break;
                var choice = view.choices().get(index);
                String label = choice.name() + " (" + location(choice.slot()) + ", x" + choice.count() + ")";
                var button = Button.builder(Component.literal(font.plainSubstrByWidth(label, 308)),
                                b -> action(container, "choose", choice.slot(), ""))
                        .bounds(x + 18, y + 68 + row * 23, 324, 20).build();
                button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(label)));
                add.accept(button);
            }
            var previous = Button.builder(Component.literal("<"), b -> { page--; rebuild.run(); })
                    .bounds(x + 18, y + 188, 36, 20).build();
            previous.active = page > 0; add.accept(previous);
            var next = Button.builder(Component.literal(">"), b -> { page++; rebuild.run(); })
                    .bounds(x + 306, y + 188, 36, 20).build();
            next.active = (page + 1) * ROWS < view.choices().size(); add.accept(next);
        }
        add.accept(Button.builder(Component.literal("Later"), b -> action(container, "later", -1, ""))
                .bounds(x + 118, y + 213, 124, 20).build());
    }
    void render(GuiGraphics g, Font font, int x, int y) {
        if (view == null) { g.drawString(font, "Loading...", x + 18, y + 43, 0xFFFFFFFF, false); return; }
        int lineY = y + 38;
        String text = view.token().isEmpty()
                ? "You promised me a cut. Shall we settle up?"
                : "Give Tamsin one " + view.selected() + "? It will be permanently removed. You receive no currency payment.";
        for (var line : font.split(Component.literal(text), 324)) {
            g.drawString(font, line, x + 18, lineY, 0xFFFFFFFF, false); lineY += font.lineHeight + 3;
        }
        if (view.token().isEmpty()) {
            if (view.choices().isEmpty()) g.drawString(font, "Nothing to offer right now.", x + 18, y + 92, 0xFFCCCCCC, false);
            String pages = (page + 1) + " / " + Math.max(1, (view.choices().size() + ROWS - 1) / ROWS);
            g.drawString(font, pages, x + 180 - font.width(pages) / 2, y + 194, 0xFFCCCCCC, false);
        }
    }
    private static String location(int slot) {
        return switch (slot) {
            case 36 -> "feet"; case 37 -> "legs"; case 38 -> "chest"; case 39 -> "head"; case 40 -> "offhand";
            case 41 -> "body"; case 42 -> "saddle";
            default -> "slot " + (slot + 1);
        };
    }
}
