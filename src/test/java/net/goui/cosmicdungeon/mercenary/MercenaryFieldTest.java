package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.block.entity.*;
import net.goui.cosmicdungeon.item.identity.ClassItemOwnership;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryFieldTest {
    private static final RegistryAccess LOOKUP=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static List<ItemStack> bag(ItemStack... stacks){return new ArrayList<>(List.of(stacks));}
    private static PotionBrewing recipes(){
        var builder=new PotionBrewing.Builder(FeatureFlags.DEFAULT_FLAGS);
        PotionBrewing.addVanillaMixes(builder);return builder.build();
    }
    private static ItemStack potion(net.minecraft.core.Holder<Potion> potion){
        return PotionContents.createItemStack(Items.POTION,potion);
    }
    @Test void insertionMergesOnlyIdenticalComponentsAndNeverChangesSource(){
        var first=new ItemStack(Items.APPLE,60);first.set(DataComponents.CUSTOM_NAME,Component.literal("Authored"));
        var other=new ItemStack(Items.APPLE,10);other.set(DataComponents.CUSTOM_NAME,Component.literal("Other"));
        var incoming=first.copyWithCount(8);var items=bag(first,other,ItemStack.EMPTY);
        assertTrue(MercenaryInventory.insert(items,incoming));
        assertEquals(64,items.get(0).getCount());assertEquals(10,items.get(1).getCount());
        assertEquals(4,items.get(2).getCount());assertEquals(8,incoming.getCount());assertEquals(60,first.getCount());
        assertTrue(ItemStack.isSameItemSameComponents(incoming,items.get(2)));
    }
    @Test void rejectedInsertionIsEntirelyAtomic(){
        var items=bag(new ItemStack(Items.APPLE,63),new ItemStack(Items.STONE,64));
        assertFalse(MercenaryInventory.insert(items,new ItemStack(Items.APPLE,2)));
        assertEquals(63,items.get(0).getCount());assertEquals(64,items.get(1).getCount());
    }
    @Test void wholeStackInsertionConservesCountsAcrossManyCapacityBoundaries(){
        for(int amount=1;amount<=128;amount++){
            var items=bag(new ItemStack(Items.APPLE,60),ItemStack.EMPTY);
            boolean success=MercenaryInventory.insert(items,new ItemStack(Items.APPLE,amount));
            assertEquals(amount<=68,success);
            assertEquals(success?60+amount:60,items.stream().mapToInt(ItemStack::getCount).sum());
            assertTrue(items.stream().allMatch(s->s.isEmpty()||s.getCount()<=s.getMaxStackSize()));
        }
    }
    @Test void foreignAndMalformedBindingNeverTransfers(){
        var contract=new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),"theurgist",2,500);
        var stack=new ItemStack(Items.BOW);var tag=new CompoundTag();tag.putString(ClassItemOwnership.KEY,UUID.randomUUID().toString());
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));assertFalse(MercenaryInventory.permitted(stack,contract));
        tag.putString(ClassItemOwnership.KEY,"malformed");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        assertFalse(MercenaryInventory.permitted(stack,contract));
        tag.putString(ClassItemOwnership.KEY,contract.id().toString());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        assertTrue(MercenaryInventory.permitted(stack,contract));assertFalse(MercenaryInventory.permitted(stack,null));
    }
    @Test void nativeRecipeChainConsumesExactlyOneReagentAndPreservesSpare(){
        var named=new ItemStack(Items.APPLE,4);named.set(DataComponents.CUSTOM_NAME,Component.literal("Do not rewrite"));
        var items=bag(potion(Potions.WATER),new ItemStack(Items.NETHER_WART,2),new ItemStack(Items.GLISTERING_MELON_SLICE),named,ItemStack.EMPTY);
        var first=MercenaryInventory.brew(items,recipes());assertNotNull(first);
        assertTrue(first.get(0).get(DataComponents.POTION_CONTENTS).is(Potions.AWKWARD));
        assertEquals(1,first.get(1).getCount());assertEquals(2,items.get(1).getCount());
        var second=MercenaryInventory.brew(first,recipes());assertNotNull(second);
        assertTrue(second.get(0).get(DataComponents.POTION_CONTENTS).is(Potions.HEALING));
        assertTrue(second.get(2).isEmpty());assertTrue(ItemStack.matches(named,second.get(3)));
    }
    @Test void splashAndLingeringConversionUseNativeRecipes(){
        var items=bag(potion(Potions.HEALING),new ItemStack(Items.GUNPOWDER),new ItemStack(Items.DRAGON_BREATH));
        var splash=MercenaryInventory.brew(items,recipes());assertNotNull(splash);assertTrue(splash.get(0).is(Items.SPLASH_POTION));
        var lingering=MercenaryInventory.brew(splash,recipes());assertNotNull(lingering);assertTrue(lingering.get(0).is(Items.LINGERING_POTION));
        assertEquals(1,lingering.stream().filter(s->s.is(Items.GLASS_BOTTLE)).mapToInt(ItemStack::getCount).sum());
    }
    @Test void invalidRecipeLeavesEveryStackIntact(){
        var items=bag(potion(Potions.WATER),new ItemStack(Items.DIRT));var before=MercenaryInventory.copy(items);
        assertNull(MercenaryInventory.brew(items,recipes()));
        for(int i=0;i<items.size();i++)assertTrue(ItemStack.matches(before.get(i),items.get(i)));
    }
    private static PotionBrewing remainderRecipe(){
        var builder=new PotionBrewing.Builder(FeatureFlags.DEFAULT_FLAGS);
        builder.addRecipe(new net.neoforged.neoforge.common.brewing.IBrewingRecipe(){
            public boolean isInput(ItemStack item){return item.is(Items.APPLE);}
            public boolean isIngredient(ItemStack item){return item.is(Items.MILK_BUCKET);}
            public ItemStack getOutput(ItemStack input,ItemStack ingredient){
                return isInput(input)&&isIngredient(ingredient)?new ItemStack(Items.DIAMOND):ItemStack.EMPTY;
            }
        });return builder.build();
    }
    @Test void registeredRecipeRemainderCannotBeLostWhenBagIsFull(){
        var items=bag(new ItemStack(Items.APPLE,2),new ItemStack(Items.MILK_BUCKET));
        assertNull(MercenaryInventory.brew(items,remainderRecipe()));assertEquals(2,items.get(0).getCount());
        assertTrue(items.get(1).is(Items.MILK_BUCKET));
        items.add(ItemStack.EMPTY);var result=MercenaryInventory.brew(items,remainderRecipe());assertNotNull(result);
        assertEquals(1,result.stream().filter(s->s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum());
        assertEquals(1,result.stream().filter(s->s.is(Items.BUCKET)).mapToInt(ItemStack::getCount).sum());
        assertEquals(1,result.stream().filter(s->s.is(Items.APPLE)).mapToInt(ItemStack::getCount).sum());
    }
    @Test void individualPotionTimersRoundTripWithoutOfflineProduction(){
        var timers=new MercenaryTimers(400,3600,Map.of()).used("minecraft:healing",200).used("minecraft:poison",400).advance(70);
        var saved=MercenaryTimers.CODEC.encodeStart(NbtOps.INSTANCE,timers).getOrThrow();
        var loaded=MercenaryTimers.CODEC.parse(NbtOps.INSTANCE,saved).getOrThrow();
        assertEquals(timers,loaded);assertEquals(3530,loaded.fallback());assertFalse(loaded.ready("minecraft:healing"));
        loaded=loaded.advance(130);assertTrue(loaded.ready("minecraft:healing"));assertFalse(loaded.ready("minecraft:poison"));
        assertEquals(0,loaded.advance(10000).fallback());assertTrue(loaded.advance(10000).potions().isEmpty());
    }
    @Test void oldEmptyTimerShapeUsesDelayedDefaults(){
        var timers=MercenaryTimers.CODEC.parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();
        assertEquals(new MercenaryTimers(400,3600,Map.of()),timers);
        assertThrows(IllegalArgumentException.class,()->new MercenaryTimers(-1,0,Map.of()));
        assertTrue(MercenaryTimers.CODEC.parse(NbtOps.INSTANCE,IntTag.valueOf(4)).error().isPresent());
    }
    @Test void samePotionSharesCooldownAcrossNativeContainers(){
        String key=MercenaryPotions.key(potion(Potions.HEALING));
        assertEquals(key,MercenaryPotions.key(PotionContents.createItemStack(Items.SPLASH_POTION,Potions.HEALING)));
        assertEquals(key,MercenaryPotions.key(PotionContents.createItemStack(Items.LINGERING_POTION,Potions.HEALING)));
        assertNotEquals(key,MercenaryPotions.key(potion(Potions.STRONG_HEALING)));
    }
    @Test void helpfulAndHarmfulRecipientDecisionsAreDisjoint(){
        assertTrue(MercenaryPotions.permits(true,true,false));assertFalse(MercenaryPotions.permits(true,false,true));
        assertFalse(MercenaryPotions.permits(false,true,false));assertTrue(MercenaryPotions.permits(false,false,true));
        assertFalse(MercenaryPotions.permits(false,false,false));assertFalse(MercenaryPotions.permits(true,false,false));
    }
    @Test void undeadInversionAndNativeEffectCategoriesAreRespected(){
        assertTrue(MercenaryPotions.helpful(MobEffects.INSTANT_HEALTH.value(),false));
        assertFalse(MercenaryPotions.helpful(MobEffects.INSTANT_HEALTH.value(),true));
        assertFalse(MercenaryPotions.helpful(MobEffects.INSTANT_DAMAGE.value(),false));
        assertTrue(MercenaryPotions.helpful(MobEffects.INSTANT_DAMAGE.value(),true));
        assertFalse(MercenaryPotions.helpful(MobEffects.POISON.value(),false));
        assertTrue(MercenaryPotions.helpful(MobEffects.FIRE_RESISTANCE.value(),false));
    }
    @Test void attackAmmunitionCannotGiveAnEnemyBeneficialPotionEffects(){
        var healing=PotionContents.createItemStack(Items.TIPPED_ARROW,Potions.HEALING);
        assertFalse(MercenaryBrain.attackArrow(healing,false));assertTrue(MercenaryBrain.attackArrow(healing,true));
        var harming=PotionContents.createItemStack(Items.TIPPED_ARROW,Potions.HARMING);
        assertTrue(MercenaryBrain.attackArrow(harming,false));assertFalse(MercenaryBrain.attackArrow(harming,true));
        var mixed=new PotionContents(Potions.POISON).withEffectAdded(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,400));
        harming.set(DataComponents.POTION_CONTENTS,mixed);assertFalse(MercenaryBrain.attackArrow(harming,false));
        assertTrue(MercenaryBrain.attackArrow(new ItemStack(Items.ARROW),false));
    }
    @Test void recoveryNeedsActualSeparationOrSustainedStall(){
        assertFalse(MercenaryBrain.recover(9,1000));assertFalse(MercenaryBrain.recover(100,90));
        assertTrue(MercenaryBrain.recover(100,100));assertTrue(MercenaryBrain.recover(257,0));
    }
    @Test void potionOverrideValidationIsBounded(){
        for(String good:List.of("minecraft:healing=200","custom=20","example:potion=1728000"))assertTrue(MercenaryConfig.validOverride(good));
        for(Object bad:List.of("minecraft:healing=0","minecraft:healing=-1","minecraft:healing=1728001",
                "minecraft:healing=bad","Bad ID=20","=20",42))assertFalse(MercenaryConfig.validOverride(bad));
    }
    @Test void slotPermissionRequiresBothOwnerAndClassWithoutRewritingItems(){
        var id=UUID.randomUUID();var hire=new MercenaryContract(id,UUID.randomUUID(),"pyroclast",2,500);
        var chest=new ClassLockedChestBlockEntity(BlockPos.ZERO,ModBlocks.PYROCLAST_CHEST.get().defaultBlockState());
        var authored=new ItemStack(Items.FIREWORK_ROCKET,7);authored.set(DataComponents.CUSTOM_NAME,Component.literal("Authored"));
        chest.setItem(0,authored.copy());
        assertTrue(MercenaryCollection.mayOpen(chest,hire));
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,LOOKUP);
        new ClassChestOwnership(id,"Mercenary").save(output);
        chest.handleUpdateTag(TagValueInput.create(ProblemReporter.DISCARDING,LOOKUP,output.buildResult()));
        assertTrue(MercenaryCollection.mayOpen(chest,hire));
        assertFalse(chest.permitsMercenary(UUID.randomUUID(),"pyroclast"));assertFalse(chest.permitsMercenary(id,"theurgist"));
        assertTrue(ItemStack.matches(authored,chest.getItem(0)));
    }
    @Test void onlyExplicitContainerKindsCanBeLooted(){
        var hire=new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),"pyroclast",2,500);
        assertTrue(MercenaryCollection.mayOpen(new ChestBlockEntity(BlockPos.ZERO,Blocks.CHEST.defaultBlockState()),hire));
        assertTrue(MercenaryCollection.mayOpen(new BarrelBlockEntity(BlockPos.ZERO,Blocks.BARREL.defaultBlockState()),hire));
        assertTrue(MercenaryCollection.mayOpen(new BrewingStandBlockEntity(BlockPos.ZERO,Blocks.BREWING_STAND.defaultBlockState()),hire));
        assertFalse(MercenaryCollection.mayOpen(new FurnaceBlockEntity(BlockPos.ZERO,Blocks.FURNACE.defaultBlockState()),hire));
    }
}
