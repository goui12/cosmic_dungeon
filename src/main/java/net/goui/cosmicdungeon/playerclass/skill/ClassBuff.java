package net.goui.cosmicdungeon.playerclass.skill;

import java.util.Optional;
import net.goui.cosmicdungeon.effect.ModMobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.*;

/** Shared player/mercenary balance catalog. Native Strength/Resistance are the same potion families. */
public enum ClassBuff {
    FLURRY("bogatyr","flurry_of_fangs","Flurry of Fangs",36000,0,"+10% melee attack speed for 30 minutes."),
    SHEATH("dragoon","blackiron_sheath","Blackiron Sheath",18000,0,"20% damage reduction for 15 minutes."),
    AEGIS("judicator","judicators_aegis","Judicator’s Aegis",36000,0,"+3 melee attack damage for 30 minutes."),
    BULWARK("judicator","defenders_bulwark","Defender’s Bulwark",36000,1,"+6 melee attack damage for 30 minutes."),
    BANISH("venefex","banish_afflictions","Banish Afflictions",0,0,"Remove harmful effects; beneficial and neutral effects remain.");
    public static final int COOLDOWN=5*60*20;
    public static final double RADIUS=10;
    private final String classId,id,title,description;private final int duration,amplifier;
    ClassBuff(String cls,String id,String title,int duration,int amplifier,String description){
        this.classId=cls;this.id=id;this.title=title;this.duration=duration;this.amplifier=amplifier;this.description=description;
    }
    public String classId(){return classId;}public String id(){return id;}public String title(){return title;}
    public String description(){return description;}public int duration(){return duration;}public int amplifier(){return amplifier;}
    public Holder<MobEffect> effect(){return switch(this){
        case FLURRY->ModMobEffects.FLURRY_OF_FANGS;case SHEATH->MobEffects.RESISTANCE;
        case AEGIS,BULWARK->MobEffects.STRENGTH;case BANISH->throw new IllegalStateException("Cleanse has no timed effect");
    };}
    public static Optional<ClassBuff> forClass(String cls,int smiteLevel){return Optional.ofNullable(switch(cls){
        case "bogatyr"->FLURRY;case "dragoon"->SHEATH;case "judicator"->smiteLevel>=3?BULWARK:AEGIS;
        case "venefex"->BANISH;default->null;
    });}
}
