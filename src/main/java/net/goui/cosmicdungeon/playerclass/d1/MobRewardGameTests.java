package net.goui.cosmicdungeon.playerclass.d1;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import java.util.*;
import net.goui.cosmicdungeon.auth.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.dungeon.d1.D1LifetimeData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.scores.Objective;
import net.goui.cosmicdungeon.economy.*;
import net.goui.cosmicdungeon.gametest.DurableRecoveryGameTestSupport;
import net.goui.cosmicdungeon.npc.tamsin.TamsinData;
import net.goui.cosmicdungeon.vendor.VendorBindingRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Native registry, actual death callbacks, group accounts and replay receipts in synchronous CI fixtures. */
public final class MobRewardGameTests {
    private static final String ORIGIN="cosmic_spawner_-12_64_300";
    private static final Map<String,Long> VALUES=Map.of("weak",2L,"common",3L,"tough",4L,"elite",6L,"miniboss",10L,"boss",100L);
    private MobRewardGameTests() {}
    private static void check(GameTestHelper helper,boolean value,String message){
        helper.assertTrue(value,Component.literal(message));
    }
    public static void coverage(GameTestHelper helper){
        var supported=new TreeSet<String>();var excluded=new TreeSet<String>();
        for(var type:BuiltInRegistries.ENTITY_TYPE){
            var id=BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if(Set.of("minecraft","cosmicdungeon").contains(id.getNamespace())&&type.getCategory()==MobCategory.MONSTER)
                supported.add(id.toString());
            else excluded.add(id.toString());
        }
        check(helper,supported.stream().filter(id->id.startsWith("minecraft:")).count()==40,
                "Every current vanilla MONSTER must be reviewed; expected 40, found "+supported);
        check(helper,supported.stream().filter(id->id.startsWith("cosmicdungeon:")).count()==5,
                "Every Cosmic Dungeon MONSTER must be reviewed; expected five");
        var registered=D1MobRewardCatalog.registrations().keySet();
        var missing=new TreeSet<>(supported);missing.removeAll(registered);
        var extra=new TreeSet<>(registered);extra.removeAll(supported);
        check(helper,missing.isEmpty()&&extra.isEmpty(),"Exact supported registry coverage; missing="+missing+", extra="+extra);
        for(String id:supported){
            var reward=D1MobRewardRules.resolve(id,Set.of(ORIGIN),new CompoundTag(),
                    D1MobRewardCatalog.defaults(),List.of(),VALUES);
            check(helper,reward.valid()&&reward.trace()>0,"Registered native type has its configured reward: "+id);
        }
        for(String id:excluded)check(helper,!registered.contains(id),"Pets, MISC and other namespaces are not inferred rewards: "+id);
        helper.succeed();
    }
    @SuppressWarnings("unchecked") private static <T> T field(Class<?> owner,String name){
        try{var field=owner.getDeclaredField(name);field.setAccessible(true);return (T)field.get(null);}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static Codec<PlayerCurrencyData> codec(){return field(PlayerCurrencyData.class,"CODEC");}
    private static CompoundTag image(PlayerCurrencyData data){
        return (CompoundTag)DurableRecoveryGameTestSupport.image(PlayerCurrencyData.class,data);
    }
    private static final class Rewards implements AutoCloseable {
        final CosmicSpawnerGameTests.Fixture f;
        final PlayerCurrencyData previousAccounts;
        final boolean previousDirty;
        final List<? extends String> previousMobs,previousSpawners;
        final double previousRadius;
        final Map<String,Long> previousValues=new HashMap<>();
        final List<Mob> mobs=new ArrayList<>();
        final ServerPlayer member;
        final D1LifetimeData previousLifetime;
        final boolean previousLifetimeDirty,previousTamsinDirty;
        final SavedDataType<D1LifetimeData> lifetimeType=field(D1LifetimeData.class,"TYPE");
        final Set<String> warnings=field(DungeonGroupSplitService.class,"WARNED");
        final Set<String> previousWarnings;
        final Objective previousObjective;
        final Integer previousScore;
        Rewards(CosmicSpawnerGameTests.Fixture fixture){
            f=fixture;var server=f.level.getServer();
            previousAccounts=PlayerCurrencyData.get(server);previousDirty=previousAccounts.isDirty();
            previousMobs=D1EconomyConfig.MOB_CATEGORIES.get();previousSpawners=D1EconomyConfig.SPAWNER_REWARDS.get();
            previousRadius=D1EconomyConfig.REWARD_RADIUS.get();
            D1EconomyConfig.REWARDS.forEach((key,value)->previousValues.put(key,value.get()));
            previousLifetime=D1LifetimeData.get(server);previousLifetimeDirty=previousLifetime.isDirty();
            previousTamsinDirty=TamsinData.get(server).isDirty();previousWarnings=Set.copyOf(warnings);
            previousObjective=server.getScoreboard().getObjective("d1_kills");
            var score=previousObjective==null?null:server.getScoreboard().getPlayerScoreInfo(f.player,previousObjective);
            previousScore=score==null?null:score.value();member=member(f);
        }
        void initialize(){
            var server=f.level.getServer();
            f.players.put(member.getUUID(),member);
            // Install isolated real journals only after try-with-resources owns cleanup; never flush fixture data.
            Codec<D1LifetimeData> lifetimeCodec=field(D1LifetimeData.class,"CODEC");
            var lifetimeCopy=lifetimeCodec.parse(NbtOps.INSTANCE,
                    DurableRecoveryGameTestSupport.image(D1LifetimeData.class,previousLifetime)).getOrThrow();
            server.overworld().getDataStorage().set(lifetimeType,lifetimeCopy);
            server.overworld().getDataStorage().set(PlayerCurrencyData.TYPE,codec().parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow());
            accounts().setCapacityTrace(f.player.getUUID(),100000);accounts().setCapacityTrace(member.getUUID(),100000);
            D1EconomyConfig.MOB_CATEGORIES.set(D1MobRewardCatalog.defaults().subList(0,27));
            D1EconomyConfig.SPAWNER_REWARDS.set(List.of());D1EconomyConfig.REWARD_RADIUS.set(60.0);
            VALUES.forEach((key,value)->D1EconomyConfig.REWARDS.get(key).set(value));
            var old=f.runs.get(f.id);
            f.runs.put(f.id,new DungeonRunRegistryData.RunRecord(f.id,old.dungeonId(),old.selectorDimensionId(),old.selectorPosLong(),
                    old.dungeonDimensionIds(),old.instanceSlot(),old.state(),old.resetReason(),old.startedAtEpochMillis(),
                    List.of(f.player.getUUID(),member.getUUID()),List.of(),List.of()));
        }
        PlayerCurrencyData accounts(){return PlayerCurrencyData.get(f.level.getServer());}
        long balance(ServerPlayer player){return accounts().getBalanceTrace(player.getUUID());}
        Mob mob(String id){
            var type=BuiltInRegistries.ENTITY_TYPE.getValue(ResourceLocation.parse(id));
            var entity=type.create(f.level,EntitySpawnReason.COMMAND);
            f.check(entity instanceof Mob,"Native reward fixture is a Mob: "+id);
            var mob=(Mob)entity;mob.setPos(f.origin);mob.setNoAi(true);mob.addTag(ORIGIN);mobs.add(mob);return mob;
        }
        void award(Mob mob){DungeonGroupSplitService.onMobKilled(mob,f.player.getUUID());}
        void unchanged(Runnable action,String label){
            var before=image(accounts());action.run();f.check(before.equals(image(accounts())),label);
        }
        void excluded(Mob mob,UUID credit,String label){
            var before=D1RunData.get(f.level.getServer()).values(f.id,"reward_mobs");
            unchanged(()->DungeonGroupSplitService.onMobKilled(mob,credit),label);
            f.check(before.equals(D1RunData.get(f.level.getServer()).values(f.id,"reward_mobs")),
                    label+" does not consume a reward identity");
        }
        void shares(Mob mob,long first,long second){
            String transaction="mob:"+f.id+":"+mob.getUUID();var rows=image(accounts()).getCompoundOrEmpty("ledger").getCompoundOrEmpty("outbox");
            var expected=Map.of(f.player.getUUID(),first,member.getUUID(),second);int found=0;
            for(String key:rows.keySet()){
                var row=rows.getCompoundOrEmpty(key);
                if(!row.getStringOr("transaction","").equals(transaction))continue;
                found++;EconomyLedger.validateRow(row);
                var owner=UUID.fromString(row.getStringOr("player",""));Long amount=expected.get(owner);
                f.check(amount!=null&&row.getLongOr("requested_trace",-1)==amount
                        &&row.getLongOr("after",-1)-row.getLongOr("before",-1)==amount,
                        "Ledger records each exact group share");
                f.check(row.getStringOr("type","").equals("dungeon_mob")&&row.getStringOr("category","").equals("generation")
                        &&row.getLongOr("run",-1)==f.id&&row.getStringOr("related","").equals(mob.getStringUUID())
                        &&row.getStringOr("status","").equals("committed"),"Ledger retains native mob/run provenance");
                f.check(accounts().hasReceipt(transaction,owner),"Account stores the once-only mob/player receipt");
            }
            f.check(found==2,"Exactly two positive shares recorded for the two eligible players");
        }
        @Override public void close(){
            for(var mob:mobs){TamsinData.get(f.level.getServer()).unbind(mob.getUUID());mob.discard();}
            RankStore.get(f.level).clearRank(member.getUUID());f.players.remove(member.getUUID());member.discard();
            D1EconomyConfig.MOB_CATEGORIES.set(previousMobs);D1EconomyConfig.SPAWNER_REWARDS.set(previousSpawners);
            D1EconomyConfig.REWARD_RADIUS.set(previousRadius);
            previousValues.forEach((key,value)->D1EconomyConfig.REWARDS.get(key).set(value));
            f.level.getServer().overworld().getDataStorage().set(PlayerCurrencyData.TYPE,previousAccounts);
            previousAccounts.setDirty(previousDirty);
            f.level.getServer().overworld().getDataStorage().set(lifetimeType,previousLifetime);
            previousLifetime.setDirty(previousLifetimeDirty);TamsinData.get(f.level.getServer()).setDirty(previousTamsinDirty);
            warnings.removeIf(warning->!previousWarnings.contains(warning));
            var board=f.level.getServer().getScoreboard();var objective=board.getObjective("d1_kills");
            if(previousObjective==null){if(objective!=null)board.removeObjective(objective);}
            else if(previousScore==null)board.resetSinglePlayerScore(f.player,previousObjective);
            else board.getOrCreatePlayerScore(f.player,previousObjective).set(previousScore);
        }
    }
    private static ServerPlayer member(CosmicSpawnerGameTests.Fixture f){
        var profile=new GameProfile(UUID.randomUUID(),"RewardMember");var server=f.level.getServer();
        var player=new ServerPlayer(server,f.level,profile,ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){
            @Override public io.netty.channel.Channel channel(){return f.channel;}
        };
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,connection,player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(Packet<?> packet){f.packets.add(packet);}
            @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener){f.packets.add(packet);}
        };
        player.setGameMode(GameType.SURVIVAL);player.setHealth(20);player.setPos(f.origin);
        return player;
    }
    public static void deathAndReplay(GameTestHelper helper){
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-204);var r=new Rewards(f)){
            r.initialize();
            var first=r.mob("minecraft:zombie_villager");var source=f.level.damageSources().generic();
            first.getPersistentData().putString(DungeonKillCredit.LAST_PLAYER,f.player.getStringUUID());
            first.setHealth(0);first.die(source);
            f.check(r.balance(f.player)==2&&r.balance(r.member)==1,"Native zombie-villager death awards three Trace with rotating remainder");
            r.shares(first,2,1);
            var credited=image(r.accounts());
            // Both duplicate native death dispatch and the direct service callback are idempotent.
            first.die(source);r.award(first);
            f.check(credited.equals(image(r.accounts())),"Repeated death/service callbacks cannot add balance or ledger rows");
            var second=r.mob("minecraft:zombie_villager");
            second.getPersistentData().putString(DungeonKillCredit.LAST_PLAYER,f.player.getStringUUID());
            second.setHealth(0);second.die(source);
            f.check(r.balance(f.player)==3&&r.balance(r.member)==3,"Next odd reward rotates the remainder to the second member");
            r.shares(second,1,2);
            var shulker=r.mob("minecraft:shulker");
            shulker.getPersistentData().putString(DungeonKillCredit.LAST_PLAYER,f.player.getStringUUID());
            shulker.setHealth(0);shulker.die(source);
            f.check(r.balance(f.player)==5&&r.balance(r.member)==5,"Native shulker death awards four Trace");
            r.shares(shulker,2,2);
            var saved=image(r.accounts());var loaded=codec().parse(NbtOps.INSTANCE,saved).getOrThrow();
            f.level.getServer().overworld().getDataStorage().set(PlayerCurrencyData.TYPE,loaded);
            f.check(r.accounts()==loaded&&loaded!=r.previousAccounts,"Actual account codec reload replaces the production account object");
            // Model loss of the transient/run-side duplicate guard while preserving the account journal.
            D1RunData.get(f.level.getServer()).clearRun(f.id);
            r.award(first);r.award(second);r.award(shulker);
            f.check(saved.equals(image(r.accounts())),"Persisted per-player receipts prevent replay after run-side markers are cleared");
            var supply=r.accounts().supplySnapshot();
            f.check(supply.getStringOr("generation","").equals("10")&&supply.getStringOr("balances","").equals("10")
                    &&supply.getStringOr("difference","").equals("0"),"All native rewards reconcile account supply and ledger generation");
            helper.succeed();
        }
    }
    public static void eligibilityAndOverrides(GameTestHelper helper){
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-205);var r=new Rewards(f)){
            r.initialize();r.member.setPos(f.origin.add(61,0,0));
            for(var registration:D1MobRewardCatalog.additions().entrySet()){
                long before=r.balance(f.player);var mob=r.mob(registration.getKey());r.award(mob);
                f.check(r.balance(f.player)-before==VALUES.get(registration.getValue()),
                        "Every approved addition reaches the real native reward service: "+registration.getKey());
            }
            f.check(r.balance(r.member)==0,"Outside-radius member receives no share");
            var vendor=r.mob("minecraft:zombie_villager");vendor.getPersistentData().putString(VendorBindingRules.PROFILE,"invalid_fixture_profile");
            r.excluded(vendor,f.player.getUUID(),"Even an unknown vendor binding remains excluded");
            var watson=r.mob("minecraft:zombie_villager");watson.getPersistentData().putLong("cosmicdungeon_d1_watson_run",f.id);
            r.excluded(watson,f.player.getUUID(),"Watson remains excluded");
            var tamsin=r.mob("minecraft:shulker");TamsinData.get(f.level.getServer()).bind(tamsin.getUUID(),
                    new TamsinData.Binding(f.level.dimension().location().toString(),0));
            r.excluded(tamsin,f.player.getUUID(),"Tamsin remains excluded");
            var untagged=r.mob("minecraft:shulker");untagged.removeTag(ORIGIN);
            r.excluded(untagged,f.player.getUUID(),"Unprovenanced world mobs remain excluded");
            r.excluded(r.mob("minecraft:shulker"),UUID.randomUUID(),"Outsider kill credit is excluded");
            r.excluded(r.mob("minecraft:shulker"),null,"Missing kill credit is excluded");

            var boundary=r.mob("minecraft:shulker");
            // Shulkers snap their native Y coordinate; measure the boundary from the actual mob position.
            r.member.setPos(boundary.position().add(60,0,0));
            long before=r.balance(f.player);r.award(boundary);
            f.check(r.balance(f.player)-before==2&&r.balance(r.member)==2,"Inclusive sixty-block radius shares the reward");
            r.member.setGameMode(GameType.SPECTATOR);assertSolo(r,"Spectator party members are excluded");
            r.member.setGameMode(GameType.SURVIVAL);RankStore.get(f.level).setRank(r.member.getUUID(),Rank.DEVELOPER);
            assertSolo(r,"Developer party members are excluded");RankStore.get(f.level).clearRank(r.member.getUUID());
            f.players.remove(r.member.getUUID());assertSolo(r,"Offline party members are excluded");f.players.put(r.member.getUUID(),r.member);

            // The actual death-position handler records the eligible location before the body is moved.
            D1RewardEvents.death(new LivingDeathEvent(r.member,f.level.damageSources().generic()));
            r.member.setHealth(0);r.member.setPos(f.origin.add(100,0,0));before=r.balance(r.member);
            r.award(r.mob("minecraft:shulker"));
            f.check(r.balance(r.member)-before==2,"Dead unrespawned member shares using the recorded death position");
            D1RunData.get(f.level.getServer()).setValue(f.id,"death_position:"+r.member.getUUID(),"malformed");
            assertSolo(r,"Malformed death position fails closed");r.member.setHealth(20);r.member.setPos(f.origin.add(61,0,0));

            var override=r.mob("minecraft:shulker");override.getPersistentData().putLong(D1MobRewardRules.TRACE,11);
            D1EconomyConfig.MOB_CATEGORIES.set(List.of("minecraft:shulker=0"));
            D1EconomyConfig.SPAWNER_REWARDS.set(List.of(ORIGIN+"=miniboss"));
            before=r.balance(f.player);r.award(override);
            f.check(r.balance(f.player)-before==11,"Explicit entity Trace overrides spawner and zero type registration");
            override=r.mob("minecraft:shulker");override.getPersistentData().putString(D1MobRewardRules.CATEGORY,"elite");
            before=r.balance(f.player);r.award(override);f.check(r.balance(f.player)-before==6,"Entity category retains precedence");
            before=r.balance(f.player);r.award(r.mob("minecraft:shulker"));
            f.check(r.balance(f.player)-before==10,"Spawner category overrides explicit zero type registration");
            D1EconomyConfig.SPAWNER_REWARDS.set(List.of());var zero=r.mob("minecraft:shulker");
            r.unchanged(()->r.award(zero),"Explicit zero registration suppresses fallback without account mutation");
            D1EconomyConfig.MOB_CATEGORIES.set(List.of());var disabled=r.mob("minecraft:zombie_villager");
            r.unchanged(()->r.award(disabled),"Empty configuration disables the fallback");
            helper.succeed();
        }
    }
    private static void assertSolo(Rewards r,String label){
        long first=r.balance(r.f.player),second=r.balance(r.member);r.award(r.mob("minecraft:shulker"));
        r.f.check(r.balance(r.f.player)-first==4&&r.balance(r.member)==second,label);
    }
}
