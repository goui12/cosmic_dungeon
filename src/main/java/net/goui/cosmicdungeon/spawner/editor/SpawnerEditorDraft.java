package net.goui.cosmicdungeon.spawner.editor;

import net.minecraft.resources.ResourceLocation;

/** Bounded editable fields only; authored stacks, loot, potentials and unknown NBT never come from clients. */
public record SpawnerEditorDraft(String entityId, String name, boolean captain, boolean boss, int cap,
        int minDelay, int maxDelay, boolean silent, boolean noAi, boolean invulnerable,
        boolean persistent, boolean nameVisible) {
    public static final int MAX_CAP = 32767, MAX_DELAY = 32767, MAX_NAME = 128;
    public SpawnerEditorDraft {
        if (entityId == null || entityId.length() > 256 || name == null || name.length() > MAX_NAME)
            throw new IllegalArgumentException("Oversized spawner draft");
    }
    public static SpawnerEditorDraft empty() {
        return new SpawnerEditorDraft("minecraft:pig", "", false, false, 0, 200, 800, false, false, false, false, false);
    }
    public String error() {
        if (ResourceLocation.tryParse(entityId) == null) return "Choose a valid entity registry ID.";
        if (name.codePoints().anyMatch(Character::isISOControl)) return "Names cannot contain control characters.";
        if (captain && !entityId.equals("minecraft:pillager")) return "Illager captain uses the pillager entity.";
        if (cap < 0 || cap > MAX_CAP) return "Cap must be between 0 (uncapped) and " + MAX_CAP + ".";
        if (minDelay < 1 || maxDelay < minDelay || maxDelay > MAX_DELAY)
            return "Delays must satisfy 1 <= minimum <= maximum <= " + MAX_DELAY + " ticks.";
        return "";
    }
}
