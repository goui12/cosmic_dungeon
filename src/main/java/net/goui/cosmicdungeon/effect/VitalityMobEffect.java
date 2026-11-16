package net.goui.cosmicdungeon.effect;

import net.goui.cosmicdungeon.playerclass.theurgist.VitalityPotions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;

/** Adds a percentage of the attribute's base, never health or another buff's inflated maximum. */
public final class VitalityMobEffect extends MobEffect {
    public static final ResourceLocation MODIFIER=ResourceLocation.fromNamespaceAndPath("cosmicdungeon","vitality");
    public VitalityMobEffect(){
        super(MobEffectCategory.BENEFICIAL,0xF4B860);
        // The native tooltip describes the percentage; execution uses the entity's actual base below.
        addAttributeModifier(Attributes.MAX_HEALTH,MODIFIER,AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                amp->VitalityPotions.amplifier(amp).percent()/100.0);
    }
    @Override public void addAttributeModifiers(AttributeMap attributes,int amplifier){
        var health=attributes.getInstance(Attributes.MAX_HEALTH);if(health==null)return;
        double baseFactor=1,totalFactor=1;
        for(var modifier:health.getModifiers()){
            if(modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_BASE)baseFactor+=modifier.amount();
            else if(modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)totalFactor*=1+modifier.amount();
        }
        double factor=baseFactor*totalFactor;
        double amount=factor>0&&Double.isFinite(factor)
                ?health.getBaseValue()*VitalityPotions.amplifier(amplifier).percent()/100.0/factor:0;
        if(!Double.isFinite(amount))amount=0;
        var previous=health.getModifier(MODIFIER);
        if(previous!=null&&previous.operation()==AttributeModifier.Operation.ADD_VALUE&&previous.amount()==amount)return;
        health.removeModifier(MODIFIER);
        health.addPermanentModifier(new AttributeModifier(MODIFIER,amount,AttributeModifier.Operation.ADD_VALUE));
    }
    @Override public boolean shouldApplyEffectTickThisTick(int remaining,int amplifier){return remaining%20==0;}
    @Override public boolean applyEffectTick(ServerLevel level,LivingEntity entity,int amplifier){
        addAttributeModifiers(entity.getAttributes(),amplifier);return true;
    }
}
