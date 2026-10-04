package net.goui.cosmicdungeon.mercenary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.playerclass.bogatyr.CompanionAllies;
import net.goui.cosmicdungeon.rift.SafeTeleportUtil;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Run-bound summons. The existing run store counts unloaded identities, not just nearby wolves. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class MercenaryWolves {
    public static final int INTERVAL=2400, CAP=5;
    static final String MARKER="cosmicdungeon_mercenary_wolf_v1";
    record Bond(long run,UUID mercenary,UUID hirer){
        static final Codec<Bond> CODEC=RecordCodecBuilder.create(i->i.group(
                Codec.LONG.fieldOf("run").forGetter(Bond::run),
                UUIDUtil.CODEC.fieldOf("mercenary").forGetter(Bond::mercenary),
                UUIDUtil.CODEC.fieldOf("hirer").forGetter(Bond::hirer)).apply(i,Bond::new));
    }
    private MercenaryWolves(){}
    public static boolean managed(Entity entity){
        return entity instanceof Wolf&&entity.getPersistentData().contains(MARKER);
    }
    public static boolean protectedCompanion(Entity entity){
        return entity instanceof MercenaryEntity||managed(entity);
    }
    static Bond bond(Wolf wolf){return wolf.getPersistentData().read(MARKER,Bond.CODEC)
            .filter(b->b.run()>0&&!b.mercenary().equals(b.hirer())).orElse(null);}
    static void mark(Wolf wolf,Bond bond){wolf.getPersistentData().store(MARKER,Bond.CODEC,bond);}
    static boolean enabled(MercenaryContract contract){return contract!=null&&"bogatyr".equals(contract.classId());}
    static int loadCooldown(ValueInput input){
        return Math.clamp(input.getIntOr("mercenary_wolf_ticks",INTERVAL),0,INTERVAL);
    }
    static int advance(int remaining,int elapsed){return Math.max(0,remaining-Math.max(0,elapsed));}
    static String key(UUID mercenary){return "mercenary_wolves:"+mercenary;}
    static String dismissed(UUID mercenary){return "mercenary_wolves_dismissed:"+mercenary;}
    static boolean remember(D1RunData data,Bond bond,UUID wolf){
        if(data.count(bond.run(),dismissed(bond.mercenary()))!=0)return false;
        var ids=data.values(bond.run(),key(bond.mercenary()));
        if(ids.contains(wolf.toString()))return true;
        return ids.size()<CAP&&data.recordUnique(bond.run(),key(bond.mercenary()),wolf.toString());
    }
    static void died(D1RunData data,Bond bond,UUID wolf,Entity.RemovalReason reason){
        if(reason==Entity.RemovalReason.KILLED)data.removeUnique(bond.run(),key(bond.mercenary()),wolf.toString());
    }
    static boolean admitted(DungeonRunRegistryData.RunRecord run,Bond bond,UUID owner,String dimension){
        if(bond==null||run==null||run.runId()!=bond.run()||!bond.hirer().equals(owner))return false;
        var hire=run.mercenaries().stream().filter(c->c.id().equals(bond.mercenary())).findFirst().orElse(null);
        return enabled(hire)&&hire.hirer().equals(bond.hirer())
                &&MercenaryLifecycle.admitted(run,hire,bond.mercenary(),dimension);
    }
    private static boolean admitted(Wolf wolf,ServerLevel level,Bond bond){
        var run=bond==null?null:DungeonRunRegistryData.get(level.getServer()).getRun(bond.run()).orElse(null);
        var owner=wolf.getOwnerReference();
        return wolf.isTame()&&admitted(run,bond,owner==null?null:owner.getUUID(),level.dimension().location().toString())
                &&D1RunData.get(level.getServer()).count(bond.run(),dismissed(bond.mercenary()))==0;
    }
    static void prepare(Wolf wolf,MercenaryEntity mercenary,LivingEntity hirer){
        wolf.setTame(true,true);wolf.setOwnerReference(EntityReference.of(hirer));
        wolf.setOrderedToSit(false);wolf.setInSittingPose(false);wolf.stopBeingAngry();
        wolf.setTarget(null);wolf.setPersistenceRequired();wolf.setHealth(wolf.getMaxHealth());
        mark(wolf,new Bond(mercenary.runId(),mercenary.contract().id(),mercenary.contract().hirer()));
    }
    public static void tick(MercenaryEntity entity,ServerLevel level,ServerPlayer hirer,int elapsed){
        if(!enabled(entity.contract()))return;
        entity.wolfTicks(advance(entity.wolfTicks(),elapsed));
        if(entity.wolfTicks()>0)return;
        var data=D1RunData.get(level.getServer());
        var bond=new Bond(entity.runId(),entity.contract().id(),entity.contract().hirer());
        if(data.count(bond.run(),dismissed(bond.mercenary()))!=0
                ||data.values(bond.run(),key(bond.mercenary())).size()>=CAP)return;
        // Failed placement retries at most once per second, without spending a successful summon.
        entity.wolfTicks(20);
        var wolf=EntityType.WOLF.create(level,EntitySpawnReason.MOB_SUMMONED);
        if(wolf==null)return;
        Vec3 location=null;
        var center=entity.blockPosition();
        outer:for(int y=0;y<=1;y++)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            if(x==0&&z==0)continue;
            var pos=center.offset(x,y,z);
            if(!level.hasChunkAt(pos)||!SafeTeleportUtil.isStandable(level,pos))continue;
            var candidate=Vec3.atBottomCenterOf(pos);
            if(!level.noCollision(wolf,wolf.getBoundingBox().move(candidate.subtract(wolf.position()))))continue;
            location=candidate;break outer;
        }
        if(location==null)return;
        wolf.setPos(location);prepare(wolf,entity,hirer);
        if(!remember(data,bond,wolf.getUUID()))return;
        boolean added=false;
        try{added=level.addFreshEntity(wolf);}
        finally{if(!added)data.removeUnique(bond.run(),key(bond.mercenary()),wolf.getUUID().toString());}
        if(added)entity.wolfTicks(INTERVAL);
    }
    static boolean belongs(Wolf wolf,long run,MercenaryContract contract){
        return new Bond(run,contract.id(),contract.hirer()).equals(bond(wolf));
    }
    public static void command(MercenaryEntity mercenary,ServerLevel level,LivingEntity target){
        if(!enabled(mercenary.contract()))return;
        for(String id:D1RunData.get(level.getServer()).values(mercenary.runId(),key(mercenary.getUUID())).stream().limit(CAP).toList()){
            UUID uuid;try{uuid=UUID.fromString(id);}catch(IllegalArgumentException invalid){continue;}
            if(!(level.getEntity(uuid) instanceof Wolf wolf)||!belongs(wolf,mercenary.runId(),mercenary.contract())
                    ||!wolf.isAlive()||wolf.isOrderedToSit())continue;
            if(target!=null&&!CompanionAllies.friendly(target))wolf.setTarget(target);
            else if(CompanionAllies.friendly(wolf.getTarget())){wolf.setTarget(null);wolf.stopBeingAngry();}
        }
    }
    public static void dismiss(MinecraftServer server,DungeonRunRegistryData.RunRecord run,MercenaryContract contract){
        if(!enabled(contract))return;
        var data=D1RunData.get(server);
        data.setCount(run.runId(),dismissed(contract.id()),1);
        var ids=data.values(run.runId(),key(contract.id()));
        for(String dimension:run.dungeonDimensionIds()){
            var level=net.goui.cosmicdungeon.block.custom.ClassSelectorTeleportUtil.resolveLevel(server,dimension);
            if(level==null)continue;
            for(String id:ids.stream().limit(CAP).toList()){
                UUID uuid;try{uuid=UUID.fromString(id);}catch(IllegalArgumentException invalid){continue;}
                if(level.getEntity(uuid) instanceof Wolf wolf&&belongs(wolf,run.runId(),contract))wolf.discard();
            }
        }
        // Unloaded wolves retain their marker and are rejected on their next join.
        data.setValue(run.runId(),key(contract.id()),null);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getEntity() instanceof Wolf wolf)||!managed(wolf))return;
        var bond=bond(wolf);
        if(!admitted(wolf,level,bond)||!remember(D1RunData.get(level.getServer()),bond,wolf.getUUID())){
            event.setCanceled(true);wolf.discard();
        }
    }
    @SubscribeEvent
    public static void removed(EntityLeaveLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getEntity() instanceof Wolf wolf)||!managed(wolf))return;
        var bond=bond(wolf);
        if(bond!=null)died(D1RunData.get(level.getServer()),bond,wolf.getUUID(),wolf.getRemovalReason());
    }
    @SubscribeEvent
    public static void tickWolf(EntityTickEvent.Post event){
        if(!(event.getEntity() instanceof Wolf wolf)||!managed(wolf)||wolf.tickCount%40!=0
                ||!(wolf.level() instanceof ServerLevel level))return;
        if(!admitted(wolf,level,bond(wolf))){wolf.discard();return;}
        if(CompanionAllies.friendly(wolf.getTarget())){wolf.setTarget(null);wolf.stopBeingAngry();}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void targeting(LivingChangeTargetEvent event){
        if(managed(event.getEntity())&&CompanionAllies.friendly(event.getNewAboutToBeSetTarget()))
            event.setNewAboutToBeSetTarget(null);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void breeding(BabyEntitySpawnEvent event){
        if(managed(event.getParentA())||managed(event.getParentB()))event.setCanceled(true);
    }
}
