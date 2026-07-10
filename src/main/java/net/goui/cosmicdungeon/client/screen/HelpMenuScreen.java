package net.goui.cosmicdungeon.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.goui.cosmicdungeon.client.HelpMenuKeybindClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class HelpMenuScreen extends Screen {
    private static final int TITLE_COLOR = 0xFFFFD98A;
    private static final int SCROLL_STEP = 18;
    private final HelpScrollPane navScroll = new HelpScrollPane();
    private final Map<String, HelpScrollPane> contentScrolls = new HashMap<>();
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
        g.drawString(font, currentPage.title(), geometry.x(HelpMenuGeometry.PAGE_TITLE_X), geometry.y(HelpMenuGeometry.PAGE_TITLE_Y), TITLE_COLOR, false);
        renderNavigation(g, mouseX, mouseY);
        renderContent(g);
        renderScrollButtons(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderNavigation(GuiGraphics g, int mouseX, int mouseY) {
        HelpMenuGeometry.Rect viewport = geometry.navViewport();
        g.enableScissor(viewport.x(), viewport.y(), viewport.x() + viewport.w(), viewport.y() + viewport.h());
        int y = viewport.y() - navScroll.offset();
        for (HelpMenuContent.Page page : HelpMenuContent.PAGES) {
            boolean hovered = rect(viewport.x(), y, HelpMenuAssets.TEXT_BUTTON_W, HelpMenuAssets.TEXT_BUTTON_H).contains(mouseX, mouseY);
            TexturedHelpButton.renderText(g, font, page.title(), viewport.x(), y, hovered, page == currentPage, page.enabled());
            y += HelpMenuAssets.TEXT_BUTTON_H + HelpMenuGeometry.BUTTON_GAP;
        }
        g.disableScissor();
    }

    private void renderContent(GuiGraphics g) {
        HelpMenuGeometry.Rect viewport = geometry.contentViewport();
        HelpScrollPane scroll = contentScroll();
        g.enableScissor(viewport.x(), viewport.y(), viewport.x() + viewport.w(), viewport.y() + viewport.h());
        HelpRichTextRenderer.render(g, font, currentPage, viewport.x(), viewport.y() - scroll.offset(), viewport.w());
        g.disableScissor();
    }

    private void renderScrollButtons(GuiGraphics g, int mouseX, int mouseY) {
        HelpScrollPane target = geometry.navViewport().contains(mouseX, mouseY) ? navScroll : contentScroll();
        if (!target.canScroll()) return;
        if (target.canScrollUp()) {
            HelpMenuGeometry.Rect up = geometry.upButton();
            TexturedHelpButton.renderArrow(g, up.x(), up.y(), true, up.contains(mouseX, mouseY));
        }
        if (target.canScrollDown()) {
            HelpMenuGeometry.Rect down = geometry.downButton();
            TexturedHelpButton.renderArrow(g, down.x(), down.y(), false, down.contains(mouseX, mouseY));
        }
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 0) {
            if (clickNavigation(event.x(), event.y()) || clickScrollButton(event.x(), event.y())) return true;
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    private boolean clickNavigation(double mouseX, double mouseY) {
        HelpMenuGeometry.Rect viewport = geometry.navViewport();
        if (!viewport.contains(mouseX, mouseY)) return false;
        int y = viewport.y() - navScroll.offset();
        for (HelpMenuContent.Page page : HelpMenuContent.PAGES) {
            if (rect(viewport.x(), y, HelpMenuAssets.TEXT_BUTTON_W, HelpMenuAssets.TEXT_BUTTON_H).contains(mouseX, mouseY)) {
                if (page.enabled()) currentPage = page;
                return true;
            }
            y += HelpMenuAssets.TEXT_BUTTON_H + HelpMenuGeometry.BUTTON_GAP;
        }
        return false;
    }

    private boolean clickScrollButton(double mouseX, double mouseY) {
        HelpScrollPane target = geometry.navViewport().contains(mouseX, mouseY) ? navScroll : contentScroll();
        if (geometry.upButton().contains(mouseX, mouseY) && target.canScrollUp()) return target.scroll(-SCROLL_STEP);
        if (geometry.downButton().contains(mouseX, mouseY) && target.canScrollDown()) return target.scroll(SCROLL_STEP);
        return false;
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
        int navHeight = HelpMenuContent.PAGES.size() * HelpMenuAssets.TEXT_BUTTON_H + Math.max(0, HelpMenuContent.PAGES.size() - 1) * HelpMenuGeometry.BUTTON_GAP;
        navScroll.update(navHeight, geometry.navViewport().h());
        contentScroll().update(HelpRichTextRenderer.measure(font, currentPage, geometry.contentViewport().w()), geometry.contentViewport().h());
    }

    private HelpScrollPane contentScroll() { return contentScrolls.computeIfAbsent(currentPage.id(), id -> new HelpScrollPane()); }
    private static HelpMenuGeometry.Rect rect(int x, int y, int w, int h) { return new HelpMenuGeometry.Rect(x, y, w, h); }
    @Override public boolean isPauseScreen() { return false; }
}
