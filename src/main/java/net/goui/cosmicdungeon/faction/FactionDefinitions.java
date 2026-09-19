package net.goui.cosmicdungeon.faction;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class FactionDefinitions {
    private FactionDefinitions() {}

    public static final ResourceLocation JHW_ID = ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "jhw");

    public static final FactionDefinition JHW = buildJhw();
    public static final ResourceLocation NPC_ID = ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "npc_vendor");
    public static final FactionDefinition NPC = new FactionDefinition(NPC_ID, -100, 100, 0, Map.of(
            FactionTier.HOSTILE, new FactionDefinition.Range(-100, 4),
            FactionTier.CORDIAL, new FactionDefinition.Range(5, 49),
            FactionTier.WARMLY, new FactionDefinition.Range(50, 99),
            FactionTier.ALLY, new FactionDefinition.Range(100, 100)));

    private static final Map<ResourceLocation, FactionDefinition> BY_ID = Map.of(
            JHW.id(), JHW, NPC.id(), NPC
    );

    public static FactionDefinition get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static Set<ResourceLocation> ids() {
        return BY_ID.keySet();
    }

    private static FactionDefinition buildJhw() {
        LinkedHashMap<FactionTier, FactionDefinition.Range> thresholds = FactionDefinition.linkedThresholds();
        thresholds.put(FactionTier.HOSTILE, new FactionDefinition.Range(0, 59));
        thresholds.put(FactionTier.SUSPICIOUS, new FactionDefinition.Range(60, 139));
        thresholds.put(FactionTier.INDIFFERENT, new FactionDefinition.Range(140, 219));
        thresholds.put(FactionTier.CORDIAL, new FactionDefinition.Range(220, 299));
        thresholds.put(FactionTier.FAVORABLE, new FactionDefinition.Range(300, 379));
        thresholds.put(FactionTier.WARMLY, new FactionDefinition.Range(380, 459));
        thresholds.put(FactionTier.ALLY, new FactionDefinition.Range(460, 500));
        return new FactionDefinition(JHW_ID, 0, 500, 80, thresholds);
    }
}
