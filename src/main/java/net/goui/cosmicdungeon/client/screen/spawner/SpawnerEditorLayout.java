package net.goui.cosmicdungeon.client.screen.spawner;

/** Screen-relative coordinates; compact layouts use paging rather than offscreen fields. */
public record SpawnerEditorLayout(int x, int y, int width, int height) {
    public static SpawnerEditorLayout fit(int screenWidth, int screenHeight) {
        int w = Math.min(760, Math.max(1, screenWidth - 16));
        int h = Math.min(440, Math.max(1, screenHeight - 16));
        return new SpawnerEditorLayout((screenWidth - w) / 2, (screenHeight - h) / 2, w, h);
    }
    public int contentX() { return x + 44; }
    public int contentY() { return y + 32; }
    public int contentWidth() { return Math.max(1, width - 56); }
    public int contentHeight() { return Math.max(1, height - 88); }
    public int footerY() { return y + height - 28; }
    public int entityRows() { return Math.max(1, (contentHeight() - 48) / 22); }
    public static int page(int requested, int total, int capacity) {
        return Math.max(0, Math.min(requested, Math.max(0, (total - 1) / Math.max(1, capacity))));
    }
}
