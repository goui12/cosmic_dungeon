package net.goui.cosmicdungeon.client.advancements;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/** Presents the existing server-supplied advancement tree and progress; never awards achievements. */
public final class CosmicAdvancementScreen extends Screen implements ClientAdvancements.Listener {
    private static final ResourceLocation COSMIC_ROOT = ResourceLocation.parse("cosmicdungeon:root");
    private static final ResourceLocation BLOOMS_GROUP = ResourceLocation.parse("cosmicdungeon:blooms");
    private final ClientAdvancements advancements;
    @Nullable private final Screen previous;
    private final Map<ResourceLocation, AdvancementNode> roots = new LinkedHashMap<>();
    private final Map<ResourceLocation, AdvancementNode> nodes = new LinkedHashMap<>();
    private final Map<ResourceLocation, AdvancementProgress> progress = new LinkedHashMap<>();
    private AdvancementGalleryLayout layout;
    private ResourceLocation selectedRoot, selectedEntry;
    private List<AdvancementNode> entries = List.of();
    private int page, descriptionScroll, descriptionMaxScroll;
    private boolean listening, dirty;
    private long completedCount;

    public CosmicAdvancementScreen(ClientAdvancements advancements, @Nullable Screen previous) {
        super(label("title"));
        this.advancements = advancements;
        this.previous = previous;
    }

    private static Component label(String key, Object... args) {
        return Component.translatable("screen.cosmicdungeon.advancements." + key, args);
    }

    @Override
    protected void init() {
        layout = AdvancementGalleryLayout.forViewport(width, height);
        if (!listening) {
            listening = true;
            advancements.setListener(this);
        }
        ensureRoot();
        buildWidgets();
        dirty = false;
    }

    private void ensureRoot() {
        if (selectedRoot == null || !roots.containsKey(selectedRoot)) {
            selectedRoot = roots.containsKey(COSMIC_ROOT) ? COSMIC_ROOT : roots.keySet().stream().findFirst().orElse(null);
            if (selectedRoot != null) advancements.setSelectedTab(roots.get(selectedRoot).holder(), true);
        }
    }

    private void buildWidgets() {
        clearWidgets();
        entries = nodes.values().stream()
                .filter(node -> node.parent() != null && node.root().holder().id().equals(selectedRoot))
                .filter(node -> !node.holder().id().equals(BLOOMS_GROUP))
                .filter(node -> node.advancement().display().filter(display -> !display.isHidden() || earned(node)).isPresent())
                .sorted(Comparator.comparing(node -> display(node).getTitle().getString(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        completedCount = entries.stream().filter(this::earned).count();
        page = Math.max(0, Math.min(page, pageCount() - 1));
        if (entries.stream().noneMatch(node -> node.holder().id().equals(selectedEntry))) {
            selectedEntry = entries.isEmpty() ? null : entries.getFirst().holder().id();
            descriptionScroll = 0;
        }
        var p = layout.panel();
        Button left = addRenderableWidget(Button.builder(Component.literal("<"), b -> chapter(-1))
                .bounds(p.x() + 12, p.y() + 32, 20, 20).build());
        Button right = addRenderableWidget(Button.builder(Component.literal(">"), b -> chapter(1))
                .bounds(p.right() - 32, p.y() + 32, 20, 20).build());
        left.active = right.active = roots.size() > 1;
        int start = page * layout.pageSize(), end = Math.min(entries.size(), start + layout.pageSize());
        for (int i = start; i < end; i++) {
            int cell = i - start;
            int x = layout.grid().x() + cell % layout.columns() * (layout.tileWidth() + AdvancementGalleryLayout.GAP);
            int y = layout.grid().y() + cell / layout.columns() * (AdvancementGalleryLayout.TILE_HEIGHT + AdvancementGalleryLayout.GAP);
            addRenderableWidget(new AchievementTile(x, y, layout.tileWidth(), entries.get(i)));
        }
        int footerY = p.bottom() - 25;
        Button back = addRenderableWidget(Button.builder(Component.literal("<"), b -> turnPage(-1))
                .bounds(p.x() + 12, footerY, 20, 18).build());
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), b -> turnPage(1))
                .bounds(p.x() + 94, footerY, 20, 18).build());
        back.active = page > 0;
        next.active = page + 1 < pageCount();
        addRenderableWidget(Button.builder(label("close"), b -> onClose()).bounds(p.right() - 82, footerY, 70, 18).build());
    }

