package net.goui.cosmicdungeon.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Protection indicator only. One server-side player cadence owns healing, regardless of effect refresh/amplifier. */
public final class CompanionshipMobEffect extends MobEffect {
    public CompanionshipMobEffect(){super(MobEffectCategory.BENEFICIAL,0x96C8ED);}
}
