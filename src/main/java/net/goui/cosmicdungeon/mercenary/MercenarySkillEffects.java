package net.goui.cosmicdungeon.mercenary;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.AbstractThrownPotion;
import java.util.function.BooleanSupplier;

/** Observe completed native applications, never attempts. At most one success per potion. */
public final class MercenarySkillEffects {
    private static final String CREDIT="cosmicdungeon_mercenary_skill_credit_v1";
    private MercenarySkillEffects() {}
    private static MercenaryEntity caster(Entity source) {
        if (!(source instanceof AbstractThrownPotion) && !(source instanceof AreaEffectCloud)) return null;
        var owner=MercenaryPotions.owner(source);
        return owner!=null && MercenaryBrewing.enabled(owner.contract()) ? owner : null;
    }
    static boolean credit(CompoundTag source, MercenarySkill skill, BooleanSupplier award) {
        var credits=source.getCompoundOrEmpty(CREDIT);
        if (credits.getBooleanOr(MercenarySkill.POSITIVE_POTIONS.id(),false)
                || credits.getBooleanOr(MercenarySkill.NEGATIVE_POTIONS.id(),false)
                || !award.getAsBoolean()) return false;
        credits.putBoolean(skill.id(),true); source.put(CREDIT,credits); return true;
    }
    private static void credit(Entity source, MercenaryEntity owner, MercenarySkill skill) {
        credit(source.getPersistentData(),skill,()->MercenarySkills.success(owner,skill));
    }
    static MercenarySkill instantResult(boolean helpful, float healthBefore, float healthAfter,
                                        float absorptionBefore, float absorptionAfter) {
        if (helpful) return healthAfter>healthBefore ? MercenarySkill.POSITIVE_POTIONS : null;
        return healthAfter+absorptionAfter<healthBefore+absorptionBefore ? MercenarySkill.NEGATIVE_POTIONS : null;
    }
    public static void instant(Entity source, LivingEntity target, MobEffect effect, Runnable apply) {
        var owner=caster(source);
        if(owner==null || !MercenaryPotions.allows(source,target,effect)){apply.run();return;}
        float health=target.getHealth(), absorption=target.getAbsorptionAmount();
        apply.run();
        var skill=instantResult(MercenaryPotions.helpful(effect,target.isInvertedHealAndHarm()),
                health,target.getHealth(),absorption,target.getAbsorptionAmount());
        if (skill!=null) credit(source,owner,skill);
    }
    static boolean improved(MobEffectInstance before, MobEffectInstance after) {
        return after!=null && (before==null || after.getAmplifier()>before.getAmplifier()
                || after.getAmplifier()==before.getAmplifier() && after.getDuration()>before.getDuration());
    }
    public static boolean timed(Entity source, LivingEntity target, MobEffectInstance incoming, BooleanSupplier apply) {
        var owner=caster(source);
        if(owner==null || !MercenaryPotions.allows(source,target,incoming.getEffect().value()))
            return apply.getAsBoolean();
        boolean positive=MercenaryPotions.helpful(incoming.getEffect().value(),target.isInvertedHealAndHarm());
        if(!positive&&(incoming.getEffect().equals(net.minecraft.world.effect.MobEffects.POISON)
                ||incoming.getEffect().equals(net.minecraft.world.effect.MobEffects.WITHER))){
            ((MercenaryPotionCredit)(Object)incoming).cosmicdungeon$dose(
                    new MercenaryPotionCredit.Dose(owner.getUUID(),owner.runId(),source.getPersistentData()));
            return apply.getAsBoolean(); // Credit only when its native effect tick causes damage.
        }
        var current=target.getEffect(incoming.getEffect());
        var before=current==null ? null : new MobEffectInstance(current);
        boolean changed=apply.getAsBoolean();
        if (changed && improved(before,target.getEffect(incoming.getEffect())))
            credit(source,owner,positive?MercenarySkill.POSITIVE_POTIONS:MercenarySkill.NEGATIVE_POTIONS);
        return changed;
    }
}
