package net.goui.cosmicdungeon.mercenary;

import net.goui.cosmicdungeon.playerclass.dragoon.LightningCombat;
import net.goui.cosmicdungeon.playerclass.skill.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.Items;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Same 10-hit bar as players; automatically arms, then spends on the next effective trident strike. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class MercenaryLightning {
    private MercenaryLightning(){}
    static boolean enabled(MercenaryEntity merc){return MercenarySkill.CHAIN_LIGHTNING.supports(merc.contract());}
    static ServerPlayer activeOwner(MercenaryEntity merc){
        return ChargedClassSkills.active(merc,SignatureSkill.CHAIN_LIGHTNING)?MercenaryBrain.hirer(merc):null;
    }
    static boolean enemy(MercenaryEntity merc,ServerPlayer owner,LivingEntity target){
        return owner!=null&&ChargedClassSkills.enemy(merc,target);
    }
    @SubscribeEvent public static void hit(LivingDamageEvent.Post event){
        var source=event.getSource();
        if(SecondarySkillDamage.active()||!(source.getEntity() instanceof MercenaryEntity merc)
                ||!(event.getEntity() instanceof Mob initial)||!ChargedClassSkills.active(merc,SignatureSkill.CHAIN_LIGHTNING)
                ||!ChargedClassSkills.enemy(merc,initial))return;
        boolean direct=source.is(DamageTypes.MOB_ATTACK)&&source.getDirectEntity()==merc;
        boolean thrown=source.is(DamageTypes.TRIDENT)&&source.getDirectEntity() instanceof ThrownTrident
                &&MercenaryPotions.owner(source.getDirectEntity())==merc;
        boolean trident=thrown||direct&&merc.getMainHandItem().is(Items.TRIDENT);
        if(!trident)return;
        float dealt=event.getNewDamage()+event.getReduction(DamageContainer.Reduction.ABSORPTION);
        if(!(dealt>0)||!Float.isFinite(dealt))return;
        boolean wasArmed=ChargedClassSkills.armed(merc);
        if(wasArmed){
            ChargedClassSkills.lightning(merc,()->LightningCombat.cast(merc,initial,source,dealt));
            return;
        }
        ChargedClassSkills.addCharge(merc,SignatureSkill.CHAIN_LIGHTNING);
        ChargedClassSkills.arm(merc);
    }
    static void tick(MercenaryEntity merc,ServerLevel level,int ticks){
        // Legacy entity bank is retained for save compatibility; new gameplay uses only the run-owned bar.
        if(activeOwner(merc)!=null)ChargedClassSkills.arm(merc);
    }
}
