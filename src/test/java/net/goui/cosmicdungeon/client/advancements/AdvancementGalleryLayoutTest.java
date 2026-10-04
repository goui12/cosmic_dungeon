package net.goui.cosmicdungeon.client.advancements;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdvancementGalleryLayoutTest {
    @Test void galleryFitsMinimumAndWideGuiScalesWithoutOverlappingDetails() {
        for (int[] viewport : new int[][]{{320,240},{426,240},{480,270},{560,315},{640,360},{960,540},{1280,720}}) {
            var layout = AdvancementGalleryLayout.forViewport(viewport[0], viewport[1]);
            var panel = layout.panel();
            var grid = layout.grid();
            var details = layout.details();
            assertTrue(panel.x() >= 0 && panel.y() >= 0);
            assertTrue(panel.right() <= viewport[0] && panel.bottom() <= viewport[1]);
            assertTrue(grid.width() > 0 && grid.height() >= AdvancementGalleryLayout.TILE_HEIGHT);
            assertTrue(details.width() >= 180 && details.height() >= 68);
            assertTrue(grid.right() <= details.x() || grid.bottom() <= details.y());
            assertTrue(details.right() <= panel.right() && details.bottom() <= panel.bottom() - 25);
            assertTrue(grid.y() >= panel.y() + 58);
            assertTrue(layout.columns() * layout.tileWidth() + (layout.columns() - 1) * AdvancementGalleryLayout.GAP <= grid.width());
            assertTrue(layout.rows() * AdvancementGalleryLayout.TILE_HEIGHT + (layout.rows() - 1) * AdvancementGalleryLayout.GAP <= grid.height());
            assertTrue(layout.pageSize() >= 1);
        }
    }
}
