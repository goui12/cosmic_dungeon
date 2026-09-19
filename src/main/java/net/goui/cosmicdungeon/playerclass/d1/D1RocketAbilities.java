package net.goui.cosmicdungeon.playerclass.d1;
import net.goui.cosmicdungeon.dungeon.d1.D1Members;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
public final class D1RocketAbilities {
    private D1RocketAbilities(){}
    public static boolean explode(FireworkRocketEntity rocket,ServerLevel level){
        if(!(rocket.getOwner() instanceof ServerPlayer player)||!"pyroclast".equals(ClassData.getClassId(player)))return false;
        var run=D1Members.run(level).orElse(null);
        if(run==null||!D1Members.inside(player,run))return false;
        String id=D1AbilityIdentity.identify(rocket.getItem());
        if(id==null||!id.startsWith("cinder"))return false;
        double radius=D1AbilityConfig.ROCKET_RADIUS.get(),power=D1AbilityConfig.get("pyroclast",id).power().get();
        Vec3 origin=rocket.position();
        for(var target:level.getEntitiesOfClass(LivingEntity.class,rocket.getBoundingBox().inflate(radius))){
            double distance=rocket.distanceTo(target);if(distance>radius)continue;
            boolean clear=false;
            for(int i=0;i<2;i++){
                Vec3 point=new Vec3(target.getX(),target.getY(0.5*i),target.getZ());
                if(level.clip(new ClipContext(origin,point,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,rocket)).getType()==HitResult.Type.MISS){clear=true;break;}
            }
            if(clear)target.hurtServer(level,rocket.damageSources().fireworks(rocket,player),(float)(power*Math.sqrt(Math.max(0,1-distance/radius))));
        }
        return true;
    }
    // TODO(M63, D2+): Ashwhisper/Boneflare/Doomscree/Soulshredder/Malice Reaver and their effects
    // require their newer linked rocket documents. Only authored D1 Cinderbite/Cindermaul are tuned here.
}
