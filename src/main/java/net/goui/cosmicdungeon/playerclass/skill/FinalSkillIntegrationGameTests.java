package net.goui.cosmicdungeon.playerclass.skill;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.effect.ModMobEffects;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.mercenary.*;
import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.npc.tamsin.*;
import net.goui.cosmicdungeon.playerclass.bogatyr.*;
import net.goui.cosmicdungeon.playerclass.d1.D1RocketAbilities;
import net.goui.cosmicdungeon.playerclass.theurgist.VitalityPotions;
import net.goui.cosmicdungeon.progression.PlayerProgressionData;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/** Final native boundaries: actual successful earning, party display state and durable winning attribution. */
public final class FinalSkillIntegrationGameTests {
    private FinalSkillIntegrationGameTests() {}
    private static MercenaryEntity merc(ChargedClassGameTests.Fixture f,String cls) {
        var contract=new MercenaryContract(UUID.randomUUID(),f.player.getUUID(),cls,2,50);
        f.runs.put(f.run,f.runs.get(f.run).withMercenaries(List.of(contract)));
        var merc=new MercenaryEntity(ModEntities.MERCENARY.get(),f.level);
        merc.initialize(f.run,contract);merc.setNoAi(true);merc.setPos(f.origin.add(1,0,0));f.entities.add(merc);
        f.check(f.level.addFreshEntity(merc)&&MercenaryBrain.hirer(merc)==f.player,"Admitted single-hire presentation fixture");
        return merc;
    }
    public static void presentation(GameTestHelper h) {
        int sequence=0;
        for(var skill:SignatureSkill.values())try(var f=new ChargedClassGameTests.Fixture(h,Long.MAX_VALUE-17001-sequence++,skill.classId())) {
            int progress=(int)skill.threshold(2,ClassSkillConfig.BASE.get(),ClassSkillConfig.STEP.get());
            PlayerProgressionData.get(f.level.getServer()).addSignatureProgress(f.player.getUUID(),skill,progress,Integer.MAX_VALUE);
            var merc=merc(f,skill.classId());var data=D1RunData.get(f.level.getServer());
            if(skill==SignatureSkill.SMITE)data.setCount(f.run,SignatureSkills.smiteKey(merc.contract().id()),10);
            else for(int i=0;i<skill.legacy().thresholdFor(2);i++)
                f.check(MercenarySkills.success(merc,skill.legacy()),"Validated mercenary success reader setup");
            for(var actor:List.<LivingEntity>of(f.player,merc)) {
                var shown=PartyVitalsSnapshot.capture(actor,true);
                f.check(shown.skills().abbreviation().equals(skill.abbreviation())&&shown.skills().level()==2,
                        "Actual stored signature level for "+skill.classId()+" / "+actor.getType());
                f.check(shown.sampledAt()==f.level.getServer().overworld().getGameTime(),"Snapshot carries server clock anchor");
                f.check(PartyVitalsSnapshot.capture(actor,false).skills().level()==2
                        &&PartyVitalsSnapshot.capture(actor,false).skills().abilities().isEmpty(),"Unavailable actors retain identity only");
            }
            if(ChargedSkillState.limit(skill)>0) {
                f.fill(skill);
                if(skill==SignatureSkill.CHAIN_LIGHTNING)f.check(ChargedClassSkills.arm(f.player),"Real lightning armed state");
                var primary=PartySkillsSnapshot.capture(f.player,true).abilities().getFirst();
                f.check(primary.charge()==ChargedSkillState.limit(skill)&&primary.maxCharge()==ChargedSkillState.limit(skill)
                        &&primary.armed()==(skill==SignatureSkill.CHAIN_LIGHTNING),"Charge snapshot is separate from proficiency");
            }
            if(ClassBuffs.selected(merc).isPresent()) {
                if(skill==SignatureSkill.NEGATIVE_POTIONS)f.player.addEffect(new MobEffectInstance(MobEffects.POISON,600));
                f.check(ClassBuffs.cast(merc),"Useful native automatic-class buff");
                var buff=PartySkillsSnapshot.capture(merc,true).abilities().stream().filter(a->a.cooldownTicks()>0).findFirst().orElseThrow();
                f.check(buff.cooldownTicks()==6000&&buff.readyAt()-f.level.getServer().overworld().getGameTime()==6000,
                        "Five-minute mercenary cooldown appears independently of effect duration");
            }
        }
        h.succeed();
    }
    private static ThrownSplashPotion splash(ChargedClassGameTests.Fixture f,Holder<Potion> potion,LivingEntity target) {
        var stack=PotionContents.createItemStack(Items.SPLASH_POTION,potion);
        var shot=new ThrownSplashPotion(f.level,f.player,stack);shot.setPos(target.position());f.entities.add(shot);
        f.check(f.level.addFreshEntity(shot),"Native potion admitted before impact");return shot;
    }
    private static void impact(ChargedClassGameTests.Fixture f,ThrownSplashPotion shot,LivingEntity target) {
        shot.onHitAsPotion(f.level,shot.getItem(),new EntityHitResult(target));
    }
    public static void progression(GameTestHelper h) {
        try(var f=new ChargedClassGameTests.Fixture(h,Long.MAX_VALUE-17011,"venefex")) {
            var first=f.mob(EntityType.CREEPER,1);var second=f.mob(EntityType.CREEPER,2);
            var slow=splash(f,Potions.SLOWNESS,first);impact(f,slow,first);
            f.check(first.hasEffect(MobEffects.SLOWNESS)&&second.hasEffect(MobEffects.SLOWNESS)
                    &&SignatureSkills.playerProgress(f.player,SignatureSkill.NEGATIVE_POTIONS)==1,
                    "One real multi-target slowness splash earns one negative-potion success");
            first.removeEffect(MobEffects.SLOWNESS);second.removeEffect(MobEffects.SLOWNESS);
            impact(f,slow,first);
            f.check(SignatureSkills.playerProgress(f.player,SignatureSkill.NEGATIVE_POTIONS)==1,"Repeated splash callback cannot earn twice");
            first.removeEffect(MobEffects.SLOWNESS);second.removeEffect(MobEffects.SLOWNESS);
            java.util.function.Consumer<MobEffectEvent.Applicable> deny=e->{
                if(e.getEntity()==first||e.getEntity()==second)e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            };
            NeoForge.EVENT_BUS.addListener(deny);
            try { impact(f,splash(f,Potions.SLOWNESS,first),first); }
            finally { NeoForge.EVENT_BUS.unregister(deny); }
            f.check(SignatureSkills.playerProgress(f.player,SignatureSkill.NEGATIVE_POTIONS)==1,"Native rejected effects earn nothing");
            var harm=splash(f,Potions.HARMING,first);float before=first.getHealth();impact(f,harm,first);
            f.check(first.getHealth()<before&&SignatureSkills.playerProgress(f.player,SignatureSkill.NEGATIVE_POTIONS)==2,
                    "Accepted instantaneous damage earns once");
            first.invulnerableTime=second.invulnerableTime=0;impact(f,harm,first);
            f.check(SignatureSkills.playerProgress(f.player,SignatureSkill.NEGATIVE_POTIONS)==2,"Repeated damage from one launch earns once");
            var poison=splash(f,Potions.POISON,first);impact(f,poison,first);
            f.check(first.hasEffect(MobEffects.POISON)&&SignatureSkills.playerProgress(f.player,SignatureSkill.NEGATIVE_POTIONS)==2,
                    "Poison application waits for actual damage");
            var active=first.getEffect(MobEffects.POISON);first.setInvulnerable(true);
            for(int i=0;i<30;i++)active.tickServer(f.level,first,()->{});
            f.check(SignatureSkills.playerProgress(f.player,SignatureSkill.NEGATIVE_POTIONS)==2,"Denied poison ticks cannot train");
            first.setInvulnerable(false);
            for(int i=0;i<30;i++){first.invulnerableTime=0;active.tickServer(f.level,first,()->{});}
            f.check(SignatureSkills.playerProgress(f.player,SignatureSkill.NEGATIVE_POTIONS)==3,"First damaging poison tick trains once");
            active=second.getEffect(MobEffects.POISON);
            for(int i=0;i<30;i++){second.invulnerableTime=0;active.tickServer(f.level,second,()->{});}
            f.check(SignatureSkills.playerProgress(f.player,SignatureSkill.NEGATIVE_POTIONS)==3,"Shared poison action prevents cross-recipient double credit");
            f.classId("theurgist");var denied=splash(f,Potions.WEAKNESS,first);f.classId("venefex");impact(f,denied,first);
            f.check(SignatureSkills.playerProgress(f.player,SignatureSkill.NEGATIVE_POTIONS)==3,"A later class switch cannot authorize an old launch");
        }
        try(var f=new ChargedClassGameTests.Fixture(h,Long.MAX_VALUE-17012,"pyroclast")) {
            var target=f.mob(EntityType.CREEPER,1);float before=target.getHealth();
            var rocket=new FireworkRocketEntity(f.level,new ItemStack(Items.FIREWORK_ROCKET),f.player,
                    f.origin.x,f.origin.y,f.origin.z,true);f.entities.add(rocket);
            f.check(f.level.addFreshEntity(rocket),"Native firework admitted");
            f.check(D1RocketAbilities.explode(rocket,f.level)&&target.getHealth()<before
                    &&SignatureSkills.playerProgress(f.player,SignatureSkill.FIREWORKS)==1,"Real firework damage trains its signature");
            target.invulnerableTime=0;D1RocketAbilities.explode(rocket,f.level);
            f.check(SignatureSkills.playerProgress(f.player,SignatureSkill.FIREWORKS)==1,"Spent rocket cannot train twice");
        }
        try(var f=new ChargedClassGameTests.Fixture(h,Long.MAX_VALUE-17013,"bogatyr")) {
            var wolf=EntityType.WOLF.create(f.level,EntitySpawnReason.COMMAND);
            f.check(wolf!=null,"Wolf created");wolf.tame(f.player);wolf.setNoAi(true);wolf.setPos(f.origin.add(1,0,0));f.entities.add(wolf);
            try {
                f.check(f.level.addFreshEntity(wolf)&&BogatyrWolfEvents.managed(wolf)
                        &&SignatureSkills.playerProgress(f.player,SignatureSkill.WOLVES)==1,"Actual admitted player wolf trains once");
                BogatyrCompanions.added(wolf);BogatyrWolfEvents.added(wolf);
                f.check(SignatureSkills.playerProgress(f.player,SignatureSkill.WOLVES)==1,"Repeated enrollment preserves one success");
            } finally {
                // Observed final removal cleans this fixture without a permanent run tombstone, even after a failed assertion.
                wolf.setRemoved(Entity.RemovalReason.KILLED);
            }
        }
        h.succeed();
    }
    private static PartyVitals.Effect effect(LivingEntity entity,String id) {
        return PartyVitalsSnapshot.capture(entity,true).effects().stream().filter(e->e.id().equals(id)).findFirst().orElseThrow();
    }
    public static void provenance(GameTestHelper h) {
        try(var f=new ChargedClassGameTests.Fixture(h,Long.MAX_VALUE-17021,"theurgist")) {
            var wolf=f.mob(EntityType.WOLF,1);var other=f.mob(EntityType.COW,3);other.setCustomName(Component.literal("OtherCaster"));
            String caster=f.player.getName().getString();
            f.check(VitalityPotions.apply(wolf,new MobEffectInstance(ModMobEffects.VITALITY,100,1),f.player),"Known vitality applied");
            var vitality=effect(wolf,ClassBuffEffects.key(ModMobEffects.VITALITY));
            f.check(vitality.displayName().equals("Favilla Vitas")&&vitality.amount().equals("+15% maximum health")
                    &&vitality.caster().equals(caster),"Vitality snapshot carries exact rank amount and winner");
            f.check(!VitalityPotions.apply(wolf,new MobEffectInstance(ModMobEffects.VITALITY,100,0),other)
                    &&effect(wolf,ClassBuffEffects.key(ModMobEffects.VITALITY)).equals(vitality),"Weaker vitality cannot refresh or steal attribution");
            f.check(ClassBuffEffects.apply(f.player,wolf,ClassBuff.BULWARK),"Winning class buff applied");
            var strength=effect(wolf,ClassBuffEffects.key(MobEffects.STRENGTH));
            f.check(strength.displayName().equals(ClassBuff.BULWARK.title())&&strength.amount().equals("+6 melee damage")
                    &&strength.caster().equals(caster),"Class buff snapshot carries amount and caster");
            f.check(!ClassBuffEffects.apply(other,wolf,ClassBuff.AEGIS)
                    &&effect(wolf,ClassBuffEffects.key(MobEffects.STRENGTH)).equals(strength),"Weaker class rank cannot steal metadata");
            wolf.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,1000,2,false,false,false),f.player);
            String slow=ClassBuffEffects.key(MobEffects.SLOWNESS);
            var provenance=wolf.getPersistentData().getCompoundOrEmpty(PartyEffectSource.KEY);
            provenance.putString("future_root","retained");
            var source=provenance.getCompoundOrEmpty(slow);source.putString("future","retained");
            // An unloaded entity pauses effect duration while world time advances; persisted end is stale on return.
            source.putLong("end",source.getLongOr("end",0)-200);
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,f.level.registryAccess());wolf.saveWithoutId(output);
            var restored=EntityType.WOLF.create(f.level,EntitySpawnReason.COMMAND);
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING,f.level.registryAccess(),output.buildResult()));
            f.check(effect(restored,slow).caster().equals(caster)
                    &&restored.getPersistentData().getCompoundOrEmpty(PartyEffectSource.KEY).getStringOr("future_root","").equals("retained")
                    &&restored.getPersistentData().getCompoundOrEmpty(PartyEffectSource.KEY).getCompoundOrEmpty(slow).getStringOr("future","").equals("retained"),
                    "Native save/load preserves current caster and unknown metadata");
            restored.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,2000,0,false,true,true),other);
            f.check(restored.getEffect(MobEffects.SLOWNESS).getAmplifier()==2
                    &&effect(restored,slow).caster().equals(caster),"Weaker cosmetic update after reload cannot steal winner provenance");
            other.addEffect(new MobEffectInstance(MobEffects.LUCK,1000,0));
            f.check(effect(other,ClassBuffEffects.key(MobEffects.LUCK)).caster().equals("Unknown"),"Source-less effect never guesses a caster");
            other.getPersistentData().remove(PartyEffectSource.KEY);
            f.check(effect(other,ClassBuffEffects.key(MobEffects.LUCK)).caster().equals("Unknown"),"Legacy missing provenance remains Unknown");
        }
        h.succeed();
    }
}
