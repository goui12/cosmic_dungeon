package net.goui.cosmicdungeon.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.goui.cosmicdungeon.client.HelpMenuKeybindClient;
import net.goui.cosmicdungeon.client.SkillKeybindClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import static net.goui.cosmicdungeon.client.screen.HelpRichTextRenderer.*;

/** A read-only guide: all interactions navigate/scroll; no container or gameplay action is attached. */
public final class HelpMenuScreen extends Screen {
    private static final int SCROLL_STEP = 18;
    private static final Component USAGE = Component.literal("Tab / Shift+Tab: focus. Enter: open a link. Wheel or Page Up / Page Down: scroll. Alt+Left or Backspace: back. Escape or your Help key: close. Item tiles are read-only examples.");
    private enum Area { BACK, HOME, CLOSE, NAV, CONTENT }
    private record Focus(Area area, int index) {}
    private record NavRow(HelpMenuContent.HelpNode node, List<FormattedCharSequence> lines, int y, int height) {}
    private final Screen parent;
    private final HelpGuideNavigation navigation = new HelpGuideNavigation();
    private final Map<String, HelpScrollPane> contentScrolls = new HashMap<>(), navScrolls = new HashMap<>();
    private HelpMenuGeometry geometry = HelpMenuGeometry.centered(0, 0);
    private List<HelpMenuContent.HelpBlock> measuredBlocks;
    private HelpRichTextRenderer.Layout document;
    private List<NavRow> rows = List.of();
    private List<Focus> focusOrder = List.of();
    private int focused = -1;

