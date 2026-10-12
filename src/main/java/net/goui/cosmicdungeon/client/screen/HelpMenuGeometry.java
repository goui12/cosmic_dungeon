package net.goui.cosmicdungeon.client.screen;

/** Logical GUI pixels: resizing and GUI scale use the same bounded, centered 80% layout. */
record HelpMenuGeometry(int left, int top, int width, int height) {
    static HelpMenuGeometry centered(int screenWidth, int screenHeight) {
        int sw = Math.max(0, screenWidth), sh = Math.max(0, screenHeight);
        int w = (int) (sw * .8), h = (int) (sh * .8);
        return new HelpMenuGeometry((sw - w) / 2, (sh - h) / 2, w, h);
    }
    Rect window() { return new Rect(left, top, width, height); }
    private int pad() { return Math.min(6, Math.min(width, height) / 10); }
    private int gap() { return Math.min(10, width / 20); }
    private int navWidth() { return Math.min(180, Math.max(0, (width - 2 * pad() - gap()) * 29 / 100)); }
    private int headerHeight() { return Math.min(58, height / 3); }
    private int footerHeight() { return Math.min(18, height / 6); }
    Rect header() { return new Rect(left + pad(), top + pad(), Math.max(0, width - 2 * pad() - 24), 18); }
    Rect close() { return new Rect(left + Math.max(0, width - pad() - 18), top + pad(), Math.min(18, width), Math.min(18, height)); }
    Rect navViewport() { return new Rect(left + pad(), top + headerHeight(), Math.max(0, navWidth() - 6), Math.max(0, height - headerHeight() - footerHeight())); }
    Rect contentViewport() { return new Rect(left + pad() + navWidth() + gap(), top + headerHeight(), Math.max(0, width - 2 * pad() - navWidth() - gap() - 6), Math.max(0, height - headerHeight() - footerHeight())); }
    Rect pageTitle() { var r = contentViewport(); return new Rect(r.x(), top + Math.min(31, height / 6), r.w(), Math.max(0, r.y() - top - Math.min(31, height / 6) - 3)); }
    Rect back() { var r = navViewport(); return new Rect(r.x(), top + Math.min(31, height / 6), r.w() / 2, 18); }
    Rect home() { var r = back(); return new Rect(r.right() + 1, r.y(), Math.max(0, navViewport().w() - r.w() - 1), r.h()); }
    Rect footer() { return new Rect(left + pad(), top + height - footerHeight() + 4, Math.max(0, width - 2 * pad()), Math.max(0, footerHeight() - 4)); }
    Rect navTrack() { var r = navViewport(); return new Rect(r.right() + 2, r.y(), 3, r.h()); }
    Rect contentTrack() { var r = contentViewport(); return new Rect(r.right() + 2, r.y(), 3, r.h()); }

    record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
        boolean contains(double px, double py) { return w > 0 && h > 0 && px >= x && px < right() && py >= y && py < bottom(); }
        boolean intersects(Rect other) { return w > 0 && h > 0 && other.w > 0 && other.h > 0 && x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y; }
        Rect move(int dx, int dy) { return new Rect(x + dx, y + dy, w, h); }
    }
}
