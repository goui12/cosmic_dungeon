package net.minecraft.world.entity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.Util;

public interface InsideBlockEffectApplier {
    InsideBlockEffectApplier NOOP = new InsideBlockEffectApplier() {
        @Override
        public void apply(InsideBlockEffectType p_404665_) {
        }

        @Override
        public void runBefore(InsideBlockEffectType p_404953_, Consumer<Entity> p_405551_) {
        }

        @Override
        public void runAfter(InsideBlockEffectType p_405305_, Consumer<Entity> p_405792_) {
        }
    };

    void apply(InsideBlockEffectType type);

    void runBefore(InsideBlockEffectType type, Consumer<Entity> effect);

    void runAfter(InsideBlockEffectType type, Consumer<Entity> effect);

    public static class StepBasedCollector implements InsideBlockEffectApplier {
        private static final InsideBlockEffectType[] APPLY_ORDER = InsideBlockEffectType.values();
        private static final int NO_STEP = -1;
        private final Set<InsideBlockEffectType> effectsInStep = EnumSet.noneOf(InsideBlockEffectType.class);
        private final Map<InsideBlockEffectType, List<Consumer<Entity>>> beforeEffectsInStep = Util.makeEnumMap(
            InsideBlockEffectType.class, p_404793_ -> new ArrayList<>()
        );
        private final Map<InsideBlockEffectType, List<Consumer<Entity>>> afterEffectsInStep = Util.makeEnumMap(
            InsideBlockEffectType.class, p_405667_ -> new ArrayList<>()
        );
        private final List<Consumer<Entity>> finalEffects = new ArrayList<>();
        private int lastStep = -1;

        public void advanceStep(int step) {
            if (this.lastStep != step) {
                this.lastStep = step;
                this.flushStep();
            }
        }

        public void applyAndClear(Entity entity) {
            this.flushStep();

            for (Consumer<Entity> consumer : this.finalEffects) {
                if (!entity.isAlive()) {
                    break;
                }

                consumer.accept(entity);
            }

            this.finalEffects.clear();
            this.lastStep = -1;
        }

        private void flushStep() {
            for (InsideBlockEffectType insideblockeffecttype : APPLY_ORDER) {
                List<Consumer<Entity>> list = this.beforeEffectsInStep.get(insideblockeffecttype);
                this.finalEffects.addAll(list);
                list.clear();
                if (this.effectsInStep.remove(insideblockeffecttype)) {
                    this.finalEffects.add(insideblockeffecttype.effect());
                }

                List<Consumer<Entity>> list1 = this.afterEffectsInStep.get(insideblockeffecttype);
                this.finalEffects.addAll(list1);
                list1.clear();
            }
        }

        @Override
        public void apply(InsideBlockEffectType type) {
            this.effectsInStep.add(type);
        }

        @Override
        public void runBefore(InsideBlockEffectType type, Consumer<Entity> effect) {
            this.beforeEffectsInStep.get(type).add(effect);
        }

        @Override
        public void runAfter(InsideBlockEffectType type, Consumer<Entity> effect) {
            this.afterEffectsInStep.get(type).add(effect);
        }
    }
}
