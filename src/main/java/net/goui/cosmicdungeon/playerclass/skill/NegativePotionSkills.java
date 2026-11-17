package net.goui.cosmicdungeon.playerclass.skill;

import net.goui.cosmicdungeon.playerclass.api.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** One successful harmful potion application per launch, independent of bow/weapon XP. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class NegativePotionSkills {
    public static final String KEY="cosmicdungeon_negative_potion_action_v1";
    private NegativePotionSkills(){}
    public interface Credit {
        Dose cosmicdungeon$playerDose();
        void cosmicdungeon$playerDose(Dose dose);
    }
    public record Dose(java.util.UUID owner,long run,net.minecraft.nbt.CompoundTag action){
        public boolean tick(net.minecraft.server.level.ServerLevel level,LivingEntity target,java.util.function.BooleanSupplier apply){
            float before=target.getHealth()+target.getAbsorptionAmount();
            boolean result=apply.getAsBoolean();
            var player=level.getServer().getPlayerList().getPlayer(owner);
            var current=player==null?null:ClassSkills.run(player);
            if(target.getHealth()+target.getAbsorptionAmount()<before&&current!=null&&current.runId()==run
                    &&ClassData.getClassId(player).equals("venefex")&&ClassSkills.enemy(target,player)
                    &&!action.getBooleanOr("credited",false)){
                action.putBoolean("credited",true);
                SignatureSkills.success(player,SignatureSkill.NEGATIVE_POTIONS);
            }
            return result;
        }
    }
    public static boolean delayed(net.minecraft.world.effect.MobEffectInstance effect){
        return effect.getEffect().equals(net.minecraft.world.effect.MobEffects.POISON)
                ||effect.getEffect().equals(net.minecraft.world.effect.MobEffects.WITHER);
    }
    public static void attach(Entity source,LivingEntity target,net.minecraft.world.effect.MobEffectInstance effect){
        if(!delayed(effect)||!useful(source,target))return;
        var player=(ServerPlayer)net.goui.cosmicdungeon.npc.tamsin.PartyEffectSource.caster(source);
        var action=source.getPersistentData().getCompoundOrEmpty(KEY);
        ((Credit)(Object)effect).cosmicdungeon$playerDose(new Dose(player.getUUID(),action.getLongOr("run",0),action));
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event){
        if(!event.getLevel().isClientSide()&&!event.loadedFromDisk()
                &&event.getEntity() instanceof AbstractThrownPotion potion)capture(potion);
    }
    public static void capture(AbstractThrownPotion potion){
        if(potion.getPersistentData().contains(KEY))return;
        var tag=new net.minecraft.nbt.CompoundTag();
        if(potion.getOwner() instanceof ServerPlayer player&&ClassData.getClassId(player).equals("venefex")
                &&ClassItemEquipmentGuard.canUse(player,potion.getItem())){
            var run=ClassSkills.run(player);
            if(run!=null){tag.putLong("run",run.runId());tag.putString("owner",player.getUUID().toString());}
        }
        // Explicit denial persists; a later class change cannot legitimize an old shot.
        potion.getPersistentData().put(KEY,tag);
    }
    public static void inherit(Entity cloud,Entity projectile){
        if(projectile.getPersistentData().contains(KEY))
            cloud.getPersistentData().put(KEY,projectile.getPersistentData().getCompoundOrEmpty(KEY).copy());
    }
    public static boolean useful(Entity source,LivingEntity target){
        Entity caster=net.goui.cosmicdungeon.npc.tamsin.PartyEffectSource.caster(source);
        if(!(caster instanceof ServerPlayer player)||source==null||source.level()!=target.level()
                ||!ClassSkills.enemy(target,player))return false;
        var tag=source.getPersistentData().getCompoundOrEmpty(KEY);var run=ClassSkills.run(player);
        return run!=null&&tag.getLongOr("run",0)==run.runId()&&tag.getStringOr("owner","").equals(player.getUUID().toString())
                &&ClassData.getClassId(player).equals("venefex")&&!tag.getBooleanOr("credited",false);
    }
    public static boolean success(Entity source,LivingEntity target){
        if(!useful(source,target))return false;
        var player=(ServerPlayer)net.goui.cosmicdungeon.npc.tamsin.PartyEffectSource.caster(source);
        var tag=source.getPersistentData().getCompoundOrEmpty(KEY);
        tag.putBoolean("credited",true); // Shared by every target/effect of this splash or lingering cloud.
        source.getPersistentData().put(KEY,tag);
        return SignatureSkills.success(player,SignatureSkill.NEGATIVE_POTIONS);
    }
}
