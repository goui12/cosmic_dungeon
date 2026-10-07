package net.goui.cosmicdungeon.client.screen.skills;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PanelScrollTest {
    @Test void wheelTrackThumbAndCaptureShareTheSameBoundedRange(){
        var scroll=new PanelScroll();scroll.configure(600,100);
        assertEquals(500,scroll.max());assertEquals(16,scroll.thumbHeight());
        assertTrue(scroll.wheel(-2));assertEquals(48,scroll.offset());
        assertTrue(scroll.press(20,scroll.thumbTop(20)+3));assertTrue(scroll.captured());
        assertTrue(scroll.drag(20,10000));assertEquals(500,scroll.offset());
        assertEquals(104,scroll.thumbTop(20));
        assertTrue(scroll.drag(20,-10000));assertEquals(0,scroll.offset());
        assertTrue(scroll.release());assertFalse(scroll.release());assertFalse(scroll.drag(20,80));
        assertTrue(scroll.press(20,119));assertEquals(500,scroll.offset());
        scroll.configure(20,100);assertEquals(0,scroll.offset());assertFalse(scroll.captured());
        assertEquals(100,scroll.thumbHeight());assertFalse(scroll.wheel(1));
    }
    @Test void emptyTinyAndNonFiniteInputCannotCorruptScroll(){
        var scroll=new PanelScroll();scroll.configure(0,0);
        assertEquals(0,scroll.thumbHeight());assertFalse(scroll.press(0,1));
        scroll.configure(Integer.MAX_VALUE,20);
        assertTrue(scroll.wheel(-Double.MAX_VALUE));assertEquals(Integer.MAX_VALUE-20,scroll.offset());
        assertFalse(scroll.wheel(Double.NaN));assertFalse(scroll.press(0,Double.POSITIVE_INFINITY));
        scroll.reset();assertEquals(0,scroll.offset());
    }
}
