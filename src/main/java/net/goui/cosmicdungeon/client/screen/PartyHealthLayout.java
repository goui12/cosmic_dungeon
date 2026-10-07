package net.goui.cosmicdungeon.client.screen;

/** Pixel geometry shared by rendering, hit testing and scroll tests. */
public record PartyHealthLayout(int x, int y, int width, int height, int columns, int rowsPerColumn) {
    public static final int HEADER = 24, ROW = 38, ICON = 10;
    public static PartyHealthLayout world(int screenWidth, int screenHeight, int count) {
        int rows = Math.max(1, (screenHeight - 90 - HEADER) / ROW);
        int columns = Math.max(1, (count + rows - 1) / rows);
        int width = Math.min(224, (screenWidth - 16 - (columns - 1) * 4) / columns);
        return new PartyHealthLayout(8, 8, width, HEADER + Math.min(rows, count) * ROW, columns, rows);
    }
    public static PartyHealthLayout inventory(int left, int screenHeight) {
        return new PartyHealthLayout(8, 8, Math.max(24, Math.min(224, left - 16)),
                Math.max(48, screenHeight - 42), 1, 0);
    }
    public int iconColumns() { return Math.max(1, (width - 16) / ICON); }
    public int rowHeight(int effects) { return 27 + Math.max(1, (effects + iconColumns() - 1) / iconColumns()) * ICON; }
    public int viewport() { return height - HEADER; }
    public int maxScroll(int content) { return Math.max(0, content - viewport()); }
    public int clampScroll(int value, int content) { return Math.max(0, Math.min(maxScroll(content), value)); }
    public int thumbHeight(int content) { return Math.min(viewport(), Math.max(12, viewport() * viewport() / Math.max(1, content))); }
    public int thumbY(int scroll, int content) {
        return y + HEADER + (maxScroll(content) == 0 ? 0 : clampScroll(scroll,content) * (viewport() - thumbHeight(content)) / maxScroll(content));
    }
    public int scrollAt(double thumbTop, int content) {
        int travel = viewport() - thumbHeight(content);
        return travel == 0 ? 0 : clampScroll((int)Math.round((thumbTop - y - HEADER) * maxScroll(content) / travel), content);
    }
    public boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
}
