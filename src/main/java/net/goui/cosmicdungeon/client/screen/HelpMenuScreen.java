package net.goui.cosmicdungeon.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import org.joml.Matrix3x2fStack;
import net.goui.cosmicdungeon.client.HelpMenuKeybindClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HelpMenuScreen extends Screen {
    private static final int TITLE_COLOR = 0xFFFFD98A;
    private static final int PANE_BACKGROUND = 0x88000000;
    private static final int SCROLL_STEP = 18;
    private static final float PAGE_TITLE_SCALE = 1.12F;
    private final HelpScrollPane navScroll = new HelpScrollPane();
    private final Map<String, HelpScrollPane> contentScrolls = new HashMap<>();
    private final Deque<HelpMenuContent.HelpNode> path = new ArrayDeque<>();
    private HelpMenuContent.HelpNode currentDirectory = HelpMenuContent.ROOT;
    private HelpMenuContent.Page currentPage = HelpMenuContent.GET_STARTED;
    private HelpMenuGeometry geometry = HelpMenuGeometry.centered(0, 0);

    public HelpMenuScreen() { super(Component.translatable("screen.cosmicdungeon.help_menu")); }

    @Override protected void init() {
        super.init();
        geometry = HelpMenuGeometry.centered(width, height);
        updateScrollRanges();
    }

    @Override public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        updateScrollRanges();
        g.blit(RenderPipelines.GUI_TEXTURED, HelpMenuAssets.BACKGROUND_LARGE, geometry.left(), geometry.top(), 0, 0, HelpMenuAssets.MENU_W, HelpMenuAssets.MENU_H, HelpMenuAssets.MENU_W, HelpMenuAssets.MENU_H);
        g.blit(RenderPipelines.GUI_TEXTURED, HelpMenuAssets.TITLE, geometry.x(HelpMenuGeometry.TITLE_X), geometry.y(HelpMenuGeometry.TITLE_Y), 0, 0, HelpMenuAssets.TITLE_W, HelpMenuAssets.TITLE_H, HelpMenuAssets.TITLE_W, HelpMenuAssets.TITLE_H);
        renderPaneBackgrounds(g);
        renderPageTitle(g);
        renderNavigation(g, mouseX, mouseY);
        renderContent(g);
        renderScrollButtons(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderPaneBackgrounds(GuiGraphics g) {
        fill(g, geometry.navBackground());
        fill(g, geometry.contentBackground());
    }

    private void fill(GuiGraphics g, HelpMenuGeometry.Rect rect) {
        g.fill(rect.x(), rect.y(), rect.x() + rect.w(), rect.y() + rect.h(), PANE_BACKGROUND);
    }

    private void renderPageTitle(GuiGraphics g) {
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(geometry.x(HelpMenuGeometry.PAGE_TITLE_X), geometry.y(HelpMenuGeometry.PAGE_TITLE_Y));
        pose.scale(PAGE_TITLE_SCALE, PAGE_TITLE_SCALE);
        g.drawString(font, currentPage.title(), 0, 0, TITLE_COLOR, false);
        pose.popMatrix();
    }

    private void renderNavigation(GuiGraphics g, int mouseX, int mouseY) {
        HelpMenuGeometry.Rect viewport = geometry.navViewport();
        g.enableScissor(viewport.x(), viewport.y(), viewport.x() + viewport.w(), viewport.y() + viewport.h());
        int y = viewport.y() - navScroll.offset();
        if (hasBackButton()) {
            y = renderNavigationButton(g, Component.literal("Back"), viewport.x(), y, mouseX, mouseY, false, true) + HelpMenuGeometry.BUTTON_GAP;
        }
        for (HelpMenuContent.HelpNode node : navigationNodes()) {
            y = renderNavigationButton(g, node.page().title(), viewport.x(), y, mouseX, mouseY, node.page() == currentPage, node.page().enabled()) + HelpMenuGeometry.BUTTON_GAP;
        }
        g.disableScissor();
    }

    private int renderNavigationButton(GuiGraphics g, Component title, int x, int y, int mouseX, int mouseY, boolean selected, boolean enabled) {
        boolean hovered = rect(x, y, HelpMenuAssets.TEXT_BUTTON_W, HelpMenuAssets.TEXT_BUTTON_H).contains(mouseX, mouseY);
        TexturedHelpButton.renderText(g, font, title, x, y, hovered, selected, enabled);
        return y + HelpMenuAssets.TEXT_BUTTON_H;
    }

    private void renderContent(GuiGraphics g) {
        HelpMenuGeometry.Rect viewport = geometry.contentViewport();
        HelpScrollPane scroll = contentScroll();
        g.enableScissor(viewport.x(), viewport.y(), viewport.x() + viewport.w(), viewport.y() + viewport.h());
        HelpRichTextRenderer.render(g, font, currentPage, viewport.x(), viewport.y() - scroll.offset(), viewport.w());
        g.disableScissor();
    }

    private void renderScrollButtons(GuiGraphics g, int mouseX, int mouseY) {
        renderScrollButtons(g, mouseX, mouseY, navScroll, geometry.navUpButton(), geometry.navDownButton());
        renderScrollButtons(g, mouseX, mouseY, contentScroll(), geometry.contentUpButton(), geometry.contentDownButton());
    }

    private void renderScrollButtons(GuiGraphics g, int mouseX, int mouseY, HelpScrollPane scroll, HelpMenuGeometry.Rect up, HelpMenuGeometry.Rect down) {
        if (!scroll.canScroll()) return;
        if (scroll.canScrollUp()) TexturedHelpButton.renderArrow(g, up.x(), up.y(), true, up.contains(mouseX, mouseY));
        if (scroll.canScrollDown()) TexturedHelpButton.renderArrow(g, down.x(), down.y(), false, down.contains(mouseX, mouseY));
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 0 && (clickNavigation(event.x(), event.y()) || clickScrollButton(event.x(), event.y()))) return true;
        return super.mouseClicked(event, isDoubleClick);
    }

    private boolean clickNavigation(double mouseX, double mouseY) {
        HelpMenuGeometry.Rect viewport = geometry.navViewport();
        if (!viewport.contains(mouseX, mouseY)) return false;
        int y = viewport.y() - navScroll.offset();
        if (hasBackButton()) {
            if (rect(viewport.x(), y, HelpMenuAssets.TEXT_BUTTON_W, HelpMenuAssets.TEXT_BUTTON_H).contains(mouseX, mouseY)) { goBack(); return true; }
            y += HelpMenuAssets.TEXT_BUTTON_H + HelpMenuGeometry.BUTTON_GAP;
        }
        for (HelpMenuContent.HelpNode node : navigationNodes()) {
            if (rect(viewport.x(), y, HelpMenuAssets.TEXT_BUTTON_W, HelpMenuAssets.TEXT_BUTTON_H).contains(mouseX, mouseY)) {
                if (node.page().enabled()) selectNode(node);
                return true;
            }
            y += HelpMenuAssets.TEXT_BUTTON_H + HelpMenuGeometry.BUTTON_GAP;
        }
        return false;
    }

    private void selectNode(HelpMenuContent.HelpNode node) {
        currentPage = node.page();
        if (node.isDirectory()) {
            path.push(currentDirectory);
            currentDirectory = node;
            navScroll.reset();
        }
    }

    private void goBack() {
        currentDirectory = path.pop();
        currentPage = currentDirectory == HelpMenuContent.ROOT ? HelpMenuContent.GET_STARTED : currentDirectory.page();
        navScroll.reset();
    }

    private boolean clickScrollButton(double mouseX, double mouseY) {
        if (clickScrollButton(mouseX, mouseY, navScroll, geometry.navUpButton(), geometry.navDownButton())) return true;
        return clickScrollButton(mouseX, mouseY, contentScroll(), geometry.contentUpButton(), geometry.contentDownButton());
    }

    private boolean clickScrollButton(double mouseX, double mouseY, HelpScrollPane scroll, HelpMenuGeometry.Rect up, HelpMenuGeometry.Rect down) {
        if (up.contains(mouseX, mouseY) && scroll.canScrollUp()) return scroll.scroll(-SCROLL_STEP);
        if (down.contains(mouseX, mouseY) && scroll.canScrollDown()) return scroll.scroll(SCROLL_STEP);
        return up.contains(mouseX, mouseY) || down.contains(mouseX, mouseY);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (geometry.navViewport().contains(mouseX, mouseY)) return navScroll.scroll(scrollY < 0 ? SCROLL_STEP : -SCROLL_STEP);
        if (geometry.contentViewport().contains(mouseX, mouseY)) return contentScroll().scroll(scrollY < 0 ? SCROLL_STEP : -SCROLL_STEP);
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE || HelpMenuKeybindClient.matchesHelpMenuKey(event)) { onClose(); return true; }
        return super.keyPressed(event);
    }

    private void updateScrollRanges() {
        int count = navigationNodes().size() + (hasBackButton() ? 1 : 0);
        int navHeight = count * HelpMenuAssets.TEXT_BUTTON_H + Math.max(0, count - 1) * HelpMenuGeometry.BUTTON_GAP;
        navScroll.update(navHeight, geometry.navViewport().h());
        contentScroll().update(HelpRichTextRenderer.measure(font, currentPage, geometry.contentViewport().w()), geometry.contentViewport().h());
    }

    private boolean hasBackButton() { return !path.isEmpty(); }
    private List<HelpMenuContent.HelpNode> navigationNodes() { return currentDirectory.children(); }
    private HelpScrollPane contentScroll() { return contentScrolls.computeIfAbsent(currentPage.id(), id -> new HelpScrollPane()); }
    private static HelpMenuGeometry.Rect rect(int x, int y, int w, int h) { return new HelpMenuGeometry.Rect(x, y, w, h); }
    @Override public boolean isPauseScreen() { return false; }
}
