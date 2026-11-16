package net.goui.cosmicdungeon.playerclass.skill;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.effect.ModMobEffects;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.gametest.DurableRecoveryGameTestSupport;
import net.goui.cosmicdungeon.mercenary.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.progression.PlayerProgressionData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/** Native effects, real action authority, party boundary and persisted cooldown/attribution fixtures. */
public final class ClassBuffGameTests {
    private ClassBuffGameTests(){}
    private static MercenaryEntity merc(ChargedClassGameTests.Fixture f,String cls){
        var contract=new MercenaryContract(UUID.randomUUID(),f.player.getUUID(),cls,2,50);
        var run=f.runs.get(f.run);var contracts=new ArrayList<>(run.mercenaries());contracts.add(contract);
        f.runs.put(f.run,run.withMercenaries(contracts));
        var merc=new MercenaryEntity(ModEntities.MERCENARY.get(),f.level);merc.initialize(f.run,contract);
        merc.setPos(f.origin.add(1,0,0));merc.setNoAi(true);f.entities.add(merc);f.level.addFreshEntity(merc);return merc;
    }
    private static void ready(ChargedClassGameTests.Fixture f,LivingEntity actor,ClassBuff buff){
        String id=actor instanceof MercenaryEntity m?"mercenary:"+m.contract().id():"player:"+actor.getUUID();
        D1RunData.get(f.level.getServer()).setValue(f.run,ClassBuffs.cooldownKey(id,buff.classId()),null);
    }
    public static void recipients(GameTestHelper h){
        try(var f=new ChargedClassGameTests.Fixture(h,Long.MAX_VALUE-15001,"bogatyr")){
            var ally=FakePlayerFactory.get(f.level,new GameProfile(UUID.randomUUID(),"BuffAlly"));
            var outsider=FakePlayerFactory.get(f.level,new GameProfile(UUID.randomUUID(),"BuffOutsider"));
            for(var p:List.of(ally,outsider)){
                var tag=p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();tag.putString(ClassData.KEY_CLASS_ID,"dragoon");
                p.getPersistentData().put(ClassData.ROOT_TAG,tag);p.setPos(f.origin.add(2,0,0));p.setHealth(20);
                f.players.put(p.getUUID(),p);f.level.addNewPlayer(p);
            }
            var old=f.runs.get(f.run);
            f.runs.put(f.run,new DungeonRunRegistryData.RunRecord(f.run,"dungeon_1","minecraft:overworld",0,
                    List.of(f.level.dimension().location().toString()),1,"ACTIVE","",0,
                    List.of(f.player.getUUID(),ally.getUUID()),List.of(),List.of()));
            try{
                var merc=merc(f,"dragoon");var wolf=f.mob(EntityType.WOLF,10);wolf.setOwnerReference(EntityReference.of(ally.getUUID()));
                var outside=f.mob(EntityType.WOLF,10.01);outside.setOwnerReference(EntityReference.of(f.player.getUUID()));
                var foreign=f.mob(EntityType.WOLF,1);foreign.setOwnerReference(EntityReference.of(f.player.getUUID()));
                foreign.getPersistentData().putLong("cosmicdungeon.bogatyr_run",f.run-1);
                var wild=f.mob(EntityType.WOLF,1);var enemy=f.mob(EntityType.ZOMBIE,1);
                f.press(ClassSkillSlot.SKILL_1);
                f.check(ClassBuffs.hasted(f.player)&&ClassBuffs.hasted(ally)&&ClassBuffs.hasted(merc)&&ClassBuffs.hasted(wolf),
                        "Buff includes caster, nearby party player, admitted mercenary and allied wolf at inclusive10");
                f.check(!ClassBuffs.hasted(outsider)&&!ClassBuffs.hasted(outside)&&!ClassBuffs.hasted(foreign)
                        &&!ClassBuffs.hasted(wild)&&!ClassBuffs.hasted(enemy),"Foreign, wild, outside-radius and hostile recipients excluded");
                f.check(f.player.getEffect(ModMobEffects.FLURRY_OF_FANGS).getDuration()==36000
                        &&Math.abs(f.player.getAttributeValue(Attributes.ATTACK_SPEED)-4.4)<.00001,
                        "Flurry grants actual10percent melee speed and30minute duration");
                f.check(ClassBuffs.remaining(f.player)==6000&&f.message("Flurry of Fangs activated."),
                        "Generic skill1 action starts exact cooldown and confirms named skill");
                f.check(!ClassSkillActions.slots(f.player).get(1).enabled(),"Current server snapshot disables cooling action");
                f.press(ClassSkillSlot.SKILL_1);
                f.check(f.player.getEffect(ModMobEffects.FLURRY_OF_FANGS).getDuration()==36000,"Repeated action cannot refresh buff");
            }finally{
                for(var p:List.of(ally,outsider)){f.level.removePlayerImmediately(p,Entity.RemovalReason.DISCARDED);f.players.remove(p.getUUID());}
            }
        }h.succeed();
    }
    public static void strengthAndDefense(GameTestHelper h){
        try(var f=new ChargedClassGameTests.Fixture(h,Long.MAX_VALUE-15002,"judicator")){
            double attack=f.player.getAttributeValue(Attributes.ATTACK_DAMAGE);f.press(ClassSkillSlot.SKILL_1);
            f.check(f.player.getAttributeValue(Attributes.ATTACK_DAMAGE)==attack+3
                    &&f.player.getEffect(MobEffects.STRENGTH).getDuration()==36000,"Aegis has native3melee damage and30minutes");
            var buff=ClassBuffEffects.source(f.player,MobEffects.STRENGTH);
            f.check(buff.getStringOr("caster","").equals(f.player.getUUID().toString())&&buff.getStringOr("skill","").equals("judicators_aegis"),
                    "Successful effect keeps caster and skill provenance");
            PlayerProgressionData.get(f.level.getServer()).addSignatureProgress(f.player.getUUID(),SignatureSkill.SMITE,30,60);
            f.check(ClassBuffs.selected(f.player).orElseThrow()==ClassBuff.BULWARK&&ClassBuffs.remaining(f.player)==6000,
                    "Rank replacement retains the same cooldown");
            ready(f,f.player,ClassBuff.BULWARK);f.press(ClassSkillSlot.SKILL_1);
            f.check(f.player.getAttributeValue(Attributes.ATTACK_DAMAGE)==attack+6&&f.player.getEffect(MobEffects.STRENGTH).getAmplifier()==1,
                    "Bulwark replaces Aegis without stacking");
            f.check(!f.player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,50000,0)),
                    "Weaker native strength cannot hide under the class buff");
            f.check(!ClassBuffEffects.apply(f.player,f.player,ClassBuff.AEGIS),"Lower class rank cannot overwrite or refresh stronger");
            f.check(f.player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,5,2)),
                    "A genuinely stronger native potion may replace the family");
            f.check(ClassBuffEffects.source(f.player,MobEffects.STRENGTH).getStringOr("skill","").isBlank(),
                    "External replacement never retains stale class caster metadata");
            var active=f.player.getEffect(MobEffects.STRENGTH);
            for(int i=0;i<5;i++)active.tickServer(f.level,f.player,()->{});
            f.check(active.getDuration()==0,"Overwritten lower class buff cannot reappear after the stronger effect expires");
            f.player.removeEffect(MobEffects.STRENGTH);f.classId("dragoon");f.press(ClassSkillSlot.SKILL_1);
            f.check(f.player.getEffect(MobEffects.RESISTANCE).getDuration()==18000,"Blackiron Sheath lasts15minutes");
            f.player.setHealth(20);f.player.invulnerableTime=0;f.player.hurtServer(f.level,f.player.damageSources().generic(),10);
            f.check(Math.abs(f.player.getHealth()-12)<.0001,"Native Sheath reduces eligible damage by20percent");
            f.check(ClassBuffs.strengthMessage("Caster",ClassBuff.BULWARK.title()).getString()
                    .equals("Caster shares renewed strength for battle. Defender’s Bulwark"),"Exact strength broadcast wording");
        }h.succeed();
    }
    public static void cleanseAndRefund(GameTestHelper h){
        try(var f=new ChargedClassGameTests.Fixture(h,Long.MAX_VALUE-15003,"venefex")){
            f.player.addEffect(new MobEffectInstance(MobEffects.POISON,600,0));
            f.player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,600,0));
            f.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,600,0));
            f.player.addEffect(new MobEffectInstance(MobEffects.GLOWING,600,0));
            f.press(ClassSkillSlot.SKILL_1);
            f.check(!f.player.hasEffect(MobEffects.POISON)&&!f.player.hasEffect(MobEffects.WEAKNESS)
                    &&f.player.hasEffect(MobEffects.NIGHT_VISION)&&f.player.hasEffect(MobEffects.GLOWING),
                    "Cleanse removes harmful effects only, retaining beneficial and neutral");
            ready(f,f.player,ClassBuff.BANISH);
            f.check(!ClassBuffs.cast(f.player)&&ClassBuffs.remaining(f.player)==0,"No-op cleanse consumes no cooldown");
            f.player.addEffect(new MobEffectInstance(MobEffects.POISON,600,0));
            java.util.function.Consumer<MobEffectEvent.Remove> deny=e->{if(e.getEntity()==f.player)e.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(deny);
            try{f.check(!ClassBuffs.cast(f.player)&&ClassBuffs.remaining(f.player)==0&&f.player.hasEffect(MobEffects.POISON),
                    "Wholly canceled cleanse refunds and preserves effects");}
            finally{NeoForge.EVENT_BUS.unregister(deny);}
            f.classId("bogatyr");
            java.util.function.Consumer<MobEffectEvent.Applicable> denyBuff=e->{if(e.getEntity()==f.player)e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);};
            NeoForge.EVENT_BUS.addListener(denyBuff);
            try{f.check(!ClassBuffs.cast(f.player)&&ClassBuffs.remaining(f.player)==0,"Wholly denied buff cannot spend cooldown");}
            finally{NeoForge.EVENT_BUS.unregister(denyBuff);}
            String cooldown=ClassBuffs.cooldownKey("player:"+f.player.getUUID(),"bogatyr");
            var data=D1RunData.get(f.level.getServer());
            data.setValue(f.run,cooldown,"0"); // An expired record must not be resurrected after cleanup.
            java.util.function.Consumer<MobEffectEvent.Applicable> retire=e->{
                if(e.getEntity()==f.player){data.clearRun(f.run);e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);}
            };
            NeoForge.EVENT_BUS.addListener(retire);
            try{f.check(!ClassBuffs.cast(f.player)&&data.values(f.run,cooldown).isEmpty(),
                    "Canceled callback cannot recreate a cleared run reservation");}
            finally{NeoForge.EVENT_BUS.unregister(retire);}
            java.util.function.Consumer<MobEffectEvent.Applicable> replace=e->{
                if(e.getEntity()==f.player){data.setValue(f.run,cooldown,"future");e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);}
            };
            NeoForge.EVENT_BUS.addListener(replace);
            try{f.check(!ClassBuffs.cast(f.player)&&data.values(f.run,cooldown).equals(List.of("future")),
                    "Canceled callback cannot overwrite newer persisted state");}
            finally{NeoForge.EVENT_BUS.unregister(replace);}

        }h.succeed();
    }
    public static void persistenceAndAuthority(GameTestHelper h){
        try(var f=new ChargedClassGameTests.Fixture(h,Long.MAX_VALUE-15004,"bogatyr")){
            f.press(ClassSkillSlot.SKILL_1);var data=D1RunData.get(f.level.getServer());data.setValue(f.run,"future_buff_field","retained");
            f.check(data.flushVerified(),"Save native cooldown");
            data=DurableRecoveryGameTestSupport.reload(f.level.getServer(),D1RunData.class,"cosmicdungeon_d1_objectives_v1");
            f.check(ClassBuffs.remaining(f.player)==6000&&data.values(f.run,"future_buff_field").equals(List.of("retained")),
                    "Native reload retains cooldown and unknown run fields");
            f.player.setHealth(0);f.check(!ClassBuffs.cast(f.player),"Dead player cannot cast");
            f.player.setHealth(20);f.check(ClassBuffs.remaining(f.player)==6000,"Recovery does not clear cooldown");
            f.classId("dragoon");f.check(ClassBuffs.remaining(f.player)==0,"Different class has its own cooldown");
            f.classId("bogatyr");f.check(ClassBuffs.remaining(f.player)==6000,"Returning class cannot bypass prior cooldown");
            var wolf=f.mob(EntityType.WOLF,1);wolf.setOwnerReference(EntityReference.of(f.player.getUUID()));
            ClassBuffEffects.apply(f.player,wolf,ClassBuff.FLURRY);
            var root=wolf.getPersistentData().getCompoundOrEmpty(ClassBuffEffects.SOURCE);
            root.getCompoundOrEmpty(ClassBuffEffects.key(ModMobEffects.FLURRY_OF_FANGS)).putString("future","kept");
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,f.level.registryAccess());wolf.saveWithoutId(out);
            var restored=EntityType.WOLF.create(f.level,EntitySpawnReason.COMMAND);
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING,f.level.registryAccess(),out.buildResult()));
            f.check(restored.hasEffect(ModMobEffects.FLURRY_OF_FANGS)
                    &&ClassBuffEffects.source(restored,ModMobEffects.FLURRY_OF_FANGS).getStringOr("name","").equals(f.player.getName().getString())
                    &&ClassBuffEffects.source(restored,ModMobEffects.FLURRY_OF_FANGS).getStringOr("future","").equals("kept"),
                    "Native entity save/load retains buff, source and unknown metadata");
            String key=ClassBuffs.cooldownKey("player:"+f.player.getUUID(),"bogatyr");
            data.setValue(f.run,key,"future-shape");
            f.check(!ClassBuffs.cast(f.player)&&ClassBuffs.remaining(f.player)==6000,"Malformed timer preserved and blocked");
            data.clearRun(f.run);f.check(ClassBuffs.remaining(f.player)==0,"Normal run cleanup clears cooldown");
        }h.succeed();
    }
    public static void mercenaries(GameTestHelper h){
        try(var f=new ChargedClassGameTests.Fixture(h,Long.MAX_VALUE-15005,"theurgist")){
            var bogatyr=merc(f,"bogatyr");var dragoon=merc(f,"dragoon");var judicator=merc(f,"judicator");var venefex=merc(f,"venefex");
            f.player.addEffect(new MobEffectInstance(MobEffects.POISON,600,0));
            for(var m:List.of(bogatyr,dragoon,judicator,venefex))ClassBuffs.tick(m);
            f.check(ClassBuffs.hasted(f.player)&&f.player.hasEffect(MobEffects.RESISTANCE)
                    &&f.player.hasEffect(MobEffects.STRENGTH)&&!f.player.hasEffect(MobEffects.POISON),
                    "All four mercenary classes use the same useful party buffs automatically");
            for(var m:List.of(bogatyr,dragoon,judicator,venefex))f.check(ClassBuffs.remaining(m)==6000,"Mercenary success owns5minute cooldown");
            f.check(ClassBuffs.selected(merc(f,"pyroclast")).isEmpty()&&ClassBuffs.selected(merc(f,"theurgist")).isEmpty(),
                    "Pyroclast receives no new buff; Theurgist skills remain separate");
            f.player.setHealth(0);f.check(!ClassBuffs.cast(bogatyr),"Unavailable hirer cannot trigger mercenary buff");
            f.player.setHealth(20);f.check(ClassBuffs.remaining(bogatyr)==6000,"Hirer recovery retains mercenary cooldown");
        }h.succeed();
    }
}