    private int pageCount() { return Math.max(1, (entries.size() + layout.pageSize() - 1) / layout.pageSize()); }
    private boolean earned(AdvancementNode node) {
        AdvancementProgress value = progress.get(node.holder().id());
        return value != null && value.isDone();
    }
    private DisplayInfo display(AdvancementNode node) { return node.advancement().display().orElseThrow(); }

    private void chapter(int delta) {
        List<ResourceLocation> keys = new ArrayList<>(roots.keySet());
        if (keys.isEmpty()) return;
        int index = Math.floorMod(keys.indexOf(selectedRoot) + delta, keys.size());
        advancements.setSelectedTab(roots.get(keys.get(index)).holder(), true);
    }

    private void turnPage(int delta) {
        int changed = Math.max(0, Math.min(pageCount() - 1, page + delta));
        if (changed != page) { page = changed; buildWidgets(); }
    }

    @Override public void tick() {
        if (dirty) { ensureRoot(); buildWidgets(); dirty = false; }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xE0060913);
        var p = layout.panel();
        AdvancementStarfield.draw(g, p);
        AdvancementStarfield.border(g, p, 0xFFC3A46B);
        g.fill(p.x() + 12, p.y() + 28, p.right() - 12, p.y() + 29, 0x997C6C55);
        g.drawString(font, title, p.x() + 16, p.y() + 13, 0xFFF3DFB1, false);
        Component count = label("progress", completedCount, entries.size());
        g.drawString(font, count, p.right() - 16 - font.width(count), p.y() + 13, 0xFFAFCAE0, false);
        AdvancementNode root = roots.get(selectedRoot);
        if (root != null) {
            Component chapterTitle = display(root).getTitle();
            g.drawCenteredString(font, font.plainSubstrByWidth(chapterTitle.getString(), p.width() - 96),
                    p.x() + p.width() / 2, p.y() + 38, 0xFFD4DDEF);
        }
        if (entries.isEmpty()) drawWrapped(g, label("empty"), layout.grid().x() + 8, layout.grid().y() + 12, layout.grid().width() - 16, 0xFFC0CBDF);
        drawDetails(g);
        g.drawCenteredString(font, label("page", page + 1, pageCount()), p.x() + 63, p.bottom() - 20, 0xFFBAC9DF);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawDetails(GuiGraphics g) {
        var box = layout.details();
        g.fill(box.x(), box.y(), box.right(), box.bottom(), 0xDD0A1225);
        AdvancementStarfield.border(g, box, 0xFF536987);
        AdvancementNode selected = nodes.get(selectedEntry);
        if (selected == null || selected.advancement().display().isEmpty()) {
            drawWrapped(g, label("details"), box.x() + 10, box.y() + 10, box.width() - 20, 0xFFB8C6DC);
            return;
        }
        DisplayInfo info = display(selected);
        g.fill(box.x() + 8, box.y() + 8, box.x() + 32, box.y() + 32, 0xFF24354A);
        g.renderItem(info.getIcon(), box.x() + 12, box.y() + 12);
        boolean compact = layout.panel().width() < 560;
        List<FormattedCharSequence> titleLines = font.split(info.getTitle(), box.width() - 50);
        int titleCount = compact ? Math.min(2, titleLines.size()) : titleLines.size();
        for (int i = 0; i < titleCount; i++)
            g.drawString(font, titleLines.get(i), box.x() + 40, box.y() + 8 + i * 11, 0xFFF1D99E, false);
        int titleBottom = box.y() + 8 + titleCount * 11;
        int top;
        if (compact) {
            // Status is already present on the selected tile; reserve this space for readable text.
            top = Math.max(box.y() + 34, titleBottom + 4);
        } else {
            int statusY = Math.max(box.y() + 36, titleBottom + 3);
            g.drawString(font, label(earned(selected) ? "earned" : "unfinished"), box.x() + 10, statusY,
                    earned(selected) ? 0xFFDBBF74 : 0xFF95A9C3, false);
            top = statusY + 15;
        }
        int bottom = box.bottom() - 8;
        List<FormattedCharSequence> lines = font.split(info.getDescription(), box.width() - 20);
        boolean overflow = lines.size() * 11 > bottom - top;
        if (overflow) bottom -= 11;
        int visible = Math.max(1, (bottom - top) / 11);
        descriptionMaxScroll = Math.max(0, lines.size() - visible);
        descriptionScroll = Math.min(descriptionScroll, descriptionMaxScroll);
        g.enableScissor(box.x() + 1, top, box.right() - 1, Math.max(top + 1, bottom));
        for (int i = descriptionScroll; i < lines.size(); i++) {
            int y = top + (i - descriptionScroll) * 11;
            if (y >= bottom) break;
            g.drawString(font, lines.get(i), box.x() + 10, y, 0xFFCAD4E6, false);
        }
        g.disableScissor();
        if (descriptionMaxScroll > 0) g.drawString(font, label("scroll_details"), box.x() + 10, box.bottom() - 12, 0xFF8BA4C8, false);
    }

