package net.goui.cosmicdungeon.client.screen;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TooltipRenderPassTest {
    @Test void nativeContentAndAllOverlaysPrecedeTheSingleTooltipFlush() {
        var pass = new TooltipRenderPass();
        var order = new ArrayList<String>();
        pass.render(() -> {
            order.add("screen");
            assertTrue(pass.deferred());
            pass.render(() -> order.add("nested overlay"), () -> fail("Early flush"), () -> fail("Early clear"));
            order.add("post overlay");
        }, () -> {
            assertFalse(pass.deferred());
            order.add("tooltip");
        }, () -> order.add("clear"));
        assertEquals(List.of("screen", "nested overlay", "post overlay", "tooltip", "clear"), order);
    }
    @Test void failedRenderDiscardsPendingTooltipAndNextFrameStillWorks() {
        var pass = new TooltipRenderPass();
        var order = new ArrayList<String>();
        assertThrows(IllegalStateException.class, () -> pass.render(
                () -> { throw new IllegalStateException("render failed"); },
                () -> fail("Do not draw failed frame"), () -> order.add("discard")));
        assertFalse(pass.deferred());
        pass.render(() -> {}, () -> order.add("next frame"), () -> order.add("clear"));
        assertEquals(List.of("discard", "next frame", "clear"), order);
    }
    @Test void failedTooltipFlushStillClearsFrameState() {
        var pass = new TooltipRenderPass();
        boolean[] cleared = {false};
        assertThrows(IllegalStateException.class, () -> pass.render(() -> {},
                () -> { throw new IllegalStateException(); }, () -> cleared[0] = true));
        assertTrue(cleared[0]);
        assertFalse(pass.deferred());
    }
}
