package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.dungeon.DungeonDifficultyEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Scale a copy once at the application boundary, before native merging or immunity checks. */
@Mixin(LivingEntity.class)
public abstract class DungeonDifficultyEffectMixin {
    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private MobEffectInstance cosmicdungeon$difficultyEffect(MobEffectInstance effect) {
        return DungeonDifficultyEvents.effect((LivingEntity)(Object)this, effect);
    }
}
