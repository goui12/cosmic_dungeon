package net.goui.cosmicdungeon.client.screen;
import java.util.List;
import net.goui.cosmicdungeon.playerclass.resource.ClassResourceKind;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ClassResourceHelpTest {
    @Test void guideListsExactlyTheResolvedServerItemsWithoutReplacingPage(){
        var page=HelpMenuContent.classGuide("theurgist");var before=page.blocks();
        var blocks=ClassResourceHelp.assemble(page,ClassResourceKind.BREWING_SUPPLIES,
                List.of(Component.literal("Server herb"),Component.literal("Server stone")));
        assertSame(before,page.blocks());
        var tail=blocks.subList(before.size(),blocks.size());
        assertEquals(List.of("Server herb","Server stone"),tail.stream()
                .filter(b->b.kind()==HelpMenuContent.Kind.BULLET).map(b->b.text().getString()).toList());
        assertEquals(2,tail.stream().filter(b->b.kind()==HelpMenuContent.Kind.BULLET).count());
    }
}
