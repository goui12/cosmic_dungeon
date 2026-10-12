package net.goui.cosmicdungeon.client.screen.skills;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Delayed legacy snapshots cannot revive retired resources or actions. */
final class ClassResourcePresentationTest {
    private ClassResourceSnapshot snapshot(String resource,int amount,boolean alive){
        return new ClassResourceSnapshot(23,resource,amount,600,true,alive,true,9);
    }
    @Test void requestsAndRecyclingAreAbsentForEveryClassEvenWithLegacyConsent(){
        for(String id:List.of("none","judicator","dragoon","pyroclast","venefex","theurgist","bogatyr"))
            for(String resource:List.of("kibble","brewing_supplies")){
                var panel=ClassResourcePresentation.panel(id,snapshot(resource,147,true),false,true);
                assertNull(panel.resourceSnapshot());
                assertTrue(panel.actions().stream().noneMatch(action->List.of("request_supplies","recycle").contains(action.id())));
                assertFalse(panel.resource().contains("Kibble"));assertFalse(panel.resource().contains("Brewing Supplies"));
            }
    }
    @Test void retiredBalancesAndPendingStatusNeverChangePresentation(){
        for(String id:List.of("bogatyr","theurgist")){
            var initial=SkillsPanelModel.initial(id);
            assertEquals(initial,ClassResourcePresentation.panel(id,null,false));
            for(boolean alive:List.of(false,true))
                for(boolean pending:List.of(false,true)){
                    var panel=ClassResourcePresentation.panel(id,snapshot(id.equals("bogatyr")?"kibble":"brewing_supplies",600,alive),pending,true);
                    assertEquals(initial,panel);
                    assertTrue(panel.actions().stream().noneMatch(action->action.tooltip().getString().contains("Kibble")
                            ||action.tooltip().getString().contains("Brewing Supplies")));
                }
        }
    }
    @Test void theurgistRetainsRealIngredientGuide(){
        var panel=ClassResourcePresentation.panel("theurgist",snapshot("brewing_supplies",600,true),false,true);
        assertTrue(panel.actions().getFirst().tooltip().getString().contains("Craft..."));
    }
}
