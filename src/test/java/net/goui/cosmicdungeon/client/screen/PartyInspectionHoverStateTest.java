package net.goui.cosmicdungeon.client.screen;

import java.util.List;
import java.util.UUID;
import net.goui.cosmicdungeon.network.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PartyInspectionHoverStateTest {
    private static final UUID A = new UUID(1, 1), B = new UUID(1, 2);
    private static PartyInspectionPayloads.View response(PartyInspectionPayloads.Request request) {
        return new PartyInspectionPayloads.View(request, true, "Member", "theurgist",
                PartyVitals.UNLOADED, List.of(new PartyInspectionPayloads.Reading("Run kills", "7")));
    }
    @Test void switchingRowsCannotFloodRequestsOrDisplayThePreviousMember() {
        var state = new PartyInspectionHoverState();
        state.hover(1, A);
        var first = state.tick();
        assertNotNull(first);
        state.receive(response(first));
        assertNotNull(state.view(A.toString()));
        state.hover(1, B);
        assertNull(state.view(A.toString()));
        for (int i = 0; i < 19; i++) {
            state.hover(1, i % 2 == 0 ? A : B);
            assertNull(state.tick());
        }
        state.hover(1, B);
        var second = state.tick();
        assertEquals(B, second.subject());
        state.receive(response(first));
        assertNull(state.view(B.toString()));
        state.receive(response(second));
        assertNotNull(state.view(B.toString()));
    }
    @Test void leavingReturningClosingAndChangingRunRejectOldReplies() {
        var state = new PartyInspectionHoverState();
        state.hover(1, A);
        var old = state.tick();
        state.clear();
        state.hover(1, A);
        state.receive(response(old));
        assertNull(state.view(A.toString()));
        state.hover(2, A);
        state.receive(response(old));
        assertNull(state.view(A.toString()));
        state.clear();
        for (int i = 0; i < 100; i++) assertNull(state.tick());
    }
    @Test void expiresStaleDataAndRejectsUnrequestedOrOutOfOrderReplies() {
        var state = new PartyInspectionHoverState();
        state.hover(1, A);
        var first = state.tick();
        state.receive(response(new PartyInspectionPayloads.Request(1, first.sequence() + 100, A)));
        assertNull(state.view(A.toString()));
        state.receive(response(first));
        PartyInspectionPayloads.Request second = null;
        for (int i = 0; i < 20; i++) { var request = state.tick(); if (request != null) second = request; }
        assertNotNull(second);
        var latest = response(second);
        state.receive(latest);
        state.receive(response(first));
        assertSame(latest, state.view(A.toString()));
        for (int i = 0; i < 41; i++) state.tick();
        assertNull(state.view(A.toString()));
        state.receive(PartyInspectionPayloads.View.denied(second));
        assertNull(state.view(A.toString()), "Duplicate sequence cannot resurrect expired data");
    }
}