    private int drawWrapped(GuiGraphics g, Component text, int x, int y, int maxWidth, int color) {
        for (FormattedCharSequence line : font.split(text, maxWidth)) {
            g.drawString(font, line, x, y, color, false);
            y += 11;
        }
        return y;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (layout.details().contains(x, y)) {
            descriptionScroll = Math.max(0, Math.min(descriptionMaxScroll, descriptionScroll + (scrollY < 0 ? 1 : -1)));
            return true;
        }
        if (layout.grid().contains(x, y) && scrollY != 0) { turnPage(scrollY < 0 ? 1 : -1); return true; }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft.options.keyAdvancements.matches(event)) { onClose(); return true; }
        return super.keyPressed(event);
    }
    @Override public void onClose() { minecraft.setScreen(previous); }
    @Override public void removed() {
        advancements.setListener(null);
        listening = false;
        var connection = minecraft.getConnection();
        if (connection != null) connection.send(ServerboundSeenAdvancementsPacket.closedScreen());
    }

    @Override public void onAddAdvancementRoot(AdvancementNode node) {
        if (node.advancement().display().isPresent()) roots.put(node.holder().id(), node);
        dirty = true;
    }
    @Override public void onRemoveAdvancementRoot(AdvancementNode node) {
        roots.remove(node.holder().id());
        dirty = true;
    }
    @Override public void onAddAdvancementTask(AdvancementNode node) {
        nodes.put(node.holder().id(), node);
        dirty = true;
    }
    @Override public void onRemoveAdvancementTask(AdvancementNode node) {
        nodes.remove(node.holder().id());
        progress.remove(node.holder().id());
        dirty = true;
    }
    @Override public void onUpdateAdvancementProgress(AdvancementNode node, AdvancementProgress value) {
        progress.put(node.holder().id(), value);
        dirty = true;
    }
    @Override public void onSelectedTabChanged(@Nullable AdvancementHolder tab) {
        ResourceLocation next = tab == null ? null : tab.id();
        if (next == null || !next.equals(selectedRoot)) {
            selectedRoot = next;
            selectedEntry = null;
            page = descriptionScroll = 0;
            dirty = true;
        }
    }
    @Override public void onAdvancementsCleared() {
        roots.clear(); nodes.clear(); progress.clear();
        selectedRoot = selectedEntry = null;
        page = descriptionScroll = 0;
        dirty = true;
    }

    private final class AchievementTile extends Button {
        private final AdvancementNode node;
        private final List<FormattedCharSequence> lines;

        AchievementTile(int x, int y, int w, AdvancementNode node) {
            super(x, y, w, AdvancementGalleryLayout.TILE_HEIGHT,
                    display(node).getTitle().copy().append(". ").append(display(node).getDescription()),
                    button -> { selectedEntry = node.holder().id(); descriptionScroll = 0; }, DEFAULT_NARRATION);
            this.node = node;
            lines = font.split(display(node).getTitle(), w - 48);
            setTooltip(Tooltip.create(display(node).getTitle().copy().append("\n").append(display(node).getDescription())));
        }

        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean done = earned(node), selected = node.holder().id().equals(selectedEntry);
            int x = getX(), y = getY();
            g.fillGradient(x, y, x + getWidth(), y + getHeight(), done ? 0xE72F3040 : 0xE7132036, 0xF00B1326);
            var rect = new AdvancementGalleryLayout.Rect(x, y, getWidth(), getHeight());
            AdvancementStarfield.border(g, rect, isHoveredOrFocused() || selected ? 0xFFF0D69B : done ? 0xFFA68D5D : 0xFF425A79);
            g.fill(x + 7, y + 9, x + 35, y + 37, done ? 0xFF594B32 : 0xFF22354F);
            g.renderItem(display(node).getIcon(), x + 13, y + 15);
            for (int i = 0; i < Math.min(3, lines.size()); i++) g.drawString(font, lines.get(i), x + 42, y + 9 + i * 10, 0xFFE2E7F0, false);
            g.drawString(font, label(done ? "earned" : "unfinished"), x + 9, y + getHeight() - 15,
                    done ? 0xFFE2C681 : 0xFF8FA6C6, false);
        }
    }
}
