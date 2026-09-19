package net.goui.cosmicdungeon.dungeon;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerBlockEntity;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.economy.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;

import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import com.mojang.logging.LogUtils;
import java.util.*;
/** Economy Internal: registered sources, sixty-block eligibility and a persistent rotating remainder. */
public final class DungeonGroupSplitService {
    private static final Set<String> WARNED=new HashSet<>();
    private DungeonGroupSplitService(){}
    public static void onMobKilled(LivingEntity mob){
        if(!(mob.level() instanceof ServerLevel level)||!(mob instanceof Mob)
                ||mob.getTags().stream().noneMatch(t->t.startsWith(CosmicSpawnerBlockEntity.COSMIC_SPAWNER_TAG_PREFIX)))return;
        var run=DungeonRunRegistryData.get(level.getServer()).findRunForInstanceDimension(level.dimension())
                .filter(r->r.stateEnum()==DungeonRunState.ACTIVE&&r.dungeonId().equals("dungeon_1")).orElse(null);
        if(run==null||net.goui.cosmicdungeon.vendor.VendorAssignmentService.hasAssignedProfile(mob)
                ||mob.getPersistentData().contains("cosmicdungeon_d1_watson_run")
                ||net.goui.cosmicdungeon.npc.tamsin.TamsinData.get(level.getServer()).binding(mob.getUUID())!=null)return;
        var data=D1RunData.get(level.getServer());
        if(!data.recordUnique(run.runId(),"reward_mobs",mob.getUUID().toString()))return;
        long pool=reward(mob);if(pool<=0)return;
        var players=new LinkedHashMap<UUID,ServerPlayer>();
        for(UUID id:run.orderedPlayers()){
            var player=level.getServer().getPlayerList().getPlayer(id);
            if(player==null||player.isSpectator()||AccessPolicy.isDeveloper(player)||player.level()!=level)continue;
            Vec3 position=player.position();
            if(!player.isAlive()){
                var death=data.values(run.runId(),"death_position:"+id);if(death.size()!=1)continue;
                String[] bits=death.getFirst().split(",");
                try{position=new Vec3(Double.parseDouble(bits[0]),Double.parseDouble(bits[1]),Double.parseDouble(bits[2]));}
                catch(RuntimeException bad){continue;}
            }
            double radius=D1EconomyConfig.REWARD_RADIUS.get();
            if(position.distanceToSqr(mob.position())<=radius*radius)players.put(id,player);
        }
        if(players.isEmpty())return;
        int cursor=data.count(run.runId(),"reward_cursor");
        var split=D1RewardRules.split(pool,run.orderedPlayers(),players.keySet(),cursor);
        data.setCount(run.runId(),"reward_cursor",split.nextCursor());
        for(var entry:players.entrySet()){
            long amount=split.shares().getOrDefault(entry.getKey(),0L);if(amount==0)continue;
            long credited=CurrencyService.reward(entry.getValue(),amount,"dungeon_mob",mob.getUUID().toString(),
                    run.runId(),"mob:"+run.runId()+":"+mob.getUUID());
            if(credited>0)entry.getValue().displayClientMessage(Component.literal("Group Split: +"+credited+" Trace"),true);
        }
    }
    private static long reward(LivingEntity mob){
        String entity=BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        var categories=new LinkedHashMap<String,Long>();
        D1EconomyConfig.REWARDS.forEach((key,value)->categories.put(key,value.get()));
        var result=D1MobRewardRules.resolve(entity,mob.getTags(),mob.getPersistentData(),
                D1EconomyConfig.MOB_CATEGORIES.get(),D1EconomyConfig.SPAWNER_REWARDS.get(),categories);
        return result.valid()?result.trace():warn(entity,result.warning());
    }
    private static long warn(String entity,String reason){
        if(WARNED.add(entity+"|"+reason))LogUtils.getLogger().warn("D1 currency: {}: {}; awarding zero Trace",entity,reason);
        return 0;
    }
    public static void clear(){WARNED.clear();}
}
