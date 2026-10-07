package net.goui.cosmicdungeon.mercenary;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Existing registered stock GameTest also exercises real native splash callbacks and run credit. */
final class MercenaryPotionRoleGameTests {
    private MercenaryPotionRoleGameTests(){}
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;final ServerLevel level;final long id;
        final ServerPlayer player;final MercenaryContract contract;final MercenaryEntity merc;
        final D1RunData data;final Vec3 origin;
        final Map<Long,DungeonRunRegistryData.RunRecord> runs;final Map<UUID,ServerPlayer> players;
        final List<Mob> enemies=new ArrayList<>();
        @SuppressWarnings("unchecked") Fixture(GameTestHelper helper,String role,long id){
            this.helper=helper;this.id=id;level=helper.getLevel();var server=level.getServer();
            origin=helper.absoluteVec(new Vec3(.5,10,.5));
            player=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"PotionRoleTest"));
            player.setPos(origin);player.setHealth(player.getMaxHealth());
            contract=new MercenaryContract(UUID.randomUUID(),player.getUUID(),role,2,50);
            var run=new DungeonRunRegistryData.RunRecord(id,"dungeon_1","minecraft:overworld",0,
                    List.of(level.dimension().location().toString()),1,"ACTIVE","",0,List.of(player.getUUID()),List.of(),List.of())
                    .withMercenaries(List.of(contract));
            var registry=DungeonRunRegistryData.get(server);data=D1RunData.get(server);
            try{
                var rf=DungeonRunRegistryData.class.getDeclaredField("runsById");rf.setAccessible(true);
                runs=(Map<Long,DungeonRunRegistryData.RunRecord>)rf.get(registry);
                var pf=PlayerList.class.getDeclaredField("playersByUUID");pf.setAccessible(true);
                players=(Map<UUID,ServerPlayer>)pf.get(server.getPlayerList());
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Native fixture unavailable",error);}
            if(runs.containsKey(id)||players.containsKey(player.getUUID()))throw new IllegalStateException("Fixture collision");
            runs.put(id,run);players.put(player.getUUID(),player);
            merc=new MercenaryEntity(ModEntities.MERCENARY.get(),level);merc.initialize(id,contract);
            merc.setNoAi(true);merc.setPos(origin);level.addFreshEntity(merc);
            check(MercenaryBrain.hirer(merc)==player,"Native fixture has an admitted living hirer");
        }
        <T extends Mob> T enemy(EntityType<T> type,double x){
            var mob=type.create(level,EntitySpawnReason.COMMAND);
            check(mob!=null,"Native hostile created");mob.setNoAi(true);mob.setPos(origin.add(x,0,0));
            level.addFreshEntity(mob);enemies.add(mob);return mob;
        }
        ThrownSplashPotion shot(ItemStack stack){
            var shot=new ThrownSplashPotion(level,merc,stack);MercenaryPotions.mark(shot,merc);return shot;
        }
        void splash(ItemStack stack,LivingEntity target){
            var shot=shot(stack);shot.setPos(target.position());
            shot.onHitAsPotion(level,stack,new EntityHitResult(target));
        }
        int count(MercenarySkill skill){return data.count(id,MercenarySkills.key(contract.id(),skill));}
        void check(boolean condition,String message){helper.assertTrue(condition,Component.literal(message));}
        @Override public void close(){
            enemies.forEach(Entity::discard);merc.discard();players.remove(player.getUUID());
            level.removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);
            runs.remove(id);data.clearRun(id);
        }
    }
    static void verify(GameTestHelper helper){
        try(var f=new Fixture(helper,"theurgist",Long.MAX_VALUE-1701)){
            var enemy=f.enemy(EntityType.SPIDER,1);enemy.setHealth(10);f.merc.setHealth(4);
            f.splash(MercenaryBrewing.create(true,0,false),f.merc);
            f.check(f.merc.getHealth()>4&&enemy.getHealth()==10,"Theurgist heals ally without helping enemies");
            f.check(f.count(MercenarySkill.POSITIVE_POTIONS)==1,"Effective heal earns one positive success");
            f.merc.setHealth(f.merc.getMaxHealth());f.splash(MercenaryBrewing.create(true,0,false),f.merc);
            f.check(f.count(MercenarySkill.POSITIVE_POTIONS)==1,"Full-health no-op earns nothing");
            f.splash(MercenaryBrewing.create(false,1,false),enemy);
            f.check(!enemy.hasEffect(MobEffects.POISON)&&f.count(MercenarySkill.NEGATIVE_POTIONS)==0,
                    "Legacy negative Theurgist splash cannot apply or earn");
            var undead=f.enemy(EntityType.ZOMBIE,2);float health=undead.getHealth();
            f.splash(MercenaryBrewing.create(true,0,false),undead);
            f.check(undead.getHealth()==health,"Positive role cannot damage undead through inversion");
            f.check(MercenarySkills.snapshot(f.level.getServer(),f.id,f.contract).size()==1,
                    "Theurgist exposes only its positive skill");
        }
        try(var f=new Fixture(helper,"venefex",Long.MAX_VALUE-1702)){
            var first=f.enemy(EntityType.CREEPER,1);var second=f.enemy(EntityType.CREEPER,2);
            var immune=f.enemy(EntityType.ZOMBIE,3);
            f.check(MercenarySkills.level(f.merc,MercenarySkill.NEGATIVE_POTIONS)==0,"Venefex starts at level zero");
            var missed=f.shot(MercenaryBrewing.create(false,0,false));missed.setPos(f.origin.add(0,20,0));
            missed.onHitAsPotion(f.level,missed.getItem(),new BlockHitResult(missed.position(),
                    net.minecraft.core.Direction.UP,net.minecraft.core.BlockPos.containing(missed.position()),false));
            f.check(f.count(MercenarySkill.NEGATIVE_POTIONS)==0,"A native splash miss earns nothing");
            first.setInvulnerable(true);second.setInvulnerable(true);
            f.splash(MercenaryBrewing.create(false,0,false),first);
            f.check(f.count(MercenarySkill.NEGATIVE_POTIONS)==0,"Immune damage and undead inversion earn nothing");
            first.setInvulnerable(false);second.setInvulnerable(false);
            first.setHealth(10);second.setHealth(10);
            float ally=f.merc.getHealth();f.splash(MercenaryBrewing.create(false,0,false),first);
            f.check(first.getHealth()<10&&second.getHealth()<10&&f.merc.getHealth()==ally,"Harm reaches enemies but spares allies");
            f.check(f.count(MercenarySkill.NEGATIVE_POTIONS)==1
                    &&MercenarySkills.level(f.merc,MercenarySkill.NEGATIVE_POTIONS)==1,"Two wounded targets credit one successful cast");
            var slow=net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                    net.minecraft.world.item.Items.SPLASH_POTION,net.minecraft.world.item.alchemy.Potions.SLOWNESS);
            f.splash(slow,first);
            f.check(first.hasEffect(MobEffects.SLOWNESS)&&f.count(MercenarySkill.NEGATIVE_POTIONS)==2,
                    "Effective non-damaging debuff earns one cast success");
            f.splash(slow,first);
            f.check(f.count(MercenarySkill.NEGATIVE_POTIONS)==2,"Unchanged native debuff earns nothing");
            first.invulnerableTime=0;second.invulnerableTime=0;
            first.setHealth(10);second.setHealth(10);
            f.splash(MercenaryBrewing.create(false,1,false),first);
            f.check(!immune.hasEffect(MobEffects.POISON)&&f.count(MercenarySkill.NEGATIVE_POTIONS)==2,
                    "Poison immunity and application alone earn no damage credit");
            var poison=first.getEffect(MobEffects.POISON);f.check(poison!=null,"Native poison installed");
            for(int i=0;i<30&&f.count(MercenarySkill.NEGATIVE_POTIONS)==2;i++)poison.tickServer(f.level,first,()->{});
            var other=second.getEffect(MobEffects.POISON);f.check(other!=null,"Second native poison installed");
            for(int i=0;i<30;i++)other.tickServer(f.level,second,()->{});
            f.check(f.count(MercenarySkill.NEGATIVE_POTIONS)==3
                    &&MercenarySkills.level(f.merc,MercenarySkill.NEGATIVE_POTIONS)==2,"Actual poison damage credits once across all targets/ticks");
            f.merc.setHealth(4);f.splash(MercenaryBrewing.create(true,0,false),f.merc);
            f.check(f.merc.getHealth()==4&&f.count(MercenarySkill.POSITIVE_POTIONS)==0,"Venefex cannot heal or earn positive skill");
            f.check(MercenarySkills.snapshot(f.level.getServer(),f.id,f.contract).getFirst().id().equals("negative_potions"),
                    "Venefex exposes its negative-only skill");
            f.merc.die(f.merc.damageSources().generic());f.merc.resumeAfterRest();
            f.check(MercenarySkills.level(f.merc,MercenarySkill.NEGATIVE_POTIONS)==2,"Death/revival retains Venefex progress");
            var next=new MercenaryEntity(ModEntities.MERCENARY.get(),f.level);next.initialize(f.id+1,f.contract);
            f.check(MercenarySkills.level(next,MercenarySkill.NEGATIVE_POTIONS)==0,"A new run resets Venefex to zero");
        }
    }
}
