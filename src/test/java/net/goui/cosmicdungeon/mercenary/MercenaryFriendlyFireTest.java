package net.goui.cosmicdungeon.mercenary;

import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.playerclass.bogatyr.CompanionAllies;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real native entities/events, without starting a level or server. */
final class MercenaryFriendlyFireTest {
    private static MercenaryEntity mercenary(){return new MercenaryEntity(ModEntities.MERCENARY.get(),null);}
    private static IronGolem friendly(){return new IronGolem(EntityType.IRON_GOLEM,null);}
    private static net.minecraft.world.entity.animal.wolf.Wolf dog(){
        var wolf=new MercenaryTestWolf();
        MercenaryWolves.mark(wolf,new MercenaryWolves.Bond(42,java.util.UUID.randomUUID(),java.util.UUID.randomUUID()));
        return wolf;
    }
    private static Zombie hostile(){return new Zombie(EntityType.ZOMBIE,null);}
    private static LivingIncomingDamageEvent damage(LivingEntity target,Entity direct,Entity cause){
        var source=new DamageSource(Holder.direct(new DamageType("test",0)),direct,cause);
        var event=new LivingIncomingDamageEvent(target,new DamageContainer(source,5));
        MercenaryBrain.friendlyDamage(event);return event;
    }
    private static final class Shot extends Arrow {
        private final Entity shooter;
        Shot(Entity shooter){super(EntityType.ARROW,null);this.shooter=shooter;}
        @Override public Entity getOwner(){return shooter;}
    }
    private static final class Cloud extends AreaEffectCloud {
        private final LivingEntity caster;
        Cloud(LivingEntity caster){super(EntityType.AREA_EFFECT_CLOUD,null);this.caster=caster;}
        @Override public LivingEntity getOwner(){return caster;}
    }
    @Test void incomingAndOutgoingFriendlyDamageAreCanceledWithoutActiveRun(){
        var merc=mercenary();var ally=friendly();
        assertTrue(CompanionAllies.friendlyType(ServerPlayer.class));
        assertTrue(damage(merc,ally,ally).isCanceled());
        assertTrue(damage(ally,merc,merc).isCanceled());
        assertTrue(damage(merc,mercenary(),null).isCanceled());
    }
    @Test void indirectDamageResolvesProjectileAndCloudOwners(){
        var merc=mercenary();var ally=friendly();
        assertTrue(damage(merc,new Shot(ally),ally).isCanceled());
        assertTrue(damage(merc,new Shot(ally),null).isCanceled());
        assertTrue(damage(merc,new Cloud(ally),null).isCanceled());
    }
    @Test void hostileAndEnvironmentalDamageRemainAllowed(){
        var merc=mercenary();var enemy=hostile();
        assertFalse(damage(merc,enemy,enemy).isCanceled());
        assertFalse(damage(merc,new Shot(enemy),enemy).isCanceled());
        assertFalse(damage(merc,null,null).isCanceled());
        assertFalse(damage(hostile(),friendly(),friendly()).isCanceled());
    }
    @Test void friendlyArrowImpactIsCanceledBeforeNativeEffectsWithoutDestroyingAmmo(){
        var arrow=new Shot(friendly());
        var event=new ProjectileImpactEvent(arrow,new EntityHitResult(mercenary()));
        MercenaryBrain.impact(event);
        assertTrue(event.isCanceled());assertFalse(arrow.isRemoved());
        var enemyArrow=new Shot(hostile());
        var enemyEvent=new ProjectileImpactEvent(enemyArrow,new EntityHitResult(mercenary()));
        MercenaryBrain.impact(enemyEvent);assertFalse(enemyEvent.isCanceled());
    }
    @Test void harmfulFriendlyPotionEffectsAreBlockedButHealingAndHostileEffectsRemain(){
        var merc=mercenary();var ally=friendly();
        for(Entity source:new Entity[]{ally,new Shot(ally),new Cloud(ally)}){
            assertFalse(MercenaryPotions.allows(source,merc,MobEffects.INSTANT_DAMAGE.value()));
            assertFalse(MercenaryPotions.allows(source,merc,MobEffects.POISON.value()));
            assertTrue(MercenaryPotions.allows(source,merc,MobEffects.INSTANT_HEALTH.value()));
            assertTrue(MercenaryPotions.allows(source,merc,MobEffects.REGENERATION.value()));
        }
        assertTrue(MercenaryPotions.allows(new Cloud(hostile()),merc,MobEffects.POISON.value()));
        assertTrue(MercenaryPotions.allows(null,merc,MobEffects.POISON.value()));
    }
    private static final class OwnedMob extends Zombie implements OwnableEntity {
        private final EntityReference<LivingEntity> owner=EntityReference.of(java.util.UUID.randomUUID());
        OwnedMob(){super(EntityType.ZOMBIE,null);}
        @Override public EntityReference<LivingEntity> getOwnerReference(){return owner;}
    }
    @Test void ownedCompanionsAreProtectedWithoutResolvingTheirOfflineOwner(){
        var pet=new OwnedMob();var merc=mercenary();
        assertTrue(damage(merc,pet,pet).isCanceled());
        assertTrue(damage(pet,merc,merc).isCanceled());
        assertTrue(damage(merc,new Shot(pet),null).isCanceled());
    }
    @Test void orphanedMercenaryProjectilesCannotRegainFriendlyDamage(){
        var shot=new Shot(null);MercenaryPotions.mark(shot,mercenary());
        assertTrue(damage(friendly(),shot,null).isCanceled());
        assertTrue(damage(mercenary(),shot,null).isCanceled());
        assertFalse(MercenaryPotions.allows(shot,friendly(),MobEffects.POISON.value()));
    }
    @Test void applicabilityHookVetoesFriendlyDebuffAtSource(){
        var event=new MobEffectEvent.Applicable(mercenary(),new MobEffectInstance(MobEffects.POISON,100),friendly());
        MercenaryBrain.friendlyEffect(event);
        assertEquals(MobEffectEvent.Applicable.Result.DO_NOT_APPLY,event.getResult());
        var healing=new MobEffectEvent.Applicable(mercenary(),new MobEffectInstance(MobEffects.REGENERATION,100),friendly());
        MercenaryBrain.friendlyEffect(healing);
        assertEquals(MobEffectEvent.Applicable.Result.DEFAULT,healing.getResult());
    }
    @Test void dogsRejectFriendlyMeleeArrowsAndCloudDamageButNotEnemies(){
        var dog=dog();var ally=friendly();var enemy=hostile();
        assertTrue(damage(dog,ally,ally).isCanceled());
        assertTrue(damage(dog,new Shot(ally),null).isCanceled());
        assertTrue(damage(dog,new Cloud(ally),null).isCanceled());
        assertTrue(damage(ally,dog,dog).isCanceled());
        assertFalse(damage(dog,enemy,enemy).isCanceled());
        assertFalse(damage(dog,null,null).isCanceled());
        var impact=new ProjectileImpactEvent(new Shot(ally),new EntityHitResult(dog));
        MercenaryBrain.impact(impact);assertTrue(impact.isCanceled());
    }
    @Test void dogsBlockFriendlyNegativePotionsWhilePreservingHealingAndEnemyEffects(){
        var dog=dog();var ally=friendly();
        for(Entity source:new Entity[]{ally,new Shot(ally),new Cloud(ally)}){
            assertFalse(MercenaryPotions.allows(source,dog,MobEffects.POISON.value()));
            assertFalse(MercenaryPotions.allows(source,dog,MobEffects.INSTANT_DAMAGE.value()));
            assertTrue(MercenaryPotions.allows(source,dog,MobEffects.INSTANT_HEALTH.value()));
            assertTrue(MercenaryPotions.allows(source,dog,MobEffects.REGENERATION.value()));
        }
        assertTrue(MercenaryPotions.allows(new Cloud(hostile()),dog,MobEffects.POISON.value()));
        var event=new MobEffectEvent.Applicable(dog,new MobEffectInstance(MobEffects.SLOWNESS,100),ally);
        MercenaryBrain.friendlyEffect(event);assertEquals(MobEffectEvent.Applicable.Result.DO_NOT_APPLY,event.getResult());
    }

}
