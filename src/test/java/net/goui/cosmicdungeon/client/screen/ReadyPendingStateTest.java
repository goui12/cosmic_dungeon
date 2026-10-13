package net.goui.cosmicdungeon.client.screen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ReadyPendingStateTest {
    @Test void authoritativeAcknowledgementImmediatelyReleasesTheWait() {
        var state = new ReadyPendingState();
        assertFalse(state.pending());
        state.begin(); assertTrue(state.pending());
        state.tick(); state.acknowledge(); assertFalse(state.pending());
        for (int tick = 0; tick < 100; tick++) state.tick();
        assertFalse(state.pending());
    }

    @Test void silentThrottleExpiresExactlyAtTheSixtyTickBoundary() {
        var state = new ReadyPendingState(); state.begin();
        for (int tick = 1; tick < 60; tick++) {
            state.tick(); assertTrue(state.pending(), "Still waiting before tick 60");
        }
        state.tick(); assertFalse(state.pending());
        state.tick(); assertFalse(state.pending(), "The counter cannot underflow into another wait");
    }

    @Test void restartAndResetDoNotInheritTheOldDeadline() {
        var state = new ReadyPendingState(); state.begin();
        for (int tick = 0; tick < 59; tick++) state.tick();
        state.begin(); state.tick(); assertTrue(state.pending(), "A new request gets its own wait");
        state.acknowledge(); state.begin();
        for (int tick = 0; tick < 59; tick++) state.tick();
        assertTrue(state.pending());
        state.tick(); assertFalse(state.pending());
        state.acknowledge(); assertFalse(state.pending());
    }
}
