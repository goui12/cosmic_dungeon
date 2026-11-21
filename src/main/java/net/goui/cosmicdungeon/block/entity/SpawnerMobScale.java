package net.goui.cosmicdungeon.block.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Per-spawner multiplier applied once to the root mob, after initialization and before placement. */
public final class SpawnerMobScale {
    public static final String KEY = "CosmicMobScale";
    public static final double DEFAULT = 1.0, MIN = 0.0625, MAX = 4.0;
    public static final String ERROR = "Mob scale must be between 0.0625 and 4.";
    private static final ResourceLocation MODIFIER = ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "spawner_mob_scale");

    private SpawnerMobScale() {}

    public static boolean valid(double value) {
        return value >= MIN && value <= MAX;
    }

    public static double read(CompoundTag configuration) {
        return configuration.contains(KEY) ? configuration.getDoubleOr(KEY, Double.NaN) : DEFAULT;
    }

    public static void write(CompoundTag configuration, double value) {
        if (!valid(value)) throw new IllegalArgumentException(ERROR);
        if (value == DEFAULT) configuration.remove(KEY);
        else configuration.putDouble(KEY, value);
    }

    public static void apply(Entity entity, double factor) {
        if (factor == DEFAULT || !valid(factor) || !(entity instanceof LivingEntity living)) return;
        var scale = living.getAttribute(Attributes.SCALE);
        if (scale == null) return;
        scale.addOrReplacePermanentModifier(new AttributeModifier(MODIFIER, factor - 1,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        // Native attribute updates normally refresh dimensions on tick, after our collision checks.
        living.refreshDimensions();
    }
}
