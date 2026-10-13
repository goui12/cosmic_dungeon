package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

public final class MercenaryWolfGameTests {
    private MercenaryWolfGameTests(){}
    public static void progression(GameTestHelper helper){
        long runId=Long.MAX_VALUE-106;
        var level=helper.getLevel();var data=D1RunData.get(level.getServer());
        var contract=new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),"bogatyr",2,50);
        var run=new DungeonRunRegistryData.RunRecord(runId,"dungeon_1","minecraft:overworld",0,
                List.of(level.dimension().location().toString()),1,"ACTIVE","",0,List.of(contract.hirer()),List.of(),List.of())
                .withMercenaries(List.of(contract));
        var merc=new MercenaryEntity(ModEntities.MERCENARY.get(),level);merc.initialize(runId,contract);
        var bond=new MercenaryWolves.Bond(runId,contract.id(),contract.hirer());
        try{
            for(int i=0;i<3;i++)MercenarySkills.record(data,run,contract,MercenarySkill.WOLVES);
            helper.assertTrue(MercenarySkills.level(merc,MercenarySkill.WOLVES)==3
                    &&MercenaryWolves.capacity(data,bond)==20,Component.literal("Skill progress never raises the twenty-dog admission limit"));
            for(int i=0;i<20;i++)helper.assertTrue(MercenaryWolves.remember(data,bond,UUID.randomUUID()),
                    Component.literal("Scaled roster admits all twenty identities"));
            helper.assertTrue(!MercenaryWolves.remember(data,bond,UUID.randomUUID()),Component.literal("Twenty-first new identity rejected"));
            merc.die(merc.damageSources().generic());merc.resumeAfterRest();
            helper.assertTrue(MercenaryWolves.capacity(data,bond)==20,Component.literal("Revival retains roster under the fixed admission cap"));
            merc.wolfTicks(600);
            merc.wolfTicks(MercenaryWolfBalance.remaining(merc.wolfTicks(),10,MercenarySkills.level(merc,MercenarySkill.WOLVES)));
            helper.assertTrue(merc.wolfTicks()<590,Component.literal("Existing summon timer adopts the shorter interval"));
            helper.assertTrue(MercenaryWolves.capacity(data,new MercenaryWolves.Bond(runId+1,contract.id(),contract.hirer()))==20,
                    Component.literal("New run starts with the same twenty-dog limit"));
            helper.succeed();
        }finally{data.clearRun(runId);data.clearRun(runId+1);}
    }
}
