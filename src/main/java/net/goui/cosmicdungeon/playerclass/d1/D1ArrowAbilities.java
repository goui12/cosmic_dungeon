package net.goui.cosmicdungeon.playerclass.d1;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.dungeon.d1.D1Members;
import net.goui.cosmicdungeon.effect.ModMobEffects;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import java.util.*;
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class D1ArrowAbilities {
    private D1ArrowAbilities(){}
    private static String ability(AbstractArrow arrow){
        if(!(arrow.getOwner() instanceof ServerPlayer owner))return null;
        var run=D1Members.run(owner.level()).orElse(null);
        if(run==null||arrow.level()!=owner.level()||!D1Members.inside(owner,run))return null;
        String id=D1AbilityIdentity.identify(arrow.getPickupItemStackOrigin());
        return id!=null&&D1AbilityConfig.get(ClassData.getClassId(owner),id)!=null?id:null;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void instant(ProjectileImpactEvent event){
        if(!(event.getProjectile() instanceof AbstractArrow arrow)
                ||!(event.getRayTraceResult() instanceof EntityHitResult hit)
                ||!(hit.getEntity() instanceof LivingEntity target)||!(arrow.level() instanceof ServerLevel level))return;
        String id=ability(arrow);
        if(id==null||!Set.of("scintilla_vitalis","lux_vitalis","spicule_breach","spicule_rend").contains(id))return;
        var owner=(ServerPlayer)arrow.getOwner();
        double power=D1AbilityConfig.get(ClassData.getClassId(owner),id).power().get();
        boolean healing=id.equals("scintilla_vitalis")||id.equals("lux_vitalis");
        if(healing&&!target.isInvertedHealAndHarm())target.heal((float)power);
        else if(!healing&&target.isInvertedHealAndHarm())
            target.heal(D1AbilityConfig.get("venefex","spicule_undead_healing").power().get().floatValue());
        else {
            if(!healing){
                long debuffs=target.getActiveEffects().stream().filter(e->e.getEffect().value().getCategory()==MobEffectCategory.HARMFUL).count();
                power*=1+Math.min(debuffs,D1AbilityConfig.SPICULE_DEBUFF_CAP.get())*D1AbilityConfig.SPICULE_DEBUFF_SCALE.get();
            }
            target.hurtServer(level,target.damageSources().indirectMagic(arrow,owner),(float)power);
        }
        // These magic arrows apply the documented amount without a second ordinary arrow wound.
        event.setCanceled(true);arrow.discard();
    }
    public static boolean apply(AbstractArrow arrow,LivingEntity target){
        String id=ability(arrow);if(id==null)return false;
        String cls=ClassData.getClassId((ServerPlayer)arrow.getOwner());
        var spell=D1AbilityConfig.get(cls,id);Holder<MobEffect> effect=switch(id){
            case "mending_sting"->ModMobEffects.MENDING_STING;
            case "verdant_jolt"->ModMobEffects.VERDANT_JOLT;
            case "tree_viper"->ModMobEffects.TREE_VIPER;
            case "bushmaster"->ModMobEffects.BUSHMASTER;
            case "fer_de_lance"->ModMobEffects.FER_DE_LANCE;
            case "pestis"->ModMobEffects.PESTIS;
            case "black_bubo"->ModMobEffects.BLACK_BUBO;
            case "vapours"->ModMobEffects.VAPOURS;
            case "melancholia"->ModMobEffects.MELANCHOLIA;
            case "deathly_stupor"->ModMobEffects.DEATHLY_STUPOR;
            case "ebonsight"->MobEffects.NIGHT_VISION;
            case "vielpiercer"->MobEffects.GLOWING;
            default->null;
        };
        if(effect==null)return false;
        var family=switch(id){
            case "mending_sting","verdant_jolt"->List.of(ModMobEffects.MENDING_STING,ModMobEffects.VERDANT_JOLT);
            case "tree_viper","bushmaster","fer_de_lance"->List.of(ModMobEffects.TREE_VIPER,ModMobEffects.BUSHMASTER,ModMobEffects.FER_DE_LANCE);
            case "pestis","black_bubo"->List.of(ModMobEffects.PESTIS,ModMobEffects.BLACK_BUBO);
            case "vapours","melancholia","deathly_stupor"->List.of(ModMobEffects.VAPOURS,ModMobEffects.MELANCHOLIA,ModMobEffects.DEATHLY_STUPOR);
            default->List.<net.neoforged.neoforge.registries.DeferredHolder<MobEffect,MobEffect>>of();
        };
        for(var other:family){
            if(other==effect||!target.hasEffect(other))continue;
            var previous=D1AbilityConfig.get(cls,other.getId().getPath());
            boolean periodic=Set.of("mending_sting","verdant_jolt","tree_viper","bushmaster","fer_de_lance").contains(id);
            double oldPower=previous.power().get()/(periodic?previous.duration().get():1.0);
            double newPower=spell.power().get()/(periodic?spell.duration().get():1.0);
            if(oldPower>newPower)return true;
            target.removeEffect(other);
        }
        target.addEffect(new MobEffectInstance(effect,spell.duration().get(),0),arrow.getOwner());
        return true;
    }
    @SubscribeEvent public static void preventForgedRename(AnvilUpdateEvent event){
        String before=D1AbilityIdentity.identify(event.getLeft()),after=D1AbilityIdentity.identify(event.getOutput());
        if((before!=null||after!=null)&&!Objects.equals(before,after))event.setCanceled(true);
    }
    // TODO(M55/M58, later tiers): magical conduits, enhanced reagents and D2+ arrows remain disabled.
    // See Theurgist April 5 overview and Venefex April 4 overview. Preserve D1 vanilla Totems.
    // TODO(M58, formula confirmation): Dad specifies increasing Spicule damage with debuffs but no
    // numeric curve. Current server config explicitly exposes the bounded ten-percent-per-effect choice.
}
