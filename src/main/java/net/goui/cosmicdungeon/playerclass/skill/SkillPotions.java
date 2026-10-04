package net.goui.cosmicdungeon.playerclass.skill;

import net.minecraft.server.level.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

/** Native potion applications wrapped individually; no item-component rewrite or replacement recipe. */
public final class SkillPotions {
    private static final ThreadLocal<ClassSkills.Attack> CURRENT = new ThreadLocal<>();
    private static final String SUPPORT = "cosmicdungeon_skill_support_v1";
    private SkillPotions() {}
    public static ClassSkills.Attack current() { return CURRENT.get(); }
    public static boolean potion(ClassSkills.Attack attack) {
        return attack != null && attack.cls().equals("theurgist") && attack.skill().equals("potions");
    }
    public static void inherit(Entity cloud, Entity projectile) {
        if (projectile.getPersistentData().contains(ClassSkills.SHOT))
            cloud.getPersistentData().put(ClassSkills.SHOT,projectile.getPersistentData().getCompoundOrEmpty(ClassSkills.SHOT).copy());
    }
    public static void drink(PotionContents contents, LivingEntity target, float scale, ItemStack stack) {
        ClassSkills.Attack attack=target instanceof ServerPlayer p ? ClassSkills.held(p,stack) : null;
        if (!potion(attack) || !(target.level() instanceof ServerLevel level)) {
            contents.applyToLivingEntity(target,scale); return;
        }
        contents.forEachEffect(effect -> {
            if (effect.getEffect().value().isInstantenous())
                instant(attack,effect.getEffect().value(),level,target,target,target,effect.getAmplifier(),1);
            else timed(attack,target,effect,target);
        },scale);
    }
    public static void instant(ClassSkills.Attack attack, MobEffect effect, ServerLevel level,
                               Entity source, Entity owner, LivingEntity target, int amplifier, double proximity) {
        boolean eligible=potion(attack) && (ClassSkills.ally(target,attack.player()) || ClassSkills.enemy(target,attack.player()));
        double scale=eligible ? 1+ClassSkills.bonus(attack,ClassSkillConfig.POTION.get()) : 1;
        float before=target.getHealth();
        var previous=CURRENT.get();
        if (eligible) CURRENT.set(attack);
        try { effect.applyInstantenousEffect(level,source,owner,target,amplifier,proximity*scale); }
        finally { if(previous==null)CURRENT.remove();else CURRENT.set(previous); }
        if (eligible && target.getHealth()>before) support(attack,target,"healing");
    }
    public static boolean timed(ClassSkills.Attack attack, LivingEntity target, MobEffectInstance incoming, Entity source) {
        boolean eligible=potion(attack) && (ClassSkills.ally(target,attack.player()) || ClassSkills.enemy(target,attack.player()));
        var old=target.getEffect(incoming.getEffect());
        boolean useful=ClassSkillRules.usefulBuff(incoming.getEffect().value().getCategory()==MobEffectCategory.BENEFICIAL,
                old==null?null:old.getAmplifier(),incoming.getAmplifier());
        if (incoming.getEffect().equals(MobEffects.REGENERATION) && target.getHealth()>=target.getMaxHealth()) useful=false;
        var applied=eligible ? incoming.withScaledDuration((float)(1+ClassSkills.bonus(attack,ClassSkillConfig.POTION.get()))) : incoming;
        boolean changed=target.addEffect(applied,source);
        if (eligible && changed && useful && target.hasEffect(incoming.getEffect()))
            support(attack,target,BuiltInRegistries.MOB_EFFECT.getKey(incoming.getEffect().value()).toString());
        return changed;
    }
    public static boolean support(ClassSkills.Attack attack, LivingEntity target, String effect) {
        if (attack==null || !ClassSkills.ally(target,attack.player()) || ClassSkillConfig.SUPPORT_XP.get()<=0) return false;
        var times=target.getPersistentData().getCompoundOrEmpty(SUPPORT);
        long now=attack.player().level().getServer().overworld().getGameTime();
        if (!ClassSkillRules.cooldownReady(now,times.getLongOr(effect,-1),ClassSkillConfig.SUPPORT_TICKS.get())) return false;
        // Recipient-local bound; no global owner/target cache and no eviction that bypasses the interval.
        if (!times.contains(effect) && times.keySet().size()>=64) return false;
        if (ClassSkillRules.budget(attack.action().getIntOr("xp_spent",0),ClassSkillConfig.SUPPORT_XP.get(),ClassSkillConfig.ACTION_CAP.get())<=0) return false;
        ClassSkills.award(attack,ClassSkillConfig.SUPPORT_XP.get(),true);
        times.putLong(effect,now);target.getPersistentData().put(SUPPORT,times);return true;
    }
    public static double arrowHealing(Entity arrow, LivingEntity target) {
        var attack=ClassSkills.projectile(arrow);
        if (attack==null || !attack.skill().equals("bow") || !(attack.cls().equals("judicator")||attack.cls().equals("theurgist"))) return 1;
        return ClassSkills.ally(target,attack.player()) ? 1+ClassSkills.bonus(attack,ClassSkillConfig.HEALING.get()) : 1;
    }
    public static int arrowDuration(Entity arrow, MobEffect effect, int duration) {
        var attack=ClassSkills.projectile(arrow);
        if (attack==null || !attack.cls().equals("venefex") || !attack.skill().equals("bow") || effect.getCategory()!=MobEffectCategory.HARMFUL) return duration;
        return duration<0 ? duration : (int)Math.min(Integer.MAX_VALUE,Math.ceil(duration*(1+ClassSkills.bonus(attack,ClassSkillConfig.DEBUFF.get()))));
    }
    public static void arrowSupport(Entity arrow, LivingEntity target, String effect) { support(ClassSkills.projectile(arrow),target,effect); }
}
