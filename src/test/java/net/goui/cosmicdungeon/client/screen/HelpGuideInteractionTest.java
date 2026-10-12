package net.goui.cosmicdungeon.client.screen;

import java.util.List;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HelpGuideInteractionTest {
    private static final HelpRichTextRenderer.Typesetter TYPE = new HelpRichTextRenderer.Typesetter() {
        private final StringSplitter splitter = new StringSplitter((codepoint, style) -> style.isBold() ? 7 : 6);
        public List<FormattedCharSequence> wrap(Component text, int width) { return Language.getInstance().getVisualOrder(splitter.splitLines(text, width, Style.EMPTY)); }
        public int lineHeight() { return 9; }
        public float glyph(int codepoint, Style style) { return style.isBold() ? 7 : 6; }
    };
    @Test void everyPageIsReachableAndLinksCannotEscapeTheEnabledTree() {
        var navigation = new HelpGuideNavigation();
        assertEquals(33, navigation.pages().size());
        for (var id : navigation.pages().keySet()) {
            assertNotNull(navigation.resolve(id));
            navigation.open(id);
            assertEquals(id, navigation.page().id());
        }
        var before = navigation.page();
        for (var id : List.of("class.deadeye", "class.metalmancer", "/give @s diamond", "https://example.com", "unknown")) {
            assertFalse(navigation.open(id));
            assertNull(navigation.resolve(id));
            assertSame(before, navigation.page());
        }
    }
    @Test void crossDirectoryLinksReturnToTheirExactOriginAndHistoryStaysBounded() {
        var n = new HelpGuideNavigation();
        n.open("classes"); n.open("class.theurgist");
        var directory = n.directory();
        n.open("npc.beatrix_farrow");
        assertTrue(n.back());
        assertEquals("class.theurgist", n.page().id());
        assertSame(directory, n.directory());
        assertTrue(n.back()); assertEquals("classes", n.page().id());
        assertTrue(n.back()); assertSame(HelpMenuContent.GET_STARTED, n.page());
        assertFalse(n.back());
        for (int i = 0; i < 200; i++) n.open(i % 2 == 0 ? "party" : "classes");
        assertEquals(HelpGuideNavigation.HISTORY_LIMIT, n.historySize());
    }
    @Test void responsivePanesRemainInsideWindowAcrossLogicalGuiScalesAndAspectRatios() {
        for (int width : new int[]{320, 427, 640, 854, 1280, 2560}) for (int height : new int[]{240, 360, 480, 720, 1440}) {
            var g = HelpMenuGeometry.centered(width, height);
            assertEquals((int) (width * .8), g.width());
            assertEquals((int) (height * .8), g.height());
            assertTrue(g.left() >= 0 && g.top() >= 0);
            for (var r : List.of(g.navViewport(), g.contentViewport(), g.pageTitle(), g.back(), g.home(), g.close(), g.navTrack(), g.contentTrack(), g.footer())) {
                assertTrue(r.x() >= g.left() && r.y() >= g.top());
                assertTrue(r.right() <= g.window().right() && r.bottom() <= g.window().bottom(), r.toString());
                assertTrue(r.w() > 0 && r.h() > 0);
            }
            assertFalse(g.navViewport().intersects(g.contentViewport()));
            assertFalse(g.pageTitle().intersects(g.contentViewport()));
        }
        assertEquals(0, HelpMenuGeometry.centered(0, 0).window().w());
    }
    @Test void wrappedInlineLinksKeepAllGlyphsClickableOnlyInsideTheScrolledViewport() {
        var link = Component.literal("Open the Theurgist class guide").withStyle(s -> s.withInsertion(HelpRichTextRenderer.LINK_PREFIX + "class.theurgist"));
        var block = new HelpMenuContent.HelpBlock(HelpMenuContent.Kind.PARAGRAPH, Component.empty(), Component.literal("See ").append(link).append(" for details."));
        var narrow = HelpRichTextRenderer.layout(TYPE, List.of(block), 80, id -> List.of());
        var wide = HelpRichTextRenderer.layout(TYPE, List.of(block), 400, id -> List.of());
        assertTrue(narrow.height() > wide.height());
        assertTrue(narrow.targets().size() >= 3);
        assertTrue(narrow.targets().stream().allMatch(t -> t.pageId().equals("class.theurgist") && t.bounds().right() <= 80));
        var target = narrow.targets().get(1);
        var v = new HelpMenuGeometry.Rect(40, 50, 80, 20);
        assertSame(target, narrow.hitInViewport(v, target.bounds().y(), 40 + target.bounds().x(), 50));
        assertNull(narrow.hitInViewport(v, target.bounds().y(), v.right(), 50));
        assertNull(narrow.hitInViewport(v, target.bounds().y(), 40, v.y() - 1));
        assertNull(narrow.hitInViewport(v, target.bounds().y(), 40, v.bottom()));
        assertNull(wide.hit(0, 0)); // Plain prefix cannot activate its neighboring link.
    }
    @Test void scrollClampsAfterResizeAndCannotOverflowOnExtremeInput() {
        var scroll = new HelpScrollPane();
        scroll.update(1000, 100);
        assertTrue(scroll.scroll(Integer.MAX_VALUE)); assertEquals(900, scroll.offset());
        assertFalse(scroll.scroll(Integer.MAX_VALUE));
        scroll.update(1000, 900); assertEquals(100, scroll.offset());
        scroll.scroll(Integer.MIN_VALUE); assertEquals(0, scroll.offset());
        scroll.reveal(950, 975); assertEquals(75, scroll.offset());
        var track = new HelpMenuGeometry.Rect(0, 10, 3, 90);
        var thumb = scroll.thumb(track);
        assertTrue(thumb.y() >= track.y() && thumb.bottom() <= track.bottom());
        scroll.update(10, 900); assertEquals(0, scroll.offset()); assertFalse(scroll.canScroll());
    }
    @Test void exampleStacksAreDetachedAndTheGuideHasNoContainerScreen() {
        assertEquals(Screen.class, HelpMenuScreen.class.getSuperclass());
        for (var topic : HelpDemoItems.TOPICS) {
            var first = HelpDemoItems.create(topic);
            var second = HelpDemoItems.create(topic);
            assertFalse(first.isEmpty(), topic);
            assertEquals(first.size(), second.size());
            for (int i = 0; i < first.size(); i++) {
                assertNotSame(first.get(i), second.get(i));
                assertEquals(1, first.get(i).getCount());
                first.get(i).setCount(0);
                assertEquals(1, second.get(i).getCount());
                assertEquals(1, HelpDemoItems.create(topic).get(i).getCount());
            }
        }
        assertTrue(HelpDemoItems.create("unknown").isEmpty());
    }
    @Test void keyHintsRemainReadOnlyMetadataAndSurviveWrapping() {
        var key = Component.literal("Feed Pack hotkey").withStyle(s -> s.withInsertion(HelpRichTextRenderer.KEY_PREFIX + "bogatyr_feed_pack"));
        var block = new HelpMenuContent.HelpBlock(HelpMenuContent.Kind.PARAGRAPH, Component.empty(), key);
        var layout = HelpRichTextRenderer.layout(TYPE, List.of(block), 70, id -> List.of());
        assertFalse(layout.targets().isEmpty());
        assertTrue(layout.targets().stream().allMatch(t -> t.pageId() == null && t.item() == null && t.keyId().equals("bogatyr_feed_pack")));
    }
}
