package net.goui.cosmicdungeon.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.goui.cosmicdungeon.client.screen.skills.TheurgistClient;
import net.goui.cosmicdungeon.network.TheurgistPayloads.Target;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

/** Uses the exact current-run/latest-death capabilities; opening and paging never sends an action. */
public final class ResurrectPickerScreen extends Screen {
    private record Row(Target target, Button button) {}
    private final AbstractContainerScreen<?> parent;
    private final net.minecraft.world.inventory.AbstractContainerMenu menu;
    private final long run;
    private final List<Row> rows = new ArrayList<>();
    private List<Target> displayed = List.of();
    private int page;
    private Button previous, next;
    private ResurrectionPresentation.Page currentPage;
    private String notice = "";
    private net.goui.cosmicdungeon.network.TheurgistPayloads.View lastView;
    private boolean lastPending;
    public ResurrectPickerScreen(AbstractContainerScreen<?> parent) {
        super(Component.literal("Resurrect..."));
        this.parent = parent;
        var player = net.minecraft.client.Minecraft.getInstance().player;
        this.menu = parent != null ? parent.getMenu() : player == null ? null : player.inventoryMenu;
        run = TheurgistClient.view().run();
    }
    private int panelWidth() { return Math.max(120, Math.min(360, width - 24)); }
    private int capacity() { return Math.max(1, (height - 130) / 24); }
    private boolean current() {
        return minecraft != null && minecraft.player != null && minecraft.getConnection() != null
                && minecraft.player.isAlive() && menu != null && minecraft.player.containerMenu == menu
                && menu.getCarried().isEmpty() && TheurgistClient.view().run() == run
                && TheurgistClient.canOpenPicker();
    }
    @Override protected void init() {
        displayed = TheurgistClient.view().targets();
        currentPage = ResurrectionPresentation.page(page, displayed.size(), capacity());
        page = currentPage.index();
        rows.clear(); lastView = null;
        int w = panelWidth(), x = (width - w) / 2;
        for (int i = currentPage.start(); i < currentPage.end(); i++) {
            Target captured = displayed.get(i);
            var button = addRenderableWidget(Button.builder(Component.literal(captured.name()), b -> {
                if (!current()) return;
                boolean sent = TheurgistClient.activate(TheurgistClient.actionId(captured));
                notice = sent ? "Resurrection offer requested." : "That offer is no longer available.";
                updateButtons();
            }).bounds(x, 66 + (i - currentPage.start()) * 24, w, 20).build());
            rows.add(new Row(captured, button));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1))
                .bounds(x, height - 52, 28, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1))
                .bounds(x + w - 28, height - 52, 28, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(width / 2 - 50, height - 28, 100, 20).build());
        updateButtons();
    }
    private void changePage(int direction) { page += direction; rebuildWidgets(); }
    private void updateButtons() {
        if (lastView == TheurgistClient.view() && lastPending == TheurgistClient.pending()) return;
        lastView = TheurgistClient.view(); lastPending = TheurgistClient.pending();
        for (var row : rows) {
            var target = TheurgistClient.view().targets().stream()
                    .filter(t -> t.player().equals(row.target().player()) && t.death().equals(row.target().death())).findFirst();
            row.button().active = current() && !TheurgistClient.pending() && target.map(Target::available).orElse(false);
            row.button().setTooltip(Tooltip.create(TheurgistClient.targetTooltip(target.orElse(row.target())).copy()
                    .append(Component.literal("\nOpen member picker: ").withStyle(net.minecraft.ChatFormatting.GRAY))
                    .append(net.goui.cosmicdungeon.client.SkillKeybindClient.hint("theurgist_resurrect"))));
        }
        previous.active = page > 0; next.active = page + 1 < currentPage.pages();
    }
    @Override public void tick() {
        if (!current()) { InventoryModalContext.dismiss(parent); return; }
        // Rebuild only when the target list changes, preserving keyboard focus during countdown updates.
        if (!sameIdentities(displayed, TheurgistClient.view().targets())) rebuildWidgets();
        updateButtons();
    }
    private static boolean sameIdentities(List<Target> left, List<Target> right) {
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++)
            if (!left.get(i).player().equals(right.get(i).player()) || !left.get(i).death().equals(right.get(i).death())
                    || !left.get(i).name().equals(right.get(i).name())) return false;
        return true;
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        int w = panelWidth(), x = (width - w) / 2;
        g.fill(x - 8, 8, x + w + 8, height - 6, 0xF016202C);
        g.drawCenteredString(font, title, width / 2, 16, 0xFFE4C98A);
        g.drawCenteredString(font, "No cost / 3-minute cooldown after success", width / 2, 31, 0xFFDDDDDD);
        g.drawCenteredString(font, ResurrectionPresentation.status(TheurgistClient.view().resurrection()), width / 2, 45, 0xFF90CAF9);
        if (displayed.isEmpty()) g.drawCenteredString(font, "No teammate currently needs resurrection.", width / 2, 72, 0xFFDDDDDD);
        if (!notice.isEmpty()) g.drawCenteredString(font, font.plainSubstrByWidth(notice, w), width / 2, height - 66, 0xFFAAFFAA);
        g.drawCenteredString(font, (page + 1) + " / " + currentPage.pages(), width / 2, height - 46, 0xFFDDDDDD);
        super.render(g, mx, my, partialTick);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() {
        if(current())InventoryModalContext.restore(parent);else InventoryModalContext.dismiss(parent);
    }
}
