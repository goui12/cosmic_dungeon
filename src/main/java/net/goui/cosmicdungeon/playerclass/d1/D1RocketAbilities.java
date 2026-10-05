package net.goui.cosmicdungeon.playerclass.d1;

import java.util.ArrayList;
import net.goui.cosmicdungeon.mercenary.*;
import net.goui.cosmicdungeon.playerclass.bogatyr.CompanionAllies;
import net.goui.cosmicdungeon.playerclass.skill.ClassSkills;
import net.minecraft.server.level.*;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid="cosmicdungeon")
public final class D1RocketAbilities {
    private static final String CAPTURED="cosmicdungeon_pyroclast_launch_checked";
    private static final String RUN="cosmicdungeon_pyroclast_rocket_run", SPENT="cosmicdungeon_pyroclast_rocket_spent";
    private D1RocketAbilities() {}

    private static long activeRun(LivingEntity owner){
        if(!"pyroclast".equals(D1ProjectileAccess.classId(owner)))return -1;
        if(owner instanceof ServerPlayer player){
            var run=ClassSkills.run(player);return run==null?-1:run.runId();
        }
        if(owner instanceof MercenaryEntity merc&&MercenaryBrain.hirer(merc)!=null)return merc.runId();
        return -1;
    }
    /** Persist launch admission, including denial; changing class/run later cannot enable a spent shot. */
    public static void capture(FireworkRocketEntity rocket){
        if(rocket.getPersistentData().contains(CAPTURED)||rocket.getPersistentData().contains(RUN)
                ||!(rocket.getOwner() instanceof LivingEntity owner))return;
        rocket.getPersistentData().putBoolean(CAPTURED,true);
        if("pyroclast".equals(D1ProjectileAccess.classId(owner)))
            rocket.getPersistentData().putLong(RUN,activeRun(owner));
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event){
        if(event.getLevel() instanceof ServerLevel&&event.getEntity() instanceof FireworkRocketEntity rocket)capture(rocket);
    }
    public static boolean enemy(LivingEntity owner,LivingEntity target){
        if(owner==target||!target.isAlive()||target.isSpectator()||target.level()!=owner.level()
                ||target instanceof MercenaryEntity||CompanionAllies.friendly(target))return false;
        if(owner instanceof MercenaryEntity merc)return MercenaryBrain.enemy(merc,target);
        return target instanceof Enemy && !(target instanceof OwnableEntity) && !target.isAlliedTo(owner);
    }
    public static boolean explode(FireworkRocketEntity rocket, ServerLevel level) {
        String id=D1AbilityIdentity.identify(rocket.getItem());
        var permission=D1ProjectileAccess.permission(rocket,rocket.getItem(),id);
        capture(rocket); // Compatible with an unmarked rocket already in flight during an upgrade.
        if(permission==D1CombatRules.Ammunition.VANILLA&&!rocket.getPersistentData().contains(RUN))return false;
        if(rocket.getPersistentData().getBooleanOr(SPENT,false))return true;
        rocket.getPersistentData().putBoolean(SPENT,true);
        if(permission==D1CombatRules.Ammunition.DENIED
                ||!(rocket.getOwner() instanceof LivingEntity owner)||owner.level()!=level
                ||activeRun(owner)<=0||rocket.getPersistentData().getLongOr(RUN,-1)!=activeRun(owner))return true;
        if(owner instanceof ServerPlayer player
                &&!net.goui.cosmicdungeon.playerclass.api.ClassItemEquipmentGuard.canUse(player,rocket.getItem()))return true;
        var spell=D1AbilityConfig.get("pyroclast",id);
        // Unnamed native rockets also use Cinderbite's base power; authored payloads remain untouched.
        if(spell==null)spell=D1AbilityConfig.get("pyroclast","cinderbite");
        double power=spell.power().get()*D1AbilityConfig.ROCKET_DAMAGE_MULTIPLIER.get();
        boolean damaged=burst(rocket,level,owner,power);
        if(damaged&&owner instanceof MercenaryEntity merc)MercenarySkills.success(merc,MercenarySkill.FIREWORKS);
        return true;
    }
    /** Damage policy is shared by native player and mercenary rocket detonations. */
    static boolean burst(FireworkRocketEntity rocket,ServerLevel level,LivingEntity owner,double power){
        double radius=D1AbilityConfig.ROCKET_RADIUS.get();
        int limit=D1AbilityConfig.ROCKET_CANDIDATE_LIMIT.get();
        var candidates=new ArrayList<LivingEntity>();int[] visited={0};
        level.getEntities().get(EntityTypeTest.forClass(LivingEntity.class),
                rocket.getBoundingBox().inflate(radius),target->{
            visited[0]++;
            if(enemy(owner,target)&&rocket.distanceToSqr(target)<radius*radius)candidates.add(target);
            return visited[0]>=limit?AbortableIterationConsumer.Continuation.ABORT:AbortableIterationConsumer.Continuation.CONTINUE;
        });
        Vec3 origin=rocket.position();boolean damaged=false;
        for(var target:candidates){
            double damage=D1CombatRules.rocketDamage(power,rocket.distanceTo(target),radius);
            if(damage<=0||!enemy(owner,target))continue;
            boolean clear=false;
            for(int i=0;i<2;i++){
                Vec3 point=new Vec3(target.getX(),target.getY(.5*i),target.getZ());
                if(level.clip(new ClipContext(origin,point,ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,rocket)).getType()==HitResult.Type.MISS){clear=true;break;}
            }
            if(clear){
                float before=target.getHealth()+target.getAbsorptionAmount();
                if(target.hurtServer(level,rocket.damageSources().fireworks(rocket,owner),(float)damage)
                        &&target.getHealth()+target.getAbsorptionAmount()<before)damaged=true;
            }
        }
        return damaged;
    }
    // Later-dungeon rocket identities remain deferred: Cinder Breeze/Whispered Cinder,
    // Cinder Smash/Cinder Cleave, Ashwhisper/Boneflare/Doomscree/Soulshredder/Malice Reaver.
    // The bounded candidate query can omit enemies in saturated rooms; it is not nearest-all.
}
