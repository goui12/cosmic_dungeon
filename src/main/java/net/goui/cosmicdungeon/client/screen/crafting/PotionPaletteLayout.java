package net.goui.cosmicdungeon.client.screen.crafting;

import java.util.*;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;

/** Scaled-pixel layout, snapped outside native slots. Two rows where space permits, then wrapping. */
public record PotionPaletteLayout(Rect panel, Rect fuel, List<Rect> variants, List<Rect> tiles) {
    private static final int TILE = 20, HEADER = 34;
    public PotionPaletteLayout { variants = List.copyOf(variants); tiles = List.copyOf(tiles); }
    public static PotionPaletteLayout of(int width, int height, int left, int top, int inventoryWidth, int count) {
        int sw = Math.max(1, width), sh = Math.max(1, height);
        int x = Math.min(sw - 1, Math.max(0, left + inventoryWidth + 3));
        int available = Math.max(1, sw - x - 3);
        int columns = Math.max(1, Math.min(Math.max(1, (count + 1) / 2), (available - 18) / TILE));
        int rows = (count + columns - 1) / columns;
        // Minecraft's supported minimum (320x240) fits all vanilla class families without scrolling.
        int panelWidth = Math.min(available, 18 + columns * TILE);
        int panelHeight = Math.min(sh - 4, HEADER + Math.max(1, rows) * TILE + 3);
        int y = Math.clamp(top, 2, Math.max(2, sh - panelHeight - 2));
        var panel = new Rect(x, y, panelWidth, Math.max(0, panelHeight));
        int bodyX = x + Math.min(16, Math.max(0, panelWidth - TILE));
        int bodyWidth = Math.max(0, panel.right() - bodyX - 2);
        var variants = new ArrayList<Rect>();
        for (int i = 0; i < 3; i++) {
            int from = bodyX + i * bodyWidth / 3, to = bodyX + (i + 1) * bodyWidth / 3;
            variants.add(new Rect(from, y + 15, Math.max(0, to - from - 1), 16));
        }
        var tiles = new ArrayList<Rect>();
        for (int i = 0; i < count; i++)
            tiles.add(new Rect(bodyX + i % columns * TILE, y + HEADER + i / columns * TILE, 18, 18));
        return new PotionPaletteLayout(panel, new Rect(x + 3, y + HEADER, 9, Math.min(40, Math.max(0, panel.bottom() - y - HEADER - 4))), variants, tiles);
    }
}
