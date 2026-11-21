package net.goui.cosmicdungeon.playerclass.skill;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.mercenary.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.bogatyr.CompanionAllies;
import net.goui.cosmicdungeon.playerclass.judicator.SmiteCombat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;

/** Run-owned bars survive actor reload/death, remain class-specific, and disappear with normal run cleanup. */
public final class ChargedClassSkills {
    public static final String PREFIX="class_charge_v1:";
    public static final String KILL="cosmicdungeon_charged_kill_v1";
    public static final String EMPTY_SMITE="Smite only works on undead! You did not damage any mobs";
    private record Ref(D1RunData data,long run,String key,SignatureSkill skill){}
    private ChargedClassSkills(){}
    public static boolean active(LivingEntity actor,SignatureSkill skill){return ref(actor,skill)!=null;}
    private static Ref ref(LivingEntity actor,SignatureSkill skill){
        if(ChargedSkillState.limit(skill)==0||!(actor.level() instanceof ServerLevel level)
                ||!actor.isAlive()||actor.isSpectator())return null;
        long run;UUID owner;String identity;
        if(actor instanceof ServerPlayer player){
            var record=ClassSkills.run(player);
            if(record==null||!skill.classId().equals(ClassData.getClassId(player)))return null;
            run=record.runId();owner=player.getUUID();identity="player:"+owner;
        }else if(actor instanceof MercenaryEntity merc){
            if(merc.contract()==null||!skill.classId().equals(merc.contract().classId())||MercenaryBrain.hirer(merc)==null)return null;
            run=merc.runId();owner=merc.contract().hirer();identity="mercenary:"+merc.contract().id();
        }else return null;
        var data=D1RunData.get(level.getServer());
        if(data.sealed(run))return null;
        var recovery=PendingDungeonRecoveryData.get(level.getServer());var handoff=recovery.handoff(owner);
        if(recovery.completed(owner)>=run||handoff!=null&&handoff.run()==run&&handoff.kind().equals("cleanup"))return null;
        return new Ref(data,run,PREFIX+identity+":"+skill.id(),skill);
    }
    private static ChargedSkillState state(Ref ref){
        if(ref==null)return ChargedSkillState.empty();
        int armed=ref.data.count(ref.run,ref.key+":armed");
        return ChargedSkillState.fromSaved(ref.skill, ref.data.count(ref.run,ref.key), armed);
    }
    private static void write(Ref ref,ChargedSkillState state){
        ref.data.setCount(ref.run,ref.key,state.charge());ref.data.setCount(ref.run,ref.key+":armed",state.armed()?1:0);
    }
    public static int charge(LivingEntity actor,SignatureSkill skill){
        var state=state(ref(actor,skill));return state.valid(skill)?state.charge():0;
    }
    public static boolean armed(LivingEntity actor){
        var state=state(ref(actor,SignatureSkill.CHAIN_LIGHTNING));
        return state.valid(SignatureSkill.CHAIN_LIGHTNING)&&state.armed();
    }
    public static boolean addCharge(LivingEntity actor,SignatureSkill skill){
        if(SecondarySkillDamage.active())return false;
        var ref=ref(actor,skill);if(ref==null)return false;
        var old=state(ref);if(!old.valid(skill))return false;
        var next=old.hit(skill);if(next.equals(old))return false;
        write(ref,next);return true;
    }
    public static boolean arm(LivingEntity actor){
        var ref=ref(actor,SignatureSkill.CHAIN_LIGHTNING);if(ref==null)return false;
        var old=state(ref);if(!old.ready(SignatureSkill.CHAIN_LIGHTNING)||old.armed())return false;
        write(ref,old.arm(ref.skill));return true;
    }
    public static int level(LivingEntity actor,SignatureSkill skill){
        if(actor instanceof ServerPlayer p)return SignatureSkills.playerLevel(p,skill);
        if(actor instanceof MercenaryEntity merc&&actor.level() instanceof ServerLevel level){
            if(skill==SignatureSkill.SMITE)return skill.level(D1RunData.get(level.getServer()).count(
                    merc.runId(),SignatureSkills.smiteKey(merc.contract().id())),250,50);
            return MercenarySkills.level(merc,skill.legacy());
        }
        return skill==SignatureSkill.SMITE?1:0;
    }
    private static void success(LivingEntity actor,SignatureSkill skill){
        if(actor instanceof ServerPlayer player){SignatureSkills.success(player,skill);return;}
        if(!(actor instanceof MercenaryEntity merc)||ref(actor,skill)==null)return;
        if(skill!=SignatureSkill.SMITE){MercenarySkills.success(merc,skill.legacy());return;}
        var data=D1RunData.get(actor.level().getServer());String key=SignatureSkills.smiteKey(merc.contract().id());
        int previous=data.count(merc.runId(),key),next=(int)Math.max(previous,Math.min(60L,(long)previous+1));
        data.setCount(merc.runId(),key,next);
    }
    /** Reserve before native damage; refund only an entirely ineffective activation. */
    public static boolean lightning(LivingEntity actor,java.util.function.IntSupplier cast){
        var ref=ref(actor,SignatureSkill.CHAIN_LIGHTNING);var old=state(ref);
        if(ref==null||!old.ready(SignatureSkill.CHAIN_LIGHTNING)||!old.armed()||SecondarySkillDamage.active())return false;
        write(ref,ChargedSkillState.empty());
        int hits=cast.getAsInt();
        if(hits<=0){write(ref,old);return false;}
        success(actor,SignatureSkill.CHAIN_LIGHTNING);return true;
    }
    public static boolean smite(LivingEntity actor){
        var ref=ref(actor,SignatureSkill.SMITE);var old=state(ref);
        if(ref==null||!old.ready(SignatureSkill.SMITE)||SecondarySkillDamage.active())return false;
        int rank=level(actor,SignatureSkill.SMITE);
        write(ref,ChargedSkillState.empty());
        int hits=SmiteCombat.cast(actor,rank);
        if(hits<=0){
            write(ref,old);
            if(actor instanceof ServerPlayer p)p.displayClientMessage(Component.literal(EMPTY_SMITE),true);
            return false;
        }
        success(actor,SignatureSkill.SMITE);
        if(actor instanceof ServerPlayer p)p.displayClientMessage(Component.literal("Smite: "+SmiteCombat.rank(rank).name()),true);
        return true;
    }
    /** Valid also after a fatal hit; callers decide whether a living target is required. */
    public static boolean enemy(LivingEntity caster,LivingEntity target){
        if(caster==target||target.level()!=caster.level()||target.isSpectator()||target instanceof OwnableEntity
                ||target instanceof net.minecraft.world.entity.animal.Animal||CompanionAllies.friendly(target)
                ||target.isAlliedTo(caster))return false;
        if(caster instanceof ServerPlayer player)return ClassSkills.enemy(target,player);
        if(caster instanceof MercenaryEntity merc){
            var owner=MercenaryBrain.hirer(merc);
            return owner!=null&&MercenaryAntiFarming.allows(merc,target)&&!target.isAlliedTo(owner)&&(target instanceof net.minecraft.world.entity.monster.Enemy
                    ||target instanceof Mob mob&&mob.getTarget()!=null&&MercenaryBrain.ally(merc,mob.getTarget()));
        }
        return false;
    }
    /** Called at the accepted native death tail, never from cancellable LivingDeathEvent. */
    public static void killed(LivingEntity victim,DamageSource source){
        if(victim.level().isClientSide()||!(victim instanceof Mob)
                ||!victim.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE)
                ||victim.getPersistentData().getBooleanOr(KILL,false))return;
        victim.getPersistentData().putBoolean(KILL,true);
        if(SecondarySkillDamage.active()||source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)
                ||!victim.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE))return;
        Entity direct=source.getEntity();
        LivingEntity actor=direct instanceof LivingEntity living?living:victim.getKillCredit();
        if(actor!=null&&!(direct instanceof LivingEntity)&&SecondarySkillDamage.remembered(victim,actor))return;
        if(actor==null||!active(actor,SignatureSkill.SMITE)||!enemy(actor,victim))return;
        if(actor instanceof ServerPlayer&&source.getDirectEntity() instanceof Projectile p
                &&ClassSkills.projectile(p)==null)return;
        addCharge(actor,SignatureSkill.SMITE);
    }
}
