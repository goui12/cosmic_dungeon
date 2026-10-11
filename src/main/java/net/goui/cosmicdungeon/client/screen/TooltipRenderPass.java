package net.goui.cosmicdungeon.client.screen;

/** One final tooltip flush after the screen and all Post overlays, scoped to one graphics instance. */
public final class TooltipRenderPass {
    private int depth;
    public boolean deferred() { return depth > 0; }
    public void render(Runnable content, Runnable flush, Runnable discard) {
        depth++;
        boolean complete = false;
        try {
            content.run();
            complete = true;
        } finally {
            if (--depth == 0) {
                try { if (complete) flush.run(); }
                finally { discard.run(); }
            }
        }
    }
}
