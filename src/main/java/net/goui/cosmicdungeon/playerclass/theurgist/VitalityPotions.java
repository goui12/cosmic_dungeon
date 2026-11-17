package net.goui.cosmicdungeon.playerclass.theurgist;

import java.util.*;
import net.goui.cosmicdungeon.effect.ModMobEffects;
import net.goui.cosmicdungeon.potion.ModPotions;
import net.goui.cosmicdungeon.playerclass.skill.*;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.mercenary.MercenaryPotions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;

/** One rank table and one family for crafted player and automatic mercenary potions. */
public final class VitalityPotions {
    public static final int DURATION=45*60*20;
    public static final String SOURCE="cosmicdungeon_vitality_v1";
    public record Rank(String id,String name,int level,int percent,int amplifier) {
        public String targetId(){return "cosmicdungeon:"+id+"_splash";}
    }
    public static final List<Rank> RANKS=List.of(
        new Rank("scintilla_vitalis","Scintilla Vitalis",0,10,0),
        new Rank("favilla_vitas","Favilla Vitas",25,15,1),
        new Rank("tepor_sanans","Tepor Sanans",35,20,2),
        new Rank("flamma_vitalis","Flamma Vitalis",42,25,3),
        new Rank("lux_vitalis","Lux Vitalis",50,30,4),
        new Rank("radius_sanans","Radius Sanans",55,35,5),
        new Rank("stella_sanans","Stella Sanans",59,40,6),
        new Rank("aurora_reficiens","Aurora Reficiens",63,45,7),
        new Rank("balsamum_solis","Balsamum Solis",66,50,8),
        new Rank("fulgor_vitae","Fulgor Vitae",68,55,9));
    private VitalityPotions(){}
    public static Rank rank(int level){
        Rank found=RANKS.getFirst();for(var candidate:RANKS)if(level>=candidate.level())found=candidate;return found;
    }
    public static Rank amplifier(int amplifier){return RANKS.get(Math.clamp(amplifier,0,RANKS.size()-1));}
    public static Optional<Rank> target(String id){return RANKS.stream().filter(r->r.targetId().equals(id)).findFirst();}
    public static boolean craftable(String target,int level){return target(target).map(r->r.equals(rank(level))).orElse(true);}
    public static int level(ServerPlayer player){return SignatureSkills.playerLevel(player,SignatureSkill.POSITIVE_POTIONS);}
    public static Holder<Potion> potion(int level){return ModPotions.VITALITY.get(rank(level).amplifier());}
    public static ItemStack splash(int level){return PotionContents.createItemStack(Items.SPLASH_POTION,potion(level));}
    public static boolean isVitality(MobEffect effect){return effect==ModMobEffects.VITALITY.get();}
    public static boolean isVitality(MobEffectInstance effect){return effect!=null&&isVitality(effect.getEffect().value());}
    public static Optional<Rank> in(ItemStack stack){
        var contents=stack.get(DataComponents.POTION_CONTENTS);if(contents==null)return Optional.empty();
        for(var effect:contents.getAllEffects())if(isVitality(effect))return Optional.of(amplifier(effect.getAmplifier()));
        return Optional.empty();
    }
    public static boolean useful(LivingEntity target,int amplifier){
        var old=target.getEffect(ModMobEffects.VITALITY);
        return old==null||amplifier>old.getAmplifier();
    }
    /** Refreshing equal/lower ranks is a no-op; it cannot extend duration or train support. */
    public static boolean apply(LivingEntity target,MobEffectInstance incoming,Entity source){
        if(target.level().isClientSide()||!isVitality(incoming)||!useful(target,incoming.getAmplifier()))return false;
        var rank=amplifier(incoming.getAmplifier());
        var effect=new MobEffectInstance(ModMobEffects.VITALITY,DURATION,rank.amplifier(),false,true,true);
        boolean changed=target.addEffect(effect,source);
        if(!changed||!target.hasEffect(ModMobEffects.VITALITY))return false;
        // Keep only the replacement tier; native hidden weaker effects must not reappear after expiry.
        var actual=target.getEffect(ModMobEffects.VITALITY);
        target.getActiveEffectsMap().put(ModMobEffects.VITALITY,new MobEffectInstance(ModMobEffects.VITALITY,
                actual.getDuration(),actual.getAmplifier(),actual.isAmbient(),actual.isVisible(),actual.showIcon()));
        var caster=source==null?null:MercenaryPotions.caster(source);
        if(caster==null)caster=source;
        var tag=target.getPersistentData().getCompoundOrEmpty(SOURCE).copy();
        tag.putInt("version",1);tag.putInt("amplifier",rank.amplifier());
        if(caster!=null){tag.putString("caster",caster.getUUID().toString());tag.putString("name",caster.getName().getString());}
        else {tag.remove("caster");tag.remove("name");}
        target.getPersistentData().put(SOURCE,tag);
        return true;
    }
    /** Existing configurable bonus curve shifted to begin at Positive Potions25; never applied to Vitality. */
    public static double bonus(int level,int cap,double maximum){
        return ClassSkillRules.bonus(Math.max(0,level-24),Math.max(1,cap),maximum);
    }
    public static Component unlockMessage(String name,Rank rank){
        return Component.literal(name+" has a new understanding of potions. ")
                .append(Component.literal(rank.name()+" unlocked.").withStyle(ChatFormatting.ITALIC));
    }
    public static void unlocked(MinecraftServer server,DungeonRunRegistryData.RunRecord run,String name,int before,int after){
        if(run==null||rank(after).equals(rank(before)))return;
        var message=unlockMessage(name,rank(after));
        for(var id:run.orderedPlayers().stream().limit(6).toList()){
            var player=server.getPlayerList().getPlayer(id);
            if(player!=null&&!run.isCompletionExited(id)&&run.containsDimension(player.level().dimension()))
                player.sendSystemMessage(message);
        }
    }
}
