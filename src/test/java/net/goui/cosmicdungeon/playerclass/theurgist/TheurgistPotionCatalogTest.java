package net.goui.cosmicdungeon.playerclass.theurgist;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TheurgistPotionCatalogTest {
    @Test void exactNativePoolsCostsAndTiers(){
        assertEquals(20,TheurgistPotionCatalog.cost(false));assertEquals(40,TheurgistPotionCatalog.cost(true));
        assertEquals(List.of(Potions.NIGHT_VISION,Potions.INVISIBILITY,Potions.FIRE_RESISTANCE,Potions.SWIFTNESS,
                Potions.HEALING,Potions.REGENERATION,Potions.STRENGTH,Potions.LUCK),TheurgistPotionCatalog.pool(false));
        assertEquals(List.of(Potions.STRONG_SWIFTNESS,Potions.STRONG_HEALING,Potions.STRONG_REGENERATION,
                Potions.STRONG_STRENGTH),TheurgistPotionCatalog.pool(true));
        for(boolean epic:List.of(false,true))for(var potion:TheurgistPotionCatalog.pool(epic)){
            assertFalse(potion.value().getEffects().isEmpty());
            for(var effect:potion.value().getEffects()){
                assertEquals(epic?1:0,effect.getAmplifier());
                assertEquals(MobEffectCategory.BENEFICIAL,effect.getEffect().value().getCategory());
            }
        }
        assertThrows(UnsupportedOperationException.class,()->TheurgistPotionCatalog.pool(false).clear());
    }
    @Test void everyRandomSlotProducesOneIndependentNativePhysicalSplash(){
        for(boolean epic:List.of(false,true))for(int index=0;index<TheurgistPotionCatalog.pool(epic).size();index++){
            final int chosen=index;
            var random=new LegacyRandomSource(0){@Override public int nextInt(int bound){
                assertEquals(TheurgistPotionCatalog.pool(epic).size(),bound);return chosen;
            }};
            var a=TheurgistPotionCatalog.create(epic,random);var b=TheurgistPotionCatalog.create(epic,random);
            assertTrue(a.is(Items.SPLASH_POTION));assertEquals(1,a.getCount());
            var contents=a.get(DataComponents.POTION_CONTENTS);assertNotNull(contents);
            assertEquals(TheurgistPotionCatalog.pool(epic).get(index),contents.potion().orElseThrow());
            assertTrue(contents.customEffects().isEmpty());assertFalse(a.has(DataComponents.CUSTOM_NAME));
            a.shrink(1);assertEquals(1,b.getCount());
        }
    }
}
