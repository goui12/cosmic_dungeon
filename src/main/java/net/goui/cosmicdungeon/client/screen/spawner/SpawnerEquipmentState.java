package net.goui.cosmicdungeon.client.screen.spawner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft.Edit;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft.Enchant;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft.Origin;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentRules;

/** A nested item editor owns its draft until Save; Cancel never reaches the parent draft. */
public final class SpawnerEquipmentState {
    public enum Page { ITEMS, BOOKS }
    private Edit edit;
    private Page page = Page.ITEMS;
    private String query = "", notice = "";
    private boolean slotOnly = true;
    private int index;
    public SpawnerEquipmentState(Edit edit) { this.edit = Objects.requireNonNull(edit); }
    public Edit edit() { return edit; }
    public void accept(Edit value) { edit = Objects.requireNonNull(value); notice = ""; }
    public Page page() { return page; }
    public void page(Page value) { page = value; index = 0; query = ""; notice = ""; }
    public String query() { return query; }
    public void query(String value) { query = value; index = 0; }
    public int index() { return index; }
    public void index(int value) { index = Math.max(0, value); }
    public boolean slotOnly() { return slotOnly; }
    public void slotOnly(boolean value) { slotOnly = value; index = 0; }
    public String notice() { return notice; }
    public void notice(String value) { notice = value; }
    public Edit item(String id) { return new Edit(Origin.ITEM, id, edit.advanced(), java.util.List.of()); }
    public Edit held() { return new Edit(Origin.HELD, "", edit.advanced(), java.util.List.of()); }
    public Edit clear() { return new Edit(Origin.CLEAR, "", edit.advanced(), java.util.List.of()); }
    public Edit mode(boolean advanced) { return new Edit(edit.origin(), edit.itemId(), advanced, edit.enchantments()); }
    public Edit enchant(String id, int level) {
        var values = new ArrayList<>(edit.enchantments());
        values.removeIf(value -> value.id().equals(id));
        values.add(new Enchant(id, level));
        values.sort(Comparator.comparing(Enchant::id));
        return new Edit(edit.origin(), edit.itemId(), edit.advanced(), values);
    }
    public static int level(int current, int normalMax, int button, boolean shift, boolean advanced) {
        if (button == 1) return shift ? 0 : Math.max(0, current - 1);
        if (button != 0) return current;
        if (shift) return normalMax;
        return Math.min(advanced ? SpawnerEquipmentRules.MAX_LEVEL : normalMax, current + 1);
    }
}
