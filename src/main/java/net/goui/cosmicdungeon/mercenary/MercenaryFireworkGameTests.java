package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.*;

public final class MercenaryFireworkGameTests {
    private MercenaryFireworkGameTests(){}
    public static void persistence(GameTestHelper helper){
        long runId=Long.MAX_VALUE-107;var level=helper.getLevel();var data=D1RunData.get(level.getServer());
        var contract=new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),"pyroclast",2,50);
        var run=new DungeonRunRegistryData.RunRecord(runId,"dungeon_1","minecraft:overworld",0,
                List.of(level.dimension().location().toString()),1,"ACTIVE","",0,List.of(contract.hirer()),List.of(),List.of())
                .withMercenaries(List.of(contract));
        var merc=new MercenaryEntity(ModEntities.MERCENARY.get(),level);merc.initialize(runId,contract);
        merc.fireworks(new MercenaryFireworkStock(2,123));merc.supplies().set(0,new ItemStack(Items.DIAMOND,3));
        try{
            for(int i=0;i<3;i++)MercenarySkills.record(data,run,contract,MercenarySkill.FIREWORKS);
            merc.die(merc.damageSources().generic());
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());
            merc.addAdditionalSaveData(output);
            var restored=new MercenaryEntity(ModEntities.MERCENARY.get(),level);
            restored.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),output.buildResult()));
            restored.resumeAfterRest();
            helper.assertTrue(restored.fireworks().equals(new MercenaryFireworkStock(2,123))
                    &&restored.supplies().getFirst().getCount()==3,Component.literal("Death and native save/reload retain stock, timer and supplies"));
            helper.assertTrue(MercenarySkills.level(restored,MercenarySkill.FIREWORKS)==3
                    &&MercenaryFireworkStock.capacity(3)==6,Component.literal("Earned Fireworks level survives revival"));
            var legacy=output.buildResult().copy();legacy.remove("mercenary_fireworks");legacy.remove("mercenary_rest");
            var old=new MercenaryEntity(ModEntities.MERCENARY.get(),level);
            old.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),legacy));
            helper.assertTrue(old.fireworks().equals(MercenaryFireworkStock.initial()),Component.literal("Legacy save gains baseline stock"));
            var fresh=new MercenaryEntity(ModEntities.MERCENARY.get(),level);fresh.initialize(runId+1,contract);
            helper.assertTrue(MercenarySkills.level(fresh,MercenarySkill.FIREWORKS)==1
                    &&fresh.fireworks().equals(MercenaryFireworkStock.initial()),Component.literal("New dungeon resets skill and stock"));
            helper.succeed();
        }finally{data.clearRun(runId);data.clearRun(runId+1);}
    }
}
