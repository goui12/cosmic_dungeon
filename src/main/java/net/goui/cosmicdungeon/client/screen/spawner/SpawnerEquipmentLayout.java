package net.goui.cosmicdungeon.client.screen.spawner;

/** Both catalogs page inside the same bounded grid, including the smallest supported GUI scale. */
public record SpawnerEquipmentLayout(SpawnerEditorLayout panel) {
    public static SpawnerEquipmentLayout fit(int width, int height) {
        return new SpawnerEquipmentLayout(SpawnerEditorLayout.fit(width, height));
    }
    public int x() { return panel.x() + 8; }
    public int width() { return panel.width() - 16; }
    public int tabsY() { return panel.y() + 27; }
    public int searchY() { return panel.y() + 51; }
    public int gridY() { return panel.y() + 75; }
    public int previewY() { return panel.footerY() - 26; }
    public int columns() { return Math.max(1, width() / 22); }
    public int rows() { return Math.max(1, (previewY() - gridY() - 3) / 22); }
    public int capacity() { return columns() * rows(); }
    public int tileX(int index) { return x() + index % columns() * 22; }
    public int tileY(int index) { return gridY() + index / columns() * 22; }
}
