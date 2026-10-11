package net.goui.cosmicdungeon.client.screen;

import java.util.Map;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;

/** Bounded presentation catalog. It never changes which classes the server enables. */
public record InventoryTheme(String classId, int accent, boolean illustrated) {
    private static final InventoryTheme NEUTRAL = new InventoryTheme(ClassKeys.CLASS_ID_NONE, 0xFF82949E, false);
    private static final Map<String, InventoryTheme> THEMES = Map.of(
            "bogatyr", new InventoryTheme("bogatyr", 0xFFE8C35C, true),
            "dragoon", new InventoryTheme("dragoon", 0xFF7989D6, true),
            "judicator", new InventoryTheme("judicator", 0xFFBC86DF, true),
            "pyroclast", new InventoryTheme("pyroclast", 0xFFD67B70, true),
            "theurgist", new InventoryTheme("theurgist", 0xFFE3E7DD, true),
            "venefex", new InventoryTheme("venefex", 0xFFA0BB62, true),
            "metalmancer", new InventoryTheme("metalmancer", 0xFFB49A65, false),
            "deadeye", new InventoryTheme("deadeye", 0xFFBB946F, false));

    public static InventoryTheme forClass(String id) { return THEMES.getOrDefault(ClassKeys.clamp(id), NEUTRAL); }
    public boolean hasClass() { return !classId.equals(ClassKeys.CLASS_ID_NONE); }
    public String artPath() { return "textures/gui/inventory/" + classId + ".png"; }
    public String emblemPath() { return "textures/entity/chest/emblem_" + classId + ".png"; }
}
