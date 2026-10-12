package net.goui.cosmicdungeon.client.screen.spawner;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEditorDraft;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft;
import net.goui.cosmicdungeon.spawner.editor.SpawnerLootDraft;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.Slot;

/** Retains every edit, including incomplete number input, without coupling it to widget lifetime. */
public final class SpawnerEditorState {
    public enum Tab { ENTITY, EQUIPMENT, SPAWNING, FLAGS, PREVIEW }
    private final UUID session;
    private final SpawnerEditorDraft original;
    private String entityId, name, cap, minDelay, maxDelay;
    private boolean captain, boss, silent, noAi, invulnerable, persistent, nameVisible;
    private boolean pending, closed, presetLoaded;
    private SpawnerLootDraft loot = SpawnerLootDraft.unchanged();
    private String notice = "", search = "";
    private Tab tab = Tab.ENTITY;
    private int page;
    private SpawnerEquipmentDraft equipment = SpawnerEquipmentDraft.unchanged();

    public SpawnerEditorState(UUID session, SpawnerEditorDraft draft) {
        this.session = Objects.requireNonNull(session);
        original = Objects.requireNonNull(draft);
        entityId = draft.entityId(); name = draft.name(); captain = draft.captain(); boss = draft.boss();
        cap = Integer.toString(draft.cap()); minDelay = Integer.toString(draft.minDelay());
        maxDelay = Integer.toString(draft.maxDelay()); silent = draft.silent(); noAi = draft.noAi();
        invulnerable = draft.invulnerable(); persistent = draft.persistent(); nameVisible = draft.nameVisible();
    }
    public UUID session() { return session; }
    public Tab tab() { return tab; }
    public void tab(Tab value) { tab = Objects.requireNonNull(value); }
    public String search() { return search; }
    public void search(String value) { search = value; page = 0; }
    public int page() { return page; }
    public void page(int value) { page = Math.max(0, value); }
    public boolean pending() { return pending; }
    public boolean closed() { return closed; }
    public String notice() { return notice; }
    public void notice(String value) { notice = value == null ? "" : value; }
    public SpawnerEquipmentDraft equipment() { return equipment; }
    public void equipment(Slot slot, SpawnerEquipmentDraft.Edit edit) {
        equipment = equipment.with(slot, edit); notice = "";
    }
    public SpawnerLootDraft loot() { return loot; }
    public void loot(SpawnerLootDraft value) { loot = Objects.requireNonNull(value); notice = ""; }
    public void presetLoaded(boolean value) { presetLoaded = value; }
    public boolean dirty() {
        return presetLoaded || loot.dirty() || equipment.dirty() || !draft().equals(original) || !cap.equals(Integer.toString(original.cap()))
                || !minDelay.equals(Integer.toString(original.minDelay()))
                || !maxDelay.equals(Integer.toString(original.maxDelay()));
    }
    public SpawnerEditorDraft draft() {
        return new SpawnerEditorDraft(entityId, name, captain, boss, number(cap), number(minDelay), number(maxDelay),
                silent, noAi, invulnerable, persistent, nameVisible);
    }
    public String error() {
        if (number(cap) < 0 || number(minDelay) < 0 || number(maxDelay) < 0)
            return "Enter whole numbers for cap and both delays.";
        return draft().error();
    }
    public boolean beginApply() {
        if (pending || closed) return false;
        notice = error();
        if (!notice.isEmpty()) return false;
        pending = true; notice = "Applying draft..."; return true;
    }
    public boolean reject(UUID receivedSession, String message) {
        if (closed || !session.equals(receivedSession)) return false;
        pending = false; notice(message); return true;
    }
    public boolean close(UUID receivedSession) {
        if (closed || !session.equals(receivedSession)) return false;
        pending = false; closed = true; return true;
    }
    public String entityId() { return entityId; }
    public void entityId(String value) { entityId = value; notice = ""; }
    public String name() { return name; }
    public void name(String value) { name = value; notice = ""; }
    public String cap() { return cap; }
    public void cap(String value) { cap = value; notice = ""; }
    public String minDelay() { return minDelay; }
    public void minDelay(String value) { minDelay = value; notice = ""; }
    public String maxDelay() { return maxDelay; }
    public void maxDelay(String value) { maxDelay = value; notice = ""; }
    public boolean captain() { return captain; }
    public void captain(boolean value) { captain = value; notice = ""; }
    public boolean boss() { return boss; }
    public void boss(boolean value) { boss = value; notice = ""; }
    public boolean silent() { return silent; }
    public void silent(boolean value) { silent = value; notice = ""; }
    public boolean noAi() { return noAi; }
    public void noAi(boolean value) { noAi = value; notice = ""; }
    public boolean invulnerable() { return invulnerable; }
    public void invulnerable(boolean value) { invulnerable = value; notice = ""; }
    public boolean persistent() { return persistent; }
    public void persistent(boolean value) { persistent = value; notice = ""; }
    public boolean nameVisible() { return nameVisible; }
    public void nameVisible(boolean value) { nameVisible = value; notice = ""; }
    public static boolean matches(String query, String id, String displayName) {
        String normalized = query.strip().toLowerCase(Locale.ROOT);
        return id.toLowerCase(Locale.ROOT).contains(normalized)
                || displayName.toLowerCase(Locale.ROOT).contains(normalized);
    }
    private static int number(String value) {
        try { return value.isEmpty() || !value.chars().allMatch(Character::isDigit) ? -1 : Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return -1; }
    }
}
