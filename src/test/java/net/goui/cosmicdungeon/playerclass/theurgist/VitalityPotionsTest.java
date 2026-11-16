package net.goui.cosmicdungeon.playerclass.theurgist;

import java.util.*;
import net.goui.cosmicdungeon.crafting.PotionRecipeCatalog;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class VitalityPotionsTest {
    @Test void approvedBoundariesHaveExactlyOneCraftableRank() {
        int[] levels={0,25,35,42,50,55,59,63,66,68};
        int[] percent={10,15,20,25,30,35,40,45,50,55};
        for(int i=0;i<levels.length;i++){
            var rank=VitalityPotions.RANKS.get(i);
            assertEquals(levels[i],rank.level());assertEquals(percent[i],rank.percent());
            assertEquals(rank,VitalityPotions.rank(levels[i]));
            if(i>0)assertEquals(VitalityPotions.RANKS.get(i-1),VitalityPotions.rank(levels[i]-1));
        }
        for(int level=0;level<=100;level++){
            int count=0;
            for(var rank:VitalityPotions.RANKS)if(VitalityPotions.craftable(rank.targetId(),level))count++;
            assertEquals(1,count);
            assertTrue(VitalityPotions.craftable("minecraft:strong_healing_splash",level));
        }
        assertEquals(VitalityPotions.RANKS.getFirst(),VitalityPotions.rank(-1));
        assertEquals(VitalityPotions.RANKS.getLast(),VitalityPotions.rank(Integer.MAX_VALUE));
        assertEquals(54000,VitalityPotions.DURATION);
    }
    @Test void everyRankHasExactlyStandardHealingRoutesAndCosts() {
        var vanilla=PotionRecipeCatalog.vanilla(FeatureFlags.VANILLA_SET);
        var catalog=PotionRecipeCatalog.classRecipes(FeatureFlags.VANILLA_SET);
        var healing=catalog.target("minecraft:healing_splash");
        var expected=catalog.routes(healing);
        assertFalse(expected.isEmpty());
        assertEquals(vanilla.targets().size()+10,catalog.targets().size());
        for(var rank:VitalityPotions.RANKS){
            assertNull(vanilla.target(rank.targetId()));
            var target=catalog.target(rank.targetId());assertNotNull(target);
            assertEquals(Set.of("theurgist"),target.classes());assertEquals(Items.SPLASH_POTION,target.container());
            var routes=catalog.routes(target);assertEquals(expected.size(),routes.size());
            for(int i=0;i<routes.size();i++){
                assertEquals(expected.get(i).inputPotion(),routes.get(i).inputPotion());
                assertEquals(expected.get(i).inputContainer(),routes.get(i).inputContainer());
                assertEquals(expected.get(i).ingredients(),routes.get(i).ingredients());
                assertEquals(expected.get(i).steps(),routes.get(i).steps());
            }
            var effect=target.potion().value().getEffects().getFirst();
            assertTrue(VitalityPotions.isVitality(effect));assertEquals(rank.amplifier(),effect.getAmplifier());
            assertEquals(54000,effect.getDuration());
        }
        for(var target:vanilla.targets())assertEquals(vanilla.routes(target),catalog.routes(target));
    }
    @Test void existingPotionBonusStartsAt25WithoutChangingItsCap() {
        for(int level:new int[]{0,1,23,24})assertEquals(0,VitalityPotions.bonus(level,50,.25));
        assertEquals(.005,VitalityPotions.bonus(25,50,.25),1e-9);
        assertEquals(.25,VitalityPotions.bonus(74,50,.25),1e-9);
        assertEquals(.25,VitalityPotions.bonus(Integer.MAX_VALUE,50,.25),1e-9);
    }
    @Test void unlockMessageNamesCasterAndItalicRank() {
        var message=VitalityPotions.unlockMessage("Ari",VitalityPotions.rank(25));
        assertEquals("Ari has a new understanding of potions. Favilla Vitas unlocked.",message.getString());
        assertTrue(message.getSiblings().getFirst().getStyle().isItalic());
    }
}
