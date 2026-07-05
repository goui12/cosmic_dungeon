package net.goui.cosmicdungeon.block.entity;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class CosmicSpawnerLootTableSummary {
    private final ResourceLocation entityTypeId;
    private final ResourceLocation lootTableId;
    private final ResourceLocation lootTableResourceId;
    private final List<DefaultDrop> defaultDrops = new ArrayList<>();
    private String missingReason;

    CosmicSpawnerLootTableSummary(ResourceLocation entityTypeId, ResourceLocation lootTableId, ResourceLocation lootTableResourceId) {
        this.entityTypeId = entityTypeId;
        this.lootTableId = lootTableId;
        this.lootTableResourceId = lootTableResourceId;
    }

    public ResourceLocation entityTypeId() { return entityTypeId; }
    public ResourceLocation lootTableId() { return lootTableId; }
    public ResourceLocation lootTableResourceId() { return lootTableResourceId; }
    public List<DefaultDrop> defaultDrops() { return List.copyOf(defaultDrops); }
    public boolean hasDefaultDrops() { return !defaultDrops.isEmpty(); }
    public Optional<String> missingReason() { return Optional.ofNullable(missingReason); }

    void addDefaultDrop(ResourceLocation itemId, ChanceDisplay chance) {
        if (itemId == null) return;
        defaultDrops.add(new DefaultDrop(itemId, chance == null ? ChanceDisplay.complex("complex") : chance));
        defaultDrops.sort(Comparator.comparing(DefaultDrop::itemId));
    }

    void setMissingReason(String missingReason) {
        this.missingReason = missingReason;
    }

    public record DefaultDrop(ResourceLocation itemId, ChanceDisplay chance) {}

    public record ChanceDisplay(Float chance, String label) {
        public static ChanceDisplay percent(float chance) {
            return new ChanceDisplay(Math.max(0f, Math.min(1f, chance)), null);
        }

        public static ChanceDisplay complex(String label) {
            return new ChanceDisplay(null, label == null || label.isBlank() ? "complex" : label);
        }

        public boolean numeric() {
            return chance != null;
        }
    }
}
