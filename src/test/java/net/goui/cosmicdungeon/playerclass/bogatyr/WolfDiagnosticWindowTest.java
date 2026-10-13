package net.goui.cosmicdungeon.playerclass.bogatyr;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class WolfDiagnosticWindowTest {
    @Test void floodKeepsExactTotalsAndFixedMemoryWithoutConstructingDiscardedSamples(){
        var window=new WolfDiagnosticWindow();var calls=new AtomicInteger();
        for(int i=0;i<1000;i++)window.record(WolfDiagnosticWindow.Event.ADDED_FRESH,"run="+i,()->"sample"+calls.incrementAndGet());
        assertEquals(1000,window.count(WolfDiagnosticWindow.Event.ADDED_FRESH));
        assertEquals(2,calls.get());assertEquals(2,window.samples());
        assertEquals(16,window.contexts());assertEquals(984,window.overflow());
        window.record(WolfDiagnosticWindow.Event.REJECT_IDENTITY,"run=0",()->"duplicate");
        assertEquals(1,window.count(WolfDiagnosticWindow.Event.REJECT_IDENTITY));
        assertTrue(window.summary().contains("ADDED_FRESH=1000"));
        window.clear();assertTrue(window.empty());assertEquals(0,window.samples());assertEquals(0,window.contexts());
    }
}
