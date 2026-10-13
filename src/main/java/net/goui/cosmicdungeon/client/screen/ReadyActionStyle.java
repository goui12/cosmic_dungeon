package net.goui.cosmicdungeon.client.screen;

/** A quiet two-second green pulse; hovering/focusing holds a steady, brighter native highlight. */
final class ReadyActionStyle {
    static final long PERIOD_MILLIS = 2000;
    static final int HIGHLIGHT = 0xFFA8F0B4;
    private ReadyActionStyle() {}
    static int tint(long timeMillis, boolean highlighted) {
        if (highlighted) return HIGHLIGHT;
        double phase = Math.floorMod(timeMillis, PERIOD_MILLIS) / (double)PERIOD_MILLIS;
        int lift = (int)Math.round(6 * (1 - Math.cos(phase * Math.PI * 2)));
        return 0xFF000000 | (0x81 + lift) << 16 | (0xD2 + lift) << 8 | 0x8E + lift;
    }
}
