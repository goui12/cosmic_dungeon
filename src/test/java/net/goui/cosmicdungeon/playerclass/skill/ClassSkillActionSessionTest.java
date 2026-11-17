package net.goui.cosmicdungeon.playerclass.skill;

import java.util.List;
import net.goui.cosmicdungeon.network.ClassSkillPayloads;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ClassSkillActionSessionTest {
    private static ClassSkillPayloads.View snapshot(ClassSkillActions.Session session) {
        return new ClassSkillPayloads.View(17,session.revision,"dragoon",ClassSkillActions.slots(),List.of());
    }
    @Test void acceptedResponseThenThrottledPressStillReceivesPeriodicAcknowledgement() {
        var session=new ClassSkillActions.Session();
        var initial=session.response(snapshot(session),true);
        assertEquals(1,initial.revision());
        assertTrue(session.acceptAction(100));
        var acceptedResponse=session.response(snapshot(session),true);
        assertEquals(2,acceptedResponse.revision());

        // The client clears pending on the first response and can press again before five ticks pass.
        assertFalse(session.acceptAction(101));
        assertTrue(session.acknowledgementPending);
        assertEquals(2,session.revision,"Throttling itself sends no packet and does not advance the session");

        // The ordinary once-per-second poll must acknowledge even though all capabilities are unchanged.
        var acknowledgement=session.response(snapshot(session),false);
        assertNotNull(acknowledgement);
        assertEquals(3,acknowledgement.revision());
        assertEquals(acceptedResponse.slots(),acknowledgement.slots());
        assertEquals(acceptedResponse.tracks(),acknowledgement.tracks());
        assertFalse(session.acknowledgementPending);
        assertNull(session.response(snapshot(session),false),"An unchanged idle session resumes delta suppression");
    }
    @Test void manyRejectedPressesCoalesceAndDoNotExtendTheThrottleWindow() {
        var session=new ClassSkillActions.Session();
        session.response(snapshot(session),true);
        assertTrue(session.acceptAction(100));
        session.response(snapshot(session),true);
        for(int i=0;i<1000;i++)assertFalse(session.acceptAction(101+i%4));
        assertEquals(2,session.revision);
        assertEquals(100,session.lastAction);
        var acknowledgement=session.response(snapshot(session),false);
        assertNotNull(acknowledgement);
        assertEquals(3,acknowledgement.revision(),"A thousand blocked presses require one acknowledgement");
        assertNull(session.response(snapshot(session),false));
        assertTrue(session.acceptAction(105),"Blocked presses must not postpone the next eligible action");
    }
    @Test void forcedLifecycleResponseAlsoClearsDeferredAcknowledgement() {
        var session=new ClassSkillActions.Session();
        session.response(snapshot(session),true);
        assertTrue(session.acceptAction(100));
        assertFalse(session.acceptAction(101));
        var ended=new ClassSkillPayloads.View(0,session.revision,"dragoon",ClassSkillActions.slots(),List.of());
        var response=session.response(ended,true);
        assertEquals(0,response.run());
        assertFalse(session.acknowledgementPending);
        assertNull(session.response(new ClassSkillPayloads.View(0,session.revision,"dragoon",
                ClassSkillActions.slots(),List.of()),false));
    }
}
