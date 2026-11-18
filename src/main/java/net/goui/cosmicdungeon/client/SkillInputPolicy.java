package net.goui.cosmicdungeon.client;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.goui.cosmicdungeon.playerclass.skill.ClassSkillSlot;

/** Testable input decisions; no native window, key mapping, packet or saved player state. */
public final class SkillInputPolicy {
    private SkillInputPolicy() {}
    public enum Action {
        PATROL("bogatyr_patrol", "bogatyr", "Patrol", 49),
        STAY("bogatyr_stay", "bogatyr", "Stay", 50),
        FEED_PACK("bogatyr_feed_pack", "bogatyr", "Feed Pack", 51),
        RESURRECT("theurgist_resurrect", "theurgist", "Resurrect...", 52),
        PRIMARY("primary_class_skill", ClassSkillSlot.PRIMARY, 71),
        SKILL_1("class_skill_1", ClassSkillSlot.SKILL_1, 90),
        SKILL_2("class_skill_2", ClassSkillSlot.SKILL_2, 88),
        SKILL_3("class_skill_3", ClassSkillSlot.SKILL_3, 67);
        public final String id, classId, label;
        public final int defaultKey;
        public final Modifier defaultModifier;
        public final ClassSkillSlot slot;
        Action(String id, String classId, String label, int defaultKey) {
            this.id = id; this.classId = classId; this.label = label; this.defaultKey = defaultKey;
            this.defaultModifier = Modifier.ALT; this.slot = null;
        }
        Action(String id, ClassSkillSlot slot, int defaultKey) {
            this.id = id; this.classId = ""; this.label = slot.label(); this.defaultKey = defaultKey;
            this.defaultModifier = Modifier.NONE; this.slot = slot;
        }
        public boolean supports(String selectedClass) { return supportsClass(classId, selectedClass); }
        public static Action forSlot(ClassSkillSlot slot) {
            for (var action : values()) if (action.slot == slot && slot != null) return action;
            throw new IllegalArgumentException("Unknown class skill slot");
        }
        public static Action forId(String id) {
            if (id == null) return null;
            if (id.startsWith("theurgist_resurrect:") || id.equals("theurgist_resurrect_none")) return RESURRECT;
            for (var action : values()) if (action.id.equals(id)) return action;
            return null;
        }
    }
    public static boolean supportsClass(String actionClass, String selectedClass) {
        return selectedClass != null && (actionClass.isEmpty()
                ? ClassKeys.isKnown(selectedClass) && !ClassKeys.CLASS_ID_NONE.equals(selectedClass)
                : actionClass.equals(selectedClass));
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
    /** A modifier key can itself be a plain binding; strip only that physical key's own bit. */
    public static Modifier modifier(int bits,int platformControlBit,Input input) {
        if(input.device()==Device.KEY) {
            int code=input.code();
            if(code==340||code==344)bits&=~1; // GLFW left/right Shift
            if(code==341||code==345)bits&=~2; // Ctrl
            if(code==342||code==346)bits&=~4; // Alt
            if(code==343||code==347)bits&=~8; // Super
        }
        return modifier(bits,platformControlBit);
    }
    static boolean quickActionsAvailable(int taggedDialogs, boolean hasListDialog) {
        return taggedDialogs == 1 || (taggedDialogs > 1 && hasListDialog);
    }

    public static boolean matches(Binding binding, Input input, Modifier modifier) {
        return !binding.unbound && modifier != null && binding.modifier == modifier && binding.input.equals(input);
    }
    public static Action resolve(String classId, List<Binding> bindings, Input input, Modifier modifier,
                                 boolean externalConflict) {
        if (externalConflict) return null;
        Action found = null;
        for (var binding : bindings) {
            if (!binding.action.supports(classId) || !matches(binding, input, modifier)) continue;
            if (found != null) return null; // Fail closed instead of arbitrarily selecting a conflicting action.
            found = binding.action;
        }
        return found;
    }
    public static boolean dispatch(Action action, String classId, Predicate<String> bogatyr,
                                   BooleanSupplier canResurrect, Runnable openPicker) {
        return dispatch(action, classId, bogatyr, canResurrect, openPicker, slot -> false);
    }
    public static boolean dispatch(Action action, String classId, Predicate<String> bogatyr,
                                   BooleanSupplier canResurrect, Runnable openPicker,
                                   Predicate<ClassSkillSlot> classSkill) {
        if (action == null || !action.supports(classId)) return false;
        if (action.slot != null) return classSkill.test(action.slot);
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
