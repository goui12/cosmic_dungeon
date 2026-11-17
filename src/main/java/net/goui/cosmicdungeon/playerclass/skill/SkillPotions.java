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
    private static double potionBonus(ClassSkills.Attack attack){
        return !potion(attack)?0:net.goui.cosmicdungeon.playerclass.theurgist.VitalityPotions.bonus(
                SignatureSkills.playerLevel(attack.player(),SignatureSkill.POSITIVE_POTIONS),
                ClassSkillConfig.CAP.get(),ClassSkillConfig.POTION.get());
    }
    public static boolean potion(ClassSkills.Attack attack) {
        return attack != null && attack.cls().equals("theurgist") && attack.skill().equals("potions");
    }
    public static void inherit(Entity cloud, Entity projectile) {
        net.goui.cosmicdungeon.playerclass.theurgist.TheurgistPotionProtection.inherit(cloud,projectile);
        net.goui.cosmicdungeon.mercenary.MercenaryPotions.inherit(cloud,projectile);
        NegativePotionSkills.inherit(cloud,projectile);
        if (projectile.getPersistentData().contains(ClassSkills.SHOT))
            cloud.getPersistentData().put(ClassSkills.SHOT,projectile.getPersistentData().getCompoundOrEmpty(ClassSkills.SHOT).copy());
    }
    public static void drink(PotionContents contents, LivingEntity target, float scale, ItemStack stack) {
        ClassSkills.Attack attack=target instanceof ServerPlayer p ? ClassSkills.held(p,stack) : null;
        if (!(target.level() instanceof ServerLevel level)) { contents.applyToLivingEntity(target,scale); return; }
        boolean vitality=net.goui.cosmicdungeon.playerclass.theurgist.VitalityPotions.in(stack).isPresent();
        if (!potion(attack)&&!vitality) { contents.applyToLivingEntity(target,scale); return; }
        contents.forEachEffect(effect -> {
            if (effect.getEffect().value().isInstantenous())
                instant(attack,effect.getEffect().value(),level,target,target,target,effect.getAmplifier(),1);
            else timed(attack,target,effect,target);
        },scale);
    }
    public static void instant(ClassSkills.Attack attack, MobEffect effect, ServerLevel level,
                               Entity source, Entity owner, LivingEntity target, int amplifier, double proximity) {
        boolean eligible=potion(attack) && (ClassSkills.ally(target,attack.player()) || ClassSkills.enemy(target,attack.player()));
        double scale=eligible ? 1+potionBonus(attack) : 1;
        float before=target.getHealth(),absorption=target.getAbsorptionAmount();
        var previous=CURRENT.get();
        if (eligible) CURRENT.set(attack);
        try { effect.applyInstantenousEffect(level,source,owner,target,amplifier,proximity*scale); }
        finally { if(previous==null)CURRENT.remove();else CURRENT.set(previous); }
        if (eligible && target.getHealth()>before) support(attack,target,"healing");
        if(target.getHealth()+target.getAbsorptionAmount()<before+absorption)NegativePotionSkills.success(source,target);
    }
    public static boolean timed(ClassSkills.Attack attack, LivingEntity target, MobEffectInstance incoming, Entity source) {
        return timed(attack,target,incoming,source,source);
    }
    public static boolean timed(ClassSkills.Attack attack,LivingEntity target,MobEffectInstance incoming,Entity source,Entity actionSource){
        boolean eligible=potion(attack) && (ClassSkills.ally(target,attack.player()) || ClassSkills.enemy(target,attack.player()));
        var old=target.getEffect(incoming.getEffect());
        boolean harmful=incoming.getEffect().value().getCategory()==MobEffectCategory.HARMFUL
                &&(old==null||incoming.getAmplifier()>old.getAmplifier());
        boolean useful=ClassSkillRules.usefulBuff(incoming.getEffect().value().getCategory()==MobEffectCategory.BENEFICIAL,
                old==null?null:old.getAmplifier(),incoming.getAmplifier());
        if (incoming.getEffect().equals(MobEffects.REGENERATION) && target.getHealth()>=target.getMaxHealth()) useful=false;
        boolean vitality=net.goui.cosmicdungeon.playerclass.theurgist.VitalityPotions.isVitality(incoming);
        var applied=eligible&&!vitality ? incoming.withScaledDuration((float)(1+potionBonus(attack))) : incoming;
        NegativePotionSkills.attach(actionSource,target,applied);
        boolean changed=vitality?net.goui.cosmicdungeon.playerclass.theurgist.VitalityPotions.apply(target,incoming,source)
                :target.addEffect(applied,source);
        if (eligible && changed && useful && target.hasEffect(incoming.getEffect()))
            support(attack,target,BuiltInRegistries.MOB_EFFECT.getKey(incoming.getEffect().value()).toString());
        if(changed&&harmful&&!NegativePotionSkills.delayed(incoming)&&target.hasEffect(incoming.getEffect()))NegativePotionSkills.success(actionSource,target);
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
        // Seed from exact legacy XP before that independent weapon counter advances.
        if (potion(attack)) {
            int credit=ClassSkillRules.budget(attack.action().getIntOr("xp_spent",0),
                    ClassSkillConfig.SUPPORT_XP.get(),ClassSkillConfig.ACTION_CAP.get());
            var data=net.goui.cosmicdungeon.progression.PlayerProgressionData.get(attack.player().level().getServer());
            int before=SignatureSkills.playerLevel(attack.player(),SignatureSkill.POSITIVE_POTIONS);
            data.addSignatureProgress(attack.player().getUUID(),SignatureSkill.POSITIVE_POTIONS,credit,
                    SignatureSkill.POSITIVE_POTIONS.threshold(SignatureSkill.PLAYER_CAP,ClassSkillConfig.BASE.get(),ClassSkillConfig.STEP.get()));
            int after=SignatureSkills.playerLevel(attack.player(),SignatureSkill.POSITIVE_POTIONS);
            net.goui.cosmicdungeon.playerclass.theurgist.VitalityPotions.unlocked(attack.player().level().getServer(),
                    ClassSkills.run(attack.player()),attack.player().getName().getString(),before,after);
        }
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
