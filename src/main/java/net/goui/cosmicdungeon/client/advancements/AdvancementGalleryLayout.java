package net.goui.cosmicdungeon.client.advancements;

/** GUI-pixel layout, including Minecraft's minimum 320 x 240 viewport. */
public record AdvancementGalleryLayout(Rect panel, Rect grid, Rect details, int columns, int rows, int tileWidth) {
    public static final int TILE_HEIGHT = 62;
    public static final int GAP = 6;

    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(double px, double py) {
            return px >= x && px < right() && py >= y && py < bottom();
        }
    }

    public static AdvancementGalleryLayout forViewport(int width, int height) {
        int w = Math.min(960, width - 16);
        int h = Math.min(640, height - 16);
        Rect panel = new Rect((width - w) / 2, (height - h) / 2, w, h);
        int x = panel.x() + 12, y = panel.y() + 58;
        int bodyW = w - 24, bodyH = h - 88;
        Rect grid, details;
        if (w >= 560) {
            int detailW = Math.min(240, Math.max(180, bodyW / 3));
            grid = new Rect(x, y, bodyW - detailW - 10, bodyH);
            details = new Rect(grid.right() + 10, y, detailW, bodyH);
        } else {
            int detailH = Math.min(100, Math.max(68, bodyH / 3));
            grid = new Rect(x, y, bodyW, bodyH - detailH - GAP);
            details = new Rect(x, grid.bottom() + GAP, bodyW, detailH);
        }
        int columns = Math.max(1, Math.min(5, (grid.width() + GAP) / 142));
        int rows = Math.max(1, (grid.height() + GAP) / (TILE_HEIGHT + GAP));
        int tileWidth = (grid.width() - (columns - 1) * GAP) / columns;
        return new AdvancementGalleryLayout(panel, grid, details, columns, rows, tileWidth);
    }

    public int pageSize() { return columns * rows; }
}
