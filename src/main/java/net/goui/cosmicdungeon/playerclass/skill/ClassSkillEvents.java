package net.goui.cosmicdungeon.playerclass.skill;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;

@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class ClassSkillEvents {
    private ClassSkillEvents() {}
    @SubscribeEvent public static void admitted(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) return;
        if (event.getEntity() instanceof ThrownTrident p && p.getOwner() instanceof ServerPlayer owner)
            ClassSkills.capture(p,owner,p.getPickupItemStackOrigin());
        else if (event.getEntity() instanceof AbstractThrownPotion p && p.getOwner() instanceof ServerPlayer owner)
            ClassSkills.capture(p,owner,p.getItem());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void damage(LivingIncomingDamageEvent event) {
        var attack=ClassSkills.attack(event.getSource());
        if (attack != null && ClassSkills.enemy(event.getEntity(),attack.player()))
            event.setAmount((float)Math.min(Float.MAX_VALUE,event.getAmount()*ClassSkills.damage(attack)));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void killed(LivingDeathEvent event) {
        var attack=ClassSkills.attack(event.getSource());
        if (attack==null || !ClassSkills.enemy(event.getEntity(),attack.player())
                || event.getEntity().getPersistentData().getBooleanOr("cosmicdungeon_skill_kill_recorded",false)) return;
        event.getEntity().getPersistentData().putBoolean("cosmicdungeon_skill_kill_recorded",true);
        ClassSkills.award(attack,ClassSkillConfig.KILL_XP.get(),attack.skill().equals("potions"));
    }
    @SubscribeEvent public static void draw(ArrowLooseEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var attack=ClassSkills.held(player,event.getBow());
        if (attack != null && attack.cls().equals("deadeye") && attack.skill().equals("bow"))
            event.setCharge((int)Math.min(Integer.MAX_VALUE,event.getCharge() /
                    (1-ClassSkills.bonus(attack,ClassSkillConfig.DRAW.get()))));
    }
}
