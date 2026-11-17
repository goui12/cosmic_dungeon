package net.goui.cosmicdungeon.playerclass.dragoon;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.particle.ModParticleTypes;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;


@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DragoonPassiveEvents {
    private static final int PARTICLES_PER_ARC = 10;
    private DragoonPassiveEvents() {}
    public static boolean chaining(){return net.goui.cosmicdungeon.playerclass.skill.SecondarySkillDamage.active();}
    @SubscribeEvent public static void onLivingDamagePost(LivingDamageEvent.Post event){
        if(chaining()||!(event.getEntity() instanceof Mob initial))return;
        var source=event.getSource();
        var attack=net.goui.cosmicdungeon.playerclass.skill.ClassSkills.attack(source);
        if(attack==null||!"dragoon".equals(attack.cls())||!"trident".equals(attack.skill())
                ||!net.goui.cosmicdungeon.playerclass.skill.ChargedClassSkills.active(attack.player(),
                    net.goui.cosmicdungeon.playerclass.skill.SignatureSkill.CHAIN_LIGHTNING)
                ||!net.goui.cosmicdungeon.playerclass.skill.ChargedClassSkills.enemy(attack.player(),initial))return;
        float dealt=event.getNewDamage()+event.getReduction(
                net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION);
        if(!(dealt>0)||!Float.isFinite(dealt))return;
        var player=attack.player();
        if(net.goui.cosmicdungeon.playerclass.skill.ChargedClassSkills.armed(player)){
            if(net.goui.cosmicdungeon.playerclass.skill.ChargedClassSkills.lightning(player,
                    ()->LightningCombat.cast(player,initial,source,dealt)))
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Chain Lightning!"),true);
        }else net.goui.cosmicdungeon.playerclass.skill.ChargedClassSkills.addCharge(player,
                net.goui.cosmicdungeon.playerclass.skill.SignatureSkill.CHAIN_LIGHTNING);
    }

    @SubscribeEvent
    public static void passiveRepair(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ClassKeys.CLASS_ID_DRAGOON.equals(ClassData.getClassId(player))
                || !player.isAlive() || player.isSpectator()
                || player.level().getServer().overworld().getGameTime() % (20L*Config.REPAIR_POLL_SECONDS.get()) != 0) return;
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET}) {
            var stack=player.getItemBySlot(slot);
            double cost=Config.PASSIVE_REPAIR_HEALTH_COST.get();
            if (player.getHealth() <= player.getMaxHealth()*Config.PASSIVE_REPAIR_HEALTH_GATE.get()
                    || player.getHealth() <= cost) break;
            if (!net.goui.cosmicdungeon.playerclass.dragoon.repair.DragoonRepairRules.isSupportedDamagedItem(stack)) continue;
            stack.setDamageValue(stack.getDamageValue()-1);
            player.setHealth((float)(player.getHealth()-cost));
            player.getInventory().setChanged();
        }
    }

    public static void spawnLightningArc(ServerLevel level, LivingEntity from, LivingEntity to) {
        Vec3 start = from.position().add(0.0D, from.getBbHeight() * 0.55D, 0.0D);
        Vec3 end = to.position().add(0.0D, to.getBbHeight() * 0.55D, 0.0D);
        Vec3 delta = end.subtract(start);
        for (int i = 1; i <= PARTICLES_PER_ARC; i++) {
            double t = i / (double) (PARTICLES_PER_ARC + 1);
            Vec3 pos = start.add(delta.scale(t));
            double arch = Math.sin(Math.PI * t) * 0.22D;
            double jitterX = (level.random.nextDouble() - 0.5D) * 0.08D;
            double jitterZ = (level.random.nextDouble() - 0.5D) * 0.08D;
            level.sendParticles(ModParticleTypes.DRAGOON_LIGHTNING.get(),
                    pos.x + jitterX, pos.y + arch, pos.z + jitterZ,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
