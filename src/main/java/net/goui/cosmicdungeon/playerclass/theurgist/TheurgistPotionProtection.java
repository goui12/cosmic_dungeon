package net.goui.cosmicdungeon.playerclass.theurgist;

import java.util.UUID;
import net.goui.cosmicdungeon.mercenary.MercenaryPotions;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.bogatyr.CompanionAllies;
import net.goui.cosmicdungeon.playerclass.skill.ClassSkills;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractThrownPotion;

/** Throw-time identity survives class changes, owner absence and native cloud saves; no item rewriting. */
public final class TheurgistPotionProtection {
    public static final String SOURCE="cosmicdungeon_theurgist_potion_v1";
    private TheurgistPotionProtection(){}
    static CompoundTag snapshot(UUID owner,boolean theurgist){
        var tag=new CompoundTag();tag.putString("owner",owner.toString());tag.putBoolean("theurgist",theurgist);return tag;
    }
    static Boolean marked(CompoundTag tag){
        try{
            UUID.fromString(tag.getStringOr("owner",""));
            return tag.getBoolean("theurgist").orElse(null);
        }catch(IllegalArgumentException invalid){return null;}
    }
    public static void capture(AbstractThrownPotion projectile){
        if(projectile.getPersistentData().contains(SOURCE)||!(projectile.getOwner() instanceof ServerPlayer owner))return;
        projectile.getPersistentData().put(SOURCE,snapshot(owner.getUUID(),"theurgist".equals(ClassData.getClassId(owner))));
    }
    private static CompoundTag origin(Entity source){
        if(source.getPersistentData().contains(SOURCE))
            return source.getPersistentData().getCompoundOrEmpty(SOURCE);
        // Old projectiles keep their existing save: read the original class snapshot where available.
        var skill=source.getPersistentData().getCompoundOrEmpty(ClassSkills.SHOT);
        if("potions".equals(skill.getStringOr("skill",""))){
            try{return snapshot(UUID.fromString(skill.getStringOr("owner","")),"theurgist".equals(skill.getStringOr("class","")));}
            catch(IllegalArgumentException invalid){/* Older empty snapshots fall back to the native caster. */}
        }
        return MercenaryPotions.caster(source) instanceof ServerPlayer owner
                ? snapshot(owner.getUUID(),"theurgist".equals(ClassData.getClassId(owner))) : new CompoundTag();
    }
    public static void inherit(Entity cloud,Entity projectile){
        if(cloud.getPersistentData().contains(SOURCE))return;
        var origin=origin(projectile);
        if(!origin.isEmpty())cloud.getPersistentData().put(SOURCE,origin.copy());
    }
    static boolean permits(boolean theurgist,boolean positive,boolean hostile,boolean ally,boolean helpful){
        return !theurgist||!positive||(!hostile&&(!ally||helpful));
    }
    public static boolean allows(Entity source,LivingEntity target,MobEffect effect){
        if(!(source instanceof AbstractThrownPotion||source instanceof AreaEffectCloud))return true;
        if(effect.getCategory()!=MobEffectCategory.BENEFICIAL)return true;
        var origin=origin(source);var marked=marked(origin);
        // A malformed explicit marker must not silently acquire a different caster after reload.
        boolean theurgist=marked!=null?marked:source.getPersistentData().contains(SOURCE);
        if(!theurgist)return true;
        var caster=MercenaryPotions.caster(source);
        boolean ally=CompanionAllies.friendly(target)||(caster!=null&&target.isAlliedTo(caster));
        boolean hostile=!ally&&(target instanceof Enemy||target instanceof Mob mob
                &&mob.getTarget()!=null&&(CompanionAllies.friendly(mob.getTarget())
                ||caster!=null&&mob.getTarget().isAlliedTo(caster)));
        return permits(true,true,hostile,ally,MercenaryPotions.helpful(effect,target.isInvertedHealAndHarm()));
    }
}
