package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;

public final class MercenarySkillsGameTests {
    private MercenarySkillsGameTests() {}
    public static void lifecycle(GameTestHelper helper) {
        long runId=Long.MAX_VALUE-100;
        var level=helper.getLevel();var data=D1RunData.get(level.getServer());
        var contract=new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),"theurgist",2,50);
        var run=new DungeonRunRegistryData.RunRecord(runId,"dungeon_1","minecraft:overworld",0,
                List.of(level.dimension().location().toString()),1,"ACTIVE","",0,List.of(contract.hirer()),List.of(),List.of())
                .withMercenaries(List.of(contract));
        var entity=new MercenaryEntity(ModEntities.MERCENARY.get(),level);entity.initialize(runId,contract);
        entity.supplies().set(0,new ItemStack(Items.DIAMOND,3));
        try {
            for(int i=0;i<10;i++)MercenarySkills.record(data,run,contract,MercenarySkill.POSITIVE_POTIONS);
            entity.die(entity.damageSources().generic());
            helper.assertTrue(entity.dormant()&&MercenarySkills.level(entity,MercenarySkill.POSITIVE_POTIONS)==5,
                    Component.literal("Dormant companion retains its run progress"));
            entity.resumeAfterRest();
            helper.assertTrue(MercenarySkills.level(entity,MercenarySkill.POSITIVE_POTIONS)==5
                    &&entity.supplies().getFirst().getCount()==3,Component.literal("Revival preserves levels and original supplies"));
            var next=new MercenaryEntity(ModEntities.MERCENARY.get(),level);next.initialize(runId+1,contract);
            helper.assertTrue(MercenarySkills.level(next,MercenarySkill.POSITIVE_POTIONS)==1,
                    Component.literal("Even a reused contract begins at level 1 in a new run"));
            helper.assertTrue(!MercenarySkills.success(entity,MercenarySkill.POSITIVE_POTIONS),
                    Component.literal("Unregistered native entity cannot earn through the public success callback"));
            helper.succeed();
        } finally { data.clearRun(runId);data.clearRun(runId+1); }
    }
    public static void effects(GameTestHelper helper) {
        var level=helper.getLevel();var target=EntityType.COW.create(level,EntitySpawnReason.COMMAND);
        helper.assertTrue(target!=null,Component.literal("Native target created"));
        target.setHealth(4);float before=target.getHealth();
        MobEffects.INSTANT_HEALTH.value().applyInstantenousEffect(level,null,null,target,0,1);
        helper.assertTrue(MercenarySkillEffects.instantResult(true,before,target.getHealth(),0,0)==MercenarySkill.POSITIVE_POTIONS,
                Component.literal("Actual native healing is a success"));
        target.setHealth(target.getMaxHealth());before=target.getHealth();
        MobEffects.INSTANT_HEALTH.value().applyInstantenousEffect(level,null,null,target,0,1);
        helper.assertTrue(MercenarySkillEffects.instantResult(true,before,target.getHealth(),0,0)==null,
                Component.literal("Full-health healing earns nothing"));
        target.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,200,1));
        var old=new MobEffectInstance(target.getEffect(MobEffects.NIGHT_VISION));
        target.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,100,0));
        helper.assertTrue(!MercenarySkillEffects.improved(old,target.getEffect(MobEffects.NIGHT_VISION)),
                Component.literal("Weaker native buff does not count"));
        target.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,400,1));
        helper.assertTrue(MercenarySkillEffects.improved(old,target.getEffect(MobEffects.NIGHT_VISION)),
                Component.literal("Effective native buff extension counts"));
        helper.succeed();
    }
}
