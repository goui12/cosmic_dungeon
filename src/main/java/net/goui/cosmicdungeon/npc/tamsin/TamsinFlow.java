package net.goui.cosmicdungeon.npc.tamsin;

/** Server-owned conversation state. Acceptance is permanent; map viewing is menu-local. */
public final class TamsinFlow {
    public enum Stage { AGREEMENT, MAP, SELECTOR, READY, TAX }
    private TamsinFlow() {}
    public static Stage initial(boolean accepted, boolean selected) {
        return !accepted ? Stage.AGREEMENT : selected ? Stage.READY : Stage.SELECTOR;
    }
    public static Stage advance(Stage current, String action, boolean selected) {
        if (current == Stage.AGREEMENT && action.equals("yes")) return Stage.MAP;
        if (current == Stage.MAP && action.equals("continue")) return selected ? Stage.READY : Stage.SELECTOR;
        return current;
    }
    public static boolean canSelect(boolean accepted, Stage stage) {
        return accepted && stage == Stage.SELECTOR;
    }
    public static boolean canReady(boolean accepted, Stage stage) {
        return accepted && (stage == Stage.SELECTOR || stage == Stage.READY);
    }
}
