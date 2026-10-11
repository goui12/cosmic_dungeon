package net.goui.cosmicdungeon.crafting;

import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PotionRecipeSelectionTest {
    private static final String HEALING="minecraft:healing_splash",STRENGTH="minecraft:strength_splash";
    private static final PotionRecipeCatalog CATALOG=PotionRecipeCatalog.vanilla(FeatureFlags.VANILLA_SET);
    private static final PotionRecipeSelection SELECT=new PotionRecipeSelection(CATALOG);
    private static ItemStack potion(Item container,Holder<Potion> potion) {return PotionContents.createItemStack(container,potion);}
    private static Map<String,Integer> stock(ItemStack... stacks){return PotionRecipeSelection.stock(List.of(stacks));}
    private static String item(Item item){return PotionRecipeSelection.itemKey(item);}
    private static String bottle(Item container,Holder<Potion> potion) {
        return PotionRecipeSelection.stock(List.of(potion(container,potion))).keySet().iterator().next();
    }
    private static Map<String,Integer> waterRecipe(int count,int powders) {
        var stock=new HashMap<String,Integer>();stock.put(bottle(Items.POTION,Potions.WATER),count);
        stock.put(item(Items.NETHER_WART),count);stock.put(item(Items.GLISTERING_MELON_SLICE),count);
        stock.put(item(Items.GUNPOWDER),count);if(powders>0)stock.put(item(Items.BLAZE_POWDER),powders);return stock;
    }
    @Test void mixedOwnedIntermediatesAreConsumedTogetherWithoutDoubleCounting() {
        var stock=waterRecipe(2,1);
        stock.put(bottle(Items.POTION,Potions.AWKWARD),3);stock.put(bottle(Items.POTION,Potions.HEALING),4);
        stock.put(bottle(Items.SPLASH_POTION,Potions.AWKWARD),1);
        stock.put(item(Items.GLISTERING_MELON_SLICE),6);stock.put(item(Items.GUNPOWDER),9);
        var before=Map.copyOf(stock);assertEquals(10,SELECT.capacity(HEALING,stock,0));
        assertEquals(before,stock,"Preview must not mutate a source stock map");
        var remaining=new HashMap<>(stock);int charges=0,steps=0,powders=0,crafted=0;
        while(true) {
            var choice=SELECT.choose(HEALING,remaining,charges);if(choice==null)break;
            steps+=choice.fuel().steps();powders+=choice.fuel().powders();charges=choice.fuel().remaining();crafted++;
            choice.requirements().forEach((key,count)->remaining.compute(key,(ignored,old)->old-count));
        }
        assertEquals(10,crafted);assertEquals(17,steps);assertEquals(1,powders);assertEquals(3,charges);
        assertEquals(0,remaining.get(item(Items.GLISTERING_MELON_SLICE)));
        assertEquals(0,remaining.get(item(Items.GUNPOWDER)));
    }
    @Test void overlappingRoutesDoNotCountTheSameSharedIngredientsTwice() {
        var stock=waterRecipe(2,1);stock.put(bottle(Items.POTION,Potions.AWKWARD),2);
        stock.put(item(Items.NETHER_WART),1);stock.put(item(Items.GLISTERING_MELON_SLICE),4);stock.put(item(Items.GUNPOWDER),4);
        assertEquals(3,SELECT.capacity(HEALING,stock,0),"Two Awkward and only one complete water route fit");
        stock.put(item(Items.GLISTERING_MELON_SLICE),1);
        assertEquals(1,SELECT.capacity(HEALING,stock,0),"Common melon stock cannot be reused for another route");
    }
    @Test void twentyFuelChargesArePreservedAcrossTheExactRefillBoundary() {
        var seven=waterRecipe(7,1);
        assertEquals(6,SELECT.capacity(HEALING,seven,0),"Six three-step recipes leave two charges, insufficient for the seventh");
        assertEquals(7,SELECT.capacity(HEALING,seven,1),"One carried charge plus one powder supplies seven recipes");
        assertEquals(6,SELECT.capacity(HEALING,waterRecipe(7,0),19),"Stored charges are usable with no physical fuel powder");
        var choice=SELECT.choose(HEALING,waterRecipe(1,1),2);assertNotNull(choice);
        assertEquals(1,choice.fuel().powders());assertEquals(19,choice.fuel().remaining());
        var exact=SELECT.choose(HEALING,waterRecipe(1,0),3);assertNotNull(exact);
        assertEquals(0,exact.fuel().powders());assertEquals(0,exact.fuel().remaining());
    }
    @Test void strengthCountsItsReagentPowderSeparatelyFromRefillPowder() {
        var stock=stock(potion(Items.POTION,Potions.WATER),new ItemStack(Items.NETHER_WART),new ItemStack(Items.GUNPOWDER),new ItemStack(Items.BLAZE_POWDER));
        assertNull(SELECT.choose(STRENGTH,stock,0));assertEquals(0,SELECT.capacity(STRENGTH,stock,0));
        var doubled=new HashMap<>(stock);doubled.put(item(Items.BLAZE_POWDER),2);
        var refill=SELECT.choose(STRENGTH,doubled,0);assertNotNull(refill);
        assertEquals(2,refill.requirements().get(item(Items.BLAZE_POWDER)));assertEquals(1,refill.fuel().powders());assertEquals(17,refill.fuel().remaining());
        var carried=SELECT.choose(STRENGTH,stock,3);assertNotNull(carried);
        assertEquals(1,carried.requirements().get(item(Items.BLAZE_POWDER)));assertEquals(0,carried.fuel().powders());assertEquals(0,carried.fuel().remaining());
    }
    @Test void missingFilledBottleNeverSubstitutesEmptyGlass() {
        var materials=stock(new ItemStack(Items.GLASS_BOTTLE,8),new ItemStack(Items.NETHER_WART,8),
                new ItemStack(Items.GLISTERING_MELON_SLICE,8),new ItemStack(Items.GUNPOWDER,8),new ItemStack(Items.BLAZE_POWDER));
        assertEquals(0,SELECT.capacity(HEALING,materials,0));
        assertNull(SELECT.choose(HEALING,materials,19));
        assertFalse(materials.containsKey(bottle(Items.POTION,Potions.WATER)));
    }
    @Test void modifiedPotionContentsAreNotOrdinaryRecipeBases() {
        var effects=potion(Items.POTION,Potions.WATER);
        effects.set(DataComponents.POTION_CONTENTS,effects.get(DataComponents.POTION_CONTENTS).withEffectAdded(new MobEffectInstance(MobEffects.POISON,200)));
        var colored=potion(Items.POTION,Potions.WATER);
        colored.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.of(Potions.WATER),Optional.of(0x00FF00),List.of(),Optional.empty()));
        var named=potion(Items.POTION,Potions.WATER);
        named.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.of(Potions.WATER),Optional.empty(),List.of(),Optional.of("custom_potion")));
        for(var modified:List.of(effects,colored,named)) {
            var available=stock(modified,new ItemStack(Items.NETHER_WART),new ItemStack(Items.GLISTERING_MELON_SLICE),
                    new ItemStack(Items.GUNPOWDER),new ItemStack(Items.BLAZE_POWDER));
            assertEquals(0,SELECT.capacity(HEALING,available,0));
            assertFalse(available.containsKey(bottle(Items.POTION,Potions.WATER)));
        }
    }
    @Test void nativeInputPredicateDistinguishesContentsAndContainer() {
        var route=CATALOG.routes(CATALOG.target(HEALING)).stream().filter(r->r.inputPotion().is(Potions.WATER)&&r.inputContainer()==Items.POTION).findFirst().orElseThrow();
        assertTrue(PotionRecipeSelection.matchesInput(route,potion(Items.POTION,Potions.WATER)));
        assertFalse(PotionRecipeSelection.matchesInput(route,potion(Items.POTION,Potions.AWKWARD)));
        assertFalse(PotionRecipeSelection.matchesInput(route,potion(Items.SPLASH_POTION,Potions.WATER)));
        assertFalse(PotionRecipeSelection.matchesInput(route,new ItemStack(Items.GLASS_BOTTLE)));
        var modified=potion(Items.POTION,Potions.WATER);modified.set(DataComponents.POTION_CONTENTS,
                modified.get(DataComponents.POTION_CONTENTS).withEffectAdded(new MobEffectInstance(MobEffects.REGENERATION,100)));
        assertFalse(PotionRecipeSelection.matchesInput(route,modified));
    }
    @Test void existingFinalPotionDoesNotCreateAFreeCopyOrRefillFuel() {
        var existing=stock(potion(Items.SPLASH_POTION,Potions.HEALING));
        assertNull(SELECT.choose(HEALING,existing,0));assertNull(SELECT.choose(HEALING,existing,19));assertEquals(0,SELECT.capacity(HEALING,existing,19));
        var base=stock(potion(Items.POTION,Potions.HEALING),new ItemStack(Items.GUNPOWDER));
        var converted=SELECT.choose(HEALING,base,1);assertNotNull(converted);assertEquals(1,converted.fuel().steps());assertEquals(0,converted.fuel().remaining());
    }
    @Test void splitInventoryCombiningSuppliesOnlyCompleteTransactions() {
        var owner=stock(potion(Items.POTION,Potions.WATER),new ItemStack(Items.NETHER_WART));
        var first=stock(new ItemStack(Items.GLISTERING_MELON_SLICE),new ItemStack(Items.GUNPOWDER));
        var second=stock(new ItemStack(Items.BLAZE_POWDER));
        assertEquals(0,SELECT.capacity(HEALING,owner,0));
        assertEquals(0,SELECT.capacity(HEALING,PotionRecipeSelection.combine(List.of(owner,first)),0));
        var pooled=PotionRecipeSelection.combine(List.of(owner,first,second));
        assertEquals(1,SELECT.capacity(HEALING,pooled,0));
        assertEquals(1,owner.get(bottle(Items.POTION,Potions.WATER)),"Combining cannot debit any owner's source map");
    }
    @Test void deterministicChoicePrefersTheShortestAffordableIntermediateRoute() {
        var stock=waterRecipe(1,1);stock.put(bottle(Items.POTION,Potions.HEALING),1);stock.put(item(Items.GUNPOWDER),2);
        var choice=SELECT.choose(HEALING,stock,0);assertNotNull(choice);
        assertEquals(1,choice.fuel().steps());assertTrue(choice.option().route().inputPotion().is(Potions.HEALING));
        assertEquals(choice,SELECT.choose(HEALING,stock,0));assertEquals(2,SELECT.capacity(HEALING,stock,0));
        assertThrows(UnsupportedOperationException.class,()->choice.requirements().clear());
        assertThrows(UnsupportedOperationException.class,()->choice.option().materials().clear());
    }
    @Test void previewCountCapsAtSixtyFourAndUnknownRecipesRemainUnavailable() {
        assertEquals(64,SELECT.capacity(HEALING,waterRecipe(128,32),0));
        assertEquals(63,SELECT.capacity(HEALING,waterRecipe(63,32),0));
        assertNull(SELECT.choose("minecraft:luck_splash",waterRecipe(1,1),0));
        assertEquals(0,SELECT.capacity("minecraft:turtle_master_splash",waterRecipe(1,1),0));
    }
}
