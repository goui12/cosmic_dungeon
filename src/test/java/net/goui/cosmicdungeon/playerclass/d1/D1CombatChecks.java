package net.goui.cosmicdungeon.playerclass.d1;

import net.goui.cosmicdungeon.Config;
import java.util.List;

public final class D1CombatChecks {
    private static int checks;
    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label);
    }
    private static void close(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 1e-8, label + ": " + actual + " expected " + expected);
    }
    public static void main(String[] args) {
        for (var entry : D1AmmunitionCatalog.entries()) {
            for (String cls : List.of("theurgist", "judicator", "venefex", "pyroclast",
                    "dragoon", "bogatyr", "deadeye", "metalmancer", "none")) {
                for (boolean active : List.of(false, true)) {
                    for (boolean binding : List.of(false, true)) {
                        var expected = active && binding && entry.classes().contains(cls)
                                ? D1CombatRules.Ammunition.ABILITY : D1CombatRules.Ammunition.DENIED;
                        check(D1CombatRules.ammunition(entry.id(), cls, active, binding) == expected,
                                entry.id() + " class/run/binding gate " + cls);
                    }
                }
            }
        }
        check(D1CombatRules.ammunition(null, "venefex", false, false)
                == D1CombatRules.Ammunition.VANILLA, "Ordinary ammunition is preserved");
        check(D1CombatRules.ammunition("future_unknown", "venefex", true, true)
                == D1CombatRules.Ammunition.VANILLA, "Unknown IDs grant no D1 effect");
        for (var entry : D1AmmunitionCatalog.entries()) {
            boolean support = List.of("mending_sting", "verdant_jolt", "scintilla_vitalis",
                    "lux_vitalis", "ebonsight").contains(entry.id());
            check(D1CombatRules.supportive(entry.id()) == support, "Only restorative arrows bypass team filtering");
        }
        check(!D1CombatRules.supportive(null), "Ordinary arrows retain team filtering");
        check(D1CombatRules.existingWins(4, 600, 500, 4, 220, false), "Pestis cannot shorten Black Bubo");
        check(!D1CombatRules.existingWins(4, 600, 100, 4, 220, false), "Equal power can extend remaining duration");
        check(D1CombatRules.existingWins(.6, 40, 1, .15, 600, false), "Weaker long slow cannot replace strong short slow");
        check(!D1CombatRules.existingWins(.15, 600, 600, .6, 40, false), "Strong short slow replaces weaker long slow");
        check(D1CombatRules.existingWins(2, 100, 80, .8, 40, true), "Equal-rate Mending is not shortened by Verdant");
        check(!D1CombatRules.existingWins(2, 100, 20, .8, 40, true), "Equal-rate Verdant extends near-expired Mending");
        check(D1CombatRules.existingWins(3, 40, 1, 8, 220, true), "Fer-de-Lance beats Bushmaster by rate, not total");
        check(!D1CombatRules.existingWins(8, 220, 220, 3, 40, true), "Higher-rate poison replaces lower rate");
        check(D1CombatRules.existingWins(4, 220, -1, 4, 600, false), "Infinite equal-power effect is retained");
        check(!D1CombatRules.existingWins(4, 220, -1, 8, 600, false), "Higher power may replace infinite weaker effect");
        for (int duration : List.of(1, 19, 20, 21, 39, 40, 41, 100, 220, 72000)) {
            for (double total : List.of(0.0, .8, 2.0, 4.0, 8.0, 1024.0)) {
                int pulses = 0;
                for (int remaining = duration; remaining > 0; remaining--)
                    if ((remaining - 1) % 20 == 0) pulses++;
                close(pulses * D1CombatRules.pulse(total, duration), total, "Configured pulse total " + duration);
            }
        }
        close(D1CombatRules.poisonPulse(1024, 1, 1), 0, "Poison cannot damage at its one-HP floor");
        close(D1CombatRules.poisonPulse(1024, 1, .5), 0, "Poison cannot heal or kill below its floor");
        close(D1CombatRules.poisonPulse(1024, 1, 3), 2, "Poison amount limited by available health");
        close(D1CombatRules.spicule(6, 0, .1, 10), 6, "Base Breach");
        close(D1CombatRules.spicule(12, 3, .1, 10), 15.6, "Three debuff Rend");
        close(D1CombatRules.spicule(12, 200, .1, 10), 24, "Spicule cap");
        close(D1CombatRules.spicule(12, 200, .1, 0), 12, "Zero cap disables scaling");
        for (double radius : List.of(.5, 5.0, 32.0)) {
            for (double power : List.of(0.0, 12.0, 15.0, 1024.0)) {
                close(D1CombatRules.rocketDamage(power, 0, radius), power, "Rocket center");
                close(D1CombatRules.rocketDamage(power, radius * .75, radius), power * .5, "Rocket square-root falloff");
                close(D1CombatRules.rocketDamage(power, radius, radius), 0, "Rocket radius boundary");
                close(D1CombatRules.rocketDamage(power, radius + .01, radius), 0, "Outside rocket radius");
            }
        }
        check(D1CombatRules.rocketDamage(12, Double.NaN, 5) == 0, "Nonfinite distance is harmless");
        for (boolean active : List.of(false, true)) {
            for (boolean thrown : List.of(false, true)) {
                for (boolean melee : List.of(false, true)) {
                    check(D1CombatRules.tridentHit(active, thrown, melee, 5) == (active && (thrown || melee)),
                            "Trident trigger requires active run and actual hit kind");
                    check(!D1CombatRules.tridentHit(active, thrown, melee, 0), "Blocked hit cannot chain");
                    check(!D1CombatRules.tridentHit(active, thrown, melee, -1), "Negative damage cannot chain");
                }
            }
        }
        check(!D1CombatRules.tridentHit(true, true, false, Double.NaN), "Nonfinite damage cannot chain");
        check(Config.CHAIN_CANDIDATE_LIMIT.get() == 256, "Dragoon candidate default");
        check(D1AbilityConfig.ROCKET_CANDIDATE_LIMIT.get() == 256, "Pyroclast candidate default");
        close(Config.CHAIN_CHANCE.get(), .03, "MASTER Classes S4 chance");
        close(Config.CHAIN_RADIUS.get(), 32, "Retained configurable chain radius");
        for (Object[] row : new Object[][]{
                {"theurgist", "mending_sting", 2.0, 100}, {"theurgist", "verdant_jolt", .8, 40},
                {"theurgist", "scintilla_vitalis", 4.0, 1}, {"theurgist", "lux_vitalis", 8.0, 1},
                {"judicator", "scintilla_vitalis", 4.0, 1}, {"judicator", "lux_vitalis", 8.0, 1},
                {"judicator", "ebonsight", 0.0, 200},
                {"judicator", "vielpiercer", 0.0, 200}, {"venefex", "tree_viper", 4.0, 100},
                {"venefex", "bushmaster", 8.0, 220}, {"venefex", "fer_de_lance", 3.0, 40},
                {"venefex", "pestis", 4.0, 220}, {"venefex", "black_bubo", 4.0, 600},
                {"venefex", "vapours", .15, 220}, {"venefex", "melancholia", .15, 600},
                {"venefex", "deathly_stupor", .6, 40}, {"venefex", "spicule_breach", 6.0, 1},
                {"venefex", "spicule_rend", 12.0, 1}, {"venefex", "spicule_undead_healing", 6.0, 1},
                {"pyroclast", "cinderbite", 12.0, 1}, {"pyroclast", "cindermaul", 15.0, 1}}) {
            var spell = D1AbilityConfig.get((String) row[0], (String) row[1]);
            close(spell.power().get(), (Double) row[2], "Newest source power " + row[1]);
            check(spell.duration().get().equals(row[3]), "Newest source duration " + row[1]);
        }
        // Cameron September20 explicitly resolves the newer Camp3 chest/overview mismatch.
        check(D1CombatRules.ammunition("lux_vitalis", "judicator", true, true)
                == D1CombatRules.Ammunition.ABILITY, "Newer Judicator Camp3 Lux is usable");
        check(D1AbilityConfig.get("judicator", "lux_vitalis") != D1AbilityConfig.get("theurgist", "lux_vitalis"),
                "Judicator Lux has its own configurable spell");
        check(D1CombatRules.ammunition("lux_vitalis", "judicator", false, true)
                == D1CombatRules.Ammunition.DENIED, "Inactive Judicator cannot use Lux");
        check(D1CombatRules.ammunition("lux_vitalis", "judicator", true, false)
                == D1CombatRules.Ammunition.DENIED, "Lux does not bypass attunement");
        check(D1CombatRules.ammunition("lux_vitalis", "dragoon", true, true)
                == D1CombatRules.Ammunition.DENIED, "Other class does not acquire Lux");
        check(D1AbilityConfig.get("pyroclast", "cinder_breeze") == null, "D2 rocket is deferred");
        System.out.println("D1 combat checks passed: " + checks);
    }
}
