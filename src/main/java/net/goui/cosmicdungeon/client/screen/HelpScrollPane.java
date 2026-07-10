package net.goui.cosmicdungeon.client.screen;

final class HelpScrollPane {
    private int offset;
    private int contentHeight;
    private int viewportHeight;

    void update(int contentHeight, int viewportHeight) {
        this.contentHeight = Math.max(0, contentHeight);
        this.viewportHeight = Math.max(0, viewportHeight);
        clamp();
    }

    int offset() { return offset; }
    boolean canScrollUp() { return offset > 0; }
    boolean canScrollDown() { return offset < maxOffset(); }
    boolean canScroll() { return maxOffset() > 0; }
    void reset() { offset = 0; }

    boolean scroll(int amount) {
        int old = offset;
        offset = Math.max(0, Math.min(offset + amount, maxOffset()));
        return offset != old;
    }

    private int maxOffset() { return Math.max(0, contentHeight - viewportHeight); }
    private void clamp() { offset = Math.max(0, Math.min(offset, maxOffset())); }
}
