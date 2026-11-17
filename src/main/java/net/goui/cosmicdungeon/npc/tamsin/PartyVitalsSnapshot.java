package net.goui.cosmicdungeon.npc.tamsin;

import java.util.Comparator;
import net.goui.cosmicdungeon.network.PartyVitals;
import net.goui.cosmicdungeon.playerclass.skill.*;
import net.goui.cosmicdungeon.playerclass.theurgist.VitalityPotions;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;

/** Server-thread read only. Never loads an entity or a chunk to populate the HUD. */
public final class PartyVitalsSnapshot {
    private PartyVitalsSnapshot() {}
    public static PartyVitals capture(LivingEntity entity,boolean inRun){
        if(entity==null)return PartyVitals.OFFLINE;
        long now=entity.level().getServer().overworld().getGameTime();
        boolean active=inRun&&!entity.isRemoved()&&entity.isAlive();
        var skills=PartySkillsSnapshot.capture(entity,active);
        if(!active)return (!inRun||entity.isRemoved()?PartyVitals.UNLOADED:PartyVitals.DEAD).withSkills(skills,now);
        var effects=entity.getActiveEffects().stream().map(e->effect(entity,e,now))
                .sorted(Comparator.comparing(PartyVitals.Effect::id)).toList();
        // Native load/attribute reductions can briefly leave current health above the new maximum.
        // Bound the read-only presentation without changing entity health or weakening the wire contract.
        float maximum=entity.getMaxHealth();
        return new PartyVitals(Math.clamp(entity.getHealth(),0,maximum),maximum,"ACTIVE",
                effects.stream().limit(PartyVitals.MAX_EFFECTS).toList(),Math.max(0,effects.size()-PartyVitals.MAX_EFFECTS),now,skills);
    }
    static PartyVitals.Effect effect(LivingEntity entity,MobEffectInstance e,long now){
        String title="",amount="";
        if(VitalityPotions.isVitality(e)){
            var rank=VitalityPotions.amplifier(e.getAmplifier());title=rank.name();amount="+"+rank.percent()+"% maximum health";
        }else{
            var tag=ClassBuffEffects.source(entity,e.getEffect());
            if(tag.getIntOr("version",0)==1&&tag.getIntOr("amplifier",-1)==e.getAmplifier()){
                title=tag.getStringOr("title","");
                String id=tag.getStringOr("skill","");
                for(var buff:ClassBuff.values())if(buff.id().equals(id)){
                    amount=switch(buff){
                        case FLURRY->"+10% melee attack speed";
                        case SHEATH->"20% damage reduction";
                        case AEGIS->"+3 melee damage";
                        case BULWARK->"+6 melee damage";
                        case BANISH->"";
                    };
                }
            }
        }
        return new PartyVitals.Effect(ClassBuffEffects.key(e.getEffect()),e.getAmplifier(),
                PartyEffectSource.bounded(title,128),amount,PartyEffectSource.name(entity,e),
                e.isInfiniteDuration()?-1:now+Math.max(0,e.getDuration()));
    }
}
