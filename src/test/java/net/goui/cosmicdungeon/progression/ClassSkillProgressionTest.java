package net.goui.cosmicdungeon.progression;

import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.playerclass.skill.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class ClassSkillProgressionTest {
    @SuppressWarnings("unchecked")
    private Codec<PlayerProgressionData> codec() throws Exception {
        var f=PlayerProgressionData.class.getDeclaredField("CODEC");f.setAccessible(true);
        return (Codec<PlayerProgressionData>)f.get(null);
    }
    private PlayerProgressionData empty() throws Exception {return codec().parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();}
    private PlayerProgressionData reload(PlayerProgressionData data) throws Exception {
        return codec().parse(NbtOps.INSTANCE,codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow()).getOrThrow();
    }
    @Test void approvedCurveAndBoundaries() {
        assertEquals(4750,ClassSkillRules.threshold(10,250,50));
        assertEquals(21250,ClassSkillRules.threshold(25,250,50));
        assertEquals(73750,ClassSkillRules.threshold(50,250,50));
        assertEquals(0,ClassSkillRules.level(249,50,250,50));
        assertEquals(1,ClassSkillRules.level(250,50,250,50));
        assertEquals(49,ClassSkillRules.level(73749,50,250,50));
        assertEquals(50,ClassSkillRules.level(Integer.MAX_VALUE,50,250,50));
    }
    @Test void capsNeverOverflowOrEraseEarnedXpAfterConfigReduction() {
        assertEquals(73750,ClassSkillRules.addXp(73749,10,73750));
        assertEquals(73750,ClassSkillRules.addXp(73750,10,21250));
        assertEquals(Integer.MAX_VALUE,ClassSkillRules.addXp(Integer.MAX_VALUE-3,100,Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,()->ClassSkillRules.addXp(0,-1,100));
    }
    @Test void bonusesInterpolateAndClampWithoutAmplifierJumps() {
        assertEquals(.125,ClassSkillRules.bonus(25,50,.25));
        assertEquals(.25,ClassSkillRules.bonus(99,50,.25));
        assertEquals(0,ClassSkillRules.bonus(-1,50,.25));
        assertEquals(.08,.03+ClassSkillRules.bonus(50,50,.05),.000001);
        assertEquals(.90,1-ClassSkillRules.bonus(50,50,.10),.000001);
    }
    @Test void classesHaveOnlyDocumentedWeaponSkills() {
        assertTrue(ClassSkillRules.known("judicator","mace"));
        assertFalse(ClassSkillRules.known("judicator","sword"));
        assertTrue(ClassSkillRules.known("theurgist","potions"));
        assertFalse(ClassSkillRules.known("bogatyr","potions"));
        assertFalse(ClassSkillRules.known("theurgist","pickaxe"));
        assertFalse(ClassSkillRules.known("metalmancer","golem"));
        assertEquals(16,ClassSkillRules.DAMAGE.size());
    }
    @Test void weaponsUseNativeIdentity() throws Exception {
        // Native JUnit bootstraps registries but not a server datapack/tag reload.
        var holder=Items.DIAMOND_SWORD.builtInRegistryHolder();
        var tags=holder.tags().toList();
        var bind=net.minecraft.core.Holder.Reference.class.getDeclaredMethod("bindTags",Collection.class);
        bind.setAccessible(true);
        try {
            bind.invoke(holder,List.of(net.minecraft.tags.ItemTags.SWORDS));
            assertEquals("sword",ClassSkills.weapon(new ItemStack(Items.DIAMOND_SWORD)));
        } finally { bind.invoke(holder,tags); }
        assertEquals("mace",ClassSkills.weapon(new ItemStack(Items.MACE)));
        assertEquals("trident",ClassSkills.weapon(new ItemStack(Items.TRIDENT)));
        assertEquals("bow",ClassSkills.weapon(new ItemStack(Items.BOW)));
        assertEquals("crossbow",ClassSkills.weapon(new ItemStack(Items.CROSSBOW)));
        assertEquals("potions",ClassSkills.weapon(new ItemStack(Items.SPLASH_POTION)));
        assertEquals("",ClassSkills.weapon(new ItemStack(Items.DIAMOND_PICKAXE)));
        assertEquals("",ClassSkills.weapon(new ItemStack(Items.WOLF_SPAWN_EGG)));
    }
    @Test void legacyDataStartsAtZeroAndPreservesExistingProgress() throws Exception {
        var data=empty();var owner=UUID.randomUUID();
        data.setLesserBlooms(owner,15);data.setCavernResidue(owner,7);data.setD1Completed(owner,true);
        var encoded=(CompoundTag)codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow();encoded.remove("class_skill_xp");
        var restored=codec().parse(NbtOps.INSTANCE,encoded).getOrThrow();
        assertEquals(0,restored.skillXp(owner,"bogatyr.sword"));assertEquals(15,restored.getLesserBlooms(owner));
        assertEquals(7,restored.getCavernResidue(owner));assertTrue(restored.isD1CompletedWithAtLeast3LesserBlooms(owner));
    }
    @Test void classesPlayersAndWeaponsRemainIndependentAcrossReloadAndOtherProgressionChanges() throws Exception {
        var a=UUID.randomUUID();var b=UUID.randomUUID();var data=empty();
        data.addSkillXp(a,"bogatyr.sword",250,73750);data.addSkillXp(a,"bogatyr.bow",10,73750);
        data.addSkillXp(a,"judicator.mace",550,73750);data.addSkillXp(b,"bogatyr.sword",30,73750);
        data.setLesserBlooms(a,20);data.setD1Completed(a,false);data.setVillageAccessUnlocked(a,true);data=reload(data);
        assertEquals(250,data.skillXp(a,"bogatyr.sword"));assertEquals(10,data.skillXp(a,"bogatyr.bow"));
        assertEquals(550,data.skillXp(a,"judicator.mace"));assertEquals(30,data.skillXp(b,"bogatyr.sword"));
        assertEquals(20,data.getLesserBlooms(a));assertTrue(data.isVillageAccessUnlocked(a));
        var snapshot=data.skills(a);assertThrows(UnsupportedOperationException.class,()->snapshot.put("bogatyr.sword",0));
    }
    @Test void corruptSkillsFailClosed() throws Exception {
        var owner=UUID.randomUUID();var data=empty();data.addSkillXp(owner,"theurgist.potions",2,73750);
        var encoded=(CompoundTag)codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow();
        var skills=encoded.getCompoundOrEmpty("class_skill_xp").getCompoundOrEmpty(owner.toString());
        skills.putInt("theurgist.potions",-1);assertTrue(codec().parse(NbtOps.INSTANCE,encoded).isError());
        skills.putInt("theurgist.potions",2);skills.putInt("forged.admin",100);
        assertTrue(codec().parse(NbtOps.INSTANCE,encoded).isError());
        assertThrows(IllegalArgumentException.class,()->data.addSkillXp(owner,"judicator.sword",10,73750));
    }
    @Test void persistedShotCannotBeRelabeledAfterClassRunOrOwnerChanges() {
        UUID owner=UUID.randomUUID();
        var shot=SkillAttackSnapshot.create(owner,"pyroclast","crossbow",71);
        shot.putInt("xp_spent",8);
        var saved=new CompoundTag();saved.put(ClassSkills.SHOT,shot);
        var restored=saved.copy().getCompoundOrEmpty(ClassSkills.SHOT);
        assertTrue(SkillAttackSnapshot.matches(restored,owner,"pyroclast",71));
        assertEquals("crossbow",restored.getStringOr("skill",""));
        assertEquals(2,ClassSkillRules.budget(restored.getIntOr("xp_spent",0),10,10));
        assertFalse(SkillAttackSnapshot.matches(restored,owner,"bogatyr",71));
        assertFalse(SkillAttackSnapshot.matches(restored,owner,"pyroclast",72));
        assertFalse(SkillAttackSnapshot.matches(restored,UUID.randomUUID(),"pyroclast",71));
        assertFalse(SkillAttackSnapshot.matches(new CompoundTag(),owner,"pyroclast",71));
        restored.putInt("xp_spent",-1);
        assertFalse(SkillAttackSnapshot.matches(restored,owner,"pyroclast",71));
    }
    @Test void PunchingWithAPotionOrRangedWeaponCannotTrainThatSkill() {
        assertTrue(ClassSkillRules.melee("sword"));
        assertTrue(ClassSkillRules.melee("mace"));
        assertTrue(ClassSkillRules.melee("trident"));
        assertFalse(ClassSkillRules.melee("bow"));
        assertFalse(ClassSkillRules.melee("crossbow"));
        assertFalse(ClassSkillRules.melee("potions"));
    }
    @Test void environmentalDamageDoesNotUseHeldWeaponOrLastDamager() {
        var source=new net.minecraft.world.damagesource.DamageSource(net.minecraft.core.Holder.direct(
                new net.minecraft.world.damagesource.DamageType("environment",0)));
        assertNull(ClassSkills.attack(source));
    }
    @Test void mixedSupportAndKillCreditsShareOnePotionBudget() {
        int spent=0;for(int request:new int[]{2,2,10,2,10})spent+=ClassSkillRules.budget(spent,request,10);
        assertEquals(10,spent);assertEquals(0,ClassSkillRules.budget(10,10,10));
        assertEquals(0,ClassSkillRules.budget(0,2,0));
    }
    @Test void equalBuffRefreshAndHarmfulEffectsDoNotTrainSupport() {
        assertTrue(ClassSkillRules.usefulBuff(true,null,0));assertTrue(ClassSkillRules.usefulBuff(true,0,1));
        assertFalse(ClassSkillRules.usefulBuff(true,1,1));assertFalse(ClassSkillRules.usefulBuff(true,2,1));
        assertFalse(ClassSkillRules.usefulBuff(false,null,0));
    }
    @Test void recipientEffectCooldownIncludesExactBoundaryAndClockRollback() {
        assertTrue(ClassSkillRules.cooldownReady(0,-1,60));assertFalse(ClassSkillRules.cooldownReady(159,100,60));
        assertTrue(ClassSkillRules.cooldownReady(160,100,60));assertFalse(ClassSkillRules.cooldownReady(10,100,60));
    }
}
