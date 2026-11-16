package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import java.util.function.BooleanSupplier;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerEntities;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.*;
import net.goui.cosmicdungeon.playerclass.bogatyr.CompanionAllies;
import net.goui.cosmicdungeon.playerclass.skill.SecondarySkillDamage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.effect.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Independent, run-scoped protections. No entity/world scans, synchronous saves or client authority. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class MercenaryAntiFarming {
    static final String PREFIX="mercenary_antifarm_v1:";
    static final String ORIGIN="cosmicdungeon_mercenary_origin_v1";
    private static final String LAST="cosmicdungeon_mercenary_last_wound_v1";
    private static final String DEAD="cosmicdungeon_antifarm_death_v1";
    private static final String MOTION="cosmicdungeon_antifarm_motion_v1";
    private static final String EFFECTS="cosmicdungeon_mercenary_dot_v1";
    static final String WARNING="I grow weary of fighting these monsters alone. You look well rested, get over here and help me!";
    static final String REFUSAL="That’s it. I’m putting my sword away. You fight, I’ll watch. You’ve shown me how that part works.";
    static final String HEAL_WARNING="Your sword arm looks fine to me. I prescribe using it!";
    static final String HEAL_REFUSAL="I'm done healing you. If you get injured, rub some dirt on it or walk it off!";
    private record Origin(long run,UUID mercenary,UUID hirer) {
        String encoded(){return run+"|"+mercenary+"|"+hirer;}
    }
    private static final ThreadLocal<Origin> EFFECT_ORIGIN=new ThreadLocal<>();
    private MercenaryAntiFarming(){}
    private static Origin decode(String text){
        try{
            var p=text.split("\\|",-1);if(p.length!=3)return null;
            long run=Long.parseLong(p[0]);
            return run>0?new Origin(run,UUID.fromString(p[1]),UUID.fromString(p[2])):null;
        }catch(IllegalArgumentException bad){return null;}
    }
    private static Origin origin(Entity entity){
        if(entity instanceof MercenaryEntity m&&m.contract()!=null)
            return new Origin(m.runId(),m.contract().id(),m.contract().hirer());
        if(entity instanceof Wolf wolf&&MercenaryWolves.managed(wolf)){
            var b=MercenaryWolves.bond(wolf);return b==null?null:new Origin(b.run(),b.mercenary(),b.hirer());
        }
        if(entity==null)return null;
        var saved=decode(entity.getPersistentData().getStringOr(ORIGIN,""));
        if(saved!=null)return saved;
        var caster=MercenaryPotions.caster(entity);
        return caster!=entity?origin(caster):null;
    }
    private static Origin origin(DamageSource source){
        var direct=origin(source.getDirectEntity());if(direct!=null)return direct;
        var actor=origin(source.getEntity());return actor!=null?actor:EFFECT_ORIGIN.get();
    }
    public static void mark(Entity shot,MercenaryEntity actor){
        var origin=origin(actor);if(origin!=null)shot.getPersistentData().putString(ORIGIN,origin.encoded());
    }
    public static void inherit(Entity child,Entity parent){
        var o=origin(parent);if(o!=null)child.getPersistentData().putString(ORIGIN,o.encoded());
    }
    private static DungeonRunRegistryData.RunRecord admitted(ServerLevel level,Origin o){
        if(o==null)return null;
        var run=D1Members.run(level).orElse(null);
        if(run==null||run.runId()!=o.run)return null;
        var hire=run.mercenaries().stream().filter(c->c.id().equals(o.mercenary)&&c.hirer().equals(o.hirer)).findFirst().orElse(null);
        if(!MercenaryLifecycle.admitted(run,hire,o.mercenary,level.dimension().location().toString()))return null;
        var recovery=PendingDungeonRecoveryData.get(level.getServer());var handoff=recovery.handoff(o.hirer);
        return recovery.completed(o.hirer)>=o.run||handoff!=null&&handoff.run()==o.run&&handoff.kind().equals("cleanup")?null:run;
    }
    /** Existing native tags are read only; old placements/presets need no conversion or chunk loading. */
    static String spawnerKey(LivingEntity target){
        return target.getTags().stream().filter(t->CosmicSpawnerEntities.position(t)!=null).sorted().findFirst()
                .map(t->target.level().dimension().location()+"|"+t).orElse(null);
    }
    private static String key(String source){return PREFIX+"spawner:"+source;}
    private static String epoch(D1RunData data,long run){
        var values=data.values(run,PREFIX+"epoch");return values.isEmpty()?"0":values.getFirst();
    }
    static int count(ServerLevel level,long run,String source){
        if(source==null)return 0;
        var data=D1RunData.get(level.getServer());var values=data.values(run,key(source));
        if(values.isEmpty())return 0;
        var p=values.getFirst().split("\\|",-1);
        if(p.length!=3||!p[0].equals("1"))return 40;
        if(!p[1].equals(epoch(data,run)))return 0;
        try{int n=Integer.parseInt(p[2]);return n>=0&&n<=40?n:40;}
        catch(NumberFormatException bad){return 40;}
    }
    static void setCount(ServerLevel level,long run,String source,int count){
        if(source==null)return;
        var data=D1RunData.get(level.getServer());
        data.setValue(run,key(source),"1|"+epoch(data,run)+"|"+Math.clamp(count,0,40));
    }
    private static void tell(ServerLevel level,Origin origin,String message){
        var owner=level.getServer().getPlayerList().getPlayer(origin.hirer);
        var run=admitted(level,origin);
        if(owner!=null&&run!=null&&D1Members.inside(owner,run))owner.displayClientMessage(Component.literal(message),false);
    }
    private static boolean allowed(Origin origin,LivingEntity target,boolean notify){
        if(origin==null)return true;
        if(!(target.level() instanceof ServerLevel level))return true;
        if(admitted(level,origin)==null)return false;
        String source=spawnerKey(target);if(count(level,origin.run,source)<40)return true;
        if(notify){
            var data=D1RunData.get(level.getServer());String key=PREFIX+"notice:"+origin.hirer;
            var old=data.values(origin.run,key);long now=level.getGameTime();boolean send=true;
            if(!old.isEmpty()){
                var p=old.getFirst().split("\\|",-1);
                if(p.length==2)try{send=!p[0].equals(target.getUUID().toString())&&now-Long.parseLong(p[1])>=100;}
                catch(NumberFormatException bad){send=false;}
            }
            if(send){tell(level,origin,REFUSAL);data.setValue(origin.run,key,target.getUUID()+"|"+now);}
        }
        return false;
    }
    public static boolean allows(MercenaryEntity actor,LivingEntity target){return allowed(origin(actor),target,true);}
    public static boolean allowsSource(Entity actor,LivingEntity target){return allowed(origin(actor),target,true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void incoming(LivingIncomingDamageEvent event){
        if(!allowed(origin(event.getSource()),event.getEntity(),true))event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hurt(LivingDamageEvent.Post event){
        if(event.getNewDamage()<=0||!(event.getEntity().level() instanceof ServerLevel level))return;
        var target=event.getEntity();var o=origin(event.getSource());
        if(o!=null&&admitted(level,o)!=null){
            var last=new CompoundTag();last.putString("origin",o.encoded());last.putLong("tick",level.getGameTime());
            target.getPersistentData().put(LAST,last);
        }else if(event.getSource().getEntity() instanceof LivingEntity)target.getPersistentData().remove(LAST);
        if(target instanceof ServerPlayer player)damaged(player);
        if(!SecondarySkillDamage.active()&&event.getSource().getEntity() instanceof ServerPlayer player
                &&target instanceof Enemy&&!CompanionAllies.friendly(target)&&!target.isAlliedTo(player)){
            var run=playerRun(player);String source=spawnerKey(target);
            if(run!=null&&source!=null&&player.level()==level&&participating(player,run.runId()))
                setCount(level,run.runId(),source,0);
        }
    }
    /** Accepted native death tail. The marker lives with the victim, never in a global UUID cache. */
    public static void killed(LivingEntity victim,DamageSource source){
        if(!(victim.level() instanceof ServerLevel level)||!(victim instanceof Mob)
                ||victim.getPersistentData().getBooleanOr(DEAD,false))return;
        victim.getPersistentData().putBoolean(DEAD,true);
        if(source.is(DamageTypes.GENERIC_KILL)||spawnerKey(victim)==null||CompanionAllies.friendly(victim))return;
        var o=origin(source);
        if(o==null&&source.getEntity()==null){
            var last=victim.getPersistentData().getCompoundOrEmpty(LAST);
            long elapsed=level.getGameTime()-last.getLongOr("tick",Long.MIN_VALUE);
            if(elapsed>=0&&elapsed<=200)o=decode(last.getStringOr("origin",""));
            if(o==null)o=origin(victim.getKillCredit());
        }
        if(o==null||admitted(level,o)==null)return;
        String key=spawnerKey(victim);int before=count(level,o.run,key),next=Math.min(40,before+1);
        setCount(level,o.run,key,next);
        if(before<30&&next==30)tell(level,o,WARNING);
        if(before<40&&next==40){
            tell(level,o,REFUSAL);
            D1RunData.get(level.getServer()).setValue(o.run,PREFIX+"notice:"+o.hirer,victim.getUUID()+"|"+level.getGameTime());
        }
    }
    public static void playerDied(ServerPlayer player){
        var level=player.level();var run=D1Members.run(level).orElse(null);
        if(run==null||!run.containsPlayer(player.getUUID())||run.isCompletionExited(player.getUUID())
                ||player.isSpectator()||AccessPolicy.isDeveloper(player))return;
        String marker=DEAD;
        if(player.getPersistentData().getBooleanOr(marker,false))return;
        player.getPersistentData().putBoolean(marker,true);
        var data=D1RunData.get(level.getServer());
        data.setValue(run.runId(),PREFIX+"epoch",UUID.randomUUID().toString());
        data.setValue(run.runId(),healingKey(player.getUUID()),null);
        player.getPersistentData().remove(MOTION);
    }
    private static DungeonRunRegistryData.RunRecord playerRun(ServerPlayer player){
        if(!player.isAlive()||player.isSpectator()||AccessPolicy.isDeveloper(player))return null;
        var run=D1Members.run(player.level()).orElse(null);
        return run!=null&&D1Members.inside(player,run)?run:null;
    }
    static String healingKey(UUID id){return PREFIX+"healing:"+id;}
    static String clockKey(UUID id){return PREFIX+"clock:"+id;}
    private static StationaryHealingState healing(ServerPlayer player,long run,int now){
        var data=D1RunData.get(player.level().getServer());var values=data.values(run,healingKey(player.getUUID()));
        if(values.isEmpty())return StationaryHealingState.begin(now,player.getX(),player.getZ());
        String raw=values.getFirst();
        var parsed=StationaryHealingState.parse(raw,now,player.getX(),player.getZ());
        if(!raw.equals(parsed.serialize())){
            data.setValue(run,PREFIX+"unrecognized_healing:"+player.getUUID(),raw);
            data.setValue(run,healingKey(player.getUUID()),parsed.serialize());
        }
        return parsed;
    }
    private static void writeHealing(ServerPlayer player,long run,StationaryHealingState before,StationaryHealingState next){
        var data=D1RunData.get(player.level().getServer());
        data.setValue(run,healingKey(player.getUUID()),next.hits()==0?null:next.serialize());
        if(!before.warned()&&next.warned())player.displayClientMessage(Component.literal(HEAL_WARNING),false);
        if(!before.blocked()&&next.blocked())player.displayClientMessage(Component.literal(HEAL_REFUSAL),false);
    }
    static void damaged(ServerPlayer player){
        var run=playerRun(player);if(run==null)return;
        var data=D1RunData.get(player.level().getServer());int now=data.count(run.runId(),clockKey(player.getUUID()));
        if(run.mercenaries().stream().anyMatch(c->c.classId().equals("theurgist"))){
            var previous=healing(player,run.runId(),now);
            var next=previous.damage(now,player.getX(),player.getZ());
            writeHealing(player,run.runId(),previous,next);
        }
    }
    static void observe(ServerPlayer player){
        var run=playerRun(player);if(run==null){player.getPersistentData().remove(MOTION);return;}
        player.getPersistentData().remove(DEAD);
        var data=D1RunData.get(player.level().getServer());String clock=clockKey(player.getUUID());
        var values=data.values(run.runId(),healingKey(player.getUUID()));
        if(!values.isEmpty()){
            int now=Math.min(Integer.MAX_VALUE-20,data.count(run.runId(),clock))+20;
            data.setCount(run.runId(),clock,now);
            var previous=healing(player,run.runId(),now);
            writeHealing(player,run.runId(),previous,previous.sample(now,player.getX(),player.getZ()));
        }
        if(!run.mercenaries().isEmpty())motion(player,run.runId());
    }
    private static void motion(ServerPlayer player,long run){
        var tag=player.getPersistentData().getCompoundOrEmpty(MOTION);long now=player.level().getGameTime();
        String dim=player.level().dimension().location().toString();var input=player.getLastClientInput();
        boolean walking=(input.forward()!=input.backward()||input.left()!=input.right())
                &&!player.isPassenger()&&!player.isFallFlying()&&!player.isInWater()
                &&player.onGround();
        boolean fresh=tag.getLongOr("run",0)!=run||!dim.equals(tag.getStringOr("dimension",""))
                ||now-tag.getLongOr("sample",-1000)>40;
        double dx=player.getX()-tag.getDoubleOr("x",player.getX()),dz=player.getZ()-tag.getDoubleOr("z",player.getZ());
        if(fresh||!walking||dx*dx+dz*dz>64){
            tag.putDouble("x",player.getX());tag.putDouble("z",player.getZ());
            if(fresh||dx*dx+dz*dz>64)tag.putLong("moved",-1000);
        }else if(dx*dx+dz*dz>=1){
            tag.putLong("moved",now);tag.putDouble("x",player.getX());tag.putDouble("z",player.getZ());
        }
        tag.putLong("run",run);tag.putString("dimension",dim);tag.putLong("sample",now);
        player.getPersistentData().put(MOTION,tag);
    }
    private static boolean participating(ServerPlayer player,long run){
        var tag=player.getPersistentData().getCompoundOrEmpty(MOTION);long now=player.level().getGameTime();
        return tag.getLongOr("run",0)==run&&tag.getStringOr("dimension","").equals(player.level().dimension().location().toString())
                &&now-tag.getLongOr("sample",-1000)<=40&&now-tag.getLongOr("moved",-1000)>=0
                &&now-tag.getLongOr("moved",-1000)<=200;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(event.getEntity() instanceof ServerPlayer player&&player.tickCount%20==0)observe(player);
    }
    public static boolean healingAllowed(MercenaryEntity merc,LivingEntity target){
        if(!(target instanceof ServerPlayer player))return true;
        var run=playerRun(player);if(run==null||run.runId()!=merc.runId())return false;
        var data=D1RunData.get(player.level().getServer());int now=data.count(run.runId(),clockKey(player.getUUID()));
        var before=healing(player,run.runId(),now);var next=before.sample(now,player.getX(),player.getZ());
        if(!before.equals(next))writeHealing(player,run.runId(),before,next);
        return next.healAllowed();
    }
    /** Resolve native poison/wither damage while its source-less hurt callback is executing. */
    static boolean effectDamage(Origin o,LivingEntity target,BooleanSupplier apply){
        if(o==null)return apply.getAsBoolean();
        if(CompanionAllies.friendly(target)||!allowed(o,target,true))return true;
        var previous=EFFECT_ORIGIN.get();EFFECT_ORIGIN.set(o);
        try{return apply.getAsBoolean();}
        finally{if(previous==null)EFFECT_ORIGIN.remove();else EFFECT_ORIGIN.set(previous);}
    }
    static boolean effectDamage(MercenaryPotionCredit.Dose dose,LivingEntity target,BooleanSupplier apply){
        var o=decode(dose.source().getStringOr(ORIGIN,""));
        if(o==null&&target.level() instanceof ServerLevel level)o=origin(dose.owner(level));
        return effectDamage(o,target,apply);
    }
    /** Store origin alongside each exact native active/hidden effect; native effect serialization is unchanged. */
    public static void saveEffects(LivingEntity target){
        if(!(target.level() instanceof ServerLevel))return;
        var root=target.getPersistentData().getCompoundOrEmpty(EFFECTS).copy();
        for(var holder:List.of(MobEffects.POISON,MobEffects.WITHER)){
            var node=target.getEffect(holder);var entries=new ArrayList<String>();
            boolean owned=false;
            for(int depth=0;node!=null&&depth<256;depth++){
                var dose=((MercenaryPotionCredit)(Object)node).cosmicdungeon$dose();
                Origin o=null;
                if(dose!=null){
                    o=decode(dose.source().getStringOr(ORIGIN,""));
                    if(o==null&&target.level() instanceof ServerLevel level)o=origin(dose.owner(level));
                }
                owned|=o!=null;
                entries.add(node.getAmplifier()+"|"+node.getDuration()+"|"+(o==null?"":o.encoded()));
                node=((MercenaryPotionCredit)(Object)node).cosmicdungeon$hidden();
            }
            String key=holder.equals(MobEffects.POISON)?"poison":"wither";
            if(owned)root.store(key,com.mojang.serialization.Codec.STRING.listOf(),entries);else root.remove(key);
        }
        if(!root.isEmpty()||target.getPersistentData().contains(EFFECTS))target.getPersistentData().put(EFFECTS,root);
    }
    /** Restore only matching native entries; effects remain paused while unloaded and retain hidden ownership. */
    public static void restoreEffects(LivingEntity target){
        if(!(target.level() instanceof ServerLevel))return;
        var root=target.getPersistentData().getCompoundOrEmpty(EFFECTS);
        for(var holder:List.of(MobEffects.POISON,MobEffects.WITHER)){
            String key=holder.equals(MobEffects.POISON)?"poison":"wither";
            var entries=root.read(key,com.mojang.serialization.Codec.STRING.listOf()).orElse(List.of());
            var node=target.getEffect(holder);
            for(int i=0;node!=null&&i<Math.min(entries.size(),256);i++){
                var parts=entries.get(i).split("\\|",3);
                if(parts.length==3)try{
                    var o=decode(parts[2]);
                    if(o!=null&&Integer.parseInt(parts[0])==node.getAmplifier()&&Integer.parseInt(parts[1])==node.getDuration()){
                        var source=new CompoundTag();source.putString(ORIGIN,o.encoded());
                        var credit=new CompoundTag();credit.putBoolean(MercenarySkill.NEGATIVE_POTIONS.id(),true);
                        source.put("cosmicdungeon_mercenary_skill_credit_v1",credit);
                        ((MercenaryPotionCredit)(Object)node).cosmicdungeon$dose(new MercenaryPotionCredit.Dose(o.mercenary,o.run,source));
                    }
                }catch(IllegalArgumentException invalid){/* Preserve unrelated or future native effects. */}
                node=((MercenaryPotionCredit)(Object)node).cosmicdungeon$hidden();
            }
        }
    }
}
