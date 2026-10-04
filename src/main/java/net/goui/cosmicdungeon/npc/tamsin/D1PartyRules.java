package net.goui.cosmicdungeon.npc.tamsin;

/** Shared admission boundaries; maximum capacity is never a required party size. */
public final class D1PartyRules {
    private D1PartyRules() {}
    public static boolean fits(int members, int maximum) {
        return maximum >= 1 && maximum <= 6 && members >= 1 && members <= maximum;
    }
    public static boolean sameStartingDimension(String current, String starting) {
        return current != null && !current.isBlank() && current.equals(starting);
    }
}
