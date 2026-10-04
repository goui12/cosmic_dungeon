package net.goui.cosmicdungeon.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;

/** Immutable run settings. Missing legacy profiles retain the old NORMAL baseline. */
public enum DungeonDifficulty {
    EASY("Easy", .5), HARD("Hard", 1), INSANE("Insane", 1.5), RIDICULOUS("Ridiculous", 2);
    private final String title;
    private final double factor;
    DungeonDifficulty(String title, double factor) { this.title = title; this.factor = factor; }
    public String title() { return title; }
    public static Optional<DungeonDifficulty> parse(String text) {
        if ("NORMAL".equalsIgnoreCase(text)) return Optional.of(HARD);
        try { return Optional.of(valueOf(text.toUpperCase(Locale.ROOT))); }
        catch (RuntimeException invalid) { return Optional.empty(); }
    }
    public List<DungeonDifficulty> completedTiers() { return List.of(values()).subList(0, ordinal() + 1); }
    public Profile defaults() {
        return new Profile(name(), factor, factor, factor, 1 / factor, factor,
                this == INSANE ? .001 : 0, 100, this == RIDICULOUS ? 1.1 : 1);
    }
    /** Transient fractional countdown phase; authored/persisted delays stay in their original units. */
    public static final class Clock {
        private double phase;
        public int step(double delayMultiplier) {
            phase += 1 / delayMultiplier;
            int whole = (int)(phase + 1.0e-9);
            phase = Math.max(0, phase - whole);
            return whole;
        }
    }
    public record Profile(String tier, double health, double damage, double spawnCount,
            double spawnDelay, double harmfulDuration, double blindnessChance, int blindnessTicks, double armorWear) {
        public static final Profile LEGACY = HARD.defaults();
        public static final Codec<Profile> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("tier").forGetter(Profile::tier),
                Codec.doubleRange(.05, 10).fieldOf("health").forGetter(Profile::health),
                Codec.doubleRange(0, 10).fieldOf("damage").forGetter(Profile::damage),
                Codec.doubleRange(.05, 10).fieldOf("spawn_count").forGetter(Profile::spawnCount),
                Codec.doubleRange(.05, 10).fieldOf("spawn_delay").forGetter(Profile::spawnDelay),
                Codec.doubleRange(.05, 10).fieldOf("harmful_duration").forGetter(Profile::harmfulDuration),
                Codec.doubleRange(0, 1).fieldOf("blindness_chance").forGetter(Profile::blindnessChance),
                Codec.intRange(1, 12000).fieldOf("blindness_ticks").forGetter(Profile::blindnessTicks),
                Codec.doubleRange(0, 10).fieldOf("armor_wear").forGetter(Profile::armorWear)
        ).apply(i, Profile::new));
        public Profile {
            if (parse(tier).isEmpty() || !valid(health, .05) || !valid(damage, 0) || !valid(spawnCount, .05)
                    || !valid(spawnDelay, .05) || !valid(harmfulDuration, .05) || !valid(armorWear, 0)
                    || !Double.isFinite(blindnessChance) || blindnessChance < 0 || blindnessChance > 1
                    || blindnessTicks < 1 || blindnessTicks > 12000) throw new IllegalArgumentException("Invalid difficulty profile");
        }
        private static boolean valid(double value, double min) { return Double.isFinite(value) && value >= min && value <= 10; }
        public DungeonDifficulty difficulty() { return parse(tier).orElseThrow(); }
        public int count(int authored, boolean boss) {
            if (authored <= 0) return 0;
            return boss ? 1 : (int)Math.min(32767, Math.max(1, Math.ceil(authored * spawnCount)));
        }
        public int wear(double original, double roll) {
            double scaled = Math.min(Integer.MAX_VALUE, Math.max(0, original * armorWear));
            int whole = (int)scaled;
            return whole < Integer.MAX_VALUE && roll < scaled - whole ? whole + 1 : whole;
        }
    }
}
