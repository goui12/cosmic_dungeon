package net.goui.cosmicdungeon.client.screen.crafting;

import java.util.*;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;

/** One bounded scroll viewport to the right of native slots. Only visible rows have hit boxes. */
public record PotionPaletteLayout(Rect panel, Rect fuel, List<Rect> tiles, Rect track, Rect thumb,
                                  int first, int maxFirst, int rowHeight) {
    public PotionPaletteLayout { tiles = List.copyOf(tiles); }
    public static PotionPaletteLayout of(int width, int height, int left, int top, int inventoryWidth, int count) {
        return of(width, height, left, top, inventoryWidth, count, 0);
    }
    public static PotionPaletteLayout of(int width, int height, int left, int top, int inventoryWidth, int count, int offset) {
        int sw = Math.max(1, width), sh = Math.max(1, height);
        int x = Math.clamp(left + inventoryWidth + 3, 0, sw);
        int w = Math.min(196, Math.max(0, sw - x - 3));
        int h = Math.max(0, Math.min(sh - 4, 290));
        int y = Math.clamp(top, 0, Math.max(0, sh - h - 2));
        int row = w < 84 ? 86 : w < 140 ? 62 : 44;
        int capacity = Math.max(0, (h - 32) / row);
        int shown = Math.min(Math.max(0, count), capacity);
        int max = Math.max(0, count - shown), first = Math.clamp(offset, 0, max);
        var rows = new ArrayList<Rect>();
        for (int i = 0; i < shown; i++) rows.add(new Rect(x + 3, y + 29 + i * row, Math.max(0, w - 13), row - 2));
        var track = new Rect(x + Math.max(0, w - 8), y + 29, Math.min(5, w), Math.max(0, shown * row - 2));
        int thumbH = max == 0 ? track.height() : Math.min(track.height(), Math.max(10, track.height() * shown / Math.max(1, count)));
        int travel = track.height() - thumbH;
        var thumb = new Rect(track.x(), track.y() + (max == 0 ? 0 : first * travel / max), track.width(), thumbH);
        return new PotionPaletteLayout(new Rect(x, y, w, h), new Rect(x + 3, y + 19, Math.max(0, w - 6), 5),
                rows, track, thumb, first, max, row);
    }
    public int indexAt(double x, double y) {
        for (int i = 0; i < tiles.size(); i++) if (tiles.get(i).contains(x, y)) return first + i;
        return -1;
    }
    public int offsetAt(double mouseY, double grab) {
        int travel = track.height() - thumb.height();
        return travel <= 0 ? 0 : Math.clamp((int)Math.round((mouseY - track.y() - grab) * maxFirst / travel), 0, maxFirst);
    }
}
