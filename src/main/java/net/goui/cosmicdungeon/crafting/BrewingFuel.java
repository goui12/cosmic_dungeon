package net.goui.cosmicdungeon.crafting;

import net.minecraft.nbt.*;

/** Bounded per-crafter fuel state and exact transition. Unused charges are never rounded away. */
public final class BrewingFuel {
    public static final int PER_POWDER = 20, MAX_STEPS = 16;
    public static final String KEY = "brewing_fuel_v1";
    private BrewingFuel() {}
    public record Cost(int steps, int powders, int remaining) {}
    public record View(int charges, int capacity) {}
    public static Cost cost(int charges, int steps) {
        if (charges < 0 || charges >= PER_POWDER || steps < 1 || steps > MAX_STEPS)
            throw new IllegalArgumentException("Brewing fuel bounds");
        int powders = Math.max(0, (steps - charges + PER_POWDER - 1) / PER_POWDER);
        return new Cost(steps, powders, charges + powders * PER_POWDER - steps);
    }
    public static CompoundTag empty() {
        var state = new CompoundTag(); state.putInt("schema", 1); state.putInt("charges", 0); state.putLong("revision", 0); return state;
    }
    public static void validate(CompoundTag state) {
        if (state.sizeInBytes() > 65536 || !(state.get("schema") instanceof IntTag) || state.getIntOr("schema", 0) != 1
                || !(state.get("charges") instanceof IntTag) || charges(state) < 0 || charges(state) >= PER_POWDER
                || !(state.get("revision") instanceof LongTag) || state.getLongOr("revision", -1) < 0)
            throw new IllegalArgumentException("Unsupported brewing fuel state; original retained");
    }
    public static int charges(CompoundTag state) { return state.getIntOr("charges", -1); }
    public static CompoundTag plan(CompoundTag before, int steps) {
        validate(before); var cost = cost(charges(before), steps); var after = before.copy();
        after.putInt("charges", cost.remaining()); after.putLong("revision", Math.addExact(before.getLongOr("revision", -1), 1));
        var plan = new CompoundTag(); plan.putInt("schema", 1); plan.put("before", before.copy()); plan.put("after", after);
        plan.putInt("steps", steps); plan.putInt("powders", cost.powders()); return plan;
    }
    public static void validatePlan(CompoundTag plan) {
        if (!(plan.get("schema") instanceof IntTag) || plan.getIntOr("schema", 0) != 1
                || !(plan.get("before") instanceof CompoundTag) || !(plan.get("after") instanceof CompoundTag)
                || !(plan.get("steps") instanceof IntTag) || !(plan.get("powders") instanceof IntTag))
            throw new IllegalArgumentException("Unsupported brewing fuel transition");
        var before = plan.getCompoundOrEmpty("before"); var after = plan.getCompoundOrEmpty("after");
        validate(before); validate(after); var expected = plan(before, plan.getIntOr("steps", -1));
        if (!after.equals(expected.getCompoundOrEmpty("after")) || plan.getIntOr("powders", -1) != expected.getIntOr("powders", -2))
            throw new IllegalArgumentException("Brewing fuel transition differs from step cost");
    }
}
