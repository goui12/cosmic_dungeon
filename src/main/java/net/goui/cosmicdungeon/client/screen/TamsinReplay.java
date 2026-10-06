package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.npc.tamsin.TamsinFlow.Stage;

/** Screen-local presentation; never changes the authoritative menu or permanent onboarding. */
final class TamsinReplay {
    private Stage preview;
    static boolean canStart(Stage live) { return live == Stage.SELECTOR || live == Stage.READY; }
    boolean active() { return preview != null; }
    Stage display(Stage live) { return active() ? preview : live; }
    boolean start(Stage live) {
        if (!canStart(live)) return false;
        preview = Stage.AGREEMENT;
        return true;
    }
    void receive(Stage live) {
        // A tax/onboarding transition supersedes a preview. Normal refresh/party updates do not.
        if (!canStart(live)) preview = null;
    }
    boolean action(String action) {
        if (!active()) return false;
        if (preview == Stage.AGREEMENT && action.equals("yes")) preview = Stage.MAP;
        else if (action.equals("no") || preview == Stage.MAP && action.equals("continue")) preview = null;
        // Even an obsolete preview click stays local rather than becoming a real onboarding action.
        return true;
    }
}
