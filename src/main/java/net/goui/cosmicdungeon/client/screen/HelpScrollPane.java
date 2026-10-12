package net.goui.cosmicdungeon.client.screen;

final class HelpScrollPane {
    private int offset, contentHeight, viewportHeight;
    void update(int contentHeight, int viewportHeight) {
        this.contentHeight = Math.max(0, contentHeight);
        this.viewportHeight = Math.max(0, viewportHeight);
        scrollTo(offset);
    }
    int offset() { return offset; }
    int contentHeight() { return contentHeight; }
    int maxOffset() { return Math.max(0, contentHeight - viewportHeight); }
    boolean canScrollUp() { return offset > 0; }
    boolean canScrollDown() { return offset < maxOffset(); }
    boolean canScroll() { return maxOffset() > 0; }
    void reset() { offset = 0; }
    boolean scroll(int amount) { return scrollTo((long) offset + amount); }
    boolean scrollTo(long position) {
        int old = offset;
        offset = (int) Math.max(0L, Math.min(position, maxOffset()));
        return offset != old;
    }
    void reveal(int top, int bottom) {
        if (top < offset) scrollTo(top);
        else if (bottom > (long) offset + viewportHeight) scrollTo((long) bottom - viewportHeight);
    }
    HelpMenuGeometry.Rect thumb(HelpMenuGeometry.Rect track) {
        int h = Math.min(track.h(), Math.max(8, (int) ((long) track.h() * viewportHeight / Math.max(1, contentHeight))));
        int y = maxOffset() == 0 ? 0 : (int) ((long) (track.h() - h) * offset / maxOffset());
        return new HelpMenuGeometry.Rect(track.x(), track.y() + y, track.w(), h);
    }
}
