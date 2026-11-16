package net.goui.cosmicdungeon.network;

import io.netty.buffer.Unpooled;
import java.util.*;
import net.goui.cosmicdungeon.playerclass.skill.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ClassSkillPayloadsTest {
    @Test void viewAndActionRoundTripWithExactSessionIdentity(){
        var view=new ClassSkillPayloads.View(72,9,"judicator",ClassSkillActions.slots(),
                List.of(new ClassSkillPayloads.Track("smite","Smite","SM",2,3,20)));
        var b=Unpooled.buffer();
        try{
            ClassSkillPayloads.View.STREAM_CODEC.encode(b,view);
            assertEquals(view,ClassSkillPayloads.View.STREAM_CODEC.decode(b));assertEquals(0,b.readableBytes());
            var action=new ClassSkillPayloads.Action(72,9,"judicator",ClassSkillSlot.PRIMARY);
            ClassSkillPayloads.Action.STREAM_CODEC.encode(b,action);
            assertEquals(action,ClassSkillPayloads.Action.STREAM_CODEC.decode(b));assertEquals(0,b.readableBytes());
        }finally{b.release();}
    }
    @Test void staleRunClassAndRevisionNeverAuthorizeAnySlot(){
        var view=new ClassSkillPayloads.View(72,9,"judicator",ClassSkillActions.slots(),List.of());
        var action=new ClassSkillPayloads.Action(72,9,"judicator",ClassSkillSlot.PRIMARY);
        assertTrue(ClassSkillActions.matches(view,action,72,"judicator"));
        assertFalse(ClassSkillActions.matches(view,action,73,"judicator"));
        assertFalse(ClassSkillActions.matches(view,action,72,"dragoon"));
        assertFalse(ClassSkillActions.matches(view,new ClassSkillPayloads.Action(72,8,"judicator",ClassSkillSlot.PRIMARY),72,"judicator"));
        assertFalse(ClassSkillActions.matches(null,action,72,"judicator"));
        assertFalse(ClassSkillActions.matches(view,action,0,"judicator"));
        assertTrue(view.slots().stream().noneMatch(ClassSkillPayloads.SlotView::enabled));
    }
    @Test void untrustedPayloadsHaveStrictBoundsAndUniqueSlots(){
        assertThrows(IllegalArgumentException.class,()->new ClassSkillPayloads.Action(0,1,"dragoon",ClassSkillSlot.PRIMARY));
        assertThrows(IllegalArgumentException.class,()->new ClassSkillPayloads.View(1,1,"dragoon",
                Collections.nCopies(4,ClassSkillPayloads.SlotView.unassigned(ClassSkillSlot.PRIMARY)),List.of()));
        assertThrows(IllegalArgumentException.class,()->new ClassSkillPayloads.Track("smite","Smite","SM",1,10,10));
        assertThrows(IllegalArgumentException.class,()->new ClassSkillPayloads.SlotView(ClassSkillSlot.PRIMARY,"","Ready","",true,0,0,0));
        assertThrows(IllegalArgumentException.class,()->new ClassSkillPayloads.View(1,1,"dragoon",ClassSkillActions.slots(),
                List.of(new ClassSkillPayloads.Track("smite","Smite","SM",1,0,10))));
        var b=Unpooled.buffer();
        try{
            b.writeByte(0x7f);assertThrows(RuntimeException.class,()->ClassSkillPayloads.View.STREAM_CODEC.decode(b));
        }finally{b.release();}
    }
}
