package net.goui.cosmicdungeon.faction;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public record FactionDefinition(
        ResourceLocation id,
        int minValue,
        int maxValue,
        int startingValue,
        Map<FactionTier, Range> tierThresholds
) {
    public FactionDefinition {
        if (id == null) throw new IllegalArgumentException("id is null");
        if (minValue > maxValue) throw new IllegalArgumentException("minValue > maxValue");
        if (startingValue < minValue || startingValue > maxValue) {
            throw new IllegalArgumentException("startingValue out of bounds");
        }
        if (tierThresholds == null || tierThresholds.isEmpty()) {
            throw new IllegalArgumentException("tierThresholds is empty");
        }
        tierThresholds = Map.copyOf(tierThresholds);
    }

    public int clamp(int value) {
        return Math.max(minValue, Math.min(maxValue, value));
    }

    public FactionTier tierFor(int value) {
        int clamped = clamp(value);
        for (Map.Entry<FactionTier, Range> entry : tierThresholds.entrySet()) {
            if (entry.getValue().contains(clamped)) return entry.getKey();
        }
        return FactionTier.INDIFFERENT;
    }

    public static LinkedHashMap<FactionTier, Range> linkedThresholds() {
        return new LinkedHashMap<>();
    }

    public record Range(int minInclusive, int maxInclusive) {
        public Range {
            if (minInclusive > maxInclusive) throw new IllegalArgumentException("minInclusive > maxInclusive");
        }

        public boolean contains(int value) {
            return value >= minInclusive && value <= maxInclusive;
        }
    }
}
