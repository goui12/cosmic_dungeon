package net.goui.cosmicdungeon.client.advancements;

import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;

/** A fixed, bounded starfield. No textures, animation, per-frame random generation or shader dependency. */
final class AdvancementStarfield {
    private static final int COUNT = 120;
    private static final float[] X = new float[COUNT], Y = new float[COUNT];
    static {
        Random random = new Random(0xC05C1CDL);
        for (int i = 0; i < COUNT; i++) { X[i] = random.nextFloat(); Y[i] = random.nextFloat(); }
    }
    private AdvancementStarfield() {}

    static void draw(GuiGraphics g, AdvancementGalleryLayout.Rect panel) {
        int x = panel.x(), y = panel.y(), w = panel.width(), h = panel.height();
        g.fillGradient(x, y, x + w, y + h, 0xFF080E25, 0xFF201432);
        g.fillGradient(x + w / 5, y, x + w * 4 / 5, y + h, 0x30255880, 0x00192C58);
        for (int i = 0; i < COUNT; i++) {
            int sx = x + 3 + (int)(X[i] * (w - 6)), sy = y + 3 + (int)(Y[i] * (h - 6));
            int color = i % 9 == 0 ? 0xFFE4D8A5 : i % 3 == 0 ? 0xCC94BDE0 : 0x667987AA;
            g.fill(sx, sy, sx + 1, sy + 1, color);
            if (i % 15 == 0) {
                g.fill(sx - 2, sy, sx + 3, sy + 1, 0x556C93BD);
                g.fill(sx, sy - 2, sx + 1, sy + 3, 0x556C93BD);
                g.fill(sx, sy, sx + 1, sy + 1, 0xFFF5EBD0);
            }
        }
    }

    static void border(GuiGraphics g, AdvancementGalleryLayout.Rect r, int color) {
        g.fill(r.x(), r.y(), r.right(), r.y() + 1, color);
        g.fill(r.x(), r.bottom() - 1, r.right(), r.bottom(), color);
        g.fill(r.x(), r.y(), r.x() + 1, r.bottom(), color);
        g.fill(r.right() - 1, r.y(), r.right(), r.bottom(), color);
    }
}
