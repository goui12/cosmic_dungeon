package net.goui.cosmicdungeon.crafting;

import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.flag.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PotionRecipeCatalogTest {
    private static PotionRecipeCatalog catalog(){return PotionRecipeCatalog.vanilla(FeatureFlags.VANILLA_SET);}
    private static PotionRecipeCatalog.Target target(PotionRecipeCatalog catalog,String id) {
        var target=catalog.target("minecraft:"+id);assertNotNull(target,id);return target;
    }
    private static List<PotionRecipeCatalog.Route> fromWater(PotionRecipeCatalog catalog,String id) {
        return catalog.routes(target(catalog,id)).stream().filter(r->r.inputContainer()==Items.POTION&&r.inputPotion().is(Potions.WATER)).toList();
    }
    private static boolean route(PotionRecipeCatalog catalog,String id,Holder<Potion> input,Item container,Item... ingredients) {
        return catalog.routes(target(catalog,id)).stream().anyMatch(r->r.inputPotion().is(input)
                &&r.inputContainer()==container&&r.ingredients().equals(List.of(ingredients)));
    }
    @Test void exactVanillaTargetFamiliesAndClasses() {
        var catalog=catalog();var actual=new TreeSet<String>();
        for(var target:catalog.targets())assertTrue(actual.add(target.id()),"Unique stable target ID");
        var expected=new TreeSet<String>();
        String[] positive={"night_vision","long_night_vision","invisibility","long_invisibility","leaping","long_leaping","strong_leaping",
                "fire_resistance","long_fire_resistance","swiftness","long_swiftness","strong_swiftness","water_breathing","long_water_breathing",
                "healing","strong_healing","regeneration","long_regeneration","strong_regeneration","strength","long_strength","strong_strength",
                "slow_falling","long_slow_falling"};
        String[] harmful={"slowness","long_slowness","strong_slowness","harming","strong_harming","poison","long_poison","strong_poison",
                "weakness","long_weakness","wind_charged","weaving","oozing","infested"};
        for(var id:positive){expected.add("minecraft:"+id+"_splash");assertEquals(Set.of("theurgist"),target(catalog,id+"_splash").classes());}
        for(var id:harmful){expected.add("minecraft:"+id+"_splash");assertEquals(Set.of("venefex"),target(catalog,id+"_splash").classes());}
        for(var id:List.of("turtle_master","long_turtle_master","strong_turtle_master")) {
            expected.add("minecraft:"+id+"_drinkable");assertEquals(Set.of("theurgist","venefex"),target(catalog,id+"_drinkable").classes());
        }
        assertEquals(expected,actual);assertEquals(41,actual.size());
    }
    @Test void everyRecordedRouteReplaysThroughTheNativeVanillaBrewer() {
        var builder=new PotionBrewing.Builder(FeatureFlags.VANILLA_SET);PotionBrewing.addVanillaMixes(builder);var nativeBrewer=builder.build();
        var catalog=catalog();int checked=0;
        for(var target:catalog.targets()) {
            assertFalse(fromWater(catalog,target.id().substring("minecraft:".length())).isEmpty(),"Every target can start from ordinary water");
            for(var route:catalog.routes(target)) {
                assertTrue(route.steps()>0&&route.steps()<=16);assertNotEquals(Items.LINGERING_POTION,route.inputContainer());
                var stack=PotionContents.createItemStack(route.inputContainer(),route.inputPotion());
                for(var ingredient:route.ingredients()) {
                    var reagent=new ItemStack(ingredient);
                    assertTrue(nativeBrewer.hasMix(stack,reagent),target.id()+" has a real next step");
                    stack=nativeBrewer.mix(reagent,stack);
                }
                assertTrue(stack.is(target.container()));
                assertTrue(stack.get(DataComponents.POTION_CONTENTS).potion().orElseThrow().is(target.potion()));
                assertEquals(1,stack.getCount());checked++;
            }
        }
        assertTrue(checked>100,"Verify intermediate and alternate routes, not only one recipe per effect");
    }
    @Test void standardHealingChargesEveryActualStepAndSupportsIntermediateBottles() {
        var catalog=catalog();
        assertTrue(route(catalog,"healing_splash",Potions.WATER,Items.POTION,Items.NETHER_WART,Items.GLISTERING_MELON_SLICE,Items.GUNPOWDER));
        assertTrue(route(catalog,"healing_splash",Potions.AWKWARD,Items.POTION,Items.GLISTERING_MELON_SLICE,Items.GUNPOWDER));
        assertTrue(route(catalog,"healing_splash",Potions.HEALING,Items.POTION,Items.GUNPOWDER));
        assertTrue(route(catalog,"healing_splash",Potions.AWKWARD,Items.SPLASH_POTION,Items.GLISTERING_MELON_SLICE));
        assertTrue(fromWater(catalog,"healing_splash").stream().allMatch(r->r.steps()==3));
    }
    @Test void gunpowderCanBeAppliedBeforeBaseOrEffectBrewing() {
        var catalog=catalog();
        assertTrue(route(catalog,"healing_splash",Potions.WATER,Items.POTION,Items.GUNPOWDER,Items.NETHER_WART,Items.GLISTERING_MELON_SLICE));
        assertTrue(route(catalog,"healing_splash",Potions.WATER,Items.POTION,Items.NETHER_WART,Items.GUNPOWDER,Items.GLISTERING_MELON_SLICE));
        assertTrue(route(catalog,"healing_splash",Potions.WATER,Items.SPLASH_POTION,Items.NETHER_WART,Items.GLISTERING_MELON_SLICE));
    }
    @Test void weaknessOmitsWartAndAwkwardConversion() {
        var catalog=catalog();
        assertTrue(route(catalog,"weakness_splash",Potions.WATER,Items.POTION,Items.FERMENTED_SPIDER_EYE,Items.GUNPOWDER));
        assertTrue(fromWater(catalog,"weakness_splash").stream().allMatch(r->r.steps()==2&&!r.ingredients().contains(Items.NETHER_WART)));
        assertFalse(catalog.routes(target(catalog,"weakness_splash")).stream().anyMatch(r->r.inputPotion().is(Potions.AWKWARD)));
    }
    @Test void harmingRetainsBothHealingAndPoisonCorruptionRoutes() {
        var catalog=catalog();
        assertTrue(route(catalog,"harming_splash",Potions.HEALING,Items.POTION,Items.FERMENTED_SPIDER_EYE,Items.GUNPOWDER));
        assertTrue(route(catalog,"harming_splash",Potions.POISON,Items.POTION,Items.FERMENTED_SPIDER_EYE,Items.GUNPOWDER));
        assertTrue(route(catalog,"harming_splash",Potions.LONG_POISON,Items.SPLASH_POTION,Items.FERMENTED_SPIDER_EYE));
        assertTrue(route(catalog,"strong_harming_splash",Potions.STRONG_HEALING,Items.SPLASH_POTION,Items.FERMENTED_SPIDER_EYE));
        assertTrue(route(catalog,"strong_harming_splash",Potions.STRONG_POISON,Items.SPLASH_POTION,Items.FERMENTED_SPIDER_EYE));
    }
    @Test void slownessAndExtendedInvisibilityRetainAlternatePaths() {
        var catalog=catalog();
        for(var input:List.of(Potions.SWIFTNESS,Potions.LEAPING))
            assertTrue(route(catalog,"slowness_splash",input,Items.SPLASH_POTION,Items.FERMENTED_SPIDER_EYE));
        for(var input:List.of(Potions.LONG_SWIFTNESS,Potions.LONG_LEAPING))
            assertTrue(route(catalog,"long_slowness_splash",input,Items.SPLASH_POTION,Items.FERMENTED_SPIDER_EYE));
        assertTrue(route(catalog,"long_invisibility_splash",Potions.NIGHT_VISION,Items.SPLASH_POTION,Items.REDSTONE,Items.FERMENTED_SPIDER_EYE));
        assertTrue(route(catalog,"long_invisibility_splash",Potions.NIGHT_VISION,Items.SPLASH_POTION,Items.FERMENTED_SPIDER_EYE,Items.REDSTONE));
    }
    @Test void strengthPowderIsARealReagentIndependentOfFuel() {
        var catalog=catalog();var routes=fromWater(catalog,"strength_splash");
        assertFalse(routes.isEmpty());assertTrue(routes.stream().allMatch(r->r.steps()==3&&Collections.frequency(r.ingredients(),Items.BLAZE_POWDER)==1));
        assertTrue(route(catalog,"strong_strength_splash",Potions.STRENGTH,Items.SPLASH_POTION,Items.GLOWSTONE_DUST));
    }
    @Test void allMixedVariantsRemainDrinkableWithoutSplashInputsOrGunpowder() {
        var catalog=catalog();
        for(var id:List.of("turtle_master","long_turtle_master","strong_turtle_master")) {
            var target=target(catalog,id+"_drinkable");assertEquals(Items.POTION,target.container());assertNull(catalog.target("minecraft:"+id+"_splash"));
            for(var route:catalog.routes(target)) {
                assertEquals(Items.POTION,route.inputContainer());assertFalse(route.ingredients().contains(Items.GUNPOWDER));
                assertFalse(route.ingredients().contains(Items.DRAGON_BREATH));
            }
        }
    }
    @Test void noUnbrewableLuckEmptyBasesLingeringOrZeroStepDuplicate() {
        var catalog=catalog();
        for(var id:List.of("luck","water","awkward","mundane","thick"))assertNull(catalog.target("minecraft:"+id+"_splash"));
        for(var target:catalog.targets()) {
            assertNotEquals(Items.LINGERING_POTION,target.container());
            assertFalse(catalog.routes(target).stream().anyMatch(r->r.inputContainer()==target.container()&&r.inputPotion().is(target.potion())));
            assertTrue(catalog.routes(target).stream().allMatch(r->!r.ingredients().isEmpty()));
        }
        assertThrows(IllegalArgumentException.class,()->new PotionRecipeCatalog.Route(target(catalog,"healing_splash"),Potions.HEALING,Items.SPLASH_POTION,List.of()));
    }
    @Test void deterministicImmutableAndForeignTargetSafe() {
        var first=catalog();var second=catalog();assertEquals(first.targets(),second.targets());
        assertThrows(UnsupportedOperationException.class,()->first.targets().clear());
        for(var target:first.targets()) {
            assertEquals(first.routes(target),second.routes(target));int previous=0;
            for(var route:first.routes(target)){assertTrue(route.steps()>=previous);previous=route.steps();}
            assertThrows(UnsupportedOperationException.class,()->target.classes().clear());
            assertThrows(UnsupportedOperationException.class,()->first.routes(target).clear());
            assertThrows(UnsupportedOperationException.class,()->first.routes(target).getFirst().ingredients().clear());
        }
        var target=target(first,"healing_splash");
        assertTrue(first.routes(new PotionRecipeCatalog.Target(target.id(),Potions.HARMING,Items.SPLASH_POTION,Set.of("venefex"))).isEmpty());
        assertTrue(first.routes(null).isEmpty());assertNull(first.target(null));assertNull(first.target("minecraft:missing"));
    }
    @Test void disabledVanillaFeatureProducesNoUsableRoutes() {
        assertTrue(PotionRecipeCatalog.vanilla(FeatureFlagSet.of()).targets().isEmpty());
        assertThrows(NullPointerException.class,()->PotionRecipeCatalog.vanilla(null));
    }
}
