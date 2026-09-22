package net.goui.cosmicdungeon.playerclass.d1;

import java.util.List;
import com.mojang.serialization.JsonOps;
import net.goui.cosmicdungeon.item.custom.D1RocketPayload;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;

/** Offline identity/access and native payload-codec checks. No game or registry bootstrap. */
public final class D1CustomAmmunitionChecks {
    private static int checks;
    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        for (var entry : D1AmmunitionCatalog.entries()) {
            String item = "cosmicdungeon:" + entry.id();
            check(D1AmmunitionCatalog.candidate(item), "Custom D1 registry recognized");
            check(D1AmmunitionCatalog.registered(item), "Recognized custom registry cannot fall back to vanilla damage");
            var facts = new D1AmmunitionCatalog.Facts(item, null, "Cosmetic rename", "", false, 0);
            check(entry.id().equals(D1AmmunitionCatalog.identify(facts)), "Registry identity survives names/payload appearance");
            check(!D1AmmunitionCatalog.canAdopt(facts, entry.id()), "Custom items cannot enter vanilla adoption");
            for (String invalid : List.of("", "unknown", entry.id().toUpperCase(java.util.Locale.ROOT))) {
                check(D1AmmunitionCatalog.identify(new D1AmmunitionCatalog.Facts(item, invalid,
                        entry.name(), "", false, 0)) == null, "Conflicting marker fails closed");
            }
            for (String cls : List.of("theurgist", "judicator", "venefex", "pyroclast", "bogatyr", "dragoon", "deadeye", "metalmancer")) {
                var permission = D1CombatRules.ammunition(entry.id(), cls, true, true);
                check((permission == D1CombatRules.Ammunition.ABILITY) == entry.classes().contains(cls),
                        "Intrinsic items retain exact D1 class gates");
                check(D1CombatRules.ammunition(entry.id(), cls, false, true) == D1CombatRules.Ammunition.DENIED,
                        "Dedicated item cannot grant effects outside active run");
                check(D1CombatRules.ammunition(entry.id(), cls, true, false) == D1CombatRules.Ammunition.DENIED,
                        "Dedicated item cannot bypass ownership/metadata checks");
            }
        }
        check(!D1AmmunitionCatalog.registered("minecraft:firework_rocket"), "Vanilla rocket fallback unchanged");
        check(!D1AmmunitionCatalog.registered(null), "Missing registry cannot impersonate D1 ammunition");
        for (String excluded : List.of("cinderkiss", "cinderbight", "cinder_breeze", "gusting_bolt", "high_velocity_arrow")) {
            check(!D1AmmunitionCatalog.candidate("cosmicdungeon:" + excluded), "No name-only or later-dungeon additions");
        }
        for (boolean maul : List.of(false, true)) {
            var payload = D1RocketPayload.defaults(maul);
            check(payload.flightDuration() == 1, "Default within documented gunpowder range");
            check(payload.explosions().size() == (maul ? 5 : 4), "Exact documented star count");
            for (var explosion : payload.explosions()) {
                check(explosion.shape() == (maul ? FireworkExplosion.Shape.LARGE_BALL : FireworkExplosion.Shape.SMALL_BALL),
                        "Source shape");
                check(explosion.hasTrail() == maul && explosion.hasTwinkle() == maul, "Source trail/flicker");
                check(explosion.colors().size() == (maul ? 2 : 1), "Orange/red versus red palette");
            }
            var encoded = Fireworks.CODEC.encodeStart(JsonOps.INSTANCE, payload).getOrThrow();
            var decoded = Fireworks.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
            check(payload.equals(decoded), "Native firework component survives JSON save round trip");
            check(D1RocketPayload.defaults(maul).equals(payload), "Independent default creation is stable");
        }
        System.out.println(checks + " D1 custom ammunition checks passed");
    }
}
