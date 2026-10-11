package net.goui.cosmicdungeon.client.screen;

/** Scaled-pixel geometry shared by native Tamsin controls and their roster hit testing. */
record D1PartyPanelLayout(int width, int height) {
    static D1PartyPanelLayout forViewport(int width, int height) {
        return new D1PartyPanelLayout(Math.min(360, Math.max(1, width - 16)),
                Math.min(240, Math.max(1, height - 16)));
    }
    int innerWidth() { return width - 20; }
    int halfWidth() { return (innerWidth() - 6) / 2; }
    int rightColumn() { return 10 + halfWidth() + 6; }
    int progressionY() { return height - 28; }
    int managementY() { return height - 52; }
    int navigationY() { return height - 76; }
    int thirdWidth() { return (innerWidth() - 12) / 3; }
    int thirdX(int index) { return 10 + index * (thirdWidth() + 6); }
}
