package net.goui.cosmicdungeon.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;

/** Compact effects use the former Craft button space, above Resurrect and away from inventory slots. */
public record InventoryEffectsLayout(Rect row, List<Rect> icons, Rect overflow, int hidden) {
    public static final int MAX_ICONS = 3;
    public InventoryEffectsLayout { icons = List.copyOf(icons); }
    public static InventoryEffectsLayout of(int left, int top, int count) {
        int total = Math.max(0, count);
        var row = new Rect(left + 98, top + 25, 73, 20);
        var icons = new ArrayList<Rect>();
        for (int i = 0; i < Math.min(MAX_ICONS, total); i++)
            icons.add(new Rect(row.x() + i * 18, row.y(), 18, row.height()));
        int hidden = Math.max(0, total - MAX_ICONS);
        return new InventoryEffectsLayout(row, icons,
                new Rect(row.x() + 54, row.y(), hidden == 0 ? 0 : 19, row.height()), hidden);
    }
    public boolean fits(int width, int height) {
        return row.x() >= 0 && row.y() >= 0 && row.right() <= width && row.bottom() <= height;
    }
}
