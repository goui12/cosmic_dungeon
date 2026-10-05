package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.goui.cosmicdungeon.playerclass.d1.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.animal.Cow;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryFireworksTest {
    @org.junit.jupiter.api.BeforeAll static void config()throws Exception{MercenaryEntryTest.config();}
    @org.junit.jupiter.api.AfterAll static void unload(){MercenaryEntryTest.unloadConfig();}
    @Test void stockAndCadenceScaleWithinIntegerAndActiveTickBounds(){
        assertEquals(new MercenaryFireworkStock(5,600),MercenaryFireworkStock.initial());
        assertEquals(5,MercenaryFireworkStock.capacity(2));assertEquals(6,MercenaryFireworkStock.capacity(3));
        assertEquals(7,MercenaryFireworkStock.capacity(5));assertEquals(29,MercenaryFireworkStock.capacity(50));
        assertEquals(32772,MercenaryFireworkStock.capacity(Integer.MAX_VALUE));
        int previous=600;
        for(int level=1;level<=65536;level++){
            int interval=MercenaryFireworkStock.interval(level);
            assertTrue(interval>=100&&interval<=previous);previous=interval;
        }
        assertEquals(600,MercenaryFireworkStock.interval(0));assertEquals(100,previous);
    }
    @Test void restockWaitsThirtySecondsAndCannotAccumulateOfflineBursts(){
        var stock=MercenaryFireworkStock.initial().spend();
        stock=stock.advance(590,1);assertEquals(4,stock.count());assertEquals(10,stock.remaining());
        stock=stock.advance(10,1);assertEquals(5,stock.count());assertEquals(600,stock.remaining());
        assertEquals(stock,stock.advance(100000,1));
        var empty=new MercenaryFireworkStock(0,600);
        assertEquals(1,empty.advance(Integer.MAX_VALUE,1).count());
        assertEquals(empty,empty.advance(-1,1));assertEquals(empty,empty.advance(0,1));
        assertThrows(IllegalStateException.class,empty::spend);
    }
    @Test void earnedCadenceAppliesImmediatelyAndReducedCapDoesNotEraseExistingStock(){
        var stock=new MercenaryFireworkStock(4,600).advance(10,25);
        assertEquals(MercenaryFireworkStock.interval(25)-10,stock.remaining());
        assertEquals(9,new MercenaryFireworkStock(9,1).advance(10,1).count());
        assertThrows(IllegalArgumentException.class,()->new MercenaryFireworkStock(-1,20));
        assertThrows(IllegalArgumentException.class,()->new MercenaryFireworkStock(0,601));
    }
    @Test void partialAndEmptyStockSurviveCodecReloadWithoutRefill(){
        for(var stock:List.of(new MercenaryFireworkStock(0,13),new MercenaryFireworkStock(4,120),
                new MercenaryFireworkStock(29,100))){
            var tag=MercenaryFireworkStock.CODEC.encodeStart(NbtOps.INSTANCE,stock).getOrThrow();
            assertEquals(stock,MercenaryFireworkStock.CODEC.parse(NbtOps.INSTANCE,tag).getOrThrow());
        }
        var corrupt=new CompoundTag();corrupt.putInt("count",-1);corrupt.putInt("remaining",1);
        assertTrue(MercenaryFireworkStock.CODEC.parse(NbtOps.INSTANCE,corrupt).error().isPresent());
    }
    private static Zombie zombie(double x,double z){
        var entity=new Zombie(EntityType.ZOMBIE,null){
            @Override public net.minecraft.world.scores.PlayerTeam getTeam(){return null;}
        };
        entity.setPos(x,0,z);return entity;
    }
    @Test void clusteredEnemiesWinAndRejectedTargetsNeverReceiveAnAimPoint(){
        var lone=zombie(0,0);var a=zombie(10,0);var b=zombie(11,0);var c=zombie(10,1);
        assertSame(a,MercenaryFireworks.choose(List.of(lone,a,b,c),t->true,3));
        assertSame(b,MercenaryFireworks.choose(List.of(lone,a,b,c),t->t!=a,3));
        assertNull(MercenaryFireworks.choose(List.of(lone,a),t->false,3));
        assertNull(MercenaryFireworks.choose(List.of(),t->true,3));
    }
    @Test void targetSamplingIsBoundedEvenWithAnOversizedCallerList(){
        var candidates=new ArrayList<LivingEntity>();for(int i=0;i<100;i++)candidates.add(zombie(i*10,0));
        var checked=new AtomicInteger();
        assertNotNull(MercenaryFireworks.choose(candidates,t->{checked.incrementAndGet();return true;},3));
        assertEquals(48,checked.get());
    }
    @Test void damageMultiplierDoesNotRewriteLegacyBasePowerOrFalloff(){
        assertEquals(10,D1AbilityConfig.ROCKET_DAMAGE_MULTIPLIER.get());
        assertEquals(120,D1CombatRules.rocketDamage(D1AbilityConfig.get("pyroclast","cinderbite").power().get()*10,0,5));
        assertEquals(150,D1CombatRules.rocketDamage(D1AbilityConfig.get("pyroclast","cindermaul").power().get()*10,0,5));
        assertTrue(D1CombatRules.rocketDamage(120,4,5)>50);assertEquals(0,D1CombatRules.rocketDamage(120,5,5));
    }
    @Test void enemyPolicyExcludesNeutralAnimalsOwnedWolvesAndTheShooter(){
        var team=new net.minecraft.world.scores.PlayerTeam(new net.minecraft.world.scores.Scoreboard(),"friendly");
        var owner=new Zombie(EntityType.ZOMBIE,null){
            @Override public net.minecraft.world.scores.PlayerTeam getTeam(){return team;}
        };
        var hostile=zombie(1,0);
        assertTrue(D1RocketAbilities.enemy(owner,hostile));
        var ally=new Zombie(EntityType.ZOMBIE,null){
            @Override public net.minecraft.world.scores.PlayerTeam getTeam(){return team;}
        };
        assertFalse(D1RocketAbilities.enemy(owner,ally));
        assertFalse(D1RocketAbilities.enemy(owner,owner));
        var vanilla=net.minecraft.data.registries.VanillaRegistries.createLookup();
        var cows=new net.minecraft.core.MappedRegistry<>(
                net.minecraft.core.registries.Registries.COW_VARIANT,com.mojang.serialization.Lifecycle.stable());
        vanilla.lookupOrThrow(net.minecraft.core.registries.Registries.COW_VARIANT).listElements()
                .forEach(entry->net.minecraft.core.Registry.register(cows,entry.key(),entry.value()));
        var registries=new net.minecraft.core.RegistryAccess.ImmutableRegistryAccess(List.of(cows.freeze()));
        var cow=new Cow(EntityType.COW,null){
            @Override public net.minecraft.core.RegistryAccess registryAccess(){return registries;}
        };
        assertFalse(D1RocketAbilities.enemy(owner,cow));
        var wolf=new MercenaryTestWolf();wolf.setOwnerReference(EntityReference.of(UUID.randomUUID()));
        assertFalse(D1RocketAbilities.enemy(owner,wolf));
    }
}
