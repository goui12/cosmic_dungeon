package net.goui.cosmicdungeon.client;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/** Testable input decisions; no native window, key mapping, packet or saved player state. */
public final class SkillInputPolicy {
    private SkillInputPolicy() {}
    public enum Action {
        PATROL("bogatyr_patrol", "bogatyr", "Patrol", 49),
        STAY("bogatyr_stay", "bogatyr", "Stay", 50),
        FEED_PACK("bogatyr_feed_pack", "bogatyr", "Feed Pack", 51),
        RESURRECT("theurgist_resurrect", "theurgist", "Resurrect...", 52);
        public final String id, classId, label;
        public final int defaultKey;
        Action(String id, String classId, String label, int defaultKey) {
            this.id = id; this.classId = classId; this.label = label; this.defaultKey = defaultKey;
        }
        public static Action forId(String id) {
            if (id == null) return null;
            if (id.startsWith("theurgist_resurrect:") || id.equals("theurgist_resurrect_none")) return RESURRECT;
            for (var action : values()) if (action.id.equals(id)) return action;
            return null;
        }
    }
    public enum Device { KEY, SCANCODE, MOUSE }
    public enum Modifier { NONE, SHIFT, CONTROL, ALT }
    public record Input(Device device, int code) {}
    public record Binding(Action action, Input input, Modifier modifier, boolean unbound) {}
    public record Context(boolean connected, boolean alive, boolean spectator, boolean focused,
                          boolean overlay, boolean allowedScreen, boolean busy, boolean typing, boolean currentMenu) {}
    public static boolean allowed(Context c) {
        return c.connected && c.alive && !c.spectator && c.focused && !c.overlay
                && c.allowedScreen && !c.busy && !c.typing && c.currentMenu;
    }
    /** null modifier represents an unsupported multi-modifier chord; lock keys are ignored by the adapter. */
    /** GLFW modifier bits: Shift=1, physical Ctrl=2, Alt=4, Super=8; ignore lock flags. */
    public static Modifier modifier(int bits, int platformControlBit) {
        int mask = bits & 15;
        if (mask == 0) return Modifier.NONE;
        if (mask == 1) return Modifier.SHIFT;
        if (mask == platformControlBit) return Modifier.CONTROL;
        if (mask == 4) return Modifier.ALT;
        return null;
    }
    public static boolean matches(Binding binding, Input input, Modifier modifier) {
        return !binding.unbound && modifier != null && binding.modifier == modifier && binding.input.equals(input);
    }
    public static Action resolve(String classId, List<Binding> bindings, Input input, Modifier modifier,
                                 boolean externalConflict) {
        if (externalConflict) return null;
        Action found = null;
        for (var binding : bindings) {
            if (!binding.action.classId.equals(classId) || !matches(binding, input, modifier)) continue;
            if (found != null) return null; // Fail closed instead of arbitrarily selecting a conflicting action.
            found = binding.action;
        }
        return found;
    }
    public static boolean dispatch(Action action, String classId, Predicate<String> bogatyr,
                                   BooleanSupplier canResurrect, Runnable openPicker) {
        if (action == null || !action.classId.equals(classId)) return false;
        if (action != Action.RESURRECT) return bogatyr.test(action.id);
        if (!canResurrect.getAsBoolean()) return false;
        openPicker.run();
        return true;
    }
    /** One physical press, even if a screen opens/closes while the key is held. Releases never execute actions. */
    public static final class Presses {
        private final Set<Input> down = new HashSet<>();
        public boolean press(Input input) { return down.add(input); }
        public void release(Input input) { down.remove(input); }
        public void clear() { down.clear(); }
    }
}
