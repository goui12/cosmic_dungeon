package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Native CI fixtures are restored synchronously; no production player/registry access bypass. */
public final class MercenaryLightningGameTests {
    private MercenaryLightningGameTests(){}
    public static void persistence(GameTestHelper helper){
        long runId=Long.MAX_VALUE-110;var level=helper.getLevel();var data=D1RunData.get(level.getServer());
        var contract=new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),"dragoon",2,50);
        var run=new DungeonRunRegistryData.RunRecord(runId,"dungeon_1","minecraft:overworld",0,
                List.of(level.dimension().location().toString()),1,"ACTIVE","",0,List.of(contract.hirer()),List.of(),List.of())
                .withMercenaries(List.of(contract));
        var merc=new MercenaryEntity(ModEntities.MERCENARY.get(),level);merc.initialize(runId,contract);
        var state=new MercenaryLightningState(37,8,Optional.of(UUID.randomUUID()),13);
        merc.lightning(state);merc.supplies().set(0,new ItemStack(Items.DIAMOND,3));
        try{
            for(int i=0;i<10;i++)MercenarySkills.record(data,run,contract,MercenarySkill.CHAIN_LIGHTNING);
            merc.die(merc.damageSources().generic());
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());
            merc.addAdditionalSaveData(output);
            var restored=new MercenaryEntity(ModEntities.MERCENARY.get(),level);
            restored.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),output.buildResult()));
            helper.assertTrue(restored.dormant()&&restored.lightning().equals(state),Component.literal("Dormant reload retains all lightning fields"));
            restored.resumeAfterRest();
            helper.assertTrue(restored.lightning().equals(state)&&restored.supplies().getFirst().getCount()==3
                    &&MercenarySkills.level(restored,MercenarySkill.CHAIN_LIGHTNING)==5,Component.literal("Revival retains charge, supplies and level 5"));
            var legacy=output.buildResult().copy();legacy.remove("mercenary_lightning");legacy.remove("mercenary_rest");
            var old=new MercenaryEntity(ModEntities.MERCENARY.get(),level);
            old.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),legacy));
            helper.assertTrue(old.lightning().equals(MercenaryLightningState.initial()),Component.literal("Legacy save defaults to empty charge"));
            var fresh=new MercenaryEntity(ModEntities.MERCENARY.get(),level);fresh.initialize(runId+1,contract);
            helper.assertTrue(MercenarySkills.level(fresh,MercenarySkill.CHAIN_LIGHTNING)==1
                    &&fresh.lightning().equals(MercenaryLightningState.initial()),Component.literal("New dungeon resets skill and charge"));
            helper.succeed();
        }finally{data.clearRun(runId);data.clearRun(runId+1);}
    }
    @SuppressWarnings("unchecked")
    public static void combat(GameTestHelper helper){
        long runId=Long.MAX_VALUE-111;var level=helper.getLevel();var server=level.getServer();
        var origin=helper.absoluteVec(new Vec3(.5,10.5,.5));
        var owner=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"LightningTest"));
        owner.setPos(origin);owner.setHealth(owner.getMaxHealth());
        var contract=new MercenaryContract(UUID.randomUUID(),owner.getUUID(),"dragoon",2,50);
        var run=new DungeonRunRegistryData.RunRecord(runId,"dungeon_1","minecraft:overworld",0,
                List.of(level.dimension().location().toString()),1,"ACTIVE","",0,List.of(owner.getUUID()),List.of(),List.of())
                .withMercenaries(List.of(contract));
        var registry=DungeonRunRegistryData.get(server);var data=D1RunData.get(server);
        Map<Long,DungeonRunRegistryData.RunRecord> runs;
        Map<UUID,ServerPlayer> players;
        try{
            var rf=DungeonRunRegistryData.class.getDeclaredField("runsById");rf.setAccessible(true);runs=(Map<Long,DungeonRunRegistryData.RunRecord>)rf.get(registry);
            var pf=PlayerList.class.getDeclaredField("playersByUUID");pf.setAccessible(true);players=(Map<UUID,ServerPlayer>)pf.get(server.getPlayerList());
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Native CI fixture unavailable",e);}
        if(runs.containsKey(runId)||players.containsKey(owner.getUUID()))throw new IllegalStateException("Fixture collision");
        runs.put(runId,run);players.put(owner.getUUID(),owner);
        var merc=new MercenaryEntity(ModEntities.MERCENARY.get(),level);merc.initialize(runId,contract);merc.setPos(origin);
        var primary=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
        var secondary=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
        var immune=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
        var wallTarget=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
        var teamTarget=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
        var angryWolf=EntityType.WOLF.create(level,EntitySpawnReason.COMMAND);
        var angryBee=EntityType.BEE.create(level,EntitySpawnReason.COMMAND);
        var angryBear=EntityType.POLAR_BEAR.create(level,EntitySpawnReason.COMMAND);
        var cow=EntityType.COW.create(level,EntitySpawnReason.COMMAND);
        var wolf=EntityType.WOLF.create(level,EntitySpawnReason.COMMAND);
        wolf.setOwnerReference(EntityReference.of(owner.getUUID()));
        var targets=List.of(primary,secondary,immune,cow,wolf,merc,wallTarget,teamTarget,angryWolf,angryBee,angryBear);
        var wall=new LinkedHashMap<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState>();
        var scoreboard=level.getScoreboard();var team=scoreboard.addPlayerTeam("chain"+owner.getUUID().toString().substring(0,8));
        scoreboard.addPlayerToTeam(owner.getScoreboardName(),team);
        scoreboard.addPlayerToTeam(teamTarget.getScoreboardName(),team);
        try{
            for(int i=0;i<targets.size();i++){
                var target=targets.get(i);target.setNoAi(true);target.setPos(origin.add(-1-i,0,0));
                level.addFreshEntity(target);
                target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);target.setHealth(200);
            }
            merc.setPos(origin);immune.setInvulnerable(true);
            for(var neutral:List.of(angryWolf,angryBee,angryBear)){
                neutral.setTarget(owner);
                helper.assertTrue(!MercenaryLightning.enemy(merc,owner,neutral),
                        Component.literal("Angry neutral animals remain protected even when targeting a party member"));
            }
            wallTarget.setPos(origin.add(3,0,0));teamTarget.setPos(origin.add(0,0,2));
            for(int y=0;y<3;y++){
                var pos=net.minecraft.core.BlockPos.containing(origin).offset(2,y,0);
                wall.put(pos,level.getBlockState(pos));
                level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            }
            helper.assertTrue(MercenaryLightning.activeOwner(merc)==owner,Component.literal("Fixture uses real active contract and online hirer guard"));
            merc.lightning(new MercenaryLightningState(48,8,Optional.of(primary.getUUID()),0));
            primary.hurtServer(level,merc.damageSources().mobAttack(merc),8);
            helper.assertTrue(merc.lightning().hits()==49,Component.literal("Native successful hit credits exactly once"));
            MercenaryLightning.tick(merc,level,0);
            helper.assertTrue(secondary.getHealth()==200,Component.literal("Forty-nine hits cannot trigger"));
            primary.invulnerableTime=0;primary.hurtServer(level,merc.damageSources().mobAttack(merc),8);
            var primaryHealth=primary.getHealth();float power=merc.lightning().damage();
            MercenaryLightning.tick(merc,level,0);
            helper.assertTrue(merc.lightning().hits()==1&&secondary.getHealth()<200&&primary.getHealth()==primaryHealth,
                    Component.literal("Fiftieth hit triggers: secondary hit counts once, original victim not hit twice"));
            helper.assertTrue(MercenarySkills.level(merc,MercenarySkill.CHAIN_LIGHTNING)==2&&merc.lightning().damage()==power,
                    Component.literal("One successful cast reaches level 2 without recursively multiplying power"));
            helper.assertTrue(immune.getHealth()==200&&cow.getHealth()==200&&wolf.getHealth()==200&&merc.getHealth()==200
                    &&wallTarget.getHealth()==200&&teamTarget.getHealth()==200
                    &&angryWolf.getHealth()==200&&angryBee.getHealth()==200&&angryBear.getHealth()==200,
                    Component.literal("Walls, scoreboard allies, immune, neutral, owned and self targets are protected"));
            merc.lightning(new MercenaryLightningState(98,power,Optional.of(primary.getUUID()),20));
            var health=secondary.getHealth();MercenaryLightning.tick(merc,level,10);
            helper.assertTrue(secondary.getHealth()==health&&merc.lightning().hits()==98,Component.literal("Banked hits do not bypass one-second cast spacing"));
            secondary.setInvulnerable(true);MercenaryLightning.tick(merc,level,10);
            helper.assertTrue(merc.lightning().hits()==98&&MercenarySkills.level(merc,MercenarySkill.CHAIN_LIGHTNING)==2,
                    Component.literal("Fully denied cast refunds charge and earns no skill success"));
            secondary.setInvulnerable(false);
            players.remove(owner.getUUID());MercenaryLightning.tick(merc,level,20);
            helper.assertTrue(merc.lightning().hits()==98&&secondary.getHealth()==health,Component.literal("Offline hirer cannot cast or drain charge"));
            helper.succeed();
        }finally{
            targets.forEach(Entity::discard);wall.forEach(level::setBlockAndUpdate);scoreboard.removePlayerTeam(team);
            runs.remove(runId);players.remove(owner.getUUID());data.clearRun(runId);
        }
    }
}
