package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.dungeon.d1.D1Members;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.playerclass.bogatyr.CompanionAllies;
import net.goui.cosmicdungeon.playerclass.d1.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.*;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** One bounded decision every ten ticks; native navigation handles motion between decisions. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class MercenaryBrain {
    private Vec3 lastPosition;
    private int stuck, attack, collection;
    public static ServerPlayer hirer(MercenaryEntity entity){
        if(entity.dormant()||!entity.isAlive()||!(entity.level() instanceof ServerLevel level))return null;
        var run=D1Members.run(level).orElse(null);
        if(!MercenaryLifecycle.admitted(run,entity.contract(),entity.getUUID(),level.dimension().location().toString())
                ||run.runId()!=entity.runId())return null;
        var player=level.getServer().getPlayerList().getPlayer(entity.contract().hirer());
        return player!=null&&player.level()==level&&D1Members.inside(player,run)?player:null;
    }
    private record Context(ServerPlayer owner,DungeonRunRegistryData.RunRecord run){}
    private static Context context(MercenaryEntity entity){
        var owner=hirer(entity);
        return owner==null?null:new Context(owner,D1Members.run(owner.level()).orElseThrow());
    }
    private static boolean ally(Context context,LivingEntity target){
        if(context==null||target instanceof MercenaryEntity m&&m.dormant()||!target.isAlive()||target.level()!=context.owner().level())return false;
        if(target instanceof ServerPlayer player)return !player.isSpectator()&&!AccessPolicy.isDeveloper(player)
                &&context.run().containsPlayer(player.getUUID())&&!context.run().isCompletionExited(player.getUUID());
        return target instanceof OwnableEntity owned&&owned.getOwnerReference()!=null
                &&context.run().containsPlayer(owned.getOwnerReference().getUUID())
                &&!context.run().isCompletionExited(owned.getOwnerReference().getUUID());
    }
    public static boolean ally(MercenaryEntity entity,LivingEntity target){return ally(context(entity),target);}
    private static boolean enemy(Context context,MercenaryEntity entity,LivingEntity target){
        if(context==null||!target.isAlive()||target.level()!=entity.level()||CompanionAllies.friendly(target)
                ||target.isAlliedTo(entity)||target.isAlliedTo(context.owner()))return false;
        return target instanceof Enemy||target instanceof Mob mob&&mob.getTarget()!=null&&ally(context,mob.getTarget());
    }
    public static boolean enemy(MercenaryEntity entity,LivingEntity target){return enemy(context(entity),entity,target);}
    public static <T extends Entity> void nearby(ServerLevel level,Class<T> type,AABB box,int limit,Consumer<T> action){
        afterSnapshot(limit,visitor->level.getEntities().get(EntityTypeTest.forClass(type),box,visitor),action);
    }
    /** Consumers can discard or move entities, so run them only after native section traversal ends. */
    static <T> void afterSnapshot(int limit,Consumer<AbortableIterationConsumer<T>> query,Consumer<T> action){
        if(limit<=0)return;
        var snapshot=new ArrayList<T>(Math.min(limit,48));
        query.accept(entity->{
            snapshot.add(entity);
            return snapshot.size()>=limit?AbortableIterationConsumer.Continuation.ABORT:AbortableIterationConsumer.Continuation.CONTINUE;
        });
        snapshot.forEach(action);
    }
    public static boolean recover(double distanceSquared,int stalled){return distanceSquared>256||distanceSquared>16&&stalled>=100;}
    public void tick(MercenaryEntity entity,ServerLevel level){
        if((entity.tickCount+entity.getId())%10!=0)return;
        var context=context(entity);
        var owner=context==null?null:context.owner();
        if(owner==null){entity.setTarget(null);entity.getNavigation().stop();lastPosition=null;stuck=0;return;}
        entity.timers(entity.timers().advance(10));attack=Math.max(0,attack-10);collection=Math.max(0,collection-10);
        double distance=entity.distanceToSqr(owner);
        if(distance>16&&lastPosition!=null&&lastPosition.distanceToSqr(entity.position())<.09)stuck+=10;else stuck=0;
        lastPosition=entity.position();
        if(recover(distance,stuck)&&teleport(entity,owner,level)){stuck=0;distance=entity.distanceToSqr(owner);}
        if(distance>256){entity.setTarget(null);entity.getNavigation().moveTo(owner,1.15);return;}
        var candidates=new ArrayList<LivingEntity>();candidates.add(owner);candidates.add(entity);
        nearby(level,LivingEntity.class,entity.getBoundingBox().inflate(8),48,target->{
            if(target!=owner&&target!=entity&&target.isAlive()&&(ally(context,target)||enemy(context,entity,target)))candidates.add(target);
        });
        candidates.sort(Comparator.comparingInt((LivingEntity target)->ally(context,target)?0:1)
                .thenComparingDouble(target->target.getHealth()/target.getMaxHealth()));
        MercenaryPotions.produce(entity,level);
        MercenaryPotions.use(entity,candidates);
        LivingEntity target=candidates.stream().filter(t->enemy(context,entity,t)&&owner.distanceToSqr(t)<=64
                &&entity.getSensing().hasLineOfSight(t)).min(Comparator.comparingDouble(entity::distanceToSqr)).orElse(null);
        entity.setTarget(target);
        if(distance>100){entity.getNavigation().moveTo(owner,1.15);return;}
        if(target!=null){
            entity.getLookControl().setLookAt(target,30,30);
            boolean ranged=entity.getMainHandItem().getItem() instanceof BowItem
                    ||entity.getMainHandItem().getItem() instanceof CrossbowItem;
            if(ranged&&ammunition(entity,target)>=0){
                entity.getNavigation().stop();
                if(attack==0&&shoot(entity,target,level))attack=40;
            }else{
                if(entity.distanceToSqr(target)>4)entity.getNavigation().moveTo(target,1);
                else {entity.getNavigation().stop();if(attack==0){
                    entity.swing(net.minecraft.world.InteractionHand.MAIN_HAND);entity.doHurtTarget(level,target);attack=20;
                }}
            }
        }else if(distance>9)entity.getNavigation().moveTo(owner,1.05);
        else entity.getNavigation().stop();
        if(collection==0){collection=40;MercenaryCollection.collect(entity,level);}
    }
    public static boolean attackArrow(ItemStack ammo,boolean inverted){
        String id=D1AbilityIdentity.identify(ammo);
        if(D1CombatRules.supportive(id)||inverted&&("spicule_breach".equals(id)||"spicule_rend".equals(id)))return false;
        var contents=ammo.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                net.minecraft.world.item.alchemy.PotionContents.EMPTY);
        for(var effect:contents.getAllEffects())if(MercenaryPotions.helpful(effect.getEffect().value(),inverted))return false;
        return true;
    }
    private static int ammunition(MercenaryEntity entity,LivingEntity target){
        for(int i=0;i<entity.supplies().size();i++){
            var ammo=entity.supplies().get(i);
            if(!(ammo.getItem() instanceof ArrowItem)||!MercenaryInventory.permitted(ammo,entity.contract())
                    ||!attackArrow(ammo,target.isInvertedHealAndHarm()))continue;
            String id=D1AbilityIdentity.identify(ammo);
            if(id!=null&&(D1CombatRules.supportive(id)
                    ||!D1AmmunitionCatalog.bindingAllowed(id,net.goui.cosmicdungeon.playerclass.api.ClassItemUtil.hasAnyAttunementMetadata(ammo),
                    net.goui.cosmicdungeon.playerclass.api.ClassItemUtil.hasCompleteValidAttunement(ammo),
                    net.goui.cosmicdungeon.playerclass.api.ClassItemUtil.getClassAttunement(ammo),
                    net.goui.cosmicdungeon.playerclass.api.ClassItemUtil.getDungeon(ammo),
                    net.goui.cosmicdungeon.playerclass.api.ClassItemUtil.getTier(ammo))))continue;
            if(D1ProjectileAccess.permission(entity,ammo,id)!=D1CombatRules.Ammunition.DENIED)return i;
        }
        return -1;
    }
    private static boolean shoot(MercenaryEntity entity,LivingEntity target,ServerLevel level){
        int index=ammunition(entity,target);if(index<0)return false;
        var ammo=entity.supplies().get(index);
        var shot=ProjectileUtil.getMobArrow(entity,ammo.copyWithCount(1),1,entity.getMainHandItem());
        MercenaryPotions.mark(shot,entity);
        shot.pickup=AbstractArrow.Pickup.DISALLOWED;
        double x=target.getX()-entity.getX(),z=target.getZ()-entity.getZ();
        shot.shoot(x,target.getY(.333)-shot.getY()+Math.sqrt(x*x+z*z)*.2,z,1.6f,2);
        if(!level.addFreshEntity(shot))return false;
        ammo.shrink(1);return true;
    }
    static boolean teleport(MercenaryEntity entity,ServerPlayer owner,ServerLevel level){
        if(entity.isPassenger()||entity.isVehicle())return false;
        for(int y=0;y<=1;y++)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            if(Math.abs(x)<2&&Math.abs(z)<2)continue;
            BlockPos pos=owner.blockPosition().offset(x,y,z);
            if(!level.hasChunkAt(pos)||!net.goui.cosmicdungeon.rift.SafeTeleportUtil.isStandable(level,pos))continue;
            Vec3 destination=Vec3.atBottomCenterOf(pos);
            var box=entity.getBoundingBox().move(destination.subtract(entity.position()));
            if(!level.noCollision(entity,box))continue;
            entity.getNavigation().stop();entity.setTarget(null);entity.setDeltaMovement(Vec3.ZERO);
            entity.teleportTo(destination.x,destination.y,destination.z);entity.fallDistance=0;return true;
        }
        return false;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void impact(ProjectileImpactEvent event){
        if(!(event.getRayTraceResult() instanceof EntityHitResult hit)||!(hit.getEntity() instanceof LivingEntity target))return;
        var shot=event.getProjectile();
        if(!(shot instanceof AbstractArrow)||!MercenaryPotions.marked(shot))return;
        var owner=MercenaryPotions.owner(shot);
        if(owner==null||!enemy(owner,target)||!attackArrow(((AbstractArrow)shot).getPickupItemStackOrigin(),target.isInvertedHealAndHarm())){
            event.setCanceled(true);shot.discard();
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void friendlyDamage(LivingIncomingDamageEvent event){
        var source=event.getSource();
        var owner=source.getEntity() instanceof MercenaryEntity m?m:MercenaryPotions.owner(source.getDirectEntity());
        if(owner!=null&&!enemy(owner,event.getEntity())
                ||owner==null&&MercenaryPotions.marked(source.getDirectEntity()))event.setCanceled(true);
    }
}
