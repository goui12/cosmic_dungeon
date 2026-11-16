package net.goui.cosmicdungeon.playerclass.skill;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.gametest.DurableRecoveryGameTestSupport;
import net.goui.cosmicdungeon.mercenary.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.progression.PlayerProgressionData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.nbt.*;

public final class SignatureSkillGameTests {
    private SignatureSkillGameTests(){}
    private static void check(GameTestHelper helper,boolean condition,String message){
        helper.assertTrue(condition,Component.literal(message));
    }
    @SuppressWarnings("unchecked") public static void playerPersistence(GameTestHelper helper){
        var server=helper.getLevel().getServer();var profile=new GameProfile(UUID.randomUUID(),"SignatureTest");
        var player=new ServerPlayer(server,helper.getLevel(),profile,ClientInformation.createDefault());
        var root=new CompoundTag();root.putString(ClassData.KEY_CLASS_ID,"theurgist");
        player.getPersistentData().put(ClassData.ROOT_TAG,root);
        var data=PlayerProgressionData.get(server);
        CompoundTag before=(CompoundTag)DurableRecoveryGameTestSupport.image(PlayerProgressionData.class,data);
        try{
            data.addSkillXp(profile.id(),"theurgist.potions",21257,73750);
            check(helper,SignatureSkills.playerSnapshot(player).getFirst().level()==25,"Legacy native potion level retained");
            data.addSignatureProgress(profile.id(),SignatureSkill.POSITIVE_POTIONS,2,
                    SignatureSkill.POSITIVE_POTIONS.threshold(100,ClassSkillConfig.BASE.get(),ClassSkillConfig.STEP.get()));
            check(helper,data.flushVerified(),"Signature progress saved through native SavedData");
            data=DurableRecoveryGameTestSupport.reload(server,PlayerProgressionData.class,PlayerProgressionData.SAVE_ID);
            var respawned=new ServerPlayer(server,helper.getLevel(),profile,ClientInformation.createDefault());
            respawned.getPersistentData().put(ClassData.ROOT_TAG,root.copy());
            check(helper,SignatureSkills.playerProgress(respawned,SignatureSkill.POSITIVE_POTIONS)==21259,
                    "New player entity and native reload preserve exact signature progress");
            check(helper,data.skillXp(profile.id(),"theurgist.potions")==21257,"Weapon XP remains independent");
            var changed=root.copy();changed.putString(ClassData.KEY_CLASS_ID,"judicator");
            respawned.getPersistentData().put(ClassData.ROOT_TAG,changed);
            check(helper,SignatureSkills.playerSnapshot(respawned).getFirst().id().equals("smite")
                    &&SignatureSkills.playerSnapshot(respawned).getFirst().level()==1,"Class switch reads the appropriate starting track");
            check(helper,!SignatureSkills.success(respawned,SignatureSkill.SMITE),"Out-of-run success callback is denied");
            helper.succeed();
        }finally{
            try{
                var f=PlayerProgressionData.class.getDeclaredField("CODEC");f.setAccessible(true);
                var codec=(com.mojang.serialization.Codec<PlayerProgressionData>)f.get(null);
                var restored=codec.parse(NbtOps.INSTANCE,before).getOrThrow();
                server.overworld().getDataStorage().set(PlayerProgressionData.TYPE,restored);
                check(helper,PlayerProgressionData.get(server).flushVerified(),"Fixture restores the original progression data");
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
    }
    public static void mercenaryCompatibility(GameTestHelper helper){
        long run=Long.MAX_VALUE-11011;
        var server=helper.getLevel().getServer();var data=D1RunData.get(server);
        try{
            for(var skill:SignatureSkill.values()){
                var contract=new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),skill.classId(),2,50);
                if(skill.legacy()!=null){
                    data.setCount(run,"mercenary_skill:"+contract.id()+":"+skill.legacy().id(),10);
                    var track=SignatureSkills.mercenarySnapshot(server,run,contract).getFirst();
                    check(helper,track.level()==skill.legacy().levelFor(10),"Legacy "+skill.title()+" curve remains unchanged");
                }else{
                    data.setCount(run,"mercenary_skill:"+contract.id()+":combat",24);
                    check(helper,SignatureSkills.mercenarySnapshot(server,run,contract).getFirst().level()==1,
                            "Old Combat kills do not masquerade as successful Smite casts");
                    data.setCount(run,SignatureSkills.smiteKey(contract.id()),60);
                    check(helper,SignatureSkills.mercenarySnapshot(server,run,contract).getFirst().level()==4,"Smite uses approved successful-cast thresholds");
                    check(helper,data.count(run,"mercenary_skill:"+contract.id()+":combat")==24,"Combat history preserved");
                }
                var fresh=SignatureSkills.mercenarySnapshot(server,run+1,contract).getFirst();
                check(helper,fresh.progress()==0,"A new run never inherits signature progress");
            }
            helper.succeed();
        }finally{data.clearRun(run);data.clearRun(run+1);}
    }
}
