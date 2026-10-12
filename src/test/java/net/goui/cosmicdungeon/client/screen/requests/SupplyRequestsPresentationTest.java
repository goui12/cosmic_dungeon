package net.goui.cosmicdungeon.client.screen.requests;

import java.util.*;
import net.goui.cosmicdungeon.client.screen.skills.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SupplyRequestsPresentationTest {
    @Test void retiredRequestActionsStayAbsentRegardlessOfOldHeadroomOrConsent(){
        for(String id:List.of("theurgist","bogatyr"))
            for(int amount:new int[]{0,12,600})
                for(boolean alive:new boolean[]{false,true})
                    for(boolean consent:new boolean[]{false,true}){
                        var resource=new ClassResourceSnapshot(8,id.equals("theurgist")?"brewing_supplies":"kibble",amount,600,true,alive,false,3);
                        var model=ClassResourcePresentation.panel(id,resource,false,consent);
                        assertTrue(model.actions().stream().noneMatch(action->action.id().equals("request_supplies")));
                    }
    }
    @Test void hoverExposesFullRequesterClassAndExactQuotedYieldAndHonestBulkSemantics(){
        var card=SupplyRequestsSnapshotTest.card(UUID.randomUUID(),17,true);var details=SupplyRequestsComponent.details(card);
        assertEquals(List.of("Requester","Theurgist","Credits 17 Brewing Supplies"),details.stream().map(c->c.getString()).toList());
        assertTrue(details.get(2).getStyle().isBold());assertEquals(TextColor.fromLegacyFormat(ChatFormatting.BLUE),details.get(2).getStyle().getColor());
        assertEquals("Accept these requests in order, using only the supplies and capacity still available.",SupplyRequestsComponent.ACCEPT_ALL_TOOLTIP);
    }
}
