package net.goui.cosmicdungeon.playerclass.api;

import net.goui.cosmicdungeon.component.ModDataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Map;

/** Utility methods for vanilla/customized items attuned as CosmicDungeon class gear. */
public final class ClassItemUtil {
    private ClassItemUtil() {}

    private static final Map<String, String> DISPLAY_NAMES = Map.of(
            ClassKeys.CLASS_ID_BOGATYR, "Bogatyr",
            ClassKeys.CLASS_ID_DEADEYE, "Deadeye",
            ClassKeys.CLASS_ID_DRAGOON, "Dragoon",
            ClassKeys.CLASS_ID_JUDICATOR, "Judicator",
            ClassKeys.CLASS_ID_METALMANCER, "Metalmancer",
            ClassKeys.CLASS_ID_PYROCLAST, "Pyroclast",
            ClassKeys.CLASS_ID_THEURGIST, "Theurgist",
            ClassKeys.CLASS_ID_VENEFEX, "Venefex"
    );

    private static final Map<String, Integer> CLASS_COLORS = Map.of(
            ClassKeys.CLASS_ID_BOGATYR, 0xfed83d,
            ClassKeys.CLASS_ID_DEADEYE, 0x835432,
            ClassKeys.CLASS_ID_DRAGOON, 0x3c44aa,
            ClassKeys.CLASS_ID_JUDICATOR, 0x8932b8,
            ClassKeys.CLASS_ID_METALMANCER, 0xf9801d,
            ClassKeys.CLASS_ID_PYROCLAST, 0xb02e26,
            ClassKeys.CLASS_ID_THEURGIST, 0xf9fffe,
            ClassKeys.CLASS_ID_VENEFEX, 0x5e7c16
    );

    public static String getClassAttunement(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String classId = stack.get(ModDataComponents.CLASS_ATTUNEMENT.get());
        if (classId == null || classId.isBlank()) return null;
        String normalized = normalizeClassId(classId);
        return isPlayableClass(normalized) ? normalized : null;
    }

    public static boolean isClassAttuned(ItemStack stack) {
        return getClassAttunement(stack) != null;
    }

    public static boolean hasAnyAttunementMetadata(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && (stack.has(ModDataComponents.CLASS_ATTUNEMENT.get())
                || stack.has(ModDataComponents.CLASS_ITEM_DUNGEON.get())
                || stack.has(ModDataComponents.CLASS_ITEM_TIER.get())
                || stack.has(ModDataComponents.CLASS_ITEM_TRACE_VALUE.get()));
    }

    public static Integer getDungeon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Integer dungeon = stack.get(ModDataComponents.CLASS_ITEM_DUNGEON.get());
        return dungeon != null && dungeon > 0 ? dungeon : null;
    }

    public static Integer getTier(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Integer tier = stack.get(ModDataComponents.CLASS_ITEM_TIER.get());
        return tier != null && tier >= 1 && tier <= 10 ? tier : null;
    }

    public static Long getTraceValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Long trace = stack.get(ModDataComponents.CLASS_ITEM_TRACE_VALUE.get());
        return trace != null && trace >= 0L ? trace : null;
    }

    public static boolean hasCompleteValidAttunement(ItemStack stack) {
        return getClassAttunement(stack) != null
                && getDungeon(stack) != null
                && getTier(stack) != null
                && getTraceValue(stack) != null;
    }

    public static void attune(ItemStack stack, String classId, int dungeon, int tier, long trace) {
        if (stack == null || stack.isEmpty()) return;
        String normalized = normalizeClassId(classId);
        stack.set(ModDataComponents.CLASS_ATTUNEMENT.get(), normalized);
        stack.set(ModDataComponents.CLASS_ITEM_DUNGEON.get(), dungeon);
        stack.set(ModDataComponents.CLASS_ITEM_TIER.get(), tier);
        stack.set(ModDataComponents.CLASS_ITEM_TRACE_VALUE.get(), trace);
    }

    public static void clearAttunement(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        stack.remove(ModDataComponents.CLASS_ATTUNEMENT.get());
        stack.remove(ModDataComponents.CLASS_ITEM_DUNGEON.get());
        stack.remove(ModDataComponents.CLASS_ITEM_TIER.get());
        stack.remove(ModDataComponents.CLASS_ITEM_TRACE_VALUE.get());
    }

    public static String displayNameForClass(String classId) {
        String normalized = normalizeClassId(classId);
        String display = DISPLAY_NAMES.get(normalized);
        if (display != null) return display;
        if (normalized.isBlank()) return "Unknown";
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }

    public static int colorForClass(String classId) {
        return CLASS_COLORS.getOrDefault(normalizeClassId(classId), 0xf9fffe);
    }

    public static String normalizeClassId(String classId) {
        return classId == null ? "" : classId.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isPlayableClass(String classId) {
        String normalized = normalizeClassId(classId);
        return !ClassKeys.CLASS_ID_NONE.equals(normalized) && ClassKeys.playableClassIds().contains(normalized);
    }
}
