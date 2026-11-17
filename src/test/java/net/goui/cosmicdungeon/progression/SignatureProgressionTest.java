package net.goui.cosmicdungeon.progression;

import com.mojang.serialization.Codec;
import java.util.*;
import net.goui.cosmicdungeon.playerclass.skill.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SignatureProgressionTest {
    @SuppressWarnings("unchecked") private Codec<PlayerProgressionData> codec() throws Exception{
        var f=PlayerProgressionData.class.getDeclaredField("CODEC");f.setAccessible(true);
        return (Codec<PlayerProgressionData>)f.get(null);
    }
    private PlayerProgressionData empty()throws Exception{return codec().parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();}
    private PlayerProgressionData reload(PlayerProgressionData data)throws Exception{
        return codec().parse(NbtOps.INSTANCE,codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow()).getOrThrow();
    }
    @Test void oldSaveReadsExactPotionXpWithoutRewritingOrDiscardingWeaponProgress()throws Exception{
        var data=empty();var id=UUID.randomUUID();
        data.addSkillXp(id,"theurgist.potions",21257,73750);data.addSkillXp(id,"dragoon.trident",550,73750);
        data.setCavernResidue(id,7);
        var tag=(CompoundTag)codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow();tag.remove("signature_skills");
        data=codec().parse(NbtOps.INSTANCE,tag).getOrThrow();
        assertEquals(21257,data.signatureProgress(id,SignatureSkill.POSITIVE_POTIONS));
        assertEquals(25,SignatureSkill.POSITIVE_POTIONS.level(21257,250,50));
        data.addSignatureProgress(id,SignatureSkill.POSITIVE_POTIONS,2,SignatureSkill.POSITIVE_POTIONS.threshold(100,250,50));
        data=reload(data);
        assertEquals(21259,data.signatureProgress(id,SignatureSkill.POSITIVE_POTIONS));
        assertEquals(21257,data.skillXp(id,"theurgist.potions"));assertEquals(550,data.skillXp(id,"dragoon.trident"));
        assertEquals(7,data.getCavernResidue(id));
    }
    @Test void separatePotionCapDoesNotAlterWeaponBalanceOrLoseProgressAtOldCap()throws Exception{
        var data=empty();var id=UUID.randomUUID();
        data.addSkillXp(id,"theurgist.potions",73750,73750);
        data.addSignatureProgress(id,SignatureSkill.POSITIVE_POTIONS,100000,SignatureSkill.POSITIVE_POTIONS.threshold(100,250,50));
        assertTrue(SignatureSkill.POSITIVE_POTIONS.level(data.signatureProgress(id,SignatureSkill.POSITIVE_POTIONS),250,50)>68);
        assertEquals(73750,data.skillXp(id,"theurgist.potions"));
    }
    @Test void smiteHasIndependentChargeFreeProficiencyCurve(){
        var s=SignatureSkill.SMITE;
        for(int value:new int[]{0,1,9})assertEquals(1,s.level(value,250,50));
        assertEquals(2,s.level(10,250,50));assertEquals(2,s.level(29,250,50));
        assertEquals(3,s.level(30,250,50));assertEquals(3,s.level(59,250,50));
        assertEquals(4,s.level(60,250,50));assertEquals(4,s.level(Integer.MAX_VALUE,250,50));
        assertEquals(20,s.nextLevelCost(10,250,50));assertEquals(0,s.nextLevelCost(60,250,50));
    }
    @Test void tracksOwnersAndFutureEntriesSurviveRepeatedSaveLoad()throws Exception{
        var a=UUID.randomUUID();var b=UUID.randomUUID();var data=empty();
        data.addSignatureProgress(a,SignatureSkill.WOLVES,6,5050);
        data.addSignatureProgress(a,SignatureSkill.FIREWORKS,3,5050);
        data.addSignatureProgress(b,SignatureSkill.WOLVES,1,5050);
        var tag=(CompoundTag)codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow();
        tag.getCompoundOrEmpty("signature_skills").getCompoundOrEmpty(a.toString())
                .getCompoundOrEmpty("values").putInt("future_expansion",91);
        data=codec().parse(NbtOps.INSTANCE,tag).getOrThrow();
        data.addSignatureProgress(a,SignatureSkill.SMITE,10,60);data=reload(reload(data));
        var saved=(CompoundTag)codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow();
        assertEquals(91,saved.getCompoundOrEmpty("signature_skills").getCompoundOrEmpty(a.toString()).getCompoundOrEmpty("values").getIntOr("future_expansion",0));
        assertEquals(6,data.signatureProgress(a,SignatureSkill.WOLVES));assertEquals(3,data.signatureProgress(a,SignatureSkill.FIREWORKS));
        assertEquals(1,data.signatureProgress(b,SignatureSkill.WOLVES));assertEquals(0,data.signatureProgress(b,SignatureSkill.SMITE));
    }
    @Test void futureVersionAndCorruptCountsFailClosedWithoutSilentlyResetting(){
        var invalid=new CompoundTag();invalid.putInt("version",2);
        assertTrue(SignatureProgress.CODEC.parse(NbtOps.INSTANCE,invalid).isError());
        invalid.putInt("version",1);var values=new CompoundTag();values.putInt("wolves",-1);invalid.put("values",values);
        assertTrue(SignatureProgress.CODEC.parse(NbtOps.INSTANCE,invalid).isError());
        assertThrows(IllegalArgumentException.class,()->SignatureProgress.EMPTY.add(SignatureSkill.WOLVES,-1,0,5050));
    }
    @Test void emptyZeroAwardDoesNotFreezeLegacySeedAndLowerCapsNeverEraseEarnedProgress(){
        assertSame(SignatureProgress.EMPTY,SignatureProgress.EMPTY.add(SignatureSkill.POSITIVE_POTIONS,0,21257,100000));
        var old=new SignatureProgress(1,Map.of("wolves",9999));
        assertEquals(9999,old.add(SignatureSkill.WOLVES,1,0,5050).get(SignatureSkill.WOLVES,0));
    }
    @Test void allSixClassesHaveExactlyOneStableSignature(){
        assertEquals(6,SignatureSkill.values().length);
        assertEquals(6,Arrays.stream(SignatureSkill.values()).map(SignatureSkill::classId).distinct().count());
        assertTrue(SignatureSkill.forClass("deadeye").isEmpty());
        for(var s:SignatureSkill.values())assertSame(s,SignatureSkill.forClass(s.classId()).orElseThrow());
    }
}
