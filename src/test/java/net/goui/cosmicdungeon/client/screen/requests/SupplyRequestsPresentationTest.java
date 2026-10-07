package net.goui.cosmicdungeon.client.screen.requests;

import java.util.*;
import net.goui.cosmicdungeon.client.screen.skills.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SupplyRequestsPresentationTest {
    @Test void enabledRequestPreservesExactStyledWordingAndRequiresRealLivingResourceHeadroom(){
        var resource=new ClassResourceSnapshot(8,"brewing_supplies",12,600,true,true,false,3);
        var model=ClassResourcePresentation.panel("theurgist",resource,false,true);
        var request=model.actions().getFirst();assertTrue(request.enabled());
        assertEquals("Click to request supplies from your group. If they accept, their supplies will be automatically recycled into your brewing supplies",
                request.tooltip().getString());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.YELLOW),request.tooltip().getStyle().getColor());
        var name=request.tooltip().getSiblings().getFirst();assertTrue(name.getStyle().isBold());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.BLUE),name.getStyle().getColor());
        assertFalse(ClassResourcePresentation.panel("theurgist",null,false,true).actions().getFirst().enabled());
        assertFalse(ClassResourcePresentation.panel("theurgist",resource,true,true).actions().getFirst().enabled());
        assertFalse(ClassResourcePresentation.panel("theurgist",resource,false,false).actions().getFirst().enabled());
        var dead=new ClassResourceSnapshot(8,"brewing_supplies",12,600,true,false,false,3);
        var full=new ClassResourceSnapshot(8,"brewing_supplies",600,600,true,true,false,3);
        assertFalse(ClassResourcePresentation.panel("theurgist",dead,false,true).actions().getFirst().enabled());
        assertFalse(ClassResourcePresentation.panel("theurgist",full,false,true).actions().getFirst().enabled());
        var kibble=new ClassResourceSnapshot(8,"kibble",1,600,true,true,false,3);
        assertTrue(ClassResourcePresentation.panel("bogatyr",kibble,false,true).actions().getFirst().tooltip().getString().endsWith("Kibble"));
    }
    @Test void hoverExposesFullRequesterClassAndExactQuotedYieldAndHonestBulkSemantics(){
        var card=SupplyRequestsSnapshotTest.card(UUID.randomUUID(),17,true);var details=SupplyRequestsComponent.details(card);
        assertEquals(List.of("Requester","Theurgist","Credits 17 Brewing Supplies"),details.stream().map(c->c.getString()).toList());
        assertTrue(details.get(2).getStyle().isBold());assertEquals(TextColor.fromLegacyFormat(ChatFormatting.BLUE),details.get(2).getStyle().getColor());
        assertEquals("Accept these requests in order, using only the supplies and capacity still available.",SupplyRequestsComponent.ACCEPT_ALL_TOOLTIP);
    }
}
