package net.goui.cosmicdungeon.client.screen.spawner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.goui.cosmicdungeon.spawner.editor.SpawnerLootDraft;
import net.goui.cosmicdungeon.spawner.editor.SpawnerLootDraft.Rule;
import net.goui.cosmicdungeon.spawner.editor.SpawnerLootDraft.Snapshot;

/** A separate editable loot draft, preserving untouched native and legacy values. */
public final class SpawnerLootState {
    private Snapshot snapshot;
    private final List<Rule> originalRules;
    private final String[] originalGear = new String[6], gear = new String[6];
    private final List<Rule> rules = new ArrayList<>();
    public SpawnerLootState(Snapshot baseline, SpawnerLootDraft staged) {
        snapshot = baseline;
        originalRules = List.copyOf(baseline.rules());
        rules.addAll(staged.replaceRules() ? staged.rules() : baseline.rules());
        for (int i = 0; i < 6; i++) {
            float value = baseline.gear().size() == 6 ? baseline.gear().get(i) : -1;
            originalGear[i] = value < 0 ? "" : percent(value);
            gear[i] = staged.gear().get(i) < 0 ? originalGear[i] : percent(staged.gear().get(i));
        }
    }
    public Snapshot snapshot() { return snapshot; }
    public void inspection(Snapshot value) {
        snapshot = new Snapshot(snapshot.gear(), snapshot.rules(), value.defaults(), value.note(), value.entityId());
    }
    public List<Rule> rules() { return List.copyOf(rules); }
    public String gear(int slot) { return gear[slot]; }
    public void gear(int slot, String value) { gear[slot] = value; }
    public void resetGear(int slot) { gear[slot] = originalGear[slot]; }
    public String error() {
        for (int i = 0; i < 6; i++)
            if (!gear[i].equals(originalGear[i]) && chance(gear[i]) < 0)
                return "Gear chances must be percentages from 0 to 100. Reset preserves the original.";
        return rules.size() > 128 ? "A spawner can have at most 128 configured loot rows." : "";
    }
    public boolean save(Rule value) {
        for (int i = 0; i < rules.size(); i++) if (rules.get(i).id().equals(value.id())) {
            rules.set(i, value); return true;
        }
        if (rules.size() >= 128) return false;
        rules.add(value); return true;
    }
    public void remove(String id) { rules.removeIf(rule -> rule.id().equals(id)); }
    public SpawnerLootDraft draft() {
        if (!error().isEmpty()) throw new IllegalArgumentException(error());
        var values = new ArrayList<Float>();
        for (int i = 0; i < 6; i++)
            values.add(gear[i].equals(originalGear[i]) ? -1f : chance(gear[i]));
        return new SpawnerLootDraft(values, !rules.equals(originalRules), List.copyOf(rules));
    }
    public static String newId() { return "row_" + UUID.randomUUID().toString().replace("-", ""); }
    public static float chance(String percent) {
        try {
            if (percent.isBlank() || !percent.equals(percent.strip())) return -1;
            float value = Float.parseFloat(percent);
            return Float.isFinite(value) && value >= 0 && value <= 100 ? value / 100f : -1;
        } catch (NumberFormatException invalid) { return -1; }
    }
    public static String percent(float value) {
        if (!Float.isFinite(value)) return "Invalid";
        return new BigDecimal(Float.toString(value)).multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString();
    }
}
