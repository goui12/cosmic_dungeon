package net.goui.cosmicdungeon.client.screen.skills;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SkillsPanelModelTest {
    @Test void existingClassIdsKeepTheirGuideIdentityAndOnlyInitialRolesHaveActionSlots(){
        for(var id:List.of("bogatyr","theurgist","judicator","dragoon","pyroclast","venefex")){
            var model=SkillsPanelModel.initial(id);
            assertEquals(id,model.classId());
            assertEquals("Resource: —",model.resource());
            assertEquals("No resource balance available.",model.resourceTooltip());
            if(Set.of("bogatyr","theurgist").contains(id)){
                assertEquals(1,model.actions().size());
                assertEquals("guide",model.actions().getFirst().id());
                assertTrue(model.actions().getFirst().enabled());
            }else assertTrue(model.actions().isEmpty(),"No invented actions for "+id);
        }
        assertEquals("none",SkillsPanelModel.initial("unknown").classId());
        assertTrue(SkillsPanelModel.initial("none").actions().isEmpty());
    }
    @Test void futureResourceAndDeadPlayerRowsUseTheSameImmutableModelWithoutInventingCounts(){
        var actions=new ArrayList<SkillsPanelModel.Action>();
        for(int i=0;i<20;i++)actions.add(new SkillsPanelModel.Action("action_"+i,"Action "+i,"Server-validated provider action",i%2==0));
        var model=new SkillsPanelModel("theurgist","Theurgist Skills","Brewing Supplies: 17","Authoritative snapshot",actions);
        actions.clear();assertEquals(20,model.actions().size());assertEquals("Brewing Supplies: 17",model.resource());
        assertThrows(UnsupportedOperationException.class,()->model.actions().clear());
        var action=model.actions().getFirst();
        assertThrows(IllegalArgumentException.class,()->new SkillsPanelModel("theurgist","Skills","—","",List.of(action,action)));
    }
}
