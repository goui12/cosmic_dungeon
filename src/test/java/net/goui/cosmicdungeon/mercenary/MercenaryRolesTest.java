package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryRolesTest {
    @org.junit.jupiter.api.BeforeAll static void config()throws Exception{MercenaryEntryTest.config();}
    @org.junit.jupiter.api.AfterAll static void unload(){MercenaryEntryTest.unloadConfig();}
    private static MercenaryEntity merc(String role){
        var entity=new MercenaryEntity(ModEntities.MERCENARY.get(),null);
        entity.initialize(42,new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),role,2,500));
        entity.timers(new MercenaryTimers(400,3600,Map.of("minecraft:healing",200)));
        return entity;
    }
    private static PotionBrewing recipes(){
        var builder=new PotionBrewing.Builder(FeatureFlags.DEFAULT_FLAGS);
        PotionBrewing.addVanillaMixes(builder);return builder.build();
    }
    private static BrewingStandBlockEntity stand(){
        return new BrewingStandBlockEntity(BlockPos.ZERO,Blocks.BREWING_STAND.defaultBlockState());
    }
    private static void ingredients(MercenaryEntity entity){
        entity.supplies().set(0,PotionContents.createItemStack(Items.POTION,Potions.WATER));
        entity.supplies().set(1,new ItemStack(Items.NETHER_WART,2));
    }
    @Test void theurgistBrewsImmediatelyWithoutSpendingOrChangingPlayerStandContents(){
        var entity=merc("theurgist");ingredients(entity);var stand=stand();
        stand.setItem(0,PotionContents.createItemStack(Items.SPLASH_POTION,Potions.HEALING));
        stand.setItem(3,new ItemStack(Items.GLOWSTONE_DUST,7));stand.setItem(4,new ItemStack(Items.BLAZE_POWDER,9));
        var before=new ArrayList<ItemStack>();for(int i=0;i<5;i++)before.add(stand.getItem(i).copy());
        var timers=entity.timers();
        assertTrue(MercenaryBrewing.brew(entity,stand,recipes()));
        assertTrue(entity.supplies().get(0).get(DataComponents.POTION_CONTENTS).is(Potions.AWKWARD));
        assertEquals(1,entity.supplies().get(1).getCount());assertEquals(timers,entity.timers());
        for(int i=0;i<5;i++)assertTrue(ItemStack.matches(before.get(i),stand.getItem(i)));
    }
    @Test void allOtherClassesRejectBrewingAndPotionSupportEvenWithLegacySupplies(){
        for(String role:MercenaryContract.CLASSES){
            if(role.equals("theurgist"))continue;
            var entity=merc(role);ingredients(entity);var before=MercenaryInventory.copy(entity.supplies());
            entity.timers(new MercenaryTimers(0,0,Map.of()));
            assertFalse(MercenaryBrewing.brew(entity,stand(),recipes()));
            MercenaryPotions.produce(entity,null);assertFalse(MercenaryPotions.use(entity,List.of()));
            assertEquals(new MercenaryTimers(0,0,Map.of()),entity.timers());
            for(int i=0;i<before.size();i++)assertTrue(ItemStack.matches(before.get(i),entity.supplies().get(i)));
            for(var stack:List.of(new ItemStack(Items.NETHER_WART),PotionContents.createItemStack(Items.POTION,Potions.HEALING)))
                assertFalse(MercenaryInventory.useful(stack,entity.contract(),recipes()));
        }
    }
    @Test void theurgistCannotBrewAwayFromAStandOrThroughALockedStand(){
        var entity=merc("theurgist");ingredients(entity);
        assertFalse(MercenaryBrewing.brew(entity,new BarrelBlockEntity(BlockPos.ZERO,Blocks.BARREL.defaultBlockState()),recipes()));
        var locked=new BrewingStandBlockEntity(BlockPos.ZERO,Blocks.BREWING_STAND.defaultBlockState()){
            @Override public boolean isLocked(){return true;}
        };
        assertFalse(MercenaryBrewing.brew(entity,locked,recipes()));
        assertTrue(entity.supplies().get(0).get(DataComponents.POTION_CONTENTS).is(Potions.WATER));
        assertEquals(2,entity.supplies().get(1).getCount());
    }
    @Test void portableProductionNoLongerBrewsAndExistingPotionCooldownsRemain(){
        var entity=merc("theurgist");ingredients(entity);
        entity.timers(new MercenaryTimers(0,3600,Map.of("minecraft:healing",123)));
        MercenaryPotions.produce(entity,null);
        assertTrue(entity.supplies().get(0).get(DataComponents.POTION_CONTENTS).is(Potions.WATER));
        assertEquals(2,entity.supplies().get(1).getCount());assertFalse(entity.timers().ready("minecraft:healing"));
    }
    @Test void onlyTheurgistRetainsTimedFallbackHealing(){
        var entity=merc("theurgist");entity.timers(new MercenaryTimers(0,0,Map.of()));
        MercenaryPotions.produce(entity,null);
        assertTrue(entity.supplies().getFirst().is(Items.SPLASH_POTION));
        assertTrue(entity.supplies().getFirst().get(DataComponents.POTION_CONTENTS).is(Potions.HEALING));
        assertEquals(MercenaryConfig.FALLBACK_TICKS.get(),entity.timers().fallback());
    }
    @Test void failedRecipeLeavesSuppliesAndStandIntact(){
        var entity=merc("theurgist");entity.supplies().set(0,new ItemStack(Items.DIRT,64));
        var before=MercenaryInventory.copy(entity.supplies());var stand=stand();
        assertFalse(MercenaryBrewing.brew(entity,stand,recipes()));
        for(int i=0;i<before.size();i++)assertTrue(ItemStack.matches(before.get(i),entity.supplies().get(i)));
        assertTrue(stand.isEmpty());
    }
}
