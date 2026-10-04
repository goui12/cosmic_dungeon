package net.goui.cosmicdungeon.dungeon;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.*;

/** Server config affects new runs only; every run persists its own immutable snapshot. */
public final class DungeonDifficultyConfig {
    private record Tuning(ModConfigSpec.DoubleValue health, ModConfigSpec.DoubleValue damage,
            ModConfigSpec.DoubleValue count, ModConfigSpec.DoubleValue delay, ModConfigSpec.DoubleValue effects,
            ModConfigSpec.DoubleValue blindness, ModConfigSpec.IntValue blindnessTicks, ModConfigSpec.DoubleValue wear) {}
    private static final Map<DungeonDifficulty, Tuning> VALUES = new EnumMap<>(DungeonDifficulty.class);
    private DungeonDifficultyConfig() {}
    public static void define(ModConfigSpec.Builder b) {
        b.comment("Instance-local settings, frozen at entry. Never changes server.properties, loot or authored spawner data.").push("DungeonDifficulty");
        for (var tier : DungeonDifficulty.values()) {
            var p = tier.defaults();
            b.push(tier.name().toLowerCase(Locale.ROOT));
            VALUES.put(tier, new Tuning(
                    b.defineInRange("healthMultiplier", p.health(), .05, 10),
                    b.defineInRange("damageMultiplier", p.damage(), 0, 10),
                    b.comment("Round up to a whole mob; boss one-shot and authored living caps remain enforced.").defineInRange("spawnCountMultiplier", p.spawnCount(), .05, 10),
                    b.comment("Multiplies countdown time without rewriting authored delay fields. Lower values mean faster waves.").defineInRange("spawnDelayMultiplier", p.spawnDelay(), .05, 10),
                    b.defineInRange("harmfulEffectDurationMultiplier", p.harmfulDuration(), .05, 10),
                    b.defineInRange("blindnessChancePerDamage", p.blindnessChance(), 0, 1),
                    b.comment("Ticks, independent of the harmful-effect duration multiplier.").defineInRange("blindnessDurationTicks", p.blindnessTicks(), 1, 12000),
                    b.comment("Fractional wear uses stochastic rounding before normal enchantment handling.").defineInRange("armorWearMultiplier", p.armorWear(), 0, 10)));
            b.pop();
        }
        b.pop();
    }
    public static DungeonDifficulty.Profile snapshot(DungeonDifficulty tier) {
        var t = VALUES.get(tier);
        return new DungeonDifficulty.Profile(tier.name(), t.health.get(), t.damage.get(), t.count.get(),
                t.delay.get(), t.effects.get(), t.blindness.get(), t.blindnessTicks.get(), t.wear.get());
    }
}
