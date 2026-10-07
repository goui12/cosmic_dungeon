package net.goui.cosmicdungeon.npc.tamsin;

import java.util.Comparator;
import net.goui.cosmicdungeon.network.PartyVitals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;

/** Server-thread read only. Never loads an entity or a chunk to populate the HUD. */
public final class PartyVitalsSnapshot {
    private PartyVitalsSnapshot() {}
    public static PartyVitals capture(LivingEntity entity, boolean inRun) {
        if (entity == null) return PartyVitals.OFFLINE;
        if (!inRun || entity.isRemoved()) return PartyVitals.UNLOADED;
        if (!entity.isAlive()) return PartyVitals.DEAD;
        var effects = entity.getActiveEffects().stream()
                .map(e -> new PartyVitals.Effect(BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect().value()).toString(), e.getAmplifier()))
                .sorted(Comparator.comparing(PartyVitals.Effect::id)).toList();
        return new PartyVitals(entity.getHealth(), entity.getMaxHealth(), "ACTIVE",
                effects.stream().limit(PartyVitals.MAX_EFFECTS).toList(), Math.max(0, effects.size() - PartyVitals.MAX_EFFECTS));
    }
}