    public HelpMenuScreen() { this(null); }
    private HelpMenuScreen(Screen parent) {
        super(Component.translatable("screen.cosmicdungeon.help_menu"));
        this.parent = parent;
    }
    public static HelpMenuScreen forClass(Screen parent, String classId) {
        var screen = new HelpMenuScreen(parent);
        screen.navigation.open(HelpMenuContent.CLASSES.id());
        screen.navigation.open(HelpMenuContent.classGuide(classId).id());
        return screen;
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override protected void init() {
        super.init();
        geometry = HelpMenuGeometry.centered(width, height);
        measuredBlocks = null;
        rebuild();
    }
    private HelpScrollPane contentScroll() { return contentScrolls.computeIfAbsent(navigation.page().id(), id -> new HelpScrollPane()); }
    private HelpScrollPane navScroll() { return navScrolls.computeIfAbsent(navigation.directory().page().id(), id -> new HelpScrollPane()); }
    private void ensureLayout() {
        var blocks = ClassResourceHelp.blocks(navigation.page());
        if (blocks != measuredBlocks) rebuild();
    }
    private void rebuild() {
        measuredBlocks = ClassResourceHelp.blocks(navigation.page());
        document = HelpRichTextRenderer.layout(font, measuredBlocks, geometry.contentViewport().w());
        contentScroll().update(document.height(), geometry.contentViewport().h());
        var nextRows = new ArrayList<NavRow>();
        int y = 0;
        for (var node : navigation.children()) {
            var wrapped = font.split(node.page().title(), Math.max(1, geometry.navViewport().w() - 8));
            int height = Math.max(22, wrapped.size() * (font.lineHeight + 2) + 8);
            nextRows.add(new NavRow(node, wrapped, y, height));
            y += height + 3;
        }
        rows = List.copyOf(nextRows);
        navScroll().update(Math.max(0, y - 3), geometry.navViewport().h());
        var order = new ArrayList<Focus>();
        if (navigation.canBack()) order.add(new Focus(Area.BACK, 0));
        order.add(new Focus(Area.HOME, 0));
        order.add(new Focus(Area.CLOSE, 0));
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).node().page().enabled()) order.add(new Focus(Area.NAV, i));
        for (int i = 0; i < document.targets().size(); i++) order.add(new Focus(Area.CONTENT, i));
        focusOrder = List.copyOf(order);
        focused = -1;
    }
    private void open(String id) {
        if (navigation.open(id)) { rebuild(); triggerImmediateNarration(false); }
    }
    private void back() {
        if (navigation.back()) { rebuild(); triggerImmediateNarration(false); }
    }
    @Override public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        ensureLayout();
        var window = geometry.window();
        var theme = HelpMenuArtwork.forPage(navigation.page().id());
        if (window.w() > 0 && window.h() > 0) {
            var crop = HelpMenuArtwork.crop(window.w(), window.h());
            g.blit(theme.texture(), window.x(), window.y(), window.right(), window.bottom(),
                    (float) crop.u(), (float) (crop.u() + crop.width()), (float) crop.v(), (float) (crop.v() + crop.height()));
        }
        fill(g, window, HelpMenuArtwork.TEXT_SCRIM);
        border(g, window, 0xFF786846);
        renderHeader(g, theme);
        renderControl(g, geometry.back(), "Back", Area.BACK, navigation.canBack(), mouseX, mouseY);
        renderControl(g, geometry.home(), "Home", Area.HOME, true, mouseX, mouseY);
        renderControl(g, geometry.close(), "×", Area.CLOSE, true, mouseX, mouseY);
        renderNavigation(g, mouseX, mouseY);
        var hovered = contentTarget(mouseX, mouseY);
        var focus = focus();
        var focusedTarget = focus != null && focus.area() == Area.CONTENT ? document.targets().get(focus.index()) : null;
        HelpRichTextRenderer.render(g, font, document, geometry.contentViewport(), contentScroll().offset(), hovered, focusedTarget);
        renderTrack(g, geometry.navTrack(), navScroll());
        renderTrack(g, geometry.contentTrack(), contentScroll());
        var footer = geometry.footer();
        clip(g, footer);
        g.drawString(font, "Tab: focus  ·  Enter: open  ·  Wheel / PgUp PgDn: scroll", footer.x(), footer.y(), 0xFFBFC8CF, false);
        g.disableScissor();
        super.render(g, mouseX, mouseY, partialTick);
        // The existing final tooltip pass flushes after all overlays; schedule after every scissor closes.
        tooltip(g, mouseX, mouseY, hovered, focusedTarget);
    }
    private void renderHeader(GuiGraphics g, HelpMenuArtwork.Theme theme) {
        var header = geometry.header();
        clip(g, header);
        g.drawString(font, Component.literal("COSMIC DUNGEON").withStyle(ChatFormatting.BOLD), header.x(), header.y() + 4, HEADING, false);
        g.disableScissor();
        if (theme.emblem() != null && geometry.width() >= 250) {
            int x = geometry.close().x() - 25;
            g.blit(RenderPipelines.GUI_TEXTURED, theme.emblem(), x, geometry.top() + 5, 0, 0, 22, 22, 32, 32, 32, 32);
        }
        var title = geometry.pageTitle();
        clip(g, title);
        int y = title.y();
        for (var line : font.split(navigation.page().title().copy().withStyle(ChatFormatting.BOLD), Math.max(1, title.w()))) {
            g.drawString(font, line, title.x(), y, HEADING, false);
            y += font.lineHeight + 2;
        }
        g.disableScissor();
    }
    private void renderControl(GuiGraphics g, HelpMenuGeometry.Rect r, String text, Area area, boolean enabled, int mx, int my) {
        boolean focus = focus() != null && focus().area() == area;
        fill(g, r, enabled && (r.contains(mx, my) || focus) ? 0xFF304B62 : 0xB0202730);
        if (focus) border(g, r, LINK);
        clip(g, r);
        g.drawString(font, text, r.x() + Math.max(0, (r.w() - font.width(text)) / 2), r.y() + Math.max(0, (r.h() - font.lineHeight) / 2), enabled ? BODY : 0xFFB0B0B0, false);
        g.disableScissor();
    }
    private void renderNavigation(GuiGraphics g, int mx, int my) {
        var viewport = geometry.navViewport();
        clip(g, viewport);
        try {
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                var r = navBounds(i);
                if (!r.intersects(viewport)) continue;
                boolean selected = row.node().page() == navigation.page();
                boolean focus = focus() != null && focus().area() == Area.NAV && focus().index() == i;
                boolean hover = viewport.contains(mx, my) && r.contains(mx, my);
                fill(g, r, selected ? 0xED403821 : hover || focus ? 0xED263C50 : 0x80121920);
                if (focus) border(g, r, LINK);
                if (selected) g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), HEADING);
                int y = r.y() + 4;
                for (var line : row.lines()) {
                    g.drawString(font, line, r.x() + 4, y, row.node().page().enabled() ? BODY : 0xFFB0B0B0, false);
                    y += font.lineHeight + 2;
                }
            }
        } finally { g.disableScissor(); }
    }
    private void renderTrack(GuiGraphics g, HelpMenuGeometry.Rect track, HelpScrollPane scroll) {
        if (!scroll.canScroll()) return;
        fill(g, track, 0xFF303842);
        fill(g, scroll.thumb(track), 0xFFC3B085);
    }
    private HelpMenuGeometry.Rect navBounds(int index) {
        var viewport = geometry.navViewport();
        var row = rows.get(index);
        return new HelpMenuGeometry.Rect(viewport.x(), viewport.y() + row.y() - navScroll().offset(), viewport.w(), row.height());
    }
    private HelpRichTextRenderer.Target contentTarget(double x, double y) {
        var viewport = geometry.contentViewport();
        return document.hitInViewport(viewport, contentScroll().offset(), x, y);
    }
    private void tooltip(GuiGraphics g, int mx, int my, Target hovered, Target focusedTarget) {
        // A hovered control wins over the previous keyboard item's tooltip.
        if (geometry.footer().contains(mx, my)) { tip(g, USAGE, mx, my); return; }
        if (geometry.pageTitle().contains(mx, my)) { tip(g, navigation.page().title(), mx, my); return; }
        if (geometry.back().contains(mx, my)) { tip(g, Component.literal("Back · Alt+Left / Backspace"), mx, my); return; }
        if (geometry.home().contains(mx, my)) { tip(g, Component.literal("Return to Getting Started"), mx, my); return; }
        if (geometry.close().contains(mx, my)) { tip(g, Component.literal("Close · Escape or your Help key"), mx, my); return; }
        if (geometry.navViewport().contains(mx, my)) {
            for (int i = 0; i < rows.size(); i++) if (navBounds(i).contains(mx, my)) { tip(g, rows.get(i).node().page().title(), mx, my); return; }
        }
        Target target = hovered != null ? hovered : focusedTarget;
        if (target != null) {
            if (hovered == null) {
                var v = geometry.contentViewport();
                mx = Math.min(v.right() - 1, v.x() + target.bounds().x());
                my = Math.max(v.y(), Math.min(v.bottom() - 1, v.y() + target.bounds().y() - contentScroll().offset()));
            }
            if (target.isItem()) { g.setTooltipForNextFrame(font, target.item(), mx, my); return; }
            if (target.keyId() != null) {
                tip(g, Component.literal("Current hotkey: ").append(SkillKeybindClient.hint(target.keyId())).append("\nChange it in Options → Controls → Cosmic Dungeon. This guide never activates skills."), mx, my);
                return;
            }
            var page = navigation.resolve(target.pageId());
            if (page != null) tip(g, Component.literal("Open: ").append(page.title()).append("\nBack returns to this page and its scroll position."), mx, my);
            return;
        }

    }
    private void tip(GuiGraphics g, Component text, int x, int y) { g.setTooltipForNextFrame(font, font.split(text, Math.max(80, Math.min(250, width - 24))), x, y); }
    private static void clip(GuiGraphics g, HelpMenuGeometry.Rect r) { g.enableScissor(r.x(), r.y(), r.right(), r.bottom()); }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        ensureLayout();
        focused = -1;
        if (event.button() == 0) {
            if (geometry.close().contains(event.x(), event.y())) { onClose(); return true; }
            if (geometry.back().contains(event.x(), event.y())) { back(); return true; }
            if (geometry.home().contains(event.x(), event.y())) { open(HelpMenuContent.GET_STARTED.id()); return true; }
            if (clickTrack(event.x(), event.y(), geometry.navTrack(), navScroll()) || clickTrack(event.x(), event.y(), geometry.contentTrack(), contentScroll())) return true;
            if (geometry.navViewport().contains(event.x(), event.y())) {
                for (int i = 0; i < rows.size(); i++) if (navBounds(i).contains(event.x(), event.y())) { open(rows.get(i).node().page().id()); return true; }
            }
            var target = contentTarget(event.x(), event.y());
            if (target != null) { if (target.pageId() != null) open(target.pageId()); return true; }
        }
        return geometry.window().contains(event.x(), event.y()) || super.mouseClicked(event, isDoubleClick);
    }
    private boolean clickTrack(double x, double y, HelpMenuGeometry.Rect track, HelpScrollPane scroll) {
        if (!track.contains(x, y) || !scroll.canScroll()) return false;
        var thumb = scroll.thumb(track);
        scroll.scrollTo(Math.round((y - track.y() - thumb.h() / 2.0) / Math.max(1, track.h() - thumb.h()) * scroll.maxOffset()));
        return true;
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (vertical == 0 || !Double.isFinite(vertical)) return false;
        ensureLayout();
        int amount = (int) Math.copySign(Math.max(1, Math.min(Integer.MAX_VALUE, Math.round(Math.abs(vertical) * SCROLL_STEP))), -vertical);
        if (geometry.navViewport().contains(x, y) || geometry.navTrack().contains(x, y)) { navScroll().scroll(amount); return true; }
        if (geometry.contentViewport().contains(x, y) || geometry.contentTrack().contains(x, y)) { contentScroll().scroll(amount); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE || HelpMenuKeybindClient.matchesHelpMenuKey(event)) { onClose(); return true; }
        ensureLayout();
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE || event.hasAltDown() && event.isLeft()) { back(); return true; }
        if (event.isCycleFocus()) {
            focused = Math.floorMod(focused < 0 ? event.hasShiftDown() ? focusOrder.size() - 1 : 0 : focused + (event.hasShiftDown() ? -1 : 1), focusOrder.size());
            var focus = focus();
            if (focus.area() == Area.NAV) { var row = rows.get(focus.index()); navScroll().reveal(row.y(), row.y() + row.height()); }
            if (focus.area() == Area.CONTENT) { var r = document.targets().get(focus.index()).bounds(); contentScroll().reveal(r.y(), r.bottom()); }
            triggerImmediateNarration(false);
            return true;
        }
        var focus = focus();
        if (event.isSelection() && focus != null) {
            switch (focus.area()) {
                case BACK -> back();
                case HOME -> open(HelpMenuContent.GET_STARTED.id());
                case CLOSE -> onClose();
                case NAV -> open(rows.get(focus.index()).node().page().id());
                case CONTENT -> { var target = document.targets().get(focus.index()); if (target.pageId() != null) open(target.pageId()); }
            }
            return true;
        }
        boolean nav = focus != null && focus.area() == Area.NAV;
        var scroll = nav ? navScroll() : contentScroll();
        int page = Math.max(1, (nav ? geometry.navViewport() : geometry.contentViewport()).h() - 12);
        switch (event.key()) {
            case GLFW.GLFW_KEY_PAGE_UP -> { scroll.scroll(-page); return true; }
            case GLFW.GLFW_KEY_PAGE_DOWN -> { scroll.scroll(page); return true; }
            case GLFW.GLFW_KEY_HOME -> { scroll.reset(); return true; }
            case GLFW.GLFW_KEY_END -> { scroll.scrollTo(scroll.maxOffset()); return true; }
            case GLFW.GLFW_KEY_UP -> { scroll.scroll(-SCROLL_STEP); return true; }
            case GLFW.GLFW_KEY_DOWN -> { scroll.scroll(SCROLL_STEP); return true; }
            default -> { return super.keyPressed(event); }
        }
    }
    private Focus focus() { return focused < 0 || focused >= focusOrder.size() ? null : focusOrder.get(focused); }
    @Override protected void updateNarrationState(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, title.copy().append(". ").append(navigation.page().title()));
        output.add(NarratedElementType.USAGE, USAGE);
        var focus = focus();
        if (focus == null) return;
        Component label = switch (focus.area()) {
            case BACK -> Component.literal("Back");
            case HOME -> Component.literal("Home");
            case CLOSE -> Component.literal("Close");
            case NAV -> rows.get(focus.index()).node().page().title();
            case CONTENT -> {
                var target = document.targets().get(focus.index());
                if (target.isItem()) yield Component.literal("Read-only example: ").append(target.item().getHoverName());
                if (target.keyId() != null) yield Component.literal("Current hotkey: ").append(SkillKeybindClient.hint(target.keyId()));
                var page = navigation.resolve(target.pageId());
                yield page == null ? Component.literal("Unavailable link") : Component.literal("Open: ").append(page.title());
            }
        };
        output.add(NarratedElementType.HINT, label);
    }
    @Override public boolean isPauseScreen() { return false; }
}
