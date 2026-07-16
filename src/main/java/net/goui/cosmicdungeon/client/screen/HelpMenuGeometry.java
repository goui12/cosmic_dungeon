package net.goui.cosmicdungeon.client.screen;

record HelpMenuGeometry(int left, int top) {
    static final int NAV_X = 16;
    static final int NAV_Y = 56;
    static final int NAV_W = 134;
    static final int NAV_H = 148;
    static final int CONTENT_X = 162;
    static final int CONTENT_Y = 58;
    static final int CONTENT_W = 190;
    static final int CONTENT_H = 146;
    static final int TITLE_X = 18;
    static final int TITLE_Y = 12;
    static final int PAGE_TITLE_X = CONTENT_X;
    static final int PAGE_TITLE_Y = 28;
    static final int BUTTON_GAP = 4;
    static final int NAV_ARROW_X = 6;
    static final int CONTENT_ARROW_X = 356;
    static final int UP_Y = 24;
    static final int DOWN_Y = 206;
    static final int PANE_PADDING = 4;

    static HelpMenuGeometry centered(int screenWidth, int screenHeight) {
        return new HelpMenuGeometry(Math.max(0, (screenWidth - HelpMenuAssets.MENU_W) / 2), Math.max(0, (screenHeight - HelpMenuAssets.MENU_H) / 2));
    }

    int x(int offset) { return left + offset; }
    int y(int offset) { return top + offset; }

    Rect navViewport() { return new Rect(x(NAV_X), y(NAV_Y), NAV_W, NAV_H); }
    Rect contentViewport() { return new Rect(x(CONTENT_X), y(CONTENT_Y), CONTENT_W, CONTENT_H); }
    Rect navBackground() { return padded(navViewport(), PANE_PADDING); }
    Rect contentBackground() { return padded(contentViewport(), PANE_PADDING); }
    Rect navUpButton() { return new Rect(x(NAV_ARROW_X), y(UP_Y), HelpMenuAssets.ARROW_W, HelpMenuAssets.ARROW_H); }
    Rect navDownButton() { return new Rect(x(NAV_ARROW_X), y(DOWN_Y), HelpMenuAssets.ARROW_W, HelpMenuAssets.ARROW_H); }
    Rect contentUpButton() { return new Rect(x(CONTENT_ARROW_X), y(UP_Y), HelpMenuAssets.ARROW_W, HelpMenuAssets.ARROW_H); }
    Rect contentDownButton() { return new Rect(x(CONTENT_ARROW_X), y(DOWN_Y), HelpMenuAssets.ARROW_W, HelpMenuAssets.ARROW_H); }

    private static Rect padded(Rect rect, int padding) {
        return new Rect(rect.x() - padding, rect.y() - padding, rect.w() + padding * 2, rect.h() + padding * 2);
    }

    record Rect(int x, int y, int w, int h) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }
    }
}
