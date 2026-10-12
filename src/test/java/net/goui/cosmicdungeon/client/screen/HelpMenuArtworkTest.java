package net.goui.cosmicdungeon.client.screen;

import java.io.ByteArrayInputStream;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class HelpMenuArtworkTest {
    @Test void manifestExactlyCoversReachablePagesWithoutEnablingDeferredClasses() {
        var queue = new ArrayDeque<HelpMenuContent.HelpNode>();
        var pages = new HashSet<String>();
        queue.add(HelpMenuContent.ROOT);
        while (!queue.isEmpty()) {
            var node = queue.remove();
            assertTrue(node.page().enabled());
            assertTrue(pages.add(node.page().id()), "Duplicate navigation target");
            queue.addAll(node.children());
        }
        assertEquals(pages, HelpMenuArtwork.pageThemes().keySet(), "Every reachable page needs an explicit theme decision");
        assertEquals(new HashSet<>(HelpMenuArtwork.pageThemes().values()), HelpMenuArtwork.themes().keySet(), "No unused packaged art");
        assertFalse(HelpMenuContent.METALMANCER.enabled());
        assertFalse(HelpMenuContent.DEADEYE.enabled());
        for (String unknown : new String[]{null, "", "class.deadeye", "class.metalmancer", "unrecognized"})
            assertEquals("guide", HelpMenuArtwork.forPage(unknown).id());
        assertThrows(UnsupportedOperationException.class, () -> HelpMenuArtwork.pageThemes().clear());
    }

    @Test void classesHaveDistinctArtAndReferenceUnmodifiedOriginalEmblemPaths() throws Exception {
        var classes = Set.of("bogatyr", "dragoon", "judicator", "pyroclast", "theurgist", "venefex");
        var paths = new HashSet<String>();
        for (String id : classes) {
            var theme = HelpMenuArtwork.forPage("class." + id);
            assertEquals(id, theme.id());
            assertTrue(paths.add(theme.texture().getPath()));
            assertEquals("textures/entity/chest/emblem_" + id + ".png", theme.emblem().getPath());
            try (var input = getClass().getResourceAsStream("/assets/cosmicdungeon/" + theme.emblem().getPath())) {
                assertNotNull(input);
                var emblem = ImageIO.read(input);
                assertEquals(32, emblem.getWidth());
                assertEquals(32, emblem.getHeight());
            }
        }
        for (var theme : HelpMenuArtwork.themes().values())
            if (!classes.contains(theme.id())) assertNull(theme.emblem());
    }

    @Test void packagedTexturesAreReadableAndFitExplicitDiskAndDecodedBudgets() throws Exception {
        long bytes = 0, decoded = 0;
        int scrimAlpha = HelpMenuArtwork.TEXT_SCRIM >>> 24;
        for (var theme : HelpMenuArtwork.themes().values()) {
            try (var input = getClass().getResourceAsStream("/assets/cosmicdungeon/" + theme.texture().getPath())) {
                assertNotNull(input, theme.id());
                byte[] png = input.readAllBytes();
                bytes += png.length;
                var image = ImageIO.read(new ByteArrayInputStream(png));
                assertNotNull(image, theme.id());
                assertEquals(480, image.getWidth());
                assertEquals(270, image.getHeight());
                decoded += (long) image.getWidth() * image.getHeight() * 4;
                // Whole-image proof also bounds every supported crop. It proves
                // contrast only when the integration applies the required scrim.
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int rgb = image.getRGB(x, y);
                        assertEquals(255, rgb >>> 24, "Opaque background expected");
                        double luminance = 0;
                        double[] weights = {0.2126, 0.7152, 0.0722};
                        for (int i = 0; i < 3; i++) {
                            double s = ((rgb >>> (16 - i * 8)) & 255) / 255.0 * (255 - scrimAlpha) / 255.0;
                            luminance += weights[i] * (s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4));
                        }
                        // #B0B0B0 bounds planned body/tip text. Each actual link
                        // color needs its own integration QA in Batch19.
                        double textLuminance = Math.pow((176 / 255.0 + 0.055) / 1.055, 2.4);
                        assertTrue((textLuminance + 0.05) / (luminance + 0.05) >= 4.5, theme.id());
                    }
                }
            }
        }
        assertTrue(bytes <= 4L * 1024 * 1024, "Packaged help PNGs exceed4MiB");
        assertTrue(decoded <= 13L * 1024 * 1024, "Full-catalog RGBA surface budget exceeds13MiB");
    }

    @Test void centerCoverPreservesAspectAndValidUvBoundsAtEverySupportedScale() {
        for (int[] size : new int[][]{{640,480},{1280,720},{1920,1080},{2560,1080},{384,240},{800,800},{3840,1080}}) {
            for (int scale : new int[]{1,2,3,4}) {
                int w = size[0] / scale, h = size[1] / scale;
                var crop = HelpMenuArtwork.crop(w, h);
                assertTrue(crop.u() >= 0 && crop.v() >= 0);
                assertTrue(crop.u() + crop.width() <= 1 && crop.v() + crop.height() <= 1);
                assertEquals(1.0, Math.max(crop.width(), crop.height()), 1e-12);
                assertEquals(0.5, crop.u() + crop.width()/2, 1e-12);
                assertEquals(0.5, crop.v() + crop.height()/2, 1e-12);
                assertEquals((double) w / h, 480 * crop.width() / (270 * crop.height()), 1e-12);
            }
        }
        assertEquals(new HelpMenuArtwork.Crop(0, 0, 1, 1), HelpMenuArtwork.crop(1920,1080));
        assertEquals(0.125, HelpMenuArtwork.crop(640,480).u(), 1e-12);
        assertTrue(Double.isFinite(HelpMenuArtwork.crop(Integer.MAX_VALUE, 1).height()));
        assertTrue(HelpMenuArtwork.crop(1, Integer.MAX_VALUE).width() > 0);
        assertThrows(IllegalArgumentException.class, () -> HelpMenuArtwork.crop(0, 100));
        assertThrows(IllegalArgumentException.class, () -> HelpMenuArtwork.crop(100, -1));
    }
}
