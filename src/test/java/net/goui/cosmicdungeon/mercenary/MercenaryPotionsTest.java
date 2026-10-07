package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.nbt.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryPotionsTest {
    @org.junit.jupiter.api.BeforeAll static void config()throws Exception{MercenaryEntryTest.config();}
    @org.junit.jupiter.api.AfterAll static void unload(){MercenaryEntryTest.unloadConfig();}
    private static MercenaryEntity merc(){return merc("theurgist");}
    private static MercenaryEntity merc(String role){
        var entity=new MercenaryEntity(ModEntities.MERCENARY.get(),null);
        entity.initialize(44,new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),role,2,50));
        return entity;
    }
    @Test void qualityAnchorsMonotonicAndBounded(){
        assertEquals(.05,MercenaryPotionBalance.qualityChance(1),1e-8);
        assertEquals(.45,MercenaryPotionBalance.qualityChance(10),1e-8);
        assertEquals(.95,MercenaryPotionBalance.qualityChance(25),1e-8);
        for(int i=1;i<1000;i++){
            assertTrue(MercenaryPotionBalance.qualityChance(i)<=MercenaryPotionBalance.qualityChance(i+1));
            assertTrue(MercenaryPotionBalance.qualityChance(i)<=.95);
        }
        assertEquals(.95,MercenaryPotionBalance.qualityChance(Integer.MAX_VALUE));
    }
    @Test void cooldownsImproveButNeverBecomeAnUnboundedLoop(){
        assertEquals(200,MercenaryPotionBalance.cadence(200,1,40));
        assertTrue(MercenaryPotionBalance.cadence(200,10,40)<200);
        int last=200;
        for(int i=1;i<1000;i++){int now=MercenaryPotionBalance.cadence(200,i,40);assertTrue(now<=last&&now>=40);last=now;}
        assertEquals(40,MercenaryPotionBalance.cadence(200,Integer.MAX_VALUE,40));
    }
    @Test void allGeneratedStockIsEffectSplashAndCatalogIncludesRequiredBuffs(){
        for(boolean positive:new boolean[]{true,false})for(int i=0;i<(positive?3:2);i++)for(boolean strong:new boolean[]{false,true}){
            var stack=MercenaryBrewing.create(positive,i,strong);
            assertTrue(MercenaryBrewing.effectSplash(stack));assertEquals(positive,MercenaryBrewing.positive(stack));
            assertEquals(i,MercenaryBrewing.kind(stack,positive));
        }
    }
    @Test void freeProductionHasHardStockAndNoCatchUpBurst(){
        for(String role:List.of("theurgist","venefex")){
            var entity=merc(role);boolean positive=role.equals("theurgist");
            for(int i=0;i<20;i++){MercenaryPotions.produce(entity,null);entity.timers(entity.timers().advance(100000));}
            assertEquals(6,MercenaryBrewing.stock(entity.supplies(),positive));
            assertEquals(0,MercenaryBrewing.stock(entity.supplies(),!positive));
            assertEquals(6,entity.supplies().stream().mapToInt(ItemStack::getCount).sum());
        }
        var empty=merc();MercenaryPotions.produce(empty,null);var saved=empty.timers();
        MercenaryPotions.produce(empty,null);
        assertEquals(1,empty.supplies().stream().mapToInt(ItemStack::getCount).sum());assertEquals(saved,empty.timers());
    }
    @Test void allClassesRejectCraftingSuppliesAndNonSplashBottles(){
        var builder=new PotionBrewing.Builder(FeatureFlags.DEFAULT_FLAGS);PotionBrewing.addVanillaMixes(builder);var recipes=builder.build();
        for(String role:MercenaryContract.CLASSES){
            var hire=new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),role,2,50);
            for(var stack:List.of(new ItemStack(Items.BONE),new ItemStack(Items.WOLF_SPAWN_EGG),
                    new ItemStack(Items.NETHER_WART),new ItemStack(Items.BLAZE_POWDER),new ItemStack(Items.GUNPOWDER),
                    PotionContents.createItemStack(Items.POTION,Potions.HEALING),
                    PotionContents.createItemStack(Items.LINGERING_POTION,Potions.POISON),
                    PotionContents.createItemStack(Items.SPLASH_POTION,Potions.WATER)))
                assertFalse(MercenaryInventory.useful(stack,hire,recipes),role+" "+stack);
        }
    }
    @Test void usefulBuffRefreshNeverDowngradesStrongerOrInfiniteEffects(){
        var weak=new MobEffectInstance(MobEffects.NIGHT_VISION,300,0);
        assertTrue(MercenaryPotionCasting.refresh(null,weak));
        assertFalse(MercenaryPotionCasting.refresh(new MobEffectInstance(MobEffects.NIGHT_VISION,5,1),weak));
        assertFalse(MercenaryPotionCasting.refresh(new MobEffectInstance(MobEffects.NIGHT_VISION,-1,0),weak));
        assertTrue(MercenaryPotionCasting.refresh(new MobEffectInstance(MobEffects.NIGHT_VISION,20,0),weak));
    }
    @Test void everyTargetAndDamageTickSharesOnePotionCreditAndFailedAwardCanRetry(){
        var source=new CompoundTag();var awards=new AtomicInteger();var owner=UUID.randomUUID();
        var first=new MercenaryPotionCredit.Dose(owner,44,source);var second=new MercenaryPotionCredit.Dose(owner,44,source);
        assertFalse(first.once(()->false));
        assertTrue(second.once(()->{awards.incrementAndGet();return true;}));
        for(int i=0;i<50;i++)assertFalse(first.once(()->{awards.incrementAndGet();return true;}));
        assertEquals(1,awards.get());
        assertFalse(MercenarySkillEffects.credit(source,MercenarySkill.POSITIVE_POTIONS,()->true));
    }
    @Test void roleFiltersBlockWrongStockMixedEffectsAndUndeadInversion(){
        var positive=merc("theurgist").contract();var negative=merc("venefex").contract();
        var healing=MercenaryBrewing.create(true,0,false);var poison=MercenaryBrewing.create(false,1,false);
        assertTrue(MercenaryBrewing.roleSplash(healing,positive));assertFalse(MercenaryBrewing.roleSplash(poison,positive));
        assertTrue(MercenaryBrewing.roleSplash(poison,negative));assertFalse(MercenaryBrewing.roleSplash(healing,negative));
        var mixed=healing.copy();mixed.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                mixed.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).withEffectAdded(new MobEffectInstance(MobEffects.POISON,100)));
        assertFalse(MercenaryBrewing.roleSplash(mixed,positive));assertFalse(MercenaryBrewing.roleSplash(mixed,negative));
        assertTrue(MercenaryPotions.roleAllows(positive,MobEffects.INSTANT_HEALTH.value(),false));
        assertFalse(MercenaryPotions.roleAllows(positive,MobEffects.INSTANT_HEALTH.value(),true));
        assertFalse(MercenaryPotions.roleAllows(positive,MobEffects.INSTANT_DAMAGE.value(),false));
        assertTrue(MercenaryPotions.roleAllows(negative,MobEffects.INSTANT_DAMAGE.value(),false));
        assertFalse(MercenaryPotions.roleAllows(negative,MobEffects.INSTANT_DAMAGE.value(),true));
        assertFalse(MercenaryPotions.roleAllows(negative,MobEffects.INSTANT_HEALTH.value(),true));
        assertTrue(MercenaryBrewing.create(false,0,true).get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).is(Potions.STRONG_HARMING));
        assertTrue(MercenaryBrewing.create(false,1,true).get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).is(Potions.STRONG_POISON));
    }
    @Test void stockAndThrowTimersRoundTripWithoutNewSchema(){
        var timers=new MercenaryTimers(400,3600,Map.of()).used(MercenaryPotionBalance.stockKey(true,false),100)
                .used(MercenaryPotionBalance.throwKey(MercenarySkill.POSITIVE_POTIONS),200).used(MercenaryPotionBalance.GLOBAL,20);
        var loaded=MercenaryTimers.CODEC.parse(NbtOps.INSTANCE,MercenaryTimers.CODEC.encodeStart(NbtOps.INSTANCE,timers).getOrThrow()).getOrThrow();
        assertEquals(timers,loaded);assertFalse(loaded.ready(MercenaryPotionBalance.GLOBAL));
        assertTrue(loaded.advance(20).ready(MercenaryPotionBalance.GLOBAL));
    }
}
