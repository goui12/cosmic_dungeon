package net.goui.cosmicdungeon.client.screen;

/** Square artwork with space reserved for title, caption and Continue. */
final class TamsinMapLayout {
    static final int TEXTURE_SIZE = 516;
    private TamsinMapLayout() {}
    record View(int width, int height, int mapEdge) {}
    static View forViewport(int width, int height) {
        int edge = Math.max(64, Math.min(TEXTURE_SIZE, Math.min(width - 52, height - 106)));
        return new View(Math.max(300, edge + 36), edge + 90, edge);
    }
}
