package net.goui.cosmicdungeon.crafting;

/** Opt-in synchronous native-test counters. Normal gameplay keeps the probe null; no logs or retained owners. */
final class CraftingWork {
    static Probe probe;
    static final class Probe {
        final boolean legacy;
        long viewCalls, quoteBuilds, entryBuilds, plannerChoices, inventoryScans, inventoryCopies, copiedSlots,
                stockBuilds, stockSlots, palettePackets;
        Probe(boolean legacy) { this.legacy = legacy; }
    }
    private CraftingWork() {}
    static Probe begin(boolean legacy) {
        if (probe != null) throw new IllegalStateException("Crafting probe already active");
        return probe = new Probe(legacy);
    }
    static void end() { probe = null; }
}
