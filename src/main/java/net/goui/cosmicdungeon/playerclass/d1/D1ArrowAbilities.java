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
    private static String identity(AbstractArrow arrow) {
        return D1AbilityIdentity.identify(arrow.getPickupItemStackOrigin());
    }
    private static D1CombatRules.Ammunition permission(AbstractArrow arrow, String id) {
        return D1ProjectileAccess.permission(arrow, arrow.getPickupItemStackOrigin(), id);
    }
    /** Narrow exception to native team friendly-fire filtering: restorative D1 aid only. */
    public static boolean canSupport(AbstractArrow arrow, ServerPlayer target) {
        String id = identity(arrow);
        if (!D1CombatRules.supportive(id) || target.isInvertedHealAndHarm()
                || permission(arrow, id) != D1CombatRules.Ammunition.ABILITY) return false;
        var run = D1Members.run(target.level()).orElse(null);
        return target.level() == arrow.level() && run != null && D1Members.inside(target, run);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void instant(ProjectileImpactEvent event){
        if(!(event.getProjectile() instanceof AbstractArrow arrow)
                ||!(event.getRayTraceResult() instanceof EntityHitResult hit)
                ||!(hit.getEntity() instanceof LivingEntity target)||!(arrow.level() instanceof ServerLevel level))return;
        String id=identity(arrow);
        var access=permission(arrow,id);
        if(access==D1CombatRules.Ammunition.VANILLA)return;
        if(access==D1CombatRules.Ammunition.DENIED){
            event.setCanceled(true);arrow.discard();return;
        }
        if(!Set.of("scintilla_vitalis","lux_vitalis","spicule_breach","spicule_rend").contains(id)){
            if(D1CombatRules.supportive(id)){
                // Source calls these aid: do not inflict an ordinary wound before regeneration/vision.
                apply(arrow,target);event.setCanceled(true);arrow.discard();
            }
            return;
        }
        var owner=(ServerPlayer)arrow.getOwner();
        double power=D1AbilityConfig.get(ClassData.getClassId(owner),id).power().get();
        boolean healing=id.equals("scintilla_vitalis")||id.equals("lux_vitalis");
        if(healing&&!target.isInvertedHealAndHarm())target.heal((float)power);
        else if(!healing&&target.isInvertedHealAndHarm())
            target.heal(D1AbilityConfig.get("venefex","spicule_undead_healing").power().get().floatValue());
        else {
            if(!healing){
                long debuffs=target.getActiveEffects().stream().filter(e->e.getEffect().value().getCategory()==MobEffectCategory.HARMFUL).count();
                power=D1CombatRules.spicule(power,debuffs,D1AbilityConfig.SPICULE_DEBUFF_SCALE.get(),D1AbilityConfig.SPICULE_DEBUFF_CAP.get());
            }
            target.hurtServer(level,target.damageSources().indirectMagic(arrow,owner),(float)power);
        }
        // These magic arrows apply the documented amount without a second ordinary arrow wound.
        event.setCanceled(true);arrow.discard();
    }
    public static boolean apply(AbstractArrow arrow,LivingEntity target){
        String id=identity(arrow);
        var access=permission(arrow,id);
        if(access==D1CombatRules.Ammunition.VANILLA)return false;
        if(access==D1CombatRules.Ammunition.DENIED)return true;
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
        if(effect==null||spell==null)return true;
        Holder<MobEffect> vanillaFamily=switch(id){
            case "mending_sting","verdant_jolt"->MobEffects.REGENERATION;
            case "tree_viper","bushmaster","fer_de_lance"->MobEffects.POISON;
            default->null;
        };
        // Preserve native undead/spider immunity and other mods' applicability vetoes.
        if(vanillaFamily!=null&&!net.neoforged.neoforge.common.CommonHooks.canMobEffectBeApplied(target,
                new MobEffectInstance(vanillaFamily,spell.duration().get(),0),arrow.getOwner()))return true;
        var incoming=new MobEffectInstance(effect,spell.duration().get(),0);
        if(!net.neoforged.neoforge.common.CommonHooks.canMobEffectBeApplied(target,incoming,arrow.getOwner()))return true;
        var family=switch(id){
            case "mending_sting","verdant_jolt"->List.of(ModMobEffects.MENDING_STING,ModMobEffects.VERDANT_JOLT);
            case "tree_viper","bushmaster","fer_de_lance"->List.of(ModMobEffects.TREE_VIPER,ModMobEffects.BUSHMASTER,ModMobEffects.FER_DE_LANCE);
            case "pestis","black_bubo"->List.of(ModMobEffects.PESTIS,ModMobEffects.BLACK_BUBO);
            case "vapours","melancholia","deathly_stupor"->List.of(ModMobEffects.VAPOURS,ModMobEffects.MELANCHOLIA,ModMobEffects.DEATHLY_STUPOR);
            default->List.<net.neoforged.neoforge.registries.DeferredHolder<MobEffect,MobEffect>>of();
        };
        // Decide across the whole family before removing anything, including legacy overlaps.
        for(var other:family){
            var current=target.getEffect(other);
            if(other==effect||current==null)continue;
            var previous=D1AbilityConfig.get(cls,other.getId().getPath());
            if(D1CombatRules.existingWins(previous.power().get(),previous.duration().get(),current.getDuration(),
                    spell.power().get(),spell.duration().get(),vanillaFamily!=null))return true;
        }
        for(var other:family){
            if(other!=effect&&target.hasEffect(other)&&!target.removeEffect(other))return true;
        }
        target.addEffect(incoming,arrow.getOwner());
        return true;
    }
    @SubscribeEvent public static void preventForgedRename(AnvilUpdateEvent event){
        String before=D1AbilityIdentity.identify(event.getLeft()),after=D1AbilityIdentity.identify(event.getOutput());
        if((before!=null||after!=null)&&!Objects.equals(before,after))event.setCanceled(true);
    }
    // TODO(M55/M58, licensed TEST): shields/cooldown, same-team support, authored components
    // and effect vetoes require acceptance. A stateful mod can change applicability after
    // preflight or veto removal within a legacy overlap; never bypass events to force it.
    // No automatic saved-effect migration. Existing native damage and effect hooks remain.
    // TODO(M55/M58, later tiers): magical conduits, enhanced reagents and D2+ arrows remain disabled.
    // See Theurgist April 5 overview and Venefex April 4 overview. Preserve D1 vanilla Totems.
    // TODO(M58, formula confirmation): Dad specifies increasing Spicule damage with debuffs but no
    // numeric curve. Current server config explicitly exposes the bounded ten-percent-per-effect choice.
}
