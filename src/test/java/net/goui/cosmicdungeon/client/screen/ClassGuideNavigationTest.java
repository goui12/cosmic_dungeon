package net.goui.cosmicdungeon.client.screen;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ClassGuideNavigationTest {
    @Test void everyClassShortcutUsesItsExactExistingDirectoryPage() {
        var directory=HelpMenuContent.classDirectory();
        var ids=List.of("bogatyr","dragoon","judicator","pyroclast","theurgist","venefex");
        assertEquals(ids.size(),directory.children().size());
        for (String id:ids) {
            var page=HelpMenuContent.classGuide(id);
            assertEquals("class."+id,page.id());
            assertTrue(page.enabled());
            assertTrue(directory.children().stream().anyMatch(node->node.page()==page));
        }
    }
    @Test void missingOrUnsupportedClassOpensIndexWithoutInventingAbilityPages() {
        for (String id:new String[]{null,"","unknown","deadeye"})
            assertSame(HelpMenuContent.CLASSES,HelpMenuContent.classGuide(id));
    }
}
