package net.goui.cosmicdungeon.playerclass.skill;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.effect.ModMobEffects;
import net.goui.cosmicdungeon.mercenary.*;
import net.goui.cosmicdungeon.network.ClassSkillPayloads.SlotView;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.AABB;

/** Shared server authority, recipients and cooldown for manual and automatic class buffs. */
public final class ClassBuffs {
    public static final String PREFIX="class_buff_ready_v1:";
    private record Context(ServerPlayer owner,DungeonRunRegistryData.RunRecord run,D1RunData data,String identity){}
    private static final Map<MercenaryEntity,Long> NEXT_CHECK=new WeakHashMap<>();
    private ClassBuffs(){}
    public static boolean hasted(LivingEntity actor){return actor.hasEffect(ModMobEffects.FLURRY_OF_FANGS);}
    private static Context context(LivingEntity actor){
        if(!(actor.level() instanceof ServerLevel level)||!actor.isAlive()||actor.isSpectator())return null;
        ServerPlayer owner;String identity;
        if(actor instanceof ServerPlayer player){owner=player;identity="player:"+player.getUUID();}
        else if(actor instanceof MercenaryEntity merc){
            owner=MercenaryBrain.hirer(merc);if(owner==null)return null;
            identity="mercenary:"+merc.contract().id();
        }else return null;
        var run=ClassSkills.run(owner);if(run==null)return null;
        if(actor instanceof MercenaryEntity merc&&merc.runId()!=run.runId())return null;
        var data=D1RunData.get(level.getServer());
        var recovery=PendingDungeonRecoveryData.get(level.getServer());var handoff=recovery.handoff(owner.getUUID());
        if(data.sealed(run.runId())||recovery.completed(owner.getUUID())>=run.runId()
                ||handoff!=null&&handoff.run()==run.runId()&&handoff.kind().equals("cleanup"))return null;
        return new Context(owner,run,data,identity);
    }
    public static Optional<ClassBuff> selected(LivingEntity actor){
        String cls=actor instanceof ServerPlayer p?ClassData.getClassId(p)
                :actor instanceof MercenaryEntity m&&m.contract()!=null?m.contract().classId():"";
        int smite=cls.equals("judicator")?ChargedClassSkills.level(actor,SignatureSkill.SMITE):1;
        return ClassBuff.forClass(cls,smite);
    }
    public static String cooldownKey(String identity,String cls){return PREFIX+identity+":"+cls;}
    private static String key(Context ctx,ClassBuff buff){return cooldownKey(ctx.identity,buff.classId());}
    private static long now(LivingEntity actor){return actor.level().getServer().overworld().getGameTime();}
    public static int remaining(LivingEntity actor){
        var ctx=context(actor);var buff=selected(actor);
        if(ctx==null||buff.isEmpty())return 0;
        return ClassBuffCooldown.remaining(ClassBuffCooldown.deadline(ctx.data.values(ctx.run.runId(),key(ctx,buff.get()))),now(actor));
    }
    public static SlotView slot(ServerPlayer player){
        var choice=selected(player);
        if(choice.isEmpty())return SlotView.unassigned(ClassSkillSlot.SKILL_1);
        var buff=choice.get();boolean active=context(player)!=null;int remaining=remaining(player);
        String detail=buff.description()+" Caster and party allies within 10 blocks. Cooldown: 5 minutes."
                +(active?"":" Requires an active dungeon.");
        return new SlotView(ClassSkillSlot.SKILL_1,buff.id(),buff.title(),detail,active&&remaining==0,remaining,0,0);
    }
    private static boolean recipient(Context ctx,LivingEntity caster,LivingEntity target){
        if(target.level()!=caster.level()||!target.isAlive()||target.isSpectator()
                ||target.distanceToSqr(caster)>ClassBuff.RADIUS*ClassBuff.RADIUS)return false;
        if(target instanceof ServerPlayer player){
            var run=ClassSkills.run(player);
            return run!=null&&run.runId()==ctx.run.runId()&&ClassSkills.ally(player,ctx.owner);
        }
        if(target instanceof MercenaryEntity merc)
            return merc.runId()==ctx.run.runId()&&!merc.dormant()&&merc.contract()!=null
                    &&!ctx.run.isCompletionExited(merc.contract().hirer())
                    &&MercenaryLifecycle.admitted(ctx.run,merc.contract(),merc.getUUID(),target.level().dimension().location().toString());
        if(!(target instanceof Wolf wolf)||!ClassSkills.ally(wolf,ctx.owner)
                ||ctx.run.isCompletionExited(wolf.getOwnerReference().getUUID()))return false;
        // Reject known foreign/retired companions while ordinary allied tamed wolves remain eligible.
        long playerRun=wolf.getPersistentData().getLongOr("cosmicdungeon.bogatyr_run",0);
        if(playerRun>0&&playerRun!=ctx.run.runId())return false;
        return !MercenaryWolves.managed(wolf)||MercenaryWolves.diagnosticRun(wolf)==ctx.run.runId();
    }
    private static List<LivingEntity> recipients(Context ctx,LivingEntity actor){
        var result=new ArrayList<LivingEntity>();
        // Spatial query, no world scan and no arbitrary party-wolf cap.
        for(var target:actor.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(actor.position(),actor.position()).inflate(ClassBuff.RADIUS),
                t->recipient(ctx,actor,t)))result.add(target);
        if(recipient(ctx,actor,actor)&&!result.contains(actor))result.addFirst(actor);
        return result;
    }
    public static Component strengthMessage(String caster,String skill){
        return Component.literal(caster+" shares renewed strength for battle. ")
                .append(Component.literal(skill).withStyle(ChatFormatting.BLUE,ChatFormatting.ITALIC));
    }
    public static boolean cast(LivingEntity actor){
        var ctx=context(actor);var selected=selected(actor);
        if(ctx==null||selected.isEmpty())return false;
        var buff=selected.get();long now=now(actor);String key=key(ctx,buff);
        var previous=ctx.data.values(ctx.run.runId(),key);
        if(!ClassBuffCooldown.ready(ClassBuffCooldown.deadline(previous),now))return false;
        var targets=recipients(ctx,actor).stream().filter(t->ClassBuffEffects.useful(t,buff)).toList();
        if(targets.isEmpty()){empty(actor,buff);return false;}
        // Reserve before invoking cancellable/reentrant native effects; refund only a known no-op.
        String reservation=Long.toString(now+ClassBuff.COOLDOWN);
        ctx.data.setValue(ctx.run.runId(),key,reservation);
        int changed=0;
        for(var target:targets){
            var current=context(actor);
            if(current==null||current.run.runId()!=ctx.run.runId()||selected(actor).orElse(null)!=buff)break;
            if(recipient(current,actor,target)&&ClassBuffEffects.apply(actor,target,buff))changed++;
        }
        if(changed==0){
            var current=context(actor);
            if(current!=null&&current.run.runId()==ctx.run.runId()&&current.identity.equals(ctx.identity)
                    &&ctx.data.values(ctx.run.runId(),key).equals(List.of(reservation)))
                ctx.data.setValue(ctx.run.runId(),key,previous.isEmpty()?null:previous.getFirst());
            empty(actor,buff);return false;
        }
        if(actor instanceof ServerPlayer player)player.displayClientMessage(Component.literal(buff.title()+" activated."),true);
        if(buff==ClassBuff.AEGIS||buff==ClassBuff.BULWARK){
            var message=strengthMessage(actor.getName().getString(),buff.title());
            for(var id:ctx.run.orderedPlayers().stream().limit(6).toList()){
                var player=actor.level().getServer().getPlayerList().getPlayer(id);
                if(player!=null&&!ctx.run.isCompletionExited(id)&&ctx.run.containsDimension(player.level().dimension()))
                    player.sendSystemMessage(message);
            }
        }
        return true;
    }
    private static void empty(LivingEntity actor,ClassBuff buff){
        if(actor instanceof ServerPlayer p)p.displayClientMessage(Component.literal("No nearby allies need "+buff.title()+"."),true);
    }
    /** Called by existing ten-tick AI; at most one useful-target decision per second per mercenary. */
    public static void tick(MercenaryEntity merc){
        if(selected(merc).isEmpty()||context(merc)==null)return;
        long now=now(merc),next=NEXT_CHECK.getOrDefault(merc,Long.MIN_VALUE);
        if(next>now&&next-now<=20)return;
        NEXT_CHECK.put(merc,now+20);
        if(remaining(merc)==0)cast(merc);
    }
    public static void stop(){NEXT_CHECK.clear();}
}
