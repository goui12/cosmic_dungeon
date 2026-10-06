package net.goui.cosmicdungeon.mercenary;

import com.mojang.serialization.Codec;
import io.netty.buffer.Unpooled;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.network.PartyPayloads;
import net.goui.cosmicdungeon.client.screen.MercenaryHudLayout;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenarySkillsTest {
    private final UUID owner=UUID.randomUUID(), id=UUID.randomUUID();
    private MercenaryContract contract(String cls) { return new MercenaryContract(id,owner,cls,2,50); }
    private DungeonRunRegistryData.RunRecord run(long id, MercenaryContract contract) {
        return new DungeonRunRegistryData.RunRecord(id,"dungeon_1","minecraft:overworld",0,
                List.of("cosmicdungeon:d1_instance_"+id),1,"ACTIVE","",0,List.of(owner),List.of(),List.of())
                .withMercenaries(List.of(contract));
    }
    @SuppressWarnings("unchecked") private Codec<D1RunData> codec() throws Exception {
        var field=D1RunData.class.getDeclaredField("CODEC");field.setAccessible(true);
        return (Codec<D1RunData>)field.get(null);
    }
    private D1RunData data() throws Exception { return codec().parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow(); }
    private D1RunData reload(D1RunData data) throws Exception {
        return codec().parse(NbtOps.INSTANCE,codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow()).getOrThrow();
    }
    @Test void curveMatchesEachIncreasingRequirementAndExample() {
        assertEquals(1,MercenarySkill.level(0));
        assertEquals(2,MercenarySkill.level(1));assertEquals(3,MercenarySkill.level(3));
        assertEquals(4,MercenarySkill.level(6));assertEquals(5,MercenarySkill.level(10));
        for(int level=2;level<=1000;level++){
            int threshold=(int)MercenarySkill.threshold(level);
            assertEquals(level-1,MercenarySkill.level(threshold-1));
            assertEquals(level,MercenarySkill.level(threshold));
            assertEquals(level,MercenarySkill.threshold(level+1)-threshold);
        }
    }
    @Test void countersCannotOverflowOrBecomeNegative() {
        assertEquals(Integer.MAX_VALUE,MercenarySkill.advance(Integer.MAX_VALUE));
        assertEquals(65536,MercenarySkill.level(Integer.MAX_VALUE));
        assertTrue(MercenarySkill.threshold(65536)<=Integer.MAX_VALUE);
        assertTrue(MercenarySkill.threshold(65537)>Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class,()->MercenarySkill.level(-1));
        assertThrows(IllegalArgumentException.class,()->MercenarySkill.advance(-1));
    }
    @Test void onlyRequestedSkillsAreAssignedToTheirClasses() {
        assertEquals(List.of(MercenarySkill.POSITIVE_POTIONS,MercenarySkill.NEGATIVE_POTIONS),
                MercenarySkill.forContract(contract("theurgist")));
        assertEquals(List.of(MercenarySkill.WOLVES),MercenarySkill.forContract(contract("bogatyr")));
        assertEquals(List.of(MercenarySkill.FIREWORKS),MercenarySkill.forContract(contract("pyroclast")));
        assertEquals(List.of(MercenarySkill.CHAIN_LIGHTNING),MercenarySkill.forContract(contract("dragoon")));
        assertTrue(MercenarySkill.forContract(contract("judicator")).isEmpty());
        assertThrows(IllegalArgumentException.class,()->MercenarySkill.fromId("invented"));
    }
    @Test void oldRunDataStartsAtOneAndUnrelatedObjectivesSurviveReload() throws Exception {
        var data=data();var contract=contract("theurgist");var run=run(1,contract);
        data.setCount(1,"kills:"+owner,7);
        assertEquals(0,MercenarySkills.successes(data,1,contract,MercenarySkill.POSITIVE_POTIONS));
        for(int i=0;i<10;i++)MercenarySkills.record(data,run,contract,MercenarySkill.POSITIVE_POTIONS);
        data=reload(data);
        assertEquals(5,MercenarySkill.level(MercenarySkills.successes(data,1,contract,MercenarySkill.POSITIVE_POTIONS)));
        assertEquals(0,MercenarySkills.successes(data,1,contract,MercenarySkill.NEGATIVE_POTIONS));
        assertEquals(7,data.count(1,"kills:"+owner));
    }
    @Test void nextRunAndOtherHiresDoNotInheritProgressEvenWhenContractIsReused() throws Exception {
        var data=data();var contract=contract("bogatyr");
        MercenarySkills.record(data,run(1,contract),contract,MercenarySkill.WOLVES);
        var other=new MercenaryContract(UUID.randomUUID(),owner,"bogatyr",3,50);
        assertEquals(0,MercenarySkills.successes(data,1,other,MercenarySkill.WOLVES));
        assertEquals(0,MercenarySkills.successes(data,2,contract,MercenarySkill.WOLVES));
        MercenarySkills.record(data,run(2,contract),contract,MercenarySkill.WOLVES);
        data.clearRun(1);data=reload(data);
        assertEquals(0,MercenarySkills.successes(data,1,contract,MercenarySkill.WOLVES));
        assertEquals(1,MercenarySkills.successes(data,2,contract,MercenarySkill.WOLVES));
    }
    @Test void wrongClassUnknownContractAndSealedOrRetiredRunsCannotEarn() throws Exception {
        var data=data();var contract=contract("bogatyr");var run=run(1,contract);
        assertEquals(-1,MercenarySkills.record(data,run,contract,MercenarySkill.POSITIVE_POTIONS));
        assertEquals(-1,MercenarySkills.record(data,run,new MercenaryContract(UUID.randomUUID(),owner,"bogatyr",4,50),MercenarySkill.WOLVES));
        assertEquals(-1,MercenarySkills.record(data,run.withCompletionExited(owner),contract,MercenarySkill.WOLVES));
        assertEquals(-1,MercenarySkills.record(data,run.withState(DungeonRunState.RESETTING,DungeonResetReason.ABANDONED),contract,MercenarySkill.WOLVES));
        data.setValue(1,"watson_outcome","success");
        assertEquals(-1,MercenarySkills.record(data,run,contract,MercenarySkill.WOLVES));
        assertEquals(0,MercenarySkills.successes(data,1,contract,MercenarySkill.WOLVES));
    }
    @Test void dormantOrWrongIdentityDimensionCannotEarn() {
        var contract=contract("bogatyr");var run=run(1,contract);
        assertTrue(MercenarySkills.admitted(run,contract,id,"cosmicdungeon:d1_instance_1",MercenarySkill.WOLVES,false));
        assertFalse(MercenarySkills.admitted(run,contract,id,"cosmicdungeon:d1_instance_1",MercenarySkill.WOLVES,true));
        assertFalse(MercenarySkills.admitted(run,contract,id,"minecraft:overworld",MercenarySkill.WOLVES,false));
        assertFalse(MercenarySkills.admitted(run,contract,UUID.randomUUID(),"cosmicdungeon:d1_instance_1",MercenarySkill.WOLVES,false));
    }
    @Test void onePotionCannotMultiplyExperienceAcrossTargetsEffectsOrReload() {
        var source=new CompoundTag();var awarded=new AtomicInteger();
        assertTrue(MercenarySkillEffects.credit(source,MercenarySkill.POSITIVE_POTIONS,()->{awarded.incrementAndGet();return true;}));
        var saved=source.copy();
        for(int i=0;i<48;i++)assertFalse(MercenarySkillEffects.credit(saved,MercenarySkill.POSITIVE_POTIONS,()->{awarded.incrementAndGet();return true;}));
        assertEquals(1,awarded.get());
        assertTrue(MercenarySkillEffects.credit(saved,MercenarySkill.NEGATIVE_POTIONS,()->{awarded.incrementAndGet();return true;}));
        assertEquals(2,awarded.get());
    }
    @Test void rejectedOrImmuneEffectDoesNotSpendTheSuccessfulPotionCredit() {
        var source=new CompoundTag();
        assertFalse(MercenarySkillEffects.credit(source,MercenarySkill.POSITIVE_POTIONS,()->false));
        assertTrue(MercenarySkillEffects.credit(source,MercenarySkill.POSITIVE_POTIONS,()->true));
        assertNull(MercenarySkillEffects.instantResult(true,20,20,0,0));
        assertNull(MercenarySkillEffects.instantResult(false,20,20,0,0));
        assertNull(MercenarySkillEffects.instantResult(false,10,14,0,0));
        assertEquals(MercenarySkill.POSITIVE_POTIONS,MercenarySkillEffects.instantResult(true,10,14,0,0));
        assertEquals(MercenarySkill.NEGATIVE_POTIONS,MercenarySkillEffects.instantResult(false,20,16,0,0));
        assertEquals(MercenarySkill.NEGATIVE_POTIONS,MercenarySkillEffects.instantResult(false,20,20,4,0));
    }
    @Test void skillRowsRoundTripWithoutLosingReviveControls() {
        var row=new PartyPayloads.Mercenary("Edmund","Owner",0,0,500,"RESPAWNING",
                new PartyPayloads.Recovery(id.toString(),12000,25,true),
                List.of(new PartyPayloads.Skill("positive_potions",10),new PartyPayloads.Skill("negative_potions",3)));
        var buf=Unpooled.buffer();try{
            PartyPayloads.Mercenary.CODEC.encode(buf,row);
            assertEquals(row,PartyPayloads.Mercenary.CODEC.decode(buf));assertEquals(0,buf.readableBytes());
        }finally{buf.release();}
        assertEquals(List.of("Edmund","Respawn 8:20","Positive Potions - Level 5 (0/5)","Negative Potions - Level 3 (0/3)"),
                MercenaryHudLayout.tooltip(row));
    }
    @Test void packetsRejectUnknownNegativeDuplicateAndOversizedSkillRows() {
        assertThrows(IllegalArgumentException.class,()->new PartyPayloads.Skill("unknown",0));
        assertThrows(IllegalArgumentException.class,()->new PartyPayloads.Skill("wolves",-1));
        var skill=new PartyPayloads.Skill("wolves",0);
        assertThrows(IllegalArgumentException.class,()->new PartyPayloads.Mercenary("a","b",20,20,-1,"ACTIVE",PartyPayloads.Recovery.NONE,List.of(skill,skill)));
        assertThrows(IllegalArgumentException.class,()->new PartyPayloads.Mercenary("a","b",20,20,-1,"ACTIVE",PartyPayloads.Recovery.NONE,
                List.of(skill,new PartyPayloads.Skill("fireworks",0),new PartyPayloads.Skill("chain_lightning",0))));
        var buf=Unpooled.buffer();try{
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT.encode(buf,3);
            assertThrows(Exception.class,()->PartyPayloads.Skill.CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list(2)).decode(buf));
        }finally{buf.release();}
    }
}
