package net.goui.cosmicdungeon.effect;
import net.goui.cosmicdungeon.playerclass.d1.D1AbilityConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.resources.ResourceLocation;
/** Source-defined totals use one pulse per second; poison retains vanilla's non-lethal floor. */
public final class D1TunedMobEffect extends MobEffect {
    private final String cls,id,kind;
    private static final ResourceLocation WEAKNESS=ResourceLocation.fromNamespaceAndPath("cosmicdungeon","d1_weakness");
    private static final ResourceLocation SLOWNESS=ResourceLocation.fromNamespaceAndPath("cosmicdungeon","d1_slowness");
    public D1TunedMobEffect(String cls,String id,String kind,int color){
        super(kind.equals("heal")?MobEffectCategory.BENEFICIAL:MobEffectCategory.HARMFUL,color);
        this.cls=cls;this.id=id;this.kind=kind;
    }
    @Override public boolean shouldApplyEffectTickThisTick(int remaining,int amplifier){
        return (kind.equals("heal")||kind.equals("poison"))&&(remaining-1)%20==0;
    }
    @Override public boolean applyEffectTick(ServerLevel level,LivingEntity target,int amplifier){
        var spell=D1AbilityConfig.get(cls,id);double amount=spell.power().get()/Math.ceil(spell.duration().get()/20.0);
        if(kind.equals("heal"))target.heal((float)amount);
        else if(kind.equals("poison")&&target.getHealth()>1)
            target.hurtServer(level,target.damageSources().magic(),(float)Math.min(amount,target.getHealth()-1));
        return true;
    }
    @Override public void addAttributeModifiers(AttributeMap attributes,int amplifier){
        if(!kind.equals("weak")&&!kind.equals("slow"))return;
        var attribute=attributes.getInstance(kind.equals("weak")?Attributes.ATTACK_DAMAGE:Attributes.MOVEMENT_SPEED);
        if(attribute==null)return;
        var key=kind.equals("weak")?WEAKNESS:SLOWNESS;
        attribute.removeModifier(key);
        double amount=D1AbilityConfig.get(cls,id).power().get();
        if(kind.equals("slow"))amount=Math.min(1.0,amount);
        attribute.addPermanentModifier(new AttributeModifier(key,-amount,kind.equals("weak")
                ?AttributeModifier.Operation.ADD_VALUE:AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    @Override public void removeAttributeModifiers(AttributeMap attributes){
        if(!kind.equals("weak")&&!kind.equals("slow"))return;
        var attribute=attributes.getInstance(kind.equals("weak")?Attributes.ATTACK_DAMAGE:Attributes.MOVEMENT_SPEED);
        if(attribute!=null)attribute.removeModifier(kind.equals("weak")?WEAKNESS:SLOWNESS);
    }
}
