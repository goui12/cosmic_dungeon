package net.goui.cosmicdungeon.client.screen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ReadyActionStyleTest {
    @Test void pulseStaysOpaqueGreenAndChangesEachChannelByAtMostTwelveLevels() {
        int[] low = {255,255,255}, high = {0,0,0};
        for (long time = 0; time <= ReadyActionStyle.PERIOD_MILLIS; time++) {
            int color = ReadyActionStyle.tint(time,false);
            assertEquals(255,color >>> 24);
            int red = color >> 16 & 255, green = color >> 8 & 255, blue = color & 255;
            assertTrue(green > red && green > blue);
            int[] channels = {red,green,blue};
            for (int i = 0; i < 3; i++) { low[i] = Math.min(low[i],channels[i]); high[i] = Math.max(high[i],channels[i]); }
        }
        for (int i = 0; i < 3; i++) {
            assertTrue(high[i] > low[i],"The ready prompt gently brightens and dims");
            assertTrue(high[i]-low[i] <= 12,"The small pulse must not become a flashing effect");
        }
    }
    @Test void cycleIsTwoSecondsAndSeamlessWhileHoverOrFocusIsSteady() {
        for (long time : new long[] {0,1,499,1000,1501,1999,Long.MAX_VALUE-2000}) {
            assertEquals(ReadyActionStyle.tint(time,false),ReadyActionStyle.tint(time+2000,false));
            assertEquals(ReadyActionStyle.HIGHLIGHT,ReadyActionStyle.tint(time,true));
        }
        assertEquals(ReadyActionStyle.tint(0,false),ReadyActionStyle.tint(1999,false));
        assertEquals(ReadyActionStyle.tint(1,false),ReadyActionStyle.tint(1999,false));
    }
}
